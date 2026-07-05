package com.neongrid.engine

/**
 * Play categories. BEGINNER is untimed. Timed modes give each piece a
 * placement deadline; ADVANCED starts tighter (20s) and loses 1s per
 * 1,500 points scored down to a floor. ADVANCED also hardens the piece
 * generator: no congestion bailouts, no relief valve, more awkward
 * shapes, and deals are only guaranteed to fit the board *now* — a bad
 * placement of the active piece can genuinely trap you.
 */
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    /** Seconds allowed to place the current piece, or null when untimed. */
    fun timerSecondsAt(score: Long): Int? = when (this) {
        BEGINNER -> null
        INTERMEDIATE -> TIMER_BASE_SECONDS
        ADVANCED -> (ADVANCED_BASE_SECONDS - score / ADVANCED_POINTS_PER_STEP)
            .coerceAtLeast(MIN_TIMER_SECONDS.toLong())
            .toInt()
    }

    companion object {
        const val TIMER_BASE_SECONDS = 30
        const val ADVANCED_BASE_SECONDS = 20
        const val ADVANCED_POINTS_PER_STEP = 1_500L
        const val MIN_TIMER_SECONDS = 4

        fun fromId(id: String?): Difficulty =
            entries.firstOrNull { it.name == id } ?: BEGINNER
    }
}
