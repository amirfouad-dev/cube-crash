package com.neongrid.engine

object Scoring {
    const val ALL_CLEAR_BONUS = 300L
    const val MAX_MULTIPLIER = 3.0f
    /** Non-clearing placements allowed before the combo streak dies. */
    const val STREAK_GRACE = 3

    /** Base points for clearing [lines] simultaneous lines: 100 * n(n+1)/2. */
    fun lineBase(lines: Int): Long = 100L * lines * (lines + 1) / 2

    /** Combo multiplier for the given streak (streak >= 1). */
    fun multiplier(streak: Int): Float =
        (1.0f + 0.25f * (streak - 1)).coerceAtMost(MAX_MULTIPLIER)

    fun clearPoints(lines: Int, streak: Int): Long =
        Math.round(lineBase(lines) * multiplier(streak).toDouble())
}
