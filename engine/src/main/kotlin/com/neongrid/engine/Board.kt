package com.neongrid.engine

/**
 * 8x8 bitboard. Bit index = row * 8 + col; bit 0 = top-left, row-major.
 * The whole board is one Long: placement validation is a single AND,
 * line detection is 16 mask ANDs.
 */
object Board {
    const val SIZE = 8
    const val CELLS = SIZE * SIZE
    const val FULL: Long = -1L // all 64 bits set

    val ROW_MASKS = LongArray(SIZE) { 0xFFL shl (it * SIZE) }
    val COL_MASKS = LongArray(SIZE) { col ->
        (0 until SIZE).fold(0L) { m, r -> m or (1L shl (r * SIZE + col)) }
    }

    fun bit(row: Int, col: Int): Long = 1L shl (row * SIZE + col)

    fun isFull(board: Long, mask: Long): Boolean = (board and mask) == mask

    fun fits(board: Long, placement: Long): Boolean = (board and placement) == 0L

    fun fillRatio(board: Long): Float = java.lang.Long.bitCount(board) / CELLS.toFloat()

    /** Row indices that are completely filled. */
    fun fullRows(board: Long): List<Int> = (0 until SIZE).filter { isFull(board, ROW_MASKS[it]) }

    /** Column indices that are completely filled. */
    fun fullCols(board: Long): List<Int> = (0 until SIZE).filter { isFull(board, COL_MASKS[it]) }

    /** Board after removing all completed rows/columns. */
    fun applyClears(board: Long): Long {
        var cleared = 0L
        for (m in ROW_MASKS) if (isFull(board, m)) cleared = cleared or m
        for (m in COL_MASKS) if (isFull(board, m)) cleared = cleared or m
        return board and cleared.inv()
    }
}
