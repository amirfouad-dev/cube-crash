package com.neongrid.app.ui.fx

import kotlin.math.sin

/**
 * Trauma-based screen shake: offset magnitude = trauma², so small hits are
 * subtle and big clears are violent. Decays linearly.
 */
class ScreenShake {
    var trauma = 0f
        private set
    private var t = 0f

    var offsetX = 0f
        private set
    var offsetY = 0f
        private set

    fun addTrauma(amount: Float) {
        trauma = (trauma + amount).coerceIn(0f, 1f)
    }

    fun update(dtSec: Float, maxOffsetPx: Float) {
        if (trauma <= 0f) {
            offsetX = 0f
            offsetY = 0f
            return
        }
        t += dtSec
        val shake = trauma * trauma
        offsetX = shake * maxOffsetPx * sin(t * 37f)
        offsetY = shake * maxOffsetPx * sin(t * 29f + 1.7f)
        trauma = (trauma - 1.5f * dtSec).coerceAtLeast(0f)
    }
}
