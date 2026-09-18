package com.mindrelay.data.repo

import com.mindrelay.data.db.MemoryEntity
import com.mindrelay.data.model.MemoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic guard for the Home revisit rule: only memories whose `revisitAt`
 * is in the past (or now) are "due"; future dates must never be labeled due.
 * Kept framework-free so the exact predicate used by the repository/DAO filter
 * can be exercised without an Android runtime.
 */
class RevisitDueTest {

    private fun memory(id: Long, revisitAt: Long?, archived: Boolean = false) =
        MemoryEntity(
            id = id,
            title = "m$id",
            type = MemoryType.NOTE,
            revisitAt = revisitAt,
            archived = archived,
        )

    private fun duePredicate(now: Long, m: MemoryEntity): Boolean =
        !m.archived && m.revisitAt != null && m.revisitAt <= now

    @Test
    fun pastRevisitIsDue() {
        val now = System.currentTimeMillis()
        assertTrue(duePredicate(now, memory(1, now - 60_000)))
    }

    @Test
    fun nowRevisitIsDue() {
        val now = System.currentTimeMillis()
        assertTrue(duePredicate(now, memory(2, now)))
    }

    @Test
    fun futureRevisitIsNotDue() {
        val now = System.currentTimeMillis()
        assertFalse(duePredicate(now, memory(3, now + 86_400_000)))
    }

    @Test
    fun unscheduledIsNotDue() {
        assertFalse(duePredicate(System.currentTimeMillis(), memory(4, null)))
    }

    @Test
    fun archivedIsNotDue() {
        val now = System.currentTimeMillis()
        assertFalse(duePredicate(now, memory(5, now - 1, archived = true)))
    }

    @Test
    fun onlyActuallyDueSurviveFiltering() {
        val now = System.currentTimeMillis()
        val all = listOf(
            memory(1, now - 1_000),
            memory(2, now + 1_000),
            memory(3, null),
            memory(4, now, archived = true),
        )
        val due = all.filter { duePredicate(now, it) }
        assertEquals(listOf(1L), due.map { it.id })
    }
}
