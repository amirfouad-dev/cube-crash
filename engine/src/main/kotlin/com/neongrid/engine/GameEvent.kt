package com.neongrid.engine

/** Emitted by the reducer; consumed by the juice layer and mission engine. */
sealed interface GameEvent {
    data class PiecePlaced(val cells: Long, val colorId: Byte) : GameEvent
    data class LinesCleared(
        val rowIdxs: List<Int>,
        val colIdxs: List<Int>,
        val clearedCells: Long,
        val combo: Int,
        val points: Long,
        /** Cell colors as they were just before the clear (for FX ghosts). */
        val colorSnapshot: ByteArray = ByteArray(0),
        /** Cell index the clear radiates from (placement anchor). */
        val originCell: Int = 0,
    ) : GameEvent {
        override fun equals(other: Any?): Boolean = other is LinesCleared &&
            other.rowIdxs == rowIdxs && other.colIdxs == colIdxs &&
            other.clearedCells == clearedCells && other.combo == combo && other.points == points
        override fun hashCode(): Int = 31 * clearedCells.hashCode() + combo
    }
    data class ComboChanged(val streak: Int) : GameEvent
    data class PieceRotated(val slot: Int, val newPieceId: Int) : GameEvent
    data object AllClear : GameEvent
    data class TrayRefilled(val pieces: List<TrayPiece>) : GameEvent
    data object GameOver : GameEvent
    data class ScoreAwarded(val delta: Long, val atCells: Long) : GameEvent
}
