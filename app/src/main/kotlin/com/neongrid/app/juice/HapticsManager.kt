package com.neongrid.app.juice

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsManager(context: Context) {

    var enabled = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** Ghost anchor snap. */
    fun tick() {
        if (!enabled) return
        vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    /** Piece placement thunk. */
    fun place() {
        if (!enabled) return
        vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    /** Line clear — escalates with line count and combo. */
    fun clear(lines: Int, combo: Int) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= 30 && vibrator?.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK) == true
        ) {
            val comp = VibrationEffect.startComposition()
            val scale = (0.4f + 0.1f * combo).coerceAtMost(1f)
            repeat(lines.coerceAtMost(4)) { i ->
                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale, i * 40)
            }
            vibrate(comp.compose())
        } else {
            val amp = (120 + 25 * combo).coerceAtMost(255)
            vibrate(VibrationEffect.createOneShot(30L + 15L * lines, amp))
        }
    }

    fun gameOver() {
        if (!enabled) return
        vibrate(VibrationEffect.createOneShot(220L, 200))
    }

    private fun vibrate(effect: VibrationEffect) {
        runCatching { vibrator?.vibrate(effect) }
    }
}
