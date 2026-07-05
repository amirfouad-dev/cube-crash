package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Playability guardrails for the hard tiers. A careful lookahead bot (a proxy
 * for an engaged human: it never traps its own next piece, and otherwise keeps
 * the board empty and un-fragmented) plays many seeded games; we assert the
 * score distribution shows real headroom past 2,000 so the levels stay
 * *progressible* despite the difficulty.
 *
 * NOTE: this is a turn-logic proxy only — it ignores the real-time placement
 * clock. It proves the puzzle can't softlock you on the way to 2,000; it does
 * not prove a human can beat a 15s/20s timer. The two hard tiers get separate
 * evidence: INSANE keeps the joint-solvability guarantee (proved directly by
 * the fairness-invariant test below), while ADVANCED relaxes it and leans on
 * this reach-2,000 distribution instead.
 */
class DifficultyPlayabilityTest {

    private companion object {
        const val TARGET = 2_000L
        const val SCORE_CAP = 6_000L // stop early once clearly progressible (bounds runtime)
        const val MOVE_CAP = 800
    }

    // ---- a careful, non-superhuman bot ----

    /** Distinct orientation ids of [id], following the rotation chain. */
    private fun rotationChainIds(id: Int): List<Int> {
        val ids = ArrayList<Int>(4)
        var cur = id
        repeat(4) {
            if (cur !in ids) ids.add(cur)
            cur = PieceCatalog.rotatedId.getValue(cur)
        }
        return ids
    }

    /** Empty cells fully enclosed by filled cells or the wall — effectively dead. */
    private fun holeCount(board: Long): Int {
        var holes = 0
        for (i in 0 until Board.CELLS) {
            if ((board ushr i) and 1L == 1L) continue
            val r = i / Board.SIZE
            val c = i % Board.SIZE
            var enclosed = true
            if (r > 0 && (board ushr (i - Board.SIZE)) and 1L == 0L) enclosed = false
            if (enclosed && r < Board.SIZE - 1 && (board ushr (i + Board.SIZE)) and 1L == 0L) enclosed = false
            if (enclosed && c > 0 && (board ushr (i - 1)) and 1L == 0L) enclosed = false
            if (enclosed && c < Board.SIZE - 1 && (board ushr (i + 1)) and 1L == 0L) enclosed = false
            if (enclosed) holes++
        }
        return holes
    }

    private fun nextFits(board: Long, nextId: Int?, difficulty: Difficulty): Boolean = when {
        nextId == null -> true
        difficulty.allowsRotation -> GameEngine.canPlaceAnyRotation(board, nextId)
        else -> GameEngine.canPlace(board, PieceCatalog.get(nextId))
    }

    /** Higher is better. Survival first (keep next placeable), then keep the board clean. */
    private fun evaluate(board: Long, nextId: Int?, difficulty: Difficulty): Double {
        var h = 0.0
        if (!nextFits(board, nextId, difficulty)) h -= 1_000_000.0
        h -= java.lang.Long.bitCount(board) * 8.0
        h -= holeCount(board) * 25.0
        return h
    }

    /** Plays one game to game-over / cap; returns final score. Invokes [onDeal] on each fresh state. */
    private fun playSmart(
        seed: Long,
        difficulty: Difficulty,
        onDeal: ((GameState) -> Unit)? = null,
    ): Long {
        var state = GameEngine.newGame(seed, difficulty).state
        onDeal?.invoke(state)
        var moves = 0
        while (!state.isGameOver && moves < MOVE_CAP && state.score < SCORE_CAP) {
            val activeId = state.tray[GameState.ACTIVE_SLOT]!!.pieceId
            val nextId = state.tray[GameState.NEXT_SLOT]?.pieceId
            val orientations =
                if (difficulty.allowsRotation) rotationChainIds(activeId) else listOf(activeId)

            var bestOrient = -1
            var bestRow = -1
            var bestCol = -1
            var bestH = Double.NEGATIVE_INFINITY
            for (oid in orientations) {
                val piece = PieceCatalog.get(oid)
                for (idx in piece.placements.indices) {
                    val mask = piece.placements[idx]
                    if ((mask and state.board) != 0L) continue
                    val after = Board.applyClears(state.board or mask)
                    val h = evaluate(after, nextId, difficulty)
                    if (h > bestH) {
                        bestH = h
                        bestOrient = oid
                        val anchor = piece.placementAnchors[idx]
                        bestRow = anchor / Board.SIZE
                        bestCol = anchor % Board.SIZE
                    }
                }
            }
            if (bestOrient < 0) break // no legal placement; game-over check agrees

            // Rotate the active piece into the chosen orientation, then place.
            var guard = 0
            while (state.tray[GameState.ACTIVE_SLOT]!!.pieceId != bestOrient && guard < 4) {
                val r = GameEngine.reduce(state, GameAction.Rotate(GameState.ACTIVE_SLOT), difficulty)
                if (r.state === state) break
                state = r.state
                guard++
            }
            state = GameEngine.reduce(
                state,
                GameAction.Place(GameState.ACTIVE_SLOT, bestRow, bestCol),
                difficulty,
            ).state
            onDeal?.invoke(state)
            moves++
        }
        return state.score
    }

    // ---- distribution helpers ----

    private fun percentile(sorted: List<Long>, p: Double): Long =
        sorted[(p * (sorted.size - 1)).toInt().coerceIn(0, sorted.lastIndex)]

    private fun report(label: String, difficulty: Difficulty, games: Int): List<Long> {
        val scores = (0 until games)
            .map { playSmart(seed = 0x9E3779B97F4A7C15uL.toLong() * (it + 1) + 1, difficulty = difficulty) }
            .sorted()
        val reached = scores.count { it >= TARGET }
        println(
            "[$label] n=$games  reach2000=${100 * reached / games}%  " +
                "p10=${percentile(scores, 0.10)}  median=${percentile(scores, 0.50)}  " +
                "p90=${percentile(scores, 0.90)}  min=${scores.first()}  max=${scores.last()}",
        )
        return scores
    }

    @Test
    fun `calibration - easy tiers show the healthy baseline`() {
        report("BEGINNER", Difficulty.BEGINNER, 40)
        report("INTERMEDIATE", Difficulty.INTERMEDIATE, 40)
    }

    @Test
    fun `advanced and insane stay progressible past 2000`() {
        val advanced = report("ADVANCED", Difficulty.ADVANCED, 150)
        val insane = report("INSANE", Difficulty.INSANE, 150)

        // Non-flaky guardrails with margin: a careful player must comfortably
        // clear 2,000 on both tiers. (Bounds tuned from the printed medians;
        // the calibration baseline shows what "healthy" looks like.)
        assertTrue(
            advanced.count { it >= TARGET } >= advanced.size * 8 / 10,
            "ADVANCED: only ${advanced.count { it >= TARGET }}/${advanced.size} careful games reached 2000",
        )
        assertTrue(
            insane.count { it >= TARGET } >= insane.size * 8 / 10,
            "INSANE: only ${insane.count { it >= TARGET }}/${insane.size} careful games reached 2000",
        )
    }

    @Test
    fun `intermediate and insane never deal an impossible pair`() {
        // Guarantee-ON tiers: whenever the active piece fits as dealt, the
        // preview must be jointly placeable after it (no rotation assumed).
        // This is the direct "never softlocks a careful player" proof.
        for (difficulty in listOf(Difficulty.INTERMEDIATE, Difficulty.INSANE)) {
            repeat(120) { g ->
                playSmart(seed = 1_234_567L * (g + 1) + 7, difficulty = difficulty) { state ->
                    if (!state.isGameOver) {
                        val active = PieceCatalog.get(state.tray[GameState.ACTIVE_SLOT]!!.pieceId)
                        val next = PieceCatalog.get(state.tray[GameState.NEXT_SLOT]!!.pieceId)
                        if (GameEngine.canPlace(state.board, active)) {
                            assertTrue(
                                PieceGenerator.sequencePlaceable(state.board, active, next),
                                "$difficulty dealt an unfair pair: ${active.name} then ${next.name} " +
                                    "on board ${java.lang.Long.toHexString(state.board)}",
                            )
                        }
                    }
                }
            }
        }
    }
}
