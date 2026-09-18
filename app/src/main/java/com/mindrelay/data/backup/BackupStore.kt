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
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline JSON backup/restore. Everything stays on the device: export writes a
 * `.json` file wherever the user picks, import validates the schema before
 * merging, and restore previews before replacing data. No cloud, no upload.
 */
class BackupStore(private val db: AppDatabase) {

    suspend fun export(context: Context, uri: Uri): Boolean = try {
        val root = JSONObject().apply {
            put("app", "MindRelay")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
        }
        db.withTransaction {
            root.put("projects", db.projectDao().listAll().toJson())
            root.put("sessions", db.sessionDao().listAll().toJson())
            root.put("entries", db.entryDao().listAll().toJson())
            root.put("captures", db.captureDao().listAll().toJson())
            root.put("memories", db.memoryDao().listAll().toJson())
            root.put("tasks", db.taskDao().listAll().toJson())
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray())
            true
        } ?: false
    } catch (_: Exception) {
        false
    }

    suspend fun import(context: Context, uri: Uri): ImportResult = try {
        val text = read(context, uri) ?: return ImportResult.ParseError("Could not read the file")
        val doc = JSONObject(text)
        val err = validate(doc)
        if (err != null) return ImportResult.ParseError(err)

        // Pre-parse (validates all rows before touching the DB).
        val projects = doc.optJSONArray("projects").toProjects()
        val sessions = doc.optJSONArray("sessions").toSessions()
        val entries = doc.optJSONArray("entries").toEntries()
        val captures = doc.optJSONArray("captures").toCaptures()
        val memories = doc.optJSONArray("memories").toMemories()
        val tasks = doc.optJSONArray("tasks").toTasks()

        advanceIdGen(projects, sessions, entries, captures, memories, tasks)
        db.withTransaction {
            db.projectDao().replace(projects)
            db.sessionDao().replace(sessions)
            db.entryDao().replace(entries)
            db.captureDao().replace(captures)
            db.memoryDao().replace(memories)
            db.taskDao().replace(tasks)
        }
        ImportResult.Ok(projects.size, captures.size, memories.size)
    } catch (e: Exception) {
        ImportResult.ParseError(e.message ?: "Invalid backup file")
    }

    suspend fun restore(context: Context, uri: Uri): RestoreResult = try {
        val text = read(context, uri) ?: return RestoreResult.Failed("Could not read the file")
        val doc = JSONObject(text)
        val err = validate(doc)
        if (err != null) return RestoreResult.Failed(err)

        val projects = doc.optJSONArray("projects").toProjects()
        val sessions = doc.optJSONArray("sessions").toSessions()
        val entries = doc.optJSONArray("entries").toEntries()
        val captures = doc.optJSONArray("captures").toCaptures()
        val memories = doc.optJSONArray("memories").toMemories()
        val tasks = doc.optJSONArray("tasks").toTasks()

        advanceIdGen(projects, sessions, entries, captures, memories, tasks)
        db.withTransaction {
            db.captureDao().deleteAll()
            db.projectDao().deleteAll()
            db.sessionDao().deleteAll()
            db.entryDao().deleteAll()
            db.memoryDao().deleteAll()
            db.taskDao().deleteAll()
            db.projectDao().replace(projects)
            db.sessionDao().replace(sessions)
            db.entryDao().replace(entries)
            db.captureDao().replace(captures)
            db.memoryDao().replace(memories)
            db.taskDao().replace(tasks)
        }
        RestoreResult.Ok
    } catch (e: Exception) {
        RestoreResult.Failed(e.message ?: "Restore failed")
    }

    /** Counts what a given file contains without importing (for the preview). */
    suspend fun inspect(context: Context, uri: Uri): Snapshot? = try {
        val text = read(context, uri) ?: return null
        val doc = JSONObject(text)
        if (validate(doc) != null) return null
        Snapshot(
            projects = doc.optJSONArray("projects")?.length() ?: 0,
            sessions = doc.optJSONArray("sessions")?.length() ?: 0,
            captures = doc.optJSONArray("captures")?.length() ?: 0,
            memories = doc.optJSONArray("memories")?.length() ?: 0,
            tasks = doc.optJSONArray("tasks")?.length() ?: 0,
        )
    } catch (_: Exception) {
        null
    }

    private fun read(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    private fun validate(doc: JSONObject): String? {
        for (key in listOf("projects", "sessions", "entries", "captures", "memories", "tasks")) {
            if (!doc.has(key)) return "Missing '$key' — this does not look like a MindRelay backup"
        }
        return null
    }
}

sealed interface ImportResult {
    data class Ok(val projects: Int, val captures: Int, val memories: Int) : ImportResult
    data class ParseError(val reason: String) : ImportResult
}

sealed interface RestoreResult {
    data object Ok : RestoreResult
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

private fun List<ProjectEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun ProjectEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("status", status.name)
    put("currentState", currentState); put("nextAction", nextAction); put("goal", goal)
    put("lastWorkedAt", lastWorkedAt ?: JSONObject.NULL); put("createdAt", createdAt)
}
private fun JSONArray?.toProjects(): List<ProjectEntity> = buildList {
    this@toProjects ?: return@buildList
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            ProjectEntity(
                id = o.optLong("id", 0),
                name = o.optString("name"),
                status = runCatching { ProjectStatus.valueOf(o.optString("status", "ACTIVE")) }.getOrDefault(ProjectStatus.ACTIVE),
                currentState = o.optString("currentState"),
                nextAction = o.optString("nextAction"),
                goal = o.optString("goal"),
                lastWorkedAt = o.optLongOrNull("lastWorkedAt"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        )
    }
}

private fun List<SessionEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun SessionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("projectId", projectId); put("title", title); put("status", status)
    put("startedAt", startedAt); put("endedAt", endedAt ?: JSONObject.NULL)
    put("completed", completed); put("discoveries", discoveries); put("unresolved", unresolved)
    put("currentNextAction", currentNextAction)
}
private fun JSONArray?.toSessions(): List<SessionEntity> = buildList {
    this@toSessions ?: return@buildList
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            SessionEntity(
                id = o.optLong("id", 0),
                projectId = o.optLong("projectId", 0),
                title = o.optString("title"),
                status = o.optString("status", "Active"),
                startedAt = o.optLong("startedAt", System.currentTimeMillis()),
                endedAt = o.optLongOrNull("endedAt"),
                completed = o.optString("completed"),
                discoveries = o.optString("discoveries"),
                unresolved = o.optString("unresolved"),
                currentNextAction = o.optString("currentNextAction"),
            )
        )
    }
}

private fun List<SessionEntryEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun SessionEntryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("sessionId", sessionId); put("text", text); put("kind", kind.name); put("createdAt", createdAt)
}
private fun JSONArray?.toEntries(): List<SessionEntryEntity> = buildList {
    this@toEntries ?: return@buildList
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            SessionEntryEntity(
                id = o.optLong("id", 0),
                sessionId = o.optLong("sessionId", 0),
                text = o.optString("text"),
                kind = runCatching { EntryKind.valueOf(o.optString("kind", "NOTE")) }.getOrDefault(EntryKind.NOTE),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        )
    }
}

private fun List<CaptureEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun CaptureEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("text", text); put("detail", detail); put("kind", kind.name); put("isVoice", isVoice)
    put("projectId", projectId ?: JSONObject.NULL); put("linkedSessionId", linkedSessionId ?: JSONObject.NULL)
    put("converted", converted.name); put("convertedTargetId", convertedTargetId ?: JSONObject.NULL)
    put("archived", archived); put("createdAt", createdAt)
}
private fun JSONArray?.toCaptures(): List<CaptureEntity> = buildList {
    this@toCaptures ?: return@buildList
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            CaptureEntity(
                id = o.optLong("id", 0),
                text = o.optString("text"),
                detail = o.optString("detail"),
                kind = runCatching { CaptureKind.valueOf(o.optString("kind", "NOTE")) }.getOrDefault(CaptureKind.NOTE),
                isVoice = o.optBoolean("isVoice", false),
                projectId = o.optLongOrNull("projectId"),
                linkedSessionId = o.optLongOrNull("linkedSessionId"),
                converted = runCatching { ConvertType.valueOf(o.optString("converted", "NONE")) }.getOrDefault(ConvertType.NONE),
                convertedTargetId = o.optLongOrNull("convertedTargetId"),
                archived = o.optBoolean("archived", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        )
    }
}

private fun List<MemoryEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun MemoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("title", title); put("content", content); put("type", type.name); put("tags", tags)
    put("projectId", projectId ?: JSONObject.NULL); put("sourceSessionId", sourceSessionId ?: JSONObject.NULL)
    put("sourceCaptureId", sourceCaptureId ?: JSONObject.NULL); put("sourceType", sourceType.name)
    put("revisitAt", revisitAt ?: JSONObject.NULL); put("archived", archived); put("createdAt", createdAt)
}
private fun JSONArray?.toMemories(): List<MemoryEntity> = buildList {
    this@toMemories ?: return@buildList
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        add(
            MemoryEntity(
                id = o.optLong("id", 0),
                title = o.optString("title"),
                content = o.optString("content"),
                type = runCatching { MemoryType.valueOf(o.optString("type", "NOTE")) }.getOrDefault(MemoryType.NOTE),
                tags = o.optString("tags"),
                projectId = o.optLongOrNull("projectId"),
                sourceSessionId = o.optLongOrNull("sourceSessionId"),
                sourceCaptureId = o.optLongOrNull("sourceCaptureId"),
                sourceType = runCatching { MemorySourceType.valueOf(o.optString("sourceType", "MANUAL")) }.getOrDefault(MemorySourceType.MANUAL),
                revisitAt = o.optLongOrNull("revisitAt"),
                archived = o.optBoolean("archived", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        )
    }
}

private fun List<TaskEntity>.toJson(): JSONArray = JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }
private fun TaskEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("text", text); put("tag", tag); put("projectId", projectId ?: JSONObject.NULL)
    put("captureId", captureId ?: JSONObject.NULL); put("dueAt", dueAt ?: JSONObject.NULL)
    put("done", done); put("createdAt", createdAt)
}
private fun JSONArray?.toTasks(): List<TaskEntity> = buildList {
    this@toTasks ?: return@buildList
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
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        )
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key)

/** Advance the process-wide id generator past every imported/restored id, so
 * future entities can never collide with data brought in from a backup file. */
private fun advanceIdGen(
    projects: List<ProjectEntity>,
    sessions: List<SessionEntity>,
    entries: List<SessionEntryEntity>,
    captures: List<CaptureEntity>,
    memories: List<MemoryEntity>,
    tasks: List<TaskEntity>,
) {
    IdGen.observe(projects.maxOfOrNull { it.id } ?: 0L)
    IdGen.observe(sessions.maxOfOrNull { it.id } ?: 0L)
    IdGen.observe(entries.maxOfOrNull { it.id } ?: 0L)
    IdGen.observe(captures.maxOfOrNull { it.id } ?: 0L)
    IdGen.observe(memories.maxOfOrNull { it.id } ?: 0L)
    IdGen.observe(tasks.maxOfOrNull { it.id } ?: 0L)
}
