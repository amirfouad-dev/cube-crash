package com.neongrid.engine

data class NearFullLine(
    val isRow: Boolean,
    val index: Int,
    val filled: Int,
    /** Empty cells of this line. */
    val gapMask: Long,
)

data class BoardMetrics(
    val fillRatio: Float,
    val nearFullLines: List<NearFullLine>,
)

object BoardAnalyzer {

    const val NEAR_FULL_MIN = 5
    const val NEAR_FULL_MAX = 7

    fun analyze(board: Long): BoardMetrics {
        val nearFull = ArrayList<NearFullLine>()
        for (i in 0 until Board.SIZE) {
            val rowFilled = java.lang.Long.bitCount(board and Board.ROW_MASKS[i])
            if (rowFilled in NEAR_FULL_MIN..NEAR_FULL_MAX) {
                nearFull.add(NearFullLine(true, i, rowFilled, Board.ROW_MASKS[i] and board.inv()))
            }
            val colFilled = java.lang.Long.bitCount(board and Board.COL_MASKS[i])
            if (colFilled in NEAR_FULL_MIN..NEAR_FULL_MAX) {
                nearFull.add(NearFullLine(false, i, colFilled, Board.COL_MASKS[i] and board.inv()))
            }
        }
        return BoardMetrics(Board.fillRatio(board), nearFull)
    }

    /**
     * True if the gap of [line] is contiguous and exactly fillable by a
     * 1xN / Nx1 bar of the gap's length — i.e. a bar "finishes" the line.
     */
    fun contiguousGapLength(line: NearFullLine): Int? {
        val gap = line.gapMask
        val positions = (0 until Board.CELLS).filter { (gap ushr it) and 1L == 1L }
        if (positions.isEmpty()) return null
        val coords = positions.map { if (line.isRow) it % Board.SIZE else it / Board.SIZE }.sorted()
        for (k in 1 until coords.size) {
            if (coords[k] != coords[k - 1] + 1) return null
        }
        return coords.size
    }

    /** True if [piece] placed somewhere fills the entire gap of [line]. */
    fun finishesLine(piece: Piece, line: NearFullLine): Boolean {
        val len = contiguousGapLength(line) ?: return false
        return if (line.isRow) {
            piece.height == 1 && piece.width == len
        } else {
            piece.width == 1 && piece.height == len
        }
    }
}
