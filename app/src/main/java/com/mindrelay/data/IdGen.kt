package com.mindrelay.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide monotonic id generator. Entity primary keys are app-assigned so
 * that backups can round-trip the exact ids and every cross-entity relationship
 * (capture → project, memory → source session, …) survives export/import/restore
 * intact.
 */
object IdGen {
    private val counter = AtomicLong(
        (System.currentTimeMillis() % 1_000_000L) * 1000L
    )

    fun next(): Long = counter.incrementAndGet()

    /** Ensure imported ids advance the generator past the largest we've seen. */
    fun observe(id: Long) {
        while (true) {
            val cur = counter.get()
            if (id <= cur) return
            if (counter.compareAndSet(cur, id)) return
        }
    }
}
