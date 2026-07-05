package com.neongrid.app.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.game.DragController
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.GameEngine
import com.neongrid.engine.GameState
import com.neongrid.engine.Piece
import com.neongrid.engine.PieceCatalog
import com.neongrid.engine.TrayPiece

/**
 * The piece dock: the active piece (slot 0 — draggable, tap to rotate) and a
 * dimmed non-interactive preview of the next piece. Drag lifts the active
 * piece to full board scale via DraggedPieceOverlay.
 */
@Composable
fun TrayRow(
    tray: List<TrayPiece?>,
    board: Long,
    dragController: DragController,
    theme: com.neongrid.app.meta.GameTheme,
    onDrop: (slot: Int, row: Int, col: Int) -> Unit,
    onRotate: (slot: Int) -> Unit,
    modifier: Modifier = Modifier,
    slotSize: Dp = 96.dp,
) {
    val active = tray.getOrNull(GameState.ACTIVE_SLOT)
    val next = tray.getOrNull(GameState.NEXT_SLOT)
    Row(
        modifier = modifier.fillMaxWidth().height(slotSize + 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(slotSize), contentAlignment = Alignment.Center) {
            if (active != null) {
                TraySlot(
                    slot = GameState.ACTIVE_SLOT,
                    trayPiece = active,
                    placeable = GameEngine.canPlaceAnyRotation(board, active.pieceId),
                    board = { board },
                    dragController = dragController,
                    theme = theme,
                    onDrop = onDrop,
                    onRotate = onRotate,
                    slotSize = slotSize,
                )
            }
        }
        Spacer(Modifier.width(36.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEXT",
                color = NeonTheme.HudDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.size(slotSize * 0.62f),
                contentAlignment = Alignment.Center,
            ) {
                if (next != null) NextPreview(next, theme, slotSize * 0.62f)
            }
        }
    }
}

/** Dimmed, non-interactive rendering of the upcoming piece. */
@Composable
private fun NextPreview(
    trayPiece: TrayPiece,
    theme: com.neongrid.app.meta.GameTheme,
    slotSize: Dp,
) {
    val piece = PieceCatalog.get(trayPiece.pieceId)
    Canvas(modifier = Modifier.size(slotSize).graphicsLayer { alpha = 0.45f }) {
        val (pw, ph) = pieceExtent(piece, size.width)
        val cell = miniCell(piece, size.width)
        val originX = (size.width - pw) / 2f
        val originY = (size.height - ph) / 2f
        val color = theme.blockColor(trayPiece.colorId)
        for ((r, c) in piece.cells) {
            drawBlock(Offset(originX + c * cell, originY + r * cell), cell, color)
        }
    }
}

@Composable
private fun TraySlot(
    slot: Int,
    trayPiece: TrayPiece,
    placeable: Boolean,
    board: () -> Long,
    dragController: DragController,
    theme: com.neongrid.app.meta.GameTheme,
    onDrop: (slot: Int, row: Int, col: Int) -> Unit,
    onRotate: (slot: Int) -> Unit,
    slotSize: Dp,
) {
    val piece = PieceCatalog.get(trayPiece.pieceId)
    // The gesture blocks below are keyed on the slot only, so they survive
    // recomposition and MUST read live values through State rather than
    // captures. Keying on trayPiece instead is not enough: a freshly dealt
    // piece that equals the previous one (same shape + color) compares equal,
    // the old gesture coroutine keeps running, and its captured board()
    // closure then validates drops against pre-placement, pre-clear occupancy
    // (drops refused on free cells / no ghost where the real fit is).
    val currentPiece by rememberUpdatedState(piece)
    val currentColorId by rememberUpdatedState(trayPiece.colorId)
    val currentBoard by rememberUpdatedState(board)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentOnRotate by rememberUpdatedState(onRotate)
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val isBeingDragged = dragController.dragging?.slot == slot

    Canvas(
        modifier = Modifier
            .size(slotSize)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer { alpha = if (isBeingDragged) 0.25f else if (placeable) 1f else 0.35f }
            .pointerInput(slot) {
                // Tap (no drag slop crossed) rotates the piece 90°.
                detectTapGestures { currentOnRotate(slot) }
            }
            .pointerInput(slot) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val p = currentPiece
                        dragController.start(
                            slot = slot,
                            piece = p,
                            colorId = currentColorId,
                            grabOffset = offset - Offset(
                                (size.width - pieceExtent(p, size.width.toFloat()).first) / 2f,
                                (size.height - pieceExtent(p, size.width.toFloat()).second) / 2f,
                            ),
                            startPos = bounds.topLeft + offset,
                        )
                        dragController.update(bounds.topLeft + offset, currentBoard())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragController.update(bounds.topLeft + change.position, currentBoard())
                    },
                    onDragEnd = {
                        val ghost = dragController.end()
                        if (ghost != null) currentOnDrop(slot, ghost.row, ghost.col)
                    },
                    onDragCancel = { dragController.cancel() },
                )
            },
    ) {
        // Mini rendering: scale the piece to fit the slot.
        val (pw, ph) = pieceExtent(piece, size.width)
        val cell = miniCell(piece, size.width)
        val originX = (size.width - pw) / 2f
        val originY = (size.height - ph) / 2f
        val color = theme.blockColor(trayPiece.colorId)
        for ((r, c) in piece.cells) {
            drawBlock(Offset(originX + c * cell, originY + r * cell), cell, color)
        }
    }
}

private fun miniCell(piece: Piece, slotPx: Float): Float {
    val maxDim = maxOf(piece.width, piece.height).coerceAtLeast(3)
    return slotPx / (maxDim + 0.5f)
}

private fun pieceExtent(piece: Piece, slotPx: Float): Pair<Float, Float> {
    val cell = miniCell(piece, slotPx)
    return piece.width * cell to piece.height * cell
}
