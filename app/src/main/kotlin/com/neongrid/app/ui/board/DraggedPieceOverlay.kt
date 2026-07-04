package com.neongrid.app.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import com.neongrid.app.game.DragController
import com.neongrid.app.ui.theme.NeonTheme

/**
 * Draws the in-flight dragged piece at board-cell scale, floated above the
 * finger. fingerPos is read inside the draw phase only, so pointer moves
 * invalidate just this canvas — zero recomposition during drag.
 */
@Composable
fun DraggedPieceOverlay(
    dragController: DragController,
    theme: com.neongrid.app.meta.GameTheme,
    modifier: Modifier = Modifier,
) {
    val session = dragController.dragging ?: return
    var myBounds = Rect.Zero

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { myBounds = it.boundsInRoot() },
    ) {
        val cell = dragController.cellSizePx
        if (cell <= 0f) return@Canvas
        // Draw-phase read: invalidates only this layer on pointer move.
        val pos = dragController.fingerPos.value
        val topLeft = dragController.pieceTopLeft(pos, session) - myBounds.topLeft
        val color = theme.blockColor(session.colorId)
        for ((r, c) in session.piece.cells) {
            drawBlock(Offset(topLeft.x + c * cell, topLeft.y + r * cell), cell, color)
        }
    }
}
