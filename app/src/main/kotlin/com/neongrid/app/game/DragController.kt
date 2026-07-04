package com.neongrid.app.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.neongrid.engine.GameEngine
import com.neongrid.engine.Piece
import kotlin.math.floor

data class DragSession(
    val slot: Int,
    val piece: Piece,
    val colorId: Byte,
    /** Pointer offset within the piece's rendered tray bounds at grab time. */
    val grabOffset: Offset,
)

data class GhostInfo(
    val row: Int,
    val col: Int,
    val valid: Boolean,
    /** Line masks that would complete if dropped here. */
    val wouldClear: Long,
)

/**
 * Drag state lives outside the ViewModel StateFlow hot path. [fingerPos] is
 * read only in the draw phase so 120Hz pointer updates never recompose the
 * tree; [ghost] changes only when the snapped anchor cell changes.
 */
class DragController {

    var dragging: DragSession? by mutableStateOf(null)
        private set

    /** Raw pointer position in root coordinates. Read in draw phase ONLY. */
    val fingerPos = mutableStateOf(Offset.Zero)

    var ghost: GhostInfo? by mutableStateOf(null)
        private set

    /** Board bounds in root coordinates; set by BoardCanvas on layout. */
    var boardBounds: Rect = Rect.Zero
    var cellSizePx: Float = 0f

    /** How far above the finger the piece floats (in cells). */
    val fingerLiftCells = 1.5f

    /** Fired when the ghost anchor cell changes (for tick haptics). */
    var onAnchorChanged: (() -> Unit)? = null

    fun start(slot: Int, piece: Piece, colorId: Byte, grabOffset: Offset, startPos: Offset) {
        dragging = DragSession(slot, piece, colorId, grabOffset)
        fingerPos.value = startPos
        ghost = null
    }

    fun update(pos: Offset, board: Long) {
        fingerPos.value = pos
        val session = dragging ?: return
        if (cellSizePx <= 0f) return

        val topLeft = pieceTopLeft(pos, session)
        val col = floor((topLeft.x - boardBounds.left) / cellSizePx + 0.5f).toInt()
        val row = floor((topLeft.y - boardBounds.top) / cellSizePx + 0.5f).toInt()

        val current = ghost
        if (current?.row == row && current.col == col) return

        val piece = session.piece
        if (!piece.inBounds(row, col)) {
            ghost = if (current != null) null else return
            return
        }
        val mask = piece.maskAt(row, col)
        val valid = (mask and board) == 0L
        ghost = GhostInfo(
            row = row,
            col = col,
            valid = valid,
            wouldClear = if (valid) GameEngine.wouldClear(board, mask) else 0L,
        )
        onAnchorChanged?.invoke()
    }

    /** Top-left of the dragged piece in root coordinates (floated above finger). */
    fun pieceTopLeft(pos: Offset, session: DragSession): Offset = Offset(
        pos.x - session.grabOffset.x,
        pos.y - session.grabOffset.y - fingerLiftCells * cellSizePx,
    )

    /** Returns the drop target, or null if invalid. Clears drag state. */
    fun end(): GhostInfo? {
        val result = ghost?.takeIf { it.valid }
        dragging = null
        ghost = null
        return result
    }

    fun cancel() {
        dragging = null
        ghost = null
    }
}
