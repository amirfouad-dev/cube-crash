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
    fun `intermediate starts at 30s and eases down gently to a 10s floor`() {
        assertEquals(30, Difficulty.INTERMEDIATE.timerSecondsAt(0))
        assertEquals(30, Difficulty.INTERMEDIATE.timerSecondsAt(2_999))
        assertEquals(29, Difficulty.INTERMEDIATE.timerSecondsAt(3_000))
        assertEquals(20, Difficulty.INTERMEDIATE.timerSecondsAt(30_000))
        assertEquals(
            Difficulty.INTERMEDIATE_MIN_SECONDS,
            Difficulty.INTERMEDIATE.timerSecondsAt(1_000_000),
        )
    }

    @Test
    fun `insane starts at 15s and loses one second per 1500 points down to a 3s floor`() {
        assertEquals(15, Difficulty.INSANE.timerSecondsAt(0))
        assertEquals(15, Difficulty.INSANE.timerSecondsAt(1_499))
        assertEquals(14, Difficulty.INSANE.timerSecondsAt(1_500))
        assertEquals(
            Difficulty.INSANE_MIN_SECONDS,
            Difficulty.INSANE.timerSecondsAt(1_000_000),
        )
    }

    @Test
    fun `only insane disables rotation`() {
        assertEquals(true, Difficulty.BEGINNER.allowsRotation)
        assertEquals(true, Difficulty.INTERMEDIATE.allowsRotation)
        assertEquals(true, Difficulty.ADVANCED.allowsRotation)
        assertEquals(false, Difficulty.INSANE.allowsRotation)
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
        assertEquals(Difficulty.INSANE, Difficulty.fromId("INSANE"))
        assertEquals(Difficulty.BEGINNER, Difficulty.fromId("garbage"))
        assertEquals(Difficulty.BEGINNER, Difficulty.fromId(null))
    }
}
