package com.mindrelay.data

import org.junit.Assert.assertTrue
import org.junit.Test

class IdGenTest {

    @Test
    fun nextIsMonotonicAndPositive() {
        val a = IdGen.next()
        val b = IdGen.next()
        val c = IdGen.next()
        assertTrue(a > 0)
        assertTrue(b > a)
        assertTrue(c > b)
    }

    @Test
    fun observePushesCounterPastRestoredIds() {
        // A restored backup may contain ids far above the current counter.
        val cur = IdGen.next()
        val big = cur + 1_000_000L
        IdGen.observe(big)
        // IdGen.next() is pre-increment, so pushing the counter to `big` means
        // the *next* issued id is big + 1 (strictly greater than big).
        assertTrue(IdGen.next() > big)
    }

    @Test
    fun observeIsIdempotentForSmallerIds() {
        val before = IdGen.next()
        IdGen.observe(before - 10L) // must not move the counter backwards
        IdGen.observe(before)       // equal: no-op
        assertTrue(IdGen.next() > before)
    }
}
