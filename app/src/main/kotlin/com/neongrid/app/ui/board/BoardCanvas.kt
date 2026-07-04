package com.neongrid.app.ui.board

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.neongrid.app.game.DragController
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.Board

/**
 * The 8x8 grid: grid lines, placed blocks, and the drag ghost preview with
 * would-clear line highlight. One Canvas — no per-cell composables.
 */
@Composable
fun BoardCanvas(
    board: Long,
    cellColors: ByteArray,
    dragController: DragController,
    theme: com.neongrid.app.meta.GameTheme,
    modifier: Modifier = Modifier,
) {
    // Reading dragController.ghost here recomposes only when the snapped
    // anchor changes, not on every pointer move.
    val ghost = dragController.ghost
    val session = dragController.dragging

    // Danger state: pulsing red edge when the grid is nearly full.
    val danger = Board.fillRatio(board) > 0.75f
    val dangerPulse = if (danger) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "danger")
        transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.55f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(650),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "dangerAlpha",
        )
    } else {
        null
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { coords ->
                dragController.boardBounds = coords.boundsInRoot()
                dragController.cellSizePx = coords.size.width / Board.SIZE.toFloat()
            },
    ) {
        val cell = size.width / Board.SIZE

        // Holo panel: plated gradient background + neon outline + corner
        // brackets. Cells are inset "sockets" — no spreadsheet grid lines.
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF14142A), Color(0xFF0C0A18)),
            ),
            cornerRadius = CornerRadius(cell * 0.22f),
        )
        drawRoundRect(
            color = NeonTheme.Cyan.copy(alpha = 0.18f),
            cornerRadius = CornerRadius(cell * 0.22f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
        )
        val armLen = cell * 0.45f
        val armW = cell * 0.045f
        val bracketColor = NeonTheme.Cyan.copy(alpha = 0.55f)
        val inset2 = armW / 2 + 1f
        val corners = arrayOf(
            floatArrayOf(inset2, inset2, 1f, 1f),
            floatArrayOf(size.width - inset2, inset2, -1f, 1f),
            floatArrayOf(inset2, size.height - inset2, 1f, -1f),
            floatArrayOf(size.width - inset2, size.height - inset2, -1f, -1f),
        )
        for (corner in corners) {
            val x = corner[0]; val y = corner[1]; val sx = corner[2]; val sy = corner[3]
            drawLine(bracketColor, Offset(x, y), Offset(x + armLen * sx, y), strokeWidth = armW)
            drawLine(bracketColor, Offset(x, y), Offset(x, y + armLen * sy), strokeWidth = armW)
        }

        // Cell sockets: recessed slots with a faint themed rim.
        for (r in 0 until Board.SIZE) {
            for (c in 0 until Board.SIZE) {
                val x = c * cell + cell * 0.07f
                val y = r * cell + cell * 0.07f
                val s = cell * 0.86f
                drawRoundRect(
                    color = Color(0x0A000000),
                    topLeft = Offset(x, y),
                    size = Size(s, s),
                    cornerRadius = CornerRadius(cell * 0.12f),
                )
                drawRoundRect(
                    color = Color(0x10FFFFFF),
                    topLeft = Offset(x, y),
                    size = Size(s, s),
                    cornerRadius = CornerRadius(cell * 0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f),
                )
            }
        }

        // Would-clear line highlight (under the blocks so blocks stay crisp).
        if (ghost != null && ghost.valid && ghost.wouldClear != 0L && session != null) {
            val glowColor = theme.blockColor(session.colorId)
            for (i in 0 until Board.CELLS) {
                if ((ghost.wouldClear ushr i) and 1L == 1L) {
                    val r = i / Board.SIZE
                    val c = i % Board.SIZE
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.30f),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.12f),
                        blendMode = BlendMode.Plus,
                    )
                }
            }
        }

        // Placed blocks.
        for (i in 0 until Board.CELLS) {
            if ((board ushr i) and 1L == 1L) {
                val r = i / Board.SIZE
                val c = i % Board.SIZE
                drawBlock(Offset(c * cell, r * cell), cell, theme.blockColor(cellColors[i]))
            }
        }

        // Danger edge pulse.
        if (dangerPulse != null) {
            drawRoundRect(
                color = NeonTheme.DangerPulse.copy(alpha = dangerPulse.value),
                cornerRadius = CornerRadius(cell * 0.12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = cell * 0.10f),
                blendMode = BlendMode.Plus,
            )
        }

        // Ghost preview of the dragged piece.
        if (ghost != null && session != null) {
            val color = if (ghost.valid) {
                theme.blockColor(session.colorId).copy(alpha = 0.45f)
            } else {
                NeonTheme.DangerPulse.copy(alpha = 0.30f)
            }
            for ((dr, dc) in session.piece.cells) {
                val r = ghost.row + dr
                val c = ghost.col + dc
                if (r in 0 until Board.SIZE && c in 0 until Board.SIZE) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(c * cell + cell * 0.06f, r * cell + cell * 0.06f),
                        size = Size(cell * 0.88f, cell * 0.88f),
                        cornerRadius = CornerRadius(cell * 0.12f),
                    )
                }
            }
        }
    }
}

/**
 * Neon block: additive radial halo + gradient core with a bright inner edge.
 * Shared by board, tray, and drag overlay so blocks look identical everywhere.
 */
fun DrawScope.drawBlock(topLeft: Offset, cell: Float, color: Color) {
    val center = Offset(topLeft.x + cell / 2, topLeft.y + cell / 2)
    // Tight halo: neon accent, not fog — big soft halos overlap between
    // neighbouring blocks and make the whole board read as blurry.
    val haloRadius = cell * 0.58f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.32f), Color.Transparent),
            center = center,
            radius = haloRadius,
        ),
        topLeft = Offset(center.x - haloRadius, center.y - haloRadius),
        size = Size(haloRadius * 2, haloRadius * 2),
        blendMode = BlendMode.Plus,
    )
    val inset = cell * 0.07f
    val coreSize = cell - inset * 2
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerpToWhite(color, 0.5f),
                color,
                lerpToWhite(color, -0.3f),
            ),
            startY = topLeft.y + inset,
            endY = topLeft.y + inset + coreSize,
        ),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(coreSize, coreSize),
        cornerRadius = CornerRadius(cell * 0.14f),
    )
    // Crisp bright rim — the sharp edge that makes the block read clean.
    drawRoundRect(
        color = lerpToWhite(color, 0.65f),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(coreSize, coreSize),
        cornerRadius = CornerRadius(cell * 0.14f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = cell * 0.04f),
    )
}

/** t > 0 lightens toward white, t < 0 darkens toward black. */
private fun lerpToWhite(color: Color, t: Float): Color = if (t >= 0f) {
    Color(
        red = color.red + (1f - color.red) * t,
        green = color.green + (1f - color.green) * t,
        blue = color.blue + (1f - color.blue) * t,
        alpha = color.alpha,
    )
} else {
    val k = 1f + t
    Color(red = color.red * k, green = color.green * k, blue = color.blue * k, alpha = color.alpha)
}
