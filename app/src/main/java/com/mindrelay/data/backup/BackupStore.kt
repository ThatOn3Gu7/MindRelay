package com.mindrelay.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.mindrelay.data.db.AppDatabase
import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.MemoryEntity
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionEntity
import com.mindrelay.data.db.SessionEntryEntity
import com.mindrelay.data.db.TaskEntity
import com.mindrelay.data.IdGen
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus
import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_APP = "MindRelay"
private const val BACKUP_VERSION = 1
private const val CREATED_AT_FALLBACK = 1L

/**
 * Offline JSON backup/restore. Everything stays on the device: export writes a
 * `.json` file wherever the user picks, import validates the whole file before
 * merging (additive, same-id upsert), and restore validates + previews counts
 * before atomically replacing data — after writing a local safety backup.
 *
 * Semantics:
 *  - Every file is parsed and validated completely before any DB mutation; a
 *    malformed file changes nothing.
 *  - `import` is an additive merge keyed by id: existing rows with the same id
 *    are replaced with the backup's row (same-id restoration), rows with new
 *    ids are added, and unrelated existing data is left untouched.
 *    Relationships always reference the imported ids, so remapping is never
 *    needed.
 *  - `restore` is a full same-id replacement: the current data is replaced by
 *    the backup's content, inside one transaction.
 *  - Settings (DataStore) are intentionally NOT part of the JSON backup.
 */
class BackupStore(private val db: AppDatabase) {

    // region Export ----------------------------------------------------------

    suspend fun export(context: Context, uri: Uri): Boolean = try {
        val root = JSONObject().apply {
            put("app", BACKUP_APP)
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
        }
        db.withTransaction {
            root.put("projects", db.projectDao().listAll().projectsToJson())
            root.put("sessions", db.sessionDao().listAll().sessionsToJson())
            root.put("entries", db.entryDao().listAll().entriesToJson())
            root.put("captures", db.captureDao().listAll().capturesToJson())
            root.put("memories", db.memoryDao().listAll().memoriesToJson())
            root.put("tasks", db.taskDao().listAll().tasksToJson())
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray())
            true
        } ?: false
    } catch (_: Exception) {
        false
    }

    // region Import / Restore -------------------------------------------------

    /**
     * Parses and validates the entire file, then merges it additively into the
     * database inside one transaction. Returns the imported entity counts (only
     * genuinely new/changed rows are reported where practical).
     */
    suspend fun import(context: Context, uri: Uri): ImportResult = try {
        val doc = parseAndValidateOrThrow(context, uri)
        db.withTransaction {
            db.projectDao().replace(doc.projects)
            db.sessionDao().replace(doc.sessions)
            db.entryDao().replace(doc.entries)
            db.captureDao().replace(doc.captures)
            db.memoryDao().replace(doc.memories)
            db.taskDao().replace(doc.tasks)
        }
        advanceIdGen(doc)
        ImportResult.Ok(doc.projects.size, doc.captures.size, doc.memories.size, doc.warnings())
    } catch (e: ImportResult.ParseError) {
        e
    } catch (e: Exception) {
        ImportResult.ParseError(e.message ?: "Invalid backup file")
    }

    /**
     * Parses, validates and counts the file, then — only if it is well-formed —
     * performs a transactional full replacement, guarded by a local safety
     * backup. No partial restore is possible.
     */
    suspend fun restore(context: Context, uri: Uri): RestoreResult = try {
        val doc = parseAndValidateOrThrow(context, uri)
        val summary = doc.snapshot()
        createSafetyBackup(context)
        db.withTransaction {
            // Delete in FK-safe order (children before parents).
            db.memoryDao().deleteAll()
            db.taskDao().deleteAll()
            db.captureDao().deleteAll()
            db.entryDao().deleteAll()
            db.sessionDao().deleteAll()
            db.projectDao().deleteAll()
            db.projectDao().replace(doc.projects)
            db.sessionDao().replace(doc.sessions)
            db.entryDao().replace(doc.entries)
            db.captureDao().replace(doc.captures)
            db.memoryDao().replace(doc.memories)
            db.taskDao().replace(doc.tasks)
        }
        advanceIdGen(doc)
        RestoreResult.Ok(summary)
    } catch (e: Exception) {
        RestoreResult.Failed(e.message ?: "Restore failed")
    }

    /** Parses and validates a file for the pre-restore preview. Returns null on any problem. */
    suspend fun inspect(context: Context, uri: Uri): Snapshot? = try {
        parseAndValidateOrThrow(context, uri).snapshot()
    } catch (_: Exception) {
        null
    }

    // region Parsing + validation --------------------------------------------

    internal data class BackupDocument(
        val projects: List<ProjectEntity>,
        val sessions: List<SessionEntity>,
        val entries: List<SessionEntryEntity>,
        val captures: List<CaptureEntity>,
        val memories: List<MemoryEntity>,
        val tasks: List<TaskEntity>,
    ) {
        fun snapshot() = Snapshot(
            projects = projects.size,
            sessions = sessions.size,
            captures = captures.size,
            memories = memories.size,
            tasks = tasks.size,
        )

        /** Human-readable hints about semantic oddities that are not fatal. */
        fun warnings(): String {
            val parts = mutableListOf<String>()
            val active = sessions.filter { it.status == SessionStatus.ACTIVE }
            if (active.groupBy { it.projectId }.any { it.value.size > 1 }) {
                parts.add("multiple active sessions on one project (the most recent is treated as active)")
            }
            return parts.joinToString("; ").ifBlank { "" }
        }

        /**
         * Repair the persisted display numbers so imports/restores never
         * violate the (projectId, displayNumber) unique index: every session
         * gets 1..N per project ordered by startedAt/id. Kept inside the parse
         * stage so validation below always sees numbers that will round-trip.
         */
        fun normalizeDisplayNumbers(): BackupDocument {
            val fixed = sessions
                .groupBy { it.projectId }
                .values
                .flatMap { group ->
                    val ordered = group.sortedWith(compareBy({ it.startedAt }, { it.id }))
                    ordered.mapIndexed { index, s -> s.copy(displayNumber = index + 1) }
                }
            return copy(sessions = fixed)
        }
    }

    private fun parseAndValidateOrThrow(context: Context, uri: Uri): BackupDocument {
        val text = read(context, uri) ?: throw ImportResult.ParseError("Could not read the file")
        return parseBackupText(text)
    }

    companion object {
        /**
         * Pure parse + validate (no Android, no database): used by import,
         * restore and inspect, and unit-tested directly on the JVM.
         */
        internal fun parseBackupText(text: String): BackupDocument {
            val doc = runCatching { JSONObject(text) }
                .getOrElse { throw ImportResult.ParseError("Not a valid JSON document") }

            // Header
            val app = doc.optString("app", "")
            if (app != BACKUP_APP) throw ImportResult.ParseError(
                "Not a MindRelay backup — the file does not identify itself as '$BACKUP_APP'. Refusing to import."
            )
            val version = doc.optInt("version", -1)
            if (version < 1) throw ImportResult.ParseError("Missing backup 'version'")
            if (version > BACKUP_VERSION) throw ImportResult.ParseError(
                "This backup was written by a newer MindRelay (version $version). Update the app and try again."
            )

            // Required collections
            val projects = doc.optJSONArray("projects") ?: throw ImportResult.ParseError("Missing 'projects'")
            doc.optJSONArray("sessions") ?: throw ImportResult.ParseError("Missing 'sessions'")
            doc.optJSONArray("entries") ?: throw ImportResult.ParseError("Missing 'entries'")
            val captures = doc.optJSONArray("captures") ?: throw ImportResult.ParseError("Missing 'captures'")
            doc.optJSONArray("memories") ?: throw ImportResult.ParseError("Missing 'memories'")
            doc.optJSONArray("tasks") ?: throw ImportResult.ParseError("Missing 'tasks'")

            // Parse every row strictly before touching the database.
            val parsed = BackupDocument(
                projects = projects.toProjects(),
                sessions = doc.optJSONArray("sessions").toSessions(),
                entries = doc.optJSONArray("entries").toEntries(),
                captures = captures.toCaptures(),
                memories = doc.optJSONArray("memories").toMemories(),
                tasks = doc.optJSONArray("tasks").toTasks(),
            ).normalizeDisplayNumbers()
            validateIntegrity(parsed)
            return parsed
        }

        internal fun advanceIdGen(d: BackupDocument) {
            IdGen.observe(d.projects.maxOfOrNull { it.id } ?: 0L)
            IdGen.observe(d.sessions.maxOfOrNull { it.id } ?: 0L)
            IdGen.observe(d.entries.maxOfOrNull { it.id } ?: 0L)
            IdGen.observe(d.captures.maxOfOrNull { it.id } ?: 0L)
            IdGen.observe(d.memories.maxOfOrNull { it.id } ?: 0L)
            IdGen.observe(d.tasks.maxOfOrNull { it.id } ?: 0L)
        }

        internal fun validateIntegrity(d: BackupDocument) {
        fun fail(msg: String): Nothing = throw ImportResult.ParseError(msg)

        // Positive + unique ids per collection. Kept as lists so duplicates are
        // detectable; referential checks below build their own sets.
        val idSets: List<Pair<String, List<Long>>> = listOf(
            "projects" to d.projects.map { it.id },
            "sessions" to d.sessions.map { it.id },
            "entries" to d.entries.map { it.id },
            "captures" to d.captures.map { it.id },
            "memories" to d.memories.map { it.id },
            "tasks" to d.tasks.map { it.id },
        )
        for ((name, ids) in idSets) {
            val dup = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            if (dup.isNotEmpty()) fail("Duplicate ids in '$name': ${dup.joinToString()}")
        }
        val allIds = mutableListOf<Long>()
        idSets.forEach { (_, ids) -> allIds += ids }
        if (allIds.any { it <= 0 }) fail("All ids must be positive integers")

        // Cross-collection id uniqueness (relationships must be unambiguous).
        val idOwnership = mutableMapOf<Long, String>()
        for ((name, ids) in idSets) {
            for (id in ids) {
                val prev = idOwnership.putIfAbsent(id, name)
                if (prev != null && prev != name) {
                    fail("Ids collide across collections: id $id appears in '$prev' and '$name'")
                }
            }
        }

        // Referential integrity / relationship consistency.
        val projectIds = d.projects.map { it.id }.toHashSet()
        val sessionIds = d.sessions.map { it.id }.toHashSet()
        val captureIds = d.captures.map { it.id }.toHashSet()
        val entryIds = d.entries.map { it.id }.toHashSet()
        val memoryIds = d.memories.map { it.id }.toHashSet()
        val taskIds = d.tasks.map { it.id }.toHashSet()

        // Id → owning project lookups used to reject provenance that contradicts
        // a row's project link (e.g. a memory assigned to project A but sourced
        // from a session/capture on project B).
        val sessionProject: Map<Long, Long> = d.sessions.associate { it.id to it.projectId }
        val captureProject: Map<Long, Long> =
            d.captures.filter { it.projectId != null }.associate { it.id to it.projectId!! }

        // Timestamps are sane (non-negative; future is tolerated for clockskew).
        fun checkTimestamps(table: String, values: List<Long>) {
            if (values.any { it < 0 }) fail("Negative timestamps found in '$table'")
        }
        checkTimestamps("projects", d.projects.map { it.createdAt } + d.projects.mapNotNull { it.lastWorkedAt })
        checkTimestamps("sessions", d.sessions.map { it.startedAt } + d.sessions.mapNotNull { it.endedAt })
        checkTimestamps("entries", d.entries.map { it.createdAt })
        checkTimestamps("captures", d.captures.map { it.createdAt })
        checkTimestamps("memories", d.memories.map { it.createdAt } + d.memories.mapNotNull { it.revisitAt })
        checkTimestamps("tasks", d.tasks.map { it.createdAt } + d.tasks.mapNotNull { it.dueAt })

        // Required strings.
        if (d.projects.any { it.name.isBlank() }) fail("A project has an empty name")
        if (d.sessions.any { it.title.isBlank() }) fail("A session has an empty title")
        if (d.captures.any { it.text.isBlank() }) fail("A capture has empty text")
        if (d.entries.any { it.text.isBlank() }) fail("A session entry has empty text")
        if (d.memories.any { it.title.isBlank() }) fail("A memory has an empty title")
        if (d.tasks.any { it.text.isBlank() }) fail("A task has empty text")

        // Enum sanity: every stored enum value must be a known name.
        val unknownProjectStatus = d.projects.map { it.status }.filterNot { ProjectStatus.entries.contains(it) }
        if (unknownProjectStatus.isNotEmpty()) fail("Invalid project status: ${unknownProjectStatus.joinToString()}")

        for (s in d.sessions) {
            if (s.projectId !in projectIds) fail("Session ${s.id} references missing project ${s.projectId}")
            if (s.endedAt != null && s.endedAt < s.startedAt) fail("Session ${s.id} ends before it starts")
            if (s.status == SessionStatus.COMPLETED && s.endedAt == null) fail("Completed session ${s.id} has no endedAt")
        }
        val activePerProject = d.sessions.filter { it.status == SessionStatus.ACTIVE }
            .groupBy { it.projectId }.filterValues { it.size > 1 }
        if (activePerProject.isNotEmpty()) fail("Multiple active sessions on project ${activePerProject.keys.joinToString()}")

        for (e in d.entries) {
            if (e.sessionId !in sessionIds) fail("Entry ${e.id} references missing session ${e.sessionId}")
        }
        for (c in d.captures) {
            if (c.projectId != null && c.projectId !in projectIds) fail("Capture ${c.id} references missing project ${c.projectId}")
            if (c.linkedSessionId != null && c.linkedSessionId !in sessionIds) fail("Capture ${c.id} references missing session ${c.linkedSessionId}")
            if (c.converted == ConvertType.PROJECT_NOTE && (c.convertedTargetId == null || c.convertedTargetId !in entryIds))
                fail("Capture ${c.id} is a project note but its target entry is missing")
            // Contradictory provenance: converting to a memory/task/note must record a target.
            when (c.converted) {
                ConvertType.NONE -> if (c.convertedTargetId != null) fail("Capture ${c.id} has a target id but is not converted")
                ConvertType.MEMORY -> if (c.convertedTargetId == null || c.convertedTargetId !in memoryIds) fail("Capture ${c.id} claims a Memory conversion with a missing target")
                ConvertType.TASK -> if (c.convertedTargetId == null || c.convertedTargetId !in taskIds) fail("Capture ${c.id} claims a Task conversion with a missing target")
                ConvertType.PROJECT_NOTE -> Unit // validated above
                ConvertType.ARCHIVED -> Unit // legacy value, tolerated
            }
        }
        for (m in d.memories) {
            if (m.projectId != null && m.projectId !in projectIds) fail("Memory ${m.id} references missing project ${m.projectId}")
            if (m.sourceSessionId != null && m.sourceSessionId !in sessionIds) fail("Memory ${m.id} references missing session ${m.sourceSessionId}")
            if (m.sourceCaptureId != null && m.sourceCaptureId !in captureIds) fail("Memory ${m.id} references missing capture ${m.sourceCaptureId}")
            // A memory's provenance must agree with its project link: it cannot
            // belong to one project while its source session/capture lives in
            // another (that would make the memory's "Related project" conflict
            // with its "Source session/capture"). Manually-created memories may
            // stay project-less without sources.
            if (m.projectId != null && m.sourceSessionId != null) {
                val sourceProject = sessionProject[m.sourceSessionId]
                if (sourceProject != null && sourceProject != m.projectId)
                    fail("Memory ${m.id} belongs to project ${m.projectId} but its source session ${m.sourceSessionId} belongs to project $sourceProject")
            }
            if (m.projectId != null && m.sourceCaptureId != null) {
                val sourceProject = captureProject[m.sourceCaptureId]
                if (sourceProject != null && sourceProject != m.projectId)
                    fail("Memory ${m.id} belongs to project ${m.projectId} but its source capture ${m.sourceCaptureId} belongs to project $sourceProject")
            }
            // Relationship consistency: when a row claims a capture/session source,
            // the referenced row must agree (both directions satisfied).
            if (m.sourceType == MemorySourceType.CAPTURE && m.sourceCaptureId == null) fail("Memory ${m.id} claims a capture source but has no sourceCaptureId")
            if (m.sourceType == MemorySourceType.SESSION && m.sourceSessionId == null) fail("Memory ${m.id} claims a session source but has no sourceSessionId")
            if (m.sourceType == MemorySourceType.NONE && (m.sourceSessionId != null || m.sourceCaptureId != null)) fail("Memory ${m.id} has a source reference but claims NONE")
        }
        for (t in d.tasks) {
            if (t.projectId != null && t.projectId !in projectIds) fail("Task ${t.id} references missing project ${t.projectId}")
            if (t.captureId != null && t.captureId !in captureIds) fail("Task ${t.id} references missing capture ${t.captureId}")
        }

        // Relationship consistency (backward direction): when a capture records
        // the id of the memory/task it was converted into, that target must
        // exist and point back at the capture.
        val captureToTarget = d.captures.filter { it.convertedTargetId != null }
            .associate { it.id to it.convertedTargetId!! }
        for (m in d.memories) {
            val origin = m.sourceCaptureId ?: continue
            val expected = captureToTarget[origin]
            if (expected != null && expected != m.id) {
                fail("Memory ${m.id} claims capture $origin, but capture $origin targets $expected")
            }
        }
        for (t in d.tasks) {
            val origin = t.captureId ?: continue
            val expected = captureToTarget[origin]
            if (expected != null && expected != t.id) {
                fail("Task ${t.id} claims capture $origin, but capture $origin targets $expected")
            }
        }
        }
    }

    private suspend fun createSafetyBackup(context: Context) {
        try {
            val dir = context.filesDir
            val bytes = JSONObject().apply {
                put("app", BACKUP_APP)
                put("version", BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("kind", "pre-restore-safety-backup")
                db.withTransaction {
                    put("projects", db.projectDao().listAll().projectsToJson())
                    put("sessions", db.sessionDao().listAll().sessionsToJson())
                    put("entries", db.entryDao().listAll().entriesToJson())
                    put("captures", db.captureDao().listAll().capturesToJson())
                    put("memories", db.memoryDao().listAll().memoriesToJson())
                    put("tasks", db.taskDao().listAll().tasksToJson())
                }
            }.toString(2).toByteArray()
            val file = java.io.File(dir, "mindrelay-pre-restore-backup.json")
            if (file.length() > 0 || file.exists()) file.delete() // overwrite last safety copy
            file.writeBytes(bytes)
        } catch (_: Exception) {
            // Safety backup is best-effort; the restore itself is still atomic.
        }
    }

    private fun read(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
}

sealed interface ImportResult {
    data class Ok(val projects: Int, val captures: Int, val memories: Int, val warnings: String = "") : ImportResult
    /** A validation failure; thrown (and catchable as an Exception) so parse
     *  aborts immediately, but always surfaced to the UI as this sealed type. */
    data class ParseError(val reason: String) : Exception(reason), ImportResult
}

sealed interface RestoreResult {
    data class Ok(val summary: Snapshot) : RestoreResult
    data class Failed(val reason: String) : RestoreResult
}

data class Snapshot(
    val projects: Int,
    val sessions: Int,
    val captures: Int,
    val memories: Int,
    val tasks: Int,
)

// ---------------------------------------------------------------------------
// org.json ↔ entity adapters (full fidelity incl. ids ⇒ relationships survive).
// ---------------------------------------------------------------------------

private fun List<ProjectEntity>.projectsToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun ProjectEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("status", status.name)
    put("currentState", currentState); put("nextAction", nextAction); put("goal", goal)
    put("lastWorkedAt", lastWorkedAt ?: JSONObject.NULL); put("createdAt", createdAt)
}
private fun JSONArray.toProjects(): List<ProjectEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            ProjectEntity(
                id = o.optLong("id", 0),
                name = o.optString("name"),
                status = o.requiredEnum("status", ProjectStatus.ACTIVE) { ProjectStatus.valueOf(it) },
                currentState = o.optString("currentState"),
                nextAction = o.optString("nextAction"),
                goal = o.optString("goal"),
                lastWorkedAt = o.optLongOrNull("lastWorkedAt"),
                createdAt = o.optLong("createdAt", CREATED_AT_FALLBACK),
            )
        )
    }
}

private fun List<SessionEntity>.sessionsToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun SessionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("projectId", projectId); put("title", title); put("status", status.name)
    put("startedAt", startedAt); put("endedAt", endedAt ?: JSONObject.NULL)
    put("displayNumber", displayNumber)
    put("completed", completed); put("discoveries", discoveries); put("unresolved", unresolved)
    put("currentNextAction", currentNextAction)
}
private fun JSONArray.toSessions(): List<SessionEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            SessionEntity(
                id = o.optLong("id", 0),
                projectId = o.optLong("projectId", 0),
                title = o.optString("title"),
                status = o.requiredEnum("status", SessionStatus.ACTIVE) { SessionStatus.valueOf(it) },
                startedAt = o.optLong("startedAt", CREATED_AT_FALLBACK),
                endedAt = o.optLongOrNull("endedAt"),
                displayNumber = o.optInt("displayNumber", 0),
                completed = o.optString("completed"),
                discoveries = o.optString("discoveries"),
                unresolved = o.optString("unresolved"),
                currentNextAction = o.optString("currentNextAction"),
            )
        )
    }
}

private fun List<SessionEntryEntity>.entriesToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun SessionEntryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("sessionId", sessionId); put("text", text); put("kind", kind.name); put("createdAt", createdAt)
}
private fun JSONArray.toEntries(): List<SessionEntryEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            SessionEntryEntity(
                id = o.optLong("id", 0),
                sessionId = o.optLong("sessionId", 0),
                text = o.optString("text"),
                kind = o.requiredEnum("kind", EntryKind.NOTE) { EntryKind.valueOf(it) },
                createdAt = o.optLong("createdAt", CREATED_AT_FALLBACK),
            )
        )
    }
}

private fun List<CaptureEntity>.capturesToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun CaptureEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("text", text); put("detail", detail); put("kind", kind.name); put("isVoice", isVoice)
    put("projectId", projectId ?: JSONObject.NULL); put("linkedSessionId", linkedSessionId ?: JSONObject.NULL)
    put("converted", converted.name); put("convertedTargetId", convertedTargetId ?: JSONObject.NULL)
    put("archived", archived); put("createdAt", createdAt)
}
private fun JSONArray.toCaptures(): List<CaptureEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            CaptureEntity(
                id = o.optLong("id", 0),
                text = o.optString("text"),
                detail = o.optString("detail"),
                kind = o.requiredEnum("kind", CaptureKind.NOTE) { CaptureKind.valueOf(it) },
                isVoice = o.optBoolean("isVoice", false),
                projectId = o.optLongOrNull("projectId"),
                linkedSessionId = o.optLongOrNull("linkedSessionId"),
                converted = o.requiredEnumOrNull("converted") { ConvertType.valueOf(it) } ?: ConvertType.NONE,
                convertedTargetId = o.optLongOrNull("convertedTargetId"),
                archived = o.optBoolean("archived", false),
                createdAt = o.optLong("createdAt", CREATED_AT_FALLBACK),
            )
        )
    }
}

private fun List<MemoryEntity>.memoriesToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun MemoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("title", title); put("content", content); put("type", type.name); put("tags", tags)
    put("projectId", projectId ?: JSONObject.NULL); put("sourceSessionId", sourceSessionId ?: JSONObject.NULL)
    put("sourceCaptureId", sourceCaptureId ?: JSONObject.NULL); put("sourceType", sourceType.name)
    put("revisitAt", revisitAt ?: JSONObject.NULL); put("archived", archived); put("createdAt", createdAt)
}
private fun JSONArray.toMemories(): List<MemoryEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            MemoryEntity(
                id = o.optLong("id", 0),
                title = o.optString("title"),
                content = o.optString("content"),
                type = o.requiredEnum("type", MemoryType.NOTE) { MemoryType.valueOf(it) },
                tags = o.optString("tags"),
                projectId = o.optLongOrNull("projectId"),
                sourceSessionId = o.optLongOrNull("sourceSessionId"),
                sourceCaptureId = o.optLongOrNull("sourceCaptureId"),
                sourceType = o.requiredEnum("sourceType", MemorySourceType.MANUAL) { MemorySourceType.valueOf(it) },
                revisitAt = o.optLongOrNull("revisitAt"),
                archived = o.optBoolean("archived", false),
                createdAt = o.optLong("createdAt", CREATED_AT_FALLBACK),
            )
        )
    }
}

private fun List<TaskEntity>.tasksToJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun TaskEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("text", text); put("tag", tag); put("projectId", projectId ?: JSONObject.NULL)
    put("captureId", captureId ?: JSONObject.NULL); put("dueAt", dueAt ?: JSONObject.NULL)
    put("done", done); put("createdAt", createdAt)
}
private fun JSONArray.toTasks(): List<TaskEntity> = buildList {
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            TaskEntity(
                id = o.optLong("id", 0),
                text = o.optString("text"),
                tag = o.optString("tag"),
                projectId = o.optLongOrNull("projectId"),
                captureId = o.optLongOrNull("captureId"),
                dueAt = o.optLongOrNull("dueAt"),
                done = o.optBoolean("done", false),
                createdAt = o.optLong("createdAt", CREATED_AT_FALLBACK),
            )
        )
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key)

/**
 * Strict enum reader: null is returned only for nullable fields; a present but
 * unknown value fails parsing instead of silently becoming a default.
 */
private fun <T> JSONObject.requiredEnum(
    key: String,
    default: T,
    parse: (String) -> T,
): T {
    if (!has(key) || isNull(key)) return default
    val raw = optString(key)
    return runCatching { parse(raw) }
        .getOrElse { throw ImportResult.ParseError("Invalid '$key' value '$raw'") }
}

private fun <T> JSONObject.requiredEnumOrNull(
    key: String,
    parse: (String) -> T,
): T? {
    if (!has(key) || isNull(key)) return null
    val raw = optString(key)
    return runCatching { parse(raw) }
        .getOrElse { throw ImportResult.ParseError("Invalid '$key' value '$raw'") }
}
