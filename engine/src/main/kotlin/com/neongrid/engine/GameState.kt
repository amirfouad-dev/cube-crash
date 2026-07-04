package com.neongrid.engine

data class TrayPiece(val pieceId: Int, val colorId: Byte)

/**
 * Immutable game state. [cellColors] is treated as immutable — the reducer
 * copies it before writing (64 bytes, trivial).
 *
 * [tray] holds exactly two entries: slot 0 is the active piece (the only
 * one that can be placed or rotated), slot 1 is the next-up preview.
 */
data class GameState(
    val board: Long,
    val cellColors: ByteArray,
    val tray: List<TrayPiece?>,
    val score: Long,
    val comboStreak: Int,
    val placementsSinceClear: Int,
    val piecesPlaced: Int,
    val linesClearedTotal: Int,
    val maxCombo: Int,
    val rngState: Long,
    val pressure: Int,
    val isGameOver: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return board == other.board &&
            cellColors.contentEquals(other.cellColors) &&
            tray == other.tray &&
            score == other.score &&
            comboStreak == other.comboStreak &&
            placementsSinceClear == other.placementsSinceClear &&
            piecesPlaced == other.piecesPlaced &&
            linesClearedTotal == other.linesClearedTotal &&
            maxCombo == other.maxCombo &&
            rngState == other.rngState &&
            pressure == other.pressure &&
            isGameOver == other.isGameOver
    }

    override fun hashCode(): Int {
        var result = board.hashCode()
        result = 31 * result + cellColors.contentHashCode()
        result = 31 * result + tray.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + rngState.hashCode()
        return result
    }

    companion object {
        const val TRAY_SIZE = 2
        const val ACTIVE_SLOT = 0
        const val NEXT_SLOT = 1
    }
}
