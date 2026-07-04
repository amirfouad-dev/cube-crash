package com.neongrid.engine

/**
 * xorshift64* PRNG. State is a single Long carried inside GameState, so games
 * are deterministic, replayable, and trivially serializable.
 */
class GameRng(seed: Long) {
    var state: Long = if (seed == 0L) -0x61c8864680b583ebL else seed
        private set

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x * 0x2545F4914F6CDD1DL
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Uniform in [0, 1). */
    fun nextFloat(): Float = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()
}
