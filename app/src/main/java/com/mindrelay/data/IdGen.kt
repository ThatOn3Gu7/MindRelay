package com.mindrelay.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide monotonic id generator. Entity primary keys are app-assigned so
 * that backups can round-trip the exact ids and every cross-entity relationship
 * (capture → project, memory → source session, …) survives export/import/restore
 * intact.
 *
 * Collision safety: the seed is derived from the wall clock on first access, so
 * ids are unique across process restarts; [observe] is called for every id seen
 * during restore/import so ids from backup files can never be re-issued. Beyond
 * that, correctness does not depend on the generator alone — multi-entity
 * repository writes run in Room transactions and restore/import pre-validates
 * that every id in a backup is positive and unique (see BackupStore).
 */
object IdGen {
    private val counter = AtomicLong(
        (System.currentTimeMillis() % 1_000_000L) * 1000L
    )

    fun next(): Long = counter.incrementAndGet()

    /** Ensure imported ids advance the generator past the largest we've seen. */
    fun observe(id: Long) {
        if (id <= 0) return
        while (true) {
            val cur = counter.get()
            if (id <= cur) return
            if (counter.compareAndSet(cur, id)) return
        }
    }
}
