package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DifficultyTest {

    @Test
    fun `beginner is untimed at any score`() {
        assertNull(Difficulty.BEGINNER.timerSecondsAt(0))
        assertNull(Difficulty.BEGINNER.timerSecondsAt(1_000_000))
    }

    @Test
    fun `intermediate is a fixed 30 seconds`() {
        assertEquals(30, Difficulty.INTERMEDIATE.timerSecondsAt(0))
        assertEquals(30, Difficulty.INTERMEDIATE.timerSecondsAt(50_000))
    }

    @Test
    fun `advanced loses one second per 2000 points`() {
        assertEquals(30, Difficulty.ADVANCED.timerSecondsAt(0))
        assertEquals(30, Difficulty.ADVANCED.timerSecondsAt(1_999))
        assertEquals(29, Difficulty.ADVANCED.timerSecondsAt(2_000))
        assertEquals(29, Difficulty.ADVANCED.timerSecondsAt(3_999))
        assertEquals(28, Difficulty.ADVANCED.timerSecondsAt(4_000))
        assertEquals(20, Difficulty.ADVANCED.timerSecondsAt(20_000))
    }

    @Test
    fun `advanced timer never drops below the floor`() {
        assertEquals(
            Difficulty.MIN_TIMER_SECONDS,
            Difficulty.ADVANCED.timerSecondsAt(2_000L * 25),
        )
        assertEquals(
            Difficulty.MIN_TIMER_SECONDS,
            Difficulty.ADVANCED.timerSecondsAt(1_000_000),
        )
    }

    @Test
    fun `fromId parses names and defaults to beginner`() {
        assertEquals(Difficulty.ADVANCED, Difficulty.fromId("ADVANCED"))
        assertEquals(Difficulty.BEGINNER, Difficulty.fromId("garbage"))
        assertEquals(Difficulty.BEGINNER, Difficulty.fromId(null))
    }
}
