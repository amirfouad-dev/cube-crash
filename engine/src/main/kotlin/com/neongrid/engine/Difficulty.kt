package com.neongrid.engine

/**
 * Per-difficulty piece-generation tuning. The defaults (see [Difficulty.gen])
 * for BEGINNER reproduce the original generator behavior byte-for-byte;
 * higher tiers strip help away.
 *
 * @param congestionSmallBoost weight multiplier for <=3-cell pieces once the
 *   board is congested (1f = no bailout).
 * @param congestionBigSuppress weight multiplier for >=6-cell pieces once the
 *   board is congested (1f = no suppression).
 * @param reliefValve if true, sustained congestion guarantees a line-finisher.
 * @param finisherBonus weight multiplier for a piece that completes a near-full line.
 * @param awkwardBoost weight multiplier for awkward shapes (S/Z, 5-cell corners); 1f = none.
 * @param requireSequenceSolvable if true, the dealt piece is guaranteed placeable
 *   *after* the piece ahead of it (the [active, next] pair is always jointly
 *   solvable). If false, the piece only has to fit the board at deal time, so a
 *   greedy placement can genuinely trap the player.
 */
data class GenParams(
    val congestionSmallBoost: Float,
    val congestionBigSuppress: Float,
    val reliefValve: Boolean,
    val finisherBonus: Float,
    val awkwardBoost: Float,
    val requireSequenceSolvable: Boolean,
)

/**
 * Play categories, easiest to hardest.
 *
 * - BEGINNER — untimed, full generator assistance.
 * - INTERMEDIATE — a gentle clock and softened assistance; a true middle step.
 * - ADVANCED — tight clock, no bailouts, trickier pieces, and the fairness
 *   guarantee relaxed so a careless placement can trap you.
 * - INSANE — everything ADVANCED has, an even tighter clock, and NO rotation:
 *   pieces play as dealt. The joint-solvability guarantee is kept on so a
 *   careful player is never handed an impossible pair — the brutality is the
 *   15s clock plus having to use every shape exactly as it comes.
 */
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    INSANE;

    /** INSANE plays pieces as dealt — no rotate mechanic. */
    val allowsRotation: Boolean get() = this != INSANE

    /** Seconds allowed to place the current piece, or null when untimed. */
    fun timerSecondsAt(score: Long): Int? = when (this) {
        BEGINNER -> null
        INTERMEDIATE -> (INTERMEDIATE_BASE_SECONDS - score / INTERMEDIATE_POINTS_PER_STEP)
            .coerceAtLeast(INTERMEDIATE_MIN_SECONDS.toLong())
            .toInt()
        ADVANCED -> (ADVANCED_BASE_SECONDS - score / ADVANCED_POINTS_PER_STEP)
            .coerceAtLeast(ADVANCED_MIN_SECONDS.toLong())
            .toInt()
        INSANE -> (INSANE_BASE_SECONDS - score / INSANE_POINTS_PER_STEP)
            .coerceAtLeast(INSANE_MIN_SECONDS.toLong())
            .toInt()
    }

    /** Generation tuning for this tier. */
    val gen: GenParams get() = when (this) {
        //                    smallBoost bigSuppress relief finisher awkward requireSeq
        BEGINNER -> GenParams(2.5f, 0.3f, true, 1.8f, 1.0f, true)
        INTERMEDIATE -> GenParams(1.5f, 0.6f, true, 1.5f, 1.2f, true)
        ADVANCED -> GenParams(1.0f, 1.0f, false, 1.2f, 1.6f, false)
        INSANE -> GenParams(1.0f, 1.0f, false, 1.2f, 1.6f, true)
    }

    companion object {
        // INTERMEDIATE: a gentle ramp that eases you in.
        const val INTERMEDIATE_BASE_SECONDS = 30
        const val INTERMEDIATE_POINTS_PER_STEP = 3_000L
        const val INTERMEDIATE_MIN_SECONDS = 10

        // ADVANCED: tight from the start, tightens fast.
        const val ADVANCED_BASE_SECONDS = 20
        const val ADVANCED_POINTS_PER_STEP = 1_500L
        const val ADVANCED_MIN_SECONDS = 4

        // INSANE: the shortest clock in the game.
        const val INSANE_BASE_SECONDS = 15
        const val INSANE_POINTS_PER_STEP = 1_500L
        const val INSANE_MIN_SECONDS = 3

        /** Retained alias: the shared minimum used by older references/tests. */
        const val MIN_TIMER_SECONDS = ADVANCED_MIN_SECONDS

        fun fromId(id: String?): Difficulty =
            entries.firstOrNull { it.name == id } ?: BEGINNER
    }
}
