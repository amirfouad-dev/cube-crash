package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PieceGeneratorTest {

    @Test
    fun `score bands`() {
        assertEquals(0, PieceGenerator.band(0))
        assertEquals(0, PieceGenerator.band(1_499))
        assertEquals(1, PieceGenerator.band(1_500))
        assertEquals(2, PieceGenerator.band(6_000))
        assertEquals(3, PieceGenerator.band(20_000))
        assertEquals(3, PieceGenerator.band(1_000_000))
    }

    @Test
    fun `sequencePlaceable accepts obviously solvable and rejects impossible`() {
        val bar1x1 = PieceCatalog.ALL.first { it.name == "1x1" }
        val sq3 = PieceCatalog.ALL.first { it.name == "3x3" }
        assertTrue(PieceGenerator.sequencePlaceable(0L, bar1x1, sq3))
        // Board free only at isolated scattered cells (no near-complete lines,
        // so no clear cascades): a second 1x1 always fits, a 3x3 never does.
        val board = scatteredHolesBoard()
        assertTrue(PieceGenerator.sequencePlaceable(board, bar1x1, bar1x1))
        assertFalse(PieceGenerator.sequencePlaceable(board, bar1x1, sq3))
    }

    /**
     * FULL board minus 16 isolated single-cell holes; most rows/cols keep two
     * gaps, so single placements don't trigger clear cascades.
     */
    private fun scatteredHolesBoard(): Long {
        var board = Board.FULL
        val holes = listOf(
            0 to 0, 0 to 3, 1 to 1, 1 to 5, 2 to 2, 2 to 7, 3 to 0, 3 to 4,
            4 to 1, 4 to 6, 5 to 3, 5 to 5, 6 to 0, 6 to 2, 7 to 4, 7 to 7,
        )
        for ((r, c) in holes) board = board and Board.bit(r, c).inv()
        return board
    }

    @Test
    fun `traySolvable accepts obviously solvable and rejects impossible`() {
        val bar1x1 = PieceCatalog.ALL.first { it.name == "1x1" }
        val sq3 = PieceCatalog.ALL.first { it.name == "3x3" }
        assertTrue(PieceGenerator.traySolvable(0L, listOf(bar1x1, bar1x1, bar1x1)))
        val board = Board.FULL and Board.bit(0, 0).inv() and Board.bit(7, 7).inv()
        assertTrue(!PieceGenerator.traySolvable(board, listOf(sq3)))
        assertTrue(PieceGenerator.traySolvable(board, listOf(bar1x1, bar1x1)))
    }

    @Test
    fun `dealt pieces are always fair - 5000 game bot simulation`() {
        var games = 0
        val gameLengths = ArrayList<Int>()
        var seed = 987654321L

        while (games < 5_000) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            var t = GameEngine.newGame(seed)
            assertFairDeal(t.state)
            var moves = 0
            while (!t.state.isGameOver && moves < 3_000) {
                var s = t.state
                // Greedy bot: place the active piece at its first valid
                // anchor, rotating it if only a rotation fits.
                var piece = PieceCatalog.get(s.tray[0]!!.pieceId)
                var k = piece.placements.indices.firstOrNull { (piece.placements[it] and s.board) == 0L }
                var spins = 0
                while (k == null && spins < 3) {
                    s = GameEngine.reduce(s, GameAction.Rotate(0)).state
                    piece = PieceCatalog.get(s.tray[0]!!.pieceId)
                    k = piece.placements.indices.firstOrNull { (piece.placements[it] and s.board) == 0L }
                    spins++
                }
                assertTrue(k != null, "active piece unplaceable in every rotation but game-over not flagged")
                val anchor = piece.placementAnchors[k!!]
                t = GameEngine.reduce(s, GameAction.Place(0, anchor / 8, anchor % 8))
                moves++
                // THE invariant: every freshly dealt pair must be fair right now.
                assertFairDeal(t.state)
            }
            gameLengths.add(moves)
            games++
        }

        val median = gameLengths.sorted()[gameLengths.size / 2]
        // Difficulty regression guard: a greedy bot should survive a sane
        // number of placements — neither instant death nor unbounded play.
        assertTrue(median in 10..1_000, "median game length $median out of expected window")
    }

    /**
     * Fairness: whenever the active piece is placeable as dealt, the preview
     * must be placeable after it (in some order of the active piece's valid
     * placements, clears applied). If the active piece only fits via a
     * rotation the generator can't promise the pair, but the game must not
     * be silently lost either — that's the game-over flag's job.
     */
    private fun assertFairDeal(state: GameState) {
        if (state.isGameOver) return
        val active = PieceCatalog.get(state.tray[0]!!.pieceId)
        val next = PieceCatalog.get(state.tray[1]!!.pieceId)
        if (GameEngine.canPlace(state.board, active)) {
            assertTrue(
                PieceGenerator.sequencePlaceable(state.board, active, next),
                "unfair deal: ${active.name} then ${next.name} on board ${java.lang.Long.toHexString(state.board)}",
            )
        }
    }

    @Test
    fun `relief valve deals a line finisher under sustained pressure`() {
        // Row 0 filled except a contiguous 2-gap; congested board (>70%).
        var board = 0L
        for (r in 0 until 6) board = board or Board.ROW_MASKS[r]
        board = board and (Board.bit(0, 3) or Board.bit(0, 4)).inv()
        val rng = GameRng(7L)
        val dealt = PieceGenerator.dealNext(
            board, rng, score = 0,
            precedingPieceId = null, recentPieceIds = emptyList(),
            pressure = PieceGenerator.RELIEF_PRESSURE,
        )
        val piece = PieceCatalog.get(dealt.pieceId)
        val metrics = BoardAnalyzer.analyze(board)
        assertTrue(
            metrics.nearFullLines.any { BoardAnalyzer.finishesLine(piece, it) },
            "expected a line-finisher under pressure, got ${piece.name}",
        )
    }

    @Test
    fun `generation is deterministic for a given rng state`() {
        val a = PieceGenerator.dealNext(0L, GameRng(99L), 0, null, emptyList(), 0)
        val b = PieceGenerator.dealNext(0L, GameRng(99L), 0, null, emptyList(), 0)
        assertEquals(a, b)
    }
}
