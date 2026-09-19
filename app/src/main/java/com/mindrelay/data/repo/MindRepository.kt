package com.mindrelay.data.repo

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.mindrelay.data.db.AppDatabase
import com.mindrelay.data.db.CaptureDao
import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.MemoryDao
import com.mindrelay.data.db.MemoryEntity
import com.mindrelay.data.db.ProjectDao
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionDao
import com.mindrelay.data.db.SessionEntity
import com.mindrelay.data.db.SessionEntryDao
import com.mindrelay.data.db.SessionEntryEntity
import com.mindrelay.data.db.TaskDao
import com.mindrelay.data.db.TaskEntity
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType
import com.mindrelay.data.IdGen
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus
import com.mindrelay.data.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Failure type surfaced to the UI for domain operations that can be rejected. */
sealed class RepoError(message: String) : Exception(message) {
    class Conflict(reason: String) : RepoError(reason)
    class NotFound(reason: String) : RepoError(reason)
}

/** Outcome of a conversion, so callers never navigate on a half-written state. */
sealed interface ConvertResult {
    data class Ok(val targetId: Long) : ConvertResult
    data class Rejected(val reason: String) : ConvertResult
}

/**
 * Single source of truth. Every write funnels through here and stays local.
 * Multi-entity writes (activity deaths, concurrent taps) are single-tap safe:
 * they run inside one Room transaction, so they either fully apply or roll
 * back, and operations that must not duplicate are rejected idempotently.
 */
class MindRepository(
    private val db: AppDatabase,
    val settings: SettingsStore,
) {
    val captures: CaptureDao = db.captureDao()
    val projects: ProjectDao = db.projectDao()
    val sessions: SessionDao = db.sessionDao()
    val entries: SessionEntryDao = db.sessionEntryDao()
    val memories: MemoryDao = db.memoryDao()
    val tasks: TaskDao = db.taskDao()

    // region Captures --------------------------------------------------------

    suspend fun saveCapture(
        text: String,
        detail: String = "",
        kind: CaptureKind,
        isVoice: Boolean = false,
        projectId: Long? = null,
    ): Long {
        val id = IdGen.next()
        captures.insert(
            CaptureEntity(
                id = id,
                text = text.trim(),
                detail = detail.trim(),
                kind = kind,
                isVoice = isVoice,
                projectId = projectId,
            )
        )
        return id
    }

    /** The project to use when "save to current project" is requested. */
    suspend fun currentProject(): ProjectEntity? =
        projects.all().first().firstOrNull { it.status == ProjectStatus.ACTIVE }

    suspend fun updateCapture(item: CaptureEntity) = captures.update(item)

    suspend fun archiveCapture(capture: CaptureEntity) {
        // Archiving only sets the archived flag; it never erases what the
        // capture was converted into (converted + convertedTargetId survive).
        captures.update(capture.copy(archived = true))
    }

    suspend fun deleteCapture(capture: CaptureEntity): Long = deleteCapture(capture.id)

    /**
     * Delete a capture and every record that references it: memories converted
     * from it and tasks created from it are removed with it, so no dangling or
     * orphaned provenance remains. Transactional.
     */
    suspend fun deleteCapture(id: Long): Long = db.withTransaction {
        // Clear the memory/task DB-level references first to respect FK order.
        memories.listAll().filter { it.sourceCaptureId == id }
            .forEach { memories.deleteById(it.id) }
        tasks.listAll().filter { it.captureId == id }
            .forEach { tasks.deleteById(it.id) }
        captures.deleteById(id)
        id
    }

    suspend fun captureById(id: Long): CaptureEntity? = captures.one(id)

    /**
     * Convert a capture into a Memory. Creates the memory and records the
     * capture's provenance in one transaction; a capture already converted to a
     * Memory is rejected (prevents duplicate promotions).
     *
     * @param projectId optional destination project; defaults to the capture's
     *   own project link so provenance (sourceCaptureId) never depends on UI
     *   state that a screen might have forgotten to pass.
     */
    suspend fun convertToMemory(
        captureId: Long,
        title: String? = null,
        content: String? = null,
        type: MemoryType = MemoryType.NOTE,
        tags: String = "",
        projectId: Long? = null,
    ): ConvertResult = db.withTransaction {
        val capture = captures.one(captureId)
            ?: return@withTransaction ConvertResult.Rejected("Capture no longer exists")
        if (capture.converted == ConvertType.MEMORY) {
            return@withTransaction ConvertResult.Rejected("This capture was already converted to a Memory")
        }
        val targetId = IdGen.next()
        memories.insert(
            MemoryEntity(
                id = targetId,
                title = (title ?: capture.text.substringBefore('.')).trim(),
                content = (content ?: run {
                    val d = capture.detail.trim()
                    if (d.isNotEmpty()) "${capture.text.trim()}\n\n$d" else capture.text.trim()
                }).trim(),
                type = type,
                tags = normalizeTags(tags),
                projectId = projectId ?: capture.projectId,
                sourceSessionId = capture.linkedSessionId,
                sourceCaptureId = capture.id,
                sourceType = MemorySourceType.CAPTURE,
            )
        )
        captures.update(capture.copy(converted = ConvertType.MEMORY, convertedTargetId = targetId))
        ConvertResult.Ok(targetId)
    }

    /**
     * Convert a capture into a Task. Creates the task and records the capture's
     * provenance in one transaction; a capture already converted to a Task is
     * rejected (prevents duplicate conversions).
     */
    suspend fun convertToTask(
        captureId: Long,
        text: String? = null,
        projectId: Long? = null,
    ): ConvertResult = db.withTransaction {
        val capture = captures.one(captureId)
            ?: return@withTransaction ConvertResult.Rejected("Capture no longer exists")
        if (capture.converted == ConvertType.TASK) {
            return@withTransaction ConvertResult.Rejected("This capture was already converted to a Task")
        }
        val targetId = IdGen.next()
        tasks.insert(
            TaskEntity(
                id = targetId,
                text = (text ?: capture.text).trim(),
                projectId = projectId ?: capture.projectId,
                captureId = capture.id,
            )
        )
        captures.update(capture.copy(converted = ConvertType.TASK, convertedTargetId = targetId))
        ConvertResult.Ok(targetId)
    }

    /**
     * Convert a capture into a Project Note. Resolves the destination project,
     * ensures an active session exists on it, appends a `NOTE` session entry,
     * links the capture to that session/entry, and stores the real target entry
     * id in provenance — all inside one transaction. Repeated conversion is
     * rejected (no duplicate note entries).
     */
    suspend fun convertToProjectNote(
        captureId: Long,
        projectId: Long? = null,
    ): ConvertResult = db.withTransaction {
        val capture = captures.one(captureId)
            ?: return@withTransaction ConvertResult.Rejected("Capture no longer exists")
        if (capture.converted == ConvertType.PROJECT_NOTE) {
            return@withTransaction ConvertResult.Rejected("This capture was already converted to a Project note")
        }
        val project = (projectId ?: capture.projectId)
            ?.let { projects.one(it) }
            ?: projects.all().first().firstOrNull { it.status == ProjectStatus.ACTIVE }
            ?: return@withTransaction ConvertResult.Rejected(
                "No project to attach this note to — create a project first"
            )
        val sid = ensureActiveSessionLocked(project.id)
        val entryId = IdGen.next()
        entries.insert(
            SessionEntryEntity(
                id = entryId,
                sessionId = sid,
                text = capture.text.trim(),
                kind = EntryKind.NOTE,
            )
        )
        captures.update(
            capture.copy(
                projectId = project.id,
                linkedSessionId = sid,
                converted = ConvertType.PROJECT_NOTE,
                convertedTargetId = entryId,
            )
        )
        ConvertResult.Ok(entryId)
    }

    // region Projects --------------------------------------------------------

    suspend fun createProject(
        name: String,
        currentState: String = "",
        nextAction: String = "",
        goal: String = "",
    ): Long {
        val id = IdGen.next()
        projects.insert(
            ProjectEntity(
                id = id,
                name = name.trim(),
                currentState = currentState.trim(),
                nextAction = nextAction.trim(),
                goal = goal.trim(),
            )
        )
        return id
    }

    suspend fun updateProject(item: ProjectEntity) = projects.update(item)

    /**
     * Explicit, additive status transition onto the existing model. Passing a
     * status just means "apply it" — PAUSED projects can be resumed, DONE
     * projects must be explicitly reactivated (ACTIVE) rather than silently
     * starting sessions.
     */
    suspend fun setProjectStatus(id: Long, status: ProjectStatus) {
        projects.one(id)?.let {
            // Preserve current values; this never re-initialises project fields.
            projects.update(it.copy(status = status))
        }
    }

    suspend fun projectById(id: Long): ProjectEntity? = projects.one(id)

    /** Fully deletes a project and everything owned by/navigable from it. Transactional. */
    suspend fun deleteProject(id: Long) = db.withTransaction {
        // Sessions cascade their entries via FK (and here defensively).
        sessions.byProjectOneShot(id).forEach { s ->
            entries.deleteBySession(s.id)
            sessions.deleteById(s.id)
        }
        tasks.clearByProject(id)
        memories.detachProject(id)
        captures.detachProject(id)
        projects.deleteById(id)
    }

    // region Sessions --------------------------------------------------------

    /** Transactional version of the active-session lookup. */
    private suspend fun activeSessionLocked(projectId: Long): SessionEntity? =
        sessions.openSessions().firstOrNull { it.projectId == projectId }

    /** Must run inside a database transaction. */
    private suspend fun ensureActiveSessionLocked(projectId: Long): Long {
        activeSessionLocked(projectId)?.let { return it.id }
        val number = nextSessionNumberOneShot(projectId)
        return sessions.insert(newActiveSession(projectId, number))
    }

    private fun newActiveSession(projectId: Long, number: Int): SessionEntity =
        SessionEntity(
            id = IdGen.next(),
            projectId = projectId,
            title = "Session $number",
            status = SessionStatus.ACTIVE,
            displayNumber = number,
        )

    /**
     * Open the active session for a project or create a new one. The
     * check-then-create runs inside one transaction so the "one active session
     * per project" invariant is mediated atomically: a concurrent second caller
     * observes the first caller's session inside its own transaction and returns
     * the same id instead of creating a duplicate.
     *
     * @return the session id, or null when the project does not exist.
     */
    suspend fun resumeOrNewSession(projectId: Long): Long? = db.withTransaction {
        activeSessionLocked(projectId)?.let { return@withTransaction it.id }
        if (projects.one(projectId) == null) return@withTransaction null
        val number = nextSessionNumberOneShot(projectId)
        return@withTransaction try {
            sessions.insert(newActiveSession(projectId, number))
        } catch (_: SQLiteConstraintException) {
            // Lost the race: another writer created the active session first.
            activeSessionLocked(projectId)?.id ?: throw RepoError.Conflict("Could not create a session")
        }
    }

    suspend fun ensureActiveSession(projectId: Long): Long? = db.withTransaction {
        activeSessionLocked(projectId)?.let { return@withTransaction it.id }
        if (projects.one(projectId) == null) return@withTransaction null
        val number = nextSessionNumberOneShot(projectId)
        return@withTransaction try {
            sessions.insert(newActiveSession(projectId, number))
        } catch (_: SQLiteConstraintException) {
            activeSessionLocked(projectId)?.id ?: throw RepoError.Conflict("Could not create a session")
        }
    }

    suspend fun addSessionEntry(sessionId: Long, text: String, kind: EntryKind): Long {
        val id = IdGen.next()
        entries.insert(SessionEntryEntity(id = id, sessionId = sessionId, text = text.trim(), kind = kind))
        return id
    }

    suspend fun updateSession(session: SessionEntity) = sessions.update(session)

    suspend fun sessionById(id: Long): SessionEntity? = sessions.one(id)

    /**
     * Complete a session handoff atomically. One transaction updates:
     * the session text/status/ended-at, the project's current state, next
     * action and last-worked timestamp, and optionally promotes discoveries to
     * a Memory. Repeated calls are ignored: a session whose DB row is already
     * COMPLETED is never completed twice and no second Memory is created.
     *
     * @return null on success, or a RepoError when the project vanished or the
     *   session was already completed by another actor.
     */
    suspend fun completeSession(
        completedText: String,
        discoveries: String,
        unresolved: String,
        nextAction: String,
        sessionId: Long,
        promoteDiscovery: Boolean,
    ): RepoError? = db.withTransaction {
        val current = sessions.one(sessionId)
            ?: return@withTransaction RepoError.NotFound("Session no longer exists")
        if (current.status == SessionStatus.COMPLETED) {
            return@withTransaction null // idempotent: never completes twice
        }
        val project = projects.one(current.projectId)
            ?: return@withTransaction RepoError.NotFound("Project no longer exists")

        val endedAt = System.currentTimeMillis()

        if (promoteDiscovery && discoveries.isNotBlank()) {
            // Only create a memory when none was promoted for this session yet.
            val already = memories.listAll().firstOrNull {
                it.sourceType == MemorySourceType.SESSION && it.sourceSessionId == sessionId
            }
            if (already == null) {
                memories.insert(
                    MemoryEntity(
                        id = IdGen.next(),
                        title = discoveries.trim().substringBefore('\n')
                            .substringBefore('.')
                            .take(72)
                            .ifBlank { "Session discovery" },
                        content = discoveries.trim(),
                        type = MemoryType.FIX,
                        projectId = project.id,
                        sourceSessionId = sessionId,
                        sourceType = MemorySourceType.SESSION,
                    )
                )
            }
        }

        val newState = listOf(
            completedText.trim().substringBefore('\n'),
            discoveries.trim().substringBefore('\n'),
        ).filter { it.isNotBlank() }.joinToString(" ")

        sessions.update(
            current.copy(
                status = SessionStatus.COMPLETED,
                endedAt = endedAt,
                completed = completedText.trim(),
                discoveries = discoveries.trim(),
                unresolved = unresolved.trim(),
                currentNextAction = nextAction.trim(),
            )
        )
        projects.update(
            project.copy(
                currentState = newState.ifBlank { project.currentState },
                nextAction = nextAction.trim().ifBlank { project.nextAction },
                lastWorkedAt = endedAt,
            )
        )
        null
    }

    /** Deletes a session and its entries, clearing references. */
    suspend fun deleteSession(id: Long) = db.withTransaction {
        entries.deleteBySession(id)
        sessions.deleteById(id)
        Unit
    }

    /**
     * Display numbering is the persisted max + 1, so a new session's "Session
     * N" label never collides with a surviving session after deletes (deleting
     * #2 from 1,2,3 yields #4 next, not a duplicate #2). Read inside the same
     * transaction as the insert (single-writer room transaction) so two
     * concurrent creators number sequentially.
     */
    private suspend fun nextSessionNumberOneShot(projectId: Long): Int =
        runCatching { sessions.maxDisplayNumber(projectId) + 1 }.getOrDefault(1)

    // region Memories --------------------------------------------------------

    suspend fun saveMemory(
        title: String,
        content: String,
        type: MemoryType,
        tags: String,
        projectId: Long?,
        revisitAt: Long?,
        sourceType: MemorySourceType = MemorySourceType.MANUAL,
        sourceSessionId: Long? = null,
        sourceCaptureId: Long? = null,
    ): Long {
        val id = IdGen.next()
        memories.insert(
            MemoryEntity(
                id = id,
                title = title.trim(),
                content = content.trim(),
                type = type,
                tags = normalizeTags(tags),
                projectId = projectId,
                sourceSessionId = sourceSessionId,
                sourceCaptureId = sourceCaptureId,
                sourceType = sourceType,
                revisitAt = revisitAt,
            )
        )
        return id
    }

    suspend fun updateMemory(item: MemoryEntity) = memories.update(item)

    suspend fun setMemoryRevisit(id: Long, at: Long?) {
        memories.one(id)?.let { memories.update(it.copy(revisitAt = at)) }
    }

    suspend fun archiveMemory(id: Long) {
        memories.one(id)?.let { memories.update(it.copy(archived = true)) }
    }

    suspend fun deleteMemory(id: Long) = memories.deleteById(id)

    suspend fun memoryById(id: Long): MemoryEntity? = memories.one(id)

    private fun normalizeTags(raw: String): String =
        raw.split(Regex("[\\s,]+"))
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .joinToString(" ") { "#$it" }

    // region Tasks -----------------------------------------------------------

    suspend fun addTask(text: String, tag: String = "", projectId: Long? = null, captureId: Long? = null): Long {
        val id = IdGen.next()
        tasks.insert(TaskEntity(id = id, text = text.trim(), tag = tag, projectId = projectId, captureId = captureId))
        return id
    }

    suspend fun setTaskDone(id: Long, done: Boolean) = tasks.setDone(id, done)

    suspend fun deleteTask(id: Long) = tasks.deleteById(id)
}
