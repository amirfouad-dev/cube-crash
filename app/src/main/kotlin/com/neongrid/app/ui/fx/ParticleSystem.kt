package com.neongrid.app.ui.fx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.neongrid.app.ui.theme.NeonTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pooled, allocation-free particle system (structure of arrays, swap-remove).
 * Updated by the shared FX frame loop; drawn with additive blending.
 */
class ParticleSystem(private val max: Int = 512) {
    private val x = FloatArray(max)
    private val y = FloatArray(max)
    private val vx = FloatArray(max)
    private val vy = FloatArray(max)
    private val life = FloatArray(max)
    private val maxLife = FloatArray(max)
    private val size = FloatArray(max)
    private val colorIdx = ByteArray(max)

    var alive = 0
        private set
    var timeScale = 1f

    fun emit(
        px: Float,
        py: Float,
        count: Int,
        color: Byte,
        speedMin: Float,
        speedMax: Float,
        sizePx: Float,
        lifeSec: Float = 0.6f,
    ) {
        repeat(count) {
            if (alive >= max) return
            val i = alive++
            val angle = Random.nextFloat() * (2f * Math.PI.toFloat())
            val speed = speedMin + Random.nextFloat() * (speedMax - speedMin)
            x[i] = px
            y[i] = py
            vx[i] = cos(angle) * speed
            vy[i] = sin(angle) * speed
            val l = lifeSec * (0.6f + Random.nextFloat() * 0.8f)
            life[i] = l
            maxLife[i] = l
            size[i] = sizePx * (0.5f + Random.nextFloat())
            colorIdx[i] = color
        }
    }

    fun update(dtSec: Float) {
        val dt = dtSec * timeScale
        var i = 0
        while (i < alive) {
            life[i] -= dt
            if (life[i] <= 0f) {
                // swap-remove
                val last = --alive
                x[i] = x[last]; y[i] = y[last]
                vx[i] = vx[last]; vy[i] = vy[last]
                life[i] = life[last]; maxLife[i] = maxLife[last]
                size[i] = size[last]; colorIdx[i] = colorIdx[last]
                continue
            }
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            // light drag + slight upward drift for that "energy dissipating" look
            val drag = 1f - 0.9f * dt
            vx[i] *= drag
            vy[i] = vy[i] * drag - 30f * dt
            i++
        }
    }

    fun draw(scope: DrawScope, theme: com.neongrid.app.meta.GameTheme) {
        for (i in 0 until alive) {
            val t = life[i] / maxLife[i]
            val color = theme.blockColor(colorIdx[i])
            scope.drawCircle(
                color = color.copy(alpha = t * 0.85f),
                radius = size[i] * (0.5f + 0.5f * t),
                center = Offset(x[i], y[i]),
                blendMode = BlendMode.Plus,
            )
        }
    }
}
