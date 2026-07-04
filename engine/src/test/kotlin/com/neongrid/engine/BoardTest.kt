package com.neongrid.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardTest {

    @Test
    fun `row masks cover exactly their row`() {
        for (r in 0 until 8) {
            assertEquals(8, java.lang.Long.bitCount(Board.ROW_MASKS[r]))
            for (c in 0 until 8) {
                assertTrue(Board.ROW_MASKS[r] and Board.bit(r, c) != 0L)
            }
        }
    }

    @Test
    fun `col masks cover exactly their column`() {
        for (c in 0 until 8) {
            assertEquals(8, java.lang.Long.bitCount(Board.COL_MASKS[c]))
            for (r in 0 until 8) {
                assertTrue(Board.COL_MASKS[c] and Board.bit(r, c) != 0L)
            }
        }
    }

    @Test
    fun `all row masks OR to full board and are disjoint`() {
        var acc = 0L
        for (m in Board.ROW_MASKS) {
            assertEquals(0L, acc and m)
            acc = acc or m
        }
        assertEquals(Board.FULL, acc)
    }

    @Test
    fun `fits detects overlap and free space`() {
        val board = Board.bit(0, 0)
        assertFalse(Board.fits(board, Board.bit(0, 0)))
        assertTrue(Board.fits(board, Board.bit(0, 1)))
    }

    @Test
    fun `fullRows and fullCols detect completed lines`() {
        assertEquals(listOf(3), Board.fullRows(Board.ROW_MASKS[3]))
        assertEquals(listOf(5), Board.fullCols(Board.COL_MASKS[5]))
        assertTrue(Board.fullRows(0L).isEmpty())
    }
}
