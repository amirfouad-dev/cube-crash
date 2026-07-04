package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEngineTest {

    private val bar1x1 = PieceCatalog.ALL.first { it.name == "1x1" }
    private val bar1x5 = PieceCatalog.ALL.first { it.name == "1x5" }
    private val bar1x3 = PieceCatalog.ALL.first { it.name == "1x3" }
    private val sq3 = PieceCatalog.ALL.first { it.name == "3x3" }

    /** Build a state directly, bypassing generation, for scenario tests. */
    private fun state(
        board: Long = 0L,
        tray: List<TrayPiece?> = listOf(TrayPiece(bar1x1.id, 1), TrayPiece(bar1x1.id, 1)),
        score: Long = 0,
        comboStreak: Int = 0,
        placementsSinceClear: Int = 0,
    ) = GameState(
        board = board,
        cellColors = ByteArray(64),
        tray = tray,
        score = score,
        comboStreak = comboStreak,
        placementsSinceClear = placementsSinceClear,
        piecesPlaced = 0,
        linesClearedTotal = 0,
        maxCombo = comboStreak,
        rngState = 42L,
        pressure = 0,
        isGameOver = false,
    )

    @Test
    fun `scoring table for simultaneous lines`() {
        assertEquals(100L, Scoring.lineBase(1))
        assertEquals(300L, Scoring.lineBase(2))
        assertEquals(600L, Scoring.lineBase(3))
        assertEquals(1000L, Scoring.lineBase(4))
        assertEquals(1500L, Scoring.lineBase(5))
    }

    @Test
    fun `combo multiplier ramps and caps`() {
        assertEquals(1.0f, Scoring.multiplier(1))
        assertEquals(1.25f, Scoring.multiplier(2))
        assertEquals(2.0f, Scoring.multiplier(5))
        assertEquals(3.0f, Scoring.multiplier(9))
        assertEquals(3.0f, Scoring.multiplier(50))
    }

    @Test
    fun `placement awards cell count and fills board`() {
        val s = state(tray = listOf(TrayPiece(bar1x3.id, 2), TrayPiece(bar1x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 0))
        assertEquals(3L, t.state.score)
        assertEquals(3, java.lang.Long.bitCount(t.state.board))
        assertEquals(2.toByte(), t.state.cellColors[0])
        assertTrue(t.events.any { it is GameEvent.PiecePlaced })
    }

    @Test
    fun `completing a row clears it and scores 100`() {
        // Row 0 filled except last 3 cells; place a 1x3 to complete it.
        val board = Board.ROW_MASKS[0] and (Board.bit(0, 5) or Board.bit(0, 6) or Board.bit(0, 7)).inv()
        val s = state(board = board, tray = listOf(TrayPiece(bar1x3.id, 1), TrayPiece(bar1x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 5))
        // Row cleared entirely; only score remains.
        assertEquals(0L, t.state.board)
        assertEquals(3L + 100L + Scoring.ALL_CLEAR_BONUS, t.state.score)
        assertEquals(1, t.state.comboStreak)
        val cleared = t.events.filterIsInstance<GameEvent.LinesCleared>().single()
        assertEquals(listOf(0), cleared.rowIdxs)
        assertTrue(t.events.any { it is GameEvent.AllClear })
    }

    @Test
    fun `simultaneous row and column both clear`() {
        // Fill row 3 except (3,3) and col 3 except (3,3); 1x1 at (3,3) completes both.
        val board = (Board.ROW_MASKS[3] or Board.COL_MASKS[3]) and Board.bit(3, 3).inv()
        val s = state(board = board)
        val t = GameEngine.reduce(s, GameAction.Place(0, 3, 3))
        assertEquals(0L, t.state.board)
        val cleared = t.events.filterIsInstance<GameEvent.LinesCleared>().single()
        assertEquals(listOf(3), cleared.rowIdxs)
        assertEquals(listOf(3), cleared.colIdxs)
        // 1 cell + 300 (2 lines, streak 1) + all clear.
        assertEquals(1L + 300L + Scoring.ALL_CLEAR_BONUS, t.state.score)
        assertEquals(2, t.state.linesClearedTotal)
    }

    @Test
    fun `combo multiplier applies to consecutive clears`() {
        val board = Board.ROW_MASKS[0] and Board.bit(0, 7).inv()
        val s = state(board = board, comboStreak = 1)
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 7))
        // streak becomes 2 → 100 * 1.25 = 125.
        val cleared = t.events.filterIsInstance<GameEvent.LinesCleared>().single()
        assertEquals(125L, cleared.points)
        assertEquals(2, t.state.comboStreak)
    }

    @Test
    fun `streak survives early non-clearing placements and dies at the grace limit`() {
        // First non-clearing placement: streak survives, counter ticks.
        var t = GameEngine.reduce(
            state(comboStreak = 3, placementsSinceClear = 0),
            GameAction.Place(0, 0, 0),
        )
        assertEquals(3, t.state.comboStreak)
        assertEquals(1, t.state.placementsSinceClear)
        // Placement that reaches the grace limit: streak dies.
        t = GameEngine.reduce(
            state(comboStreak = 3, placementsSinceClear = Scoring.STREAK_GRACE - 1),
            GameAction.Place(0, 0, 0),
        )
        assertEquals(0, t.state.comboStreak)
        assertTrue(t.events.any { it is GameEvent.ComboChanged && it.streak == 0 })
    }

    @Test
    fun `placing promotes the preview and deals a fresh piece`() {
        val s = state(tray = listOf(TrayPiece(bar1x3.id, 2), TrayPiece(bar1x1.id, 3)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 0))
        assertEquals(2, t.state.tray.size)
        // The preview moved into the active slot, color intact.
        assertEquals(bar1x1.id, t.state.tray[0]!!.pieceId)
        assertEquals(3.toByte(), t.state.tray[0]!!.colorId)
        // A new preview was dealt behind it.
        assertNotNull(t.state.tray[1])
        assertTrue(t.events.any { it is GameEvent.TrayRefilled })
    }

    @Test
    fun `only the active slot can be placed`() {
        val s = state()
        val t = GameEngine.reduce(s, GameAction.Place(1, 0, 0))
        assertEquals(s.board, t.state.board)
        assertEquals(s.tray, t.state.tray)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `invalid placement is a no-op`() {
        val s = state(board = Board.bit(0, 0))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 0))
        assertEquals(s.board, t.state.board)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `out of bounds placement is a no-op`() {
        val s = state(tray = listOf(TrayPiece(bar1x5.id, 1), TrayPiece(bar1x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 4)) // 1x5 at col 4 → col 8 out of bounds
        assertEquals(0L, t.state.board)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `game over when the promoted piece fits in no rotation`() {
        // Board free only at isolated scattered cells — placing the 1x1
        // clears nothing, and the promoted 3x3 has nowhere to go.
        var board = Board.FULL
        val holes = listOf(
            0 to 0, 0 to 3, 1 to 1, 1 to 5, 2 to 2, 2 to 7, 3 to 0, 3 to 4,
            4 to 1, 4 to 6, 5 to 3, 5 to 5, 6 to 0, 6 to 2, 7 to 4, 7 to 7,
        )
        for ((r, c) in holes) board = board and Board.bit(r, c).inv()
        val s = state(board = board, tray = listOf(TrayPiece(bar1x1.id, 1), TrayPiece(sq3.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 0, 0))
        assertTrue(t.state.isGameOver)
        assertTrue(t.events.any { it is GameEvent.GameOver })
    }

    @Test
    fun `time up ends the run`() {
        val s = state(score = 1234)
        val t = GameEngine.reduce(s, GameAction.TimeUp)
        assertTrue(t.state.isGameOver)
        assertTrue(t.events.any { it is GameEvent.GameOver })
        // Board and score are untouched — only the run ends.
        assertEquals(s.board, t.state.board)
        assertEquals(s.score, t.state.score)
    }

    @Test
    fun `time up on a finished game is a no-op`() {
        val s = state().copy(isGameOver = true)
        val t = GameEngine.reduce(s, GameAction.TimeUp)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `rotate swaps the active piece orientation and keeps color`() {
        val s = state(tray = listOf(TrayPiece(bar1x3.id, 4), TrayPiece(bar1x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Rotate(0))
        val rotated = t.state.tray[0]!!
        val piece = PieceCatalog.get(rotated.pieceId)
        assertEquals(3, piece.height) // 1x3 -> 3x1
        assertEquals(1, piece.width)
        assertEquals(4.toByte(), rotated.colorId)
        assertTrue(t.events.any { it is GameEvent.PieceRotated })
        // Four rotations return to the original piece.
        var st = t.state
        repeat(3) { st = GameEngine.reduce(st, GameAction.Rotate(0)).state }
        assertEquals(bar1x3.id, st.tray[0]!!.pieceId)
    }

    @Test
    fun `rotating the preview is a no-op`() {
        val s = state(tray = listOf(TrayPiece(bar1x1.id, 1), TrayPiece(bar1x3.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Rotate(1))
        assertEquals(bar1x3.id, t.state.tray[1]!!.pieceId)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `rotating a symmetric piece is a no-op`() {
        val s = state(tray = listOf(TrayPiece(bar1x1.id, 1), TrayPiece(bar1x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Rotate(0))
        assertEquals(bar1x1.id, t.state.tray[0]!!.pieceId)
        assertTrue(t.events.isEmpty())
    }

    @Test
    fun `game survives when only a rotation of the piece fits`() {
        // Only the top row is free: a vertical 5x1 doesn't fit, but its
        // horizontal rotation does — must NOT be game over.
        val bar5x1 = PieceCatalog.ALL.first { it.name == "5x1" }
        var board = Board.FULL
        for (c in 0 until 8) board = board and Board.bit(0, c).inv()
        board = board and Board.bit(7, 7).inv() // free cell for the 1x1 we place
        val s = state(board = board, tray = listOf(TrayPiece(bar1x1.id, 1), TrayPiece(bar5x1.id, 1)))
        val t = GameEngine.reduce(s, GameAction.Place(0, 7, 7))
        assertFalse(t.state.isGameOver, "5x1 can rotate to 1x5 and fit in the free row")
    }

    @Test
    fun `wouldClear predicts completed lines`() {
        val board = Board.ROW_MASKS[2] and Board.bit(2, 4).inv()
        val mask = GameEngine.wouldClear(board, Board.bit(2, 4))
        assertEquals(Board.ROW_MASKS[2], mask)
        assertEquals(0L, GameEngine.wouldClear(0L, Board.bit(0, 0)))
    }

    @Test
    fun `new game deals an active piece plus a preview`() {
        val t = GameEngine.newGame(seed = 123L)
        assertEquals(2, t.state.tray.size)
        assertEquals(2, t.state.tray.count { it != null })
        assertEquals(0L, t.state.board)
        assertFalse(t.state.isGameOver)
        val active = PieceCatalog.get(t.state.tray[0]!!.pieceId)
        val next = PieceCatalog.get(t.state.tray[1]!!.pieceId)
        assertTrue(PieceGenerator.sequencePlaceable(0L, active, next))
    }
}
