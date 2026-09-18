package com.mindrelay.data.backup

import com.mindrelay.data.db.CaptureEntity
import com.mindrelay.data.db.MemoryEntity
import com.mindrelay.data.db.ProjectEntity
import com.mindrelay.data.db.SessionEntity
import com.mindrelay.data.db.SessionEntryEntity
import com.mindrelay.data.db.TaskEntity
import com.mindrelay.data.model.CaptureKind
import com.mindrelay.data.model.ConvertType
import com.mindrelay.data.model.EntryKind
import com.mindrelay.data.model.MemorySourceType
import com.mindrelay.data.model.MemoryType
import com.mindrelay.data.model.ProjectStatus
import com.mindrelay.data.model.SessionStatus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Pure JVM tests for the backup parse/validation layer. Focused on the
 * invariants that matter for a personal-memory app: full validation before any
 * mutation, correct round-tripping, and hard-failing on malformed input.
 */
class BackupStoreTest {

    // region fixtures --------------------------------------------------------

    private fun minimalValidJson(): JSONObject = JSONObject()
        .put("app", "MindRelay")
        .put("version", 1)
        .put("exportedAt", 1_700_000_000_000L)
        .put("projects", JSONArray())
        .put("sessions", JSONArray())
        .put("entries", JSONArray())
        .put("captures", JSONArray())
        .put("memories", JSONArray())
        .put("tasks", JSONArray())

    private fun project(id: Long = 1, name: String = "Project") =
        ProjectEntity(id = id, name = name, status = ProjectStatus.ACTIVE, createdAt = 100L)

    private fun session(id: Long, projectId: Long, status: SessionStatus = SessionStatus.ACTIVE, endedAt: Long? = null) =
        SessionEntity(id = id, projectId = projectId, title = "Session", status = status, startedAt = 100L, endedAt = endedAt)

    private fun entry(id: Long, sessionId: Long) =
        SessionEntryEntity(id = id, sessionId = sessionId, text = "entry", kind = EntryKind.NOTE, createdAt = 100L)

    private fun capture(id: Long, converted: ConvertType = ConvertType.NONE, targetId: Long? = null) =
        CaptureEntity(id = id, text = "capture", kind = CaptureKind.NOTE, converted = converted, convertedTargetId = targetId, createdAt = 100L)

    private fun memory(id: Long, sourceCaptureId: Long? = null, sourceSessionId: Long? = null, sourceType: MemorySourceType = MemorySourceType.MANUAL) =
        MemoryEntity(id = id, title = "memory", type = MemoryType.NOTE, sourceCaptureId = sourceCaptureId, sourceSessionId = sourceSessionId, sourceType = sourceType, createdAt = 100L)

    private fun task(id: Long, captureId: Long? = null, projectId: Long? = null) =
        TaskEntity(id = id, text = "task", captureId = captureId, projectId = projectId, createdAt = 100L)

    private fun doc(
        projects: List<ProjectEntity> = emptyList(),
        sessions: List<SessionEntity> = emptyList(),
        entries: List<SessionEntryEntity> = emptyList(),
        captures: List<CaptureEntity> = emptyList(),
        memories: List<MemoryEntity> = emptyList(),
        tasks: List<TaskEntity> = emptyList(),
    ) = BackupStore.BackupDocument(projects, sessions, entries, captures, memories, tasks)

    private fun assertParseError(block: () -> Unit) {
        try {
            block()
            fail("Expected a ParseError, but no exception was thrown")
        } catch (e: ImportResult.ParseError) {
            // Expected.
        }
    }

    // region export/import round trip ----------------------------------------

    @Test
    fun roundTripPreservesIdsAndRelationships() {
        val p = project(10, "Pump")
        val s = session(20, 10, status = SessionStatus.COMPLETED, endedAt = 200L)
        val e = entry(30, 20)
        // A capture converted to a project note (target = entry 30).
        val cNote = capture(40, converted = ConvertType.PROJECT_NOTE, targetId = 30)
            .copy(projectId = 10, linkedSessionId = 20)
        // A capture converted to a memory (target = memory 50).
        val cMemory = capture(41, converted = ConvertType.MEMORY, targetId = 50)
        val m = memory(50, sourceCaptureId = 41, sourceType = MemorySourceType.CAPTURE)
            .copy(projectId = 10, sourceSessionId = 20)
        // A capture converted to a task (target = task 60).
        val cTask = capture(42, converted = ConvertType.TASK, targetId = 60)
        val t = task(60, captureId = 42, projectId = 10)

        // Parsed (strict) from a well-formed document; every relationship and
        // conversion provenance must survive the round trip.
        val parsed = BackupStore.parseBackupText(
            JSONObject()
                .put("app", "MindRelay")
                .put("version", 1)
                .put("projects", JSONArray().put(JSONObject()
                    .put("id", 10).put("name", "Pump").put("status", "ACTIVE")
                    .put("currentState", "").put("nextAction", "").put("goal", "")
                    .put("createdAt", 100L)))
                .put("sessions", JSONArray().put(JSONObject()
                    .put("id", 20).put("projectId", 10).put("title", "Session").put("status", "COMPLETED")
                    .put("startedAt", 100L).put("endedAt", 200L)))
                .put("entries", JSONArray().put(JSONObject()
                    .put("id", 30).put("sessionId", 20).put("text", "entry").put("kind", "NOTE")
                    .put("createdAt", 100L)))
                .put("captures", JSONArray()
                    .put(JSONObject().put("id", 40).put("text", "capture").put("kind", "NOTE")
                        .put("projectId", 10L).put("linkedSessionId", 20L)
                        .put("converted", "PROJECT_NOTE").put("convertedTargetId", 30L)
                        .put("archived", false).put("createdAt", 100L))
                    .put(JSONObject().put("id", 41).put("text", "capture").put("kind", "IDEA")
                        .put("converted", "MEMORY").put("convertedTargetId", 50L)
                        .put("archived", false).put("createdAt", 100L))
                    .put(JSONObject().put("id", 42).put("text", "capture").put("kind", "TODO")
                        .put("converted", "TASK").put("convertedTargetId", 60L)
                        .put("archived", false).put("createdAt", 100L)))
                .put("memories", JSONArray().put(JSONObject()
                    .put("id", 50).put("title", "memory").put("type", "NOTE")
                    .put("projectId", 10L).put("sourceSessionId", 20L).put("sourceCaptureId", 41L)
                    .put("sourceType", "CAPTURE").put("archived", false).put("createdAt", 100L)))
                .put("tasks", JSONArray().put(JSONObject()
                    .put("id", 60).put("text", "task").put("projectId", 10L).put("captureId", 42L)
                    .put("done", false).put("createdAt", 100L)))
                .toString()
        )

        assertTrue(parsed.captures.size == 3)
        assertTrue(parsed.captures.first { it.id == 40L }.converted == ConvertType.PROJECT_NOTE)
        assertTrue(parsed.captures.first { it.id == 40L }.convertedTargetId == 30L)
        assertTrue(parsed.captures.first { it.id == 41L }.converted == ConvertType.MEMORY)
        assertTrue(parsed.memories[0].sourceCaptureId == 41L)
        assertTrue(parsed.memories[0].sourceSessionId == 20L)
        assertTrue(parsed.tasks[0].captureId == 42L)

        // And the same document passes strict integrity validation.
        BackupStore.validateIntegrity(parsed)
    }

    // region malformed / missing ---------------------------------------------

    @Test
    fun rejectsMalformedJson() {
        assertParseError { BackupStore.parseBackupText("{ not json") }
    }

    @Test
    fun rejectsWrongAppIdentity() {
        val body = minimalValidJson().put("app", "NotMindRelay")
        assertParseError { BackupStore.parseBackupText(body.toString()) }
    }

    @Test
    fun rejectsMissingVersion() {
        val body = minimalValidJson().also { it.remove("version") }
        assertParseError { BackupStore.parseBackupText(body.toString()) }
    }

    @Test
    fun rejectsUnsupportedFutureVersion() {
        val body = minimalValidJson().put("version", 999)
        assertParseError { BackupStore.parseBackupText(body.toString()) }
    }

    @Test
    fun rejectsMissingCollection() {
        val body = minimalValidJson().also { it.remove("captures") }
        assertParseError { BackupStore.parseBackupText(body.toString()) }
    }

    @Test
    fun rejectsInvalidEnumValueInsteadOfDefaulting() {
        val body = minimalValidJson()
            .put("projects", JSONArray().put(JSONObject()
                .put("id", 1).put("name", "P").put("status", "NOT_A_STATUS").put("createdAt", 100L)))
        assertParseError { BackupStore.parseBackupText(body.toString()) }
    }

    // region integrity -------------------------------------------------------

    @Test
    fun rejectsDuplicateIdsInCollection() {
        assertParseError {
            BackupStore.validateIntegrity(
                doc(projects = listOf(project(5), project(5)))
            )
        }
    }

    @Test
    fun rejectsZeroOrNegativeIds() {
        assertParseError {
            BackupStore.validateIntegrity(doc(projects = listOf(project(0))))
        }
    }

    @Test
    fun rejectsBrokenReferences() {
        assertParseError {
            BackupStore.validateIntegrity(
                doc(sessions = listOf(session(20, projectId = 999))) // missing project
            )
        }
    }

    @Test
    fun rejectsMultipleActiveSessionsPerProject() {
        assertParseError {
            BackupStore.validateIntegrity(
                doc(
                    projects = listOf(project(1)),
                    sessions = listOf(session(11, 1), session(12, 1)),
                )
            )
        }
    }

    @Test
    fun rejectsContradictoryConversionTarget() {
        // Capture claims a MEMORY conversion but the id points at nothing.
        assertParseError {
            BackupStore.validateIntegrity(
                doc(captures = listOf(capture(40, ConvertType.MEMORY, targetId = 50)))
            )
        }
    }

    @Test
    fun acceptsLegacyArchivedConversionMarker() {
        // Old backups may carry converted = "ARCHIVED" without a target id; that
        // must still validate so they can be restored.
        BackupStore.validateIntegrity(
            doc(captures = listOf(capture(40, ConvertType.ARCHIVED, targetId = null)))
        )
    }

    @Test
    fun legacySessionsGetRenumberedPerProjectWithoutCollisions() {
        // A legacy backup carries no displayNumber (all zero). Parsing must
        // renumber 1..N per project so the (projectId, displayNumber) unique
        // index can never be violated — even for a single project with many
        // sessions, and across multiple projects.
        val json = minimalValidJson()
            .put("projects", JSONArray()
                .put(JSONObject().put("id", 1).put("name", "P1").put("status", "ACTIVE").put("createdAt", 100L))
                .put(JSONObject().put("id", 2).put("name", "P2").put("status", "ACTIVE").put("createdAt", 100L)))
            .put("sessions", JSONArray()
                .put(JSONObject().put("id", 10).put("projectId", 1).put("title", "S").put("status", "COMPLETED")
                    .put("startedAt", 100L).put("endedAt", 200L))
                .put(JSONObject().put("id", 11).put("projectId", 1).put("title", "S").put("status", "COMPLETED")
                    .put("startedAt", 300L).put("endedAt", 400L))
                .put(JSONObject().put("id", 12).put("projectId", 2).put("title", "S").put("status", "COMPLETED")
                    .put("startedAt", 100L).put("endedAt", 200L)))
        val parsed = BackupStore.parseBackupText(json.toString())
        val p1 = parsed.sessions.filter { it.projectId == 1L }.sortedBy { it.startedAt }
        val p2 = parsed.sessions.filter { it.projectId == 2L }
        assertTrue(p1.map { it.displayNumber } == listOf(1, 2))
        assertTrue(p2.single().displayNumber == 1)
        // No (projectId, displayNumber) collision anywhere.
        val pairs = parsed.sessions.map { it.projectId to it.displayNumber }
        assertTrue(pairs.size == pairs.distinct().size)
    }

    @Test
    fun validatesRelationshipBothDirections() {
        // Memory claims capture 40, but capture 40 targets memory 99 → inconsistent.
        assertParseError {
            BackupStore.validateIntegrity(
                doc(
                    captures = listOf(capture(40, ConvertType.MEMORY, targetId = 99)),
                    memories = listOf(memory(50, sourceCaptureId = 40, sourceType = MemorySourceType.CAPTURE)),
                )
            )
        }
    }

    @Test
    fun validDocumentValidates() {
        val p = project(1)
        val s = session(2, 1, status = SessionStatus.COMPLETED, endedAt = 200L)
        val e = entry(3, 2)
        val c = capture(4, ConvertType.PROJECT_NOTE, targetId = 3).copy(projectId = 1, linkedSessionId = 2)
        val m = memory(5, sourceSessionId = 2, sourceType = MemorySourceType.SESSION).copy(projectId = 1)
        val t = task(6, projectId = 1)
        BackupStore.validateIntegrity(doc(listOf(p), listOf(s), listOf(e), listOf(c), listOf(m), listOf(t)))
    }
}
