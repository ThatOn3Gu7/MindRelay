package com.mindrelay.data.repo

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
import com.mindrelay.data.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Single source of truth. Every write funnels through here and stays local. */
class MindRepository(
    db: AppDatabase,
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

    suspend fun updateCapture(item: CaptureEntity) = captures.update(item)

    /** Convert a capture while preserving provenance (`converted` + target id). */
    suspend fun convertCapture(capture: CaptureEntity, to: ConvertType, targetId: Long? = null) {
        captures.update(capture.copy(converted = to, convertedTargetId = targetId))
    }

    suspend fun archiveCapture(item: CaptureEntity) {
        captures.update(item.copy(archived = true, converted = ConvertType.ARCHIVED))
    }

    suspend fun deleteCapture(item: CaptureEntity) = captures.delete(item)

    suspend fun captureById(id: Long): CaptureEntity? = captures.one(id)

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

    suspend fun setProjectStatus(id: Long, status: ProjectStatus) {
        projects.one(id)?.let { projects.update(it.copy(status = status)) }
    }

    suspend fun projectById(id: Long): ProjectEntity? = projects.one(id)

    suspend fun deleteProject(id: Long) {
        val projectSessions = sessions.byProject(id).first()
        for (s in projectSessions) {
            entries.deleteBySession(s.id)
            sessions.deleteById(s.id)
        }
        tasks.clearByProject(id)
        memories.detachProject(id)
        captures.detachProject(id)
        projects.deleteById(id)
    }

    // region Sessions --------------------------------------------------------

    /** Open the active session for a project or create a new one from its next action. */
    suspend fun resumeOrNewSession(projectId: Long): Long {
        sessions.activeForProject(projectId)?.let { return it.id }
        val project = projects.one(projectId)
        val suffix = when {
            project?.nextAction.isNullOrBlank() -> "Working session"
            else -> project!!.nextAction.substringBefore('.').take(36)
        }
        val number = nextSessionNumber(projectId)
        return sessions.insert(
            SessionEntity(id = IdGen.next(), projectId = projectId, title = "Session $number · ${suffix.trim()}", status = "Active")
        )
    }

    suspend fun ensureActiveSession(projectId: Long): Long {
        sessions.activeForProject(projectId)?.let { return it.id }
        val number = nextSessionNumber(projectId)
        return sessions.insert(
            SessionEntity(id = IdGen.next(), projectId = projectId, title = "Session $number", status = "Active")
        )
    }

    suspend fun addSessionEntry(sessionId: Long, text: String, kind: EntryKind): Long {
        val id = IdGen.next()
        entries.insert(SessionEntryEntity(id = id, sessionId = sessionId, text = text.trim(), kind = kind))
        return id
    }

    suspend fun updateSession(session: SessionEntity) = sessions.update(session)

    suspend fun sessionById(id: Long): SessionEntity? = sessions.one(id)

    /**
     * End a session atomically: mark it completed and update the project's
     * currentState, nextAction and lastWorkedAt.
     */
    suspend fun endSession(session: SessionEntity) {
        val project = projects.one(session.projectId) ?: return
        val newState = listOf(
            session.completed.trim().substringBefore('\n'),
            session.discoveries.trim().substringBefore('\n'),
        ).filter { it.isNotBlank() }.joinToString(" ")
        sessions.update(
            session.copy(status = "Completed", endedAt = System.currentTimeMillis())
        )
        projects.update(
            project.copy(
                currentState = newState.ifBlank { project.currentState },
                nextAction = session.currentNextAction.trim().ifBlank { project.nextAction },
                lastWorkedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun promoteDiscoveryToMemory(session: SessionEntity): Long? {
        if (session.discoveries.isBlank()) return null
        val project = projects.one(session.projectId)
        val title = session.discoveries.trim().substringBefore('\n').substringBefore('.').take(72)
        return memories.insert(
            MemoryEntity(
                id = IdGen.next(),
                title = title,
                content = session.discoveries.trim(),
                type = MemoryType.FIX,
                projectId = project?.id,
                sourceSessionId = session.id,
                sourceType = MemorySourceType.SESSION,
            )
        )
    }

    private suspend fun nextSessionNumber(projectId: Long): Int =
        try {
            sessions.sessionCount(projectId).first() + 1
        } catch (_: Exception) {
            1
        }

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
