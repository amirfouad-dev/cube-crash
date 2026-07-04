package com.neongrid.app.ui.fx

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.Board

/**
 * Animates ghosts of cleared cells. The engine removes cells from committed
 * state instantly; this draws the flash -> scale -> fade of the past.
 */
class ClearAnimator {

    private class ClearAnim(
        val cells: Long,
        val colorSnapshot: ByteArray,
        val originCell: Int,
        val durationScale: Float,
        var ageSec: Float = 0f,
    )

    private val anims = ArrayList<ClearAnim>()
    val active: Boolean get() = anims.isNotEmpty()

    /** Per-cell stagger sweeping outward from the placed piece. */
    private val staggerSecPerCell = 0.012f
    private val flashSec = 0.12f
    private val fadeSec = 0.26f

    fun push(cells: Long, colorSnapshot: ByteArray, originCell: Int, big: Boolean) {
        anims.add(ClearAnim(cells, colorSnapshot, originCell, if (big) 1.8f else 1f))
    }

    fun update(dtSec: Float) {
        val it = anims.iterator()
        while (it.hasNext()) {
            val a = it.next()
            a.ageSec += dtSec
            val maxDelay = 14 * staggerSecPerCell // worst-case cell distance
            if (a.ageSec > (maxDelay + (flashSec + fadeSec) * a.durationScale)) it.remove()
        }
    }

    fun draw(scope: DrawScope, cellPx: Float, theme: com.neongrid.app.meta.GameTheme) {
        for (a in anims) {
            val originR = a.originCell / Board.SIZE
            val originC = a.originCell % Board.SIZE
            for (i in 0 until Board.CELLS) {
                if ((a.cells ushr i) and 1L != 1L) continue
                val r = i / Board.SIZE
                val c = i % Board.SIZE
                val dist = kotlin.math.abs(r - originR) + kotlin.math.abs(c - originC)
                val local = a.ageSec - dist * staggerSecPerCell
                if (local < 0f) {
                    // Not reached yet: still draw the block as it was.
                    scope.drawCellGhost(r, c, cellPx, theme.blockColor(a.colorSnapshot[i]), 1f, 1f)
                    continue
                }
                val flash = flashSec * a.durationScale
                val fade = fadeSec * a.durationScale
                when {
                    local < flash -> {
                        val t = local / flash
                        val color = lerpColor(theme.blockColor(a.colorSnapshot[i]), Color.White, t)
                        scope.drawCellGhost(r, c, cellPx, color, 1f, 1f + 0.15f * t)
                    }
                    local < flash + fade -> {
                        val t = (local - flash) / fade
                        val color = Color.White.copy(alpha = 1f - t)
                        scope.drawCellGhost(r, c, cellPx, color, 1f - t, 1.15f + 0.25f * t)
                    }
                }
            }
        }
    }

    /** Cells whose flash moment happened within the last frame — emit particles there. */
    fun collectFlashingCells(dtSec: Float, out: MutableList<Pair<Int, Byte>>) {
        for (a in anims) {
            val originR = a.originCell / Board.SIZE
            val originC = a.originCell % Board.SIZE
            for (i in 0 until Board.CELLS) {
                if ((a.cells ushr i) and 1L != 1L) continue
                val dist = kotlin.math.abs(i / Board.SIZE - originR) + kotlin.math.abs(i % Board.SIZE - originC)
                val flashAt = dist * staggerSecPerCell
                if (a.ageSec >= flashAt && a.ageSec - dtSec < flashAt) {
                    out.add(i to a.colorSnapshot[i])
                }
            }
        }
    }
}

private fun DrawScope.drawCellGhost(r: Int, c: Int, cell: Float, color: Color, alpha: Float, scale: Float) {
    val cx = c * cell + cell / 2
    val cy = r * cell + cell / 2
    val half = cell * 0.44f * scale
    drawRoundRect(
        color = color.copy(alpha = color.alpha * alpha),
        topLeft = Offset(cx - half, cy - half),
        size = Size(half * 2, half * 2),
        cornerRadius = CornerRadius(cell * 0.14f),
        blendMode = BlendMode.Plus,
    )
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
