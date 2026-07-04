package com.neongrid.engine

/**
 * Play categories. BEGINNER is untimed. Timed modes give each piece a
 * placement deadline; ADVANCED tightens it by 1s per 2,000 points scored
 * (30s, then 29s at 2,000, 28s at 4,000, ...) down to a floor.
 */
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    /** Seconds allowed to place the current piece, or null when untimed. */
    fun timerSecondsAt(score: Long): Int? = when (this) {
        BEGINNER -> null
        INTERMEDIATE -> TIMER_BASE_SECONDS
        ADVANCED -> (TIMER_BASE_SECONDS - score / POINTS_PER_TIMER_STEP)
            .coerceAtLeast(MIN_TIMER_SECONDS.toLong())
            .toInt()
    }

    companion object {
        const val TIMER_BASE_SECONDS = 30
        const val POINTS_PER_TIMER_STEP = 2_000L
        const val MIN_TIMER_SECONDS = 5

        fun fromId(id: String?): Difficulty =
            entries.firstOrNull { it.name == id } ?: BEGINNER
    }
}
