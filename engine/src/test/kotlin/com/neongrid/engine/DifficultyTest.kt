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
    fun `advanced starts at 20s and loses one second per 1500 points`() {
        assertEquals(20, Difficulty.ADVANCED.timerSecondsAt(0))
        assertEquals(20, Difficulty.ADVANCED.timerSecondsAt(1_499))
        assertEquals(19, Difficulty.ADVANCED.timerSecondsAt(1_500))
        assertEquals(19, Difficulty.ADVANCED.timerSecondsAt(2_999))
        assertEquals(18, Difficulty.ADVANCED.timerSecondsAt(3_000))
        assertEquals(10, Difficulty.ADVANCED.timerSecondsAt(15_000))
    }

    @Test
    fun `advanced timer never drops below the floor`() {
        assertEquals(
            Difficulty.MIN_TIMER_SECONDS,
            Difficulty.ADVANCED.timerSecondsAt(1_500L * 16),
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
