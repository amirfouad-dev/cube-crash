package com.neongrid.app.ui.hud

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.Scoring

/**
 * Neon heat bar: fills with combo streak, visibly drains as the streak
 * approaches death (3 non-clearing placements). The "keep the chain alive"
 * tension made visible.
 */
@Composable
fun ComboMeter(
    comboStreak: Int,
    placementsSinceClear: Int,
    modifier: Modifier = Modifier,
) {
    val heat = (comboStreak / 8f).coerceIn(0f, 1f)
    val grace = if (comboStreak > 0) {
        1f - placementsSinceClear / Scoring.STREAK_GRACE.toFloat()
    } else {
        0f
    }
    val animatedFill by animateFloatAsState(
        targetValue = heat * grace,
        animationSpec = tween(300),
        label = "comboFill",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(6.dp)) {
        drawRoundRect(
            color = NeonTheme.GridLine,
            cornerRadius = CornerRadius(size.height / 2),
        )
        if (animatedFill > 0f) {
            val w = size.width * animatedFill
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonTheme.Cyan, NeonTheme.Magenta, NeonTheme.Amber),
                    startX = 0f,
                    endX = size.width,
                ),
                size = Size(w, size.height),
                cornerRadius = CornerRadius(size.height / 2),
            )
            // additive glow cap at the leading edge
            drawCircle(
                color = NeonTheme.Amber.copy(alpha = 0.6f),
                radius = size.height * 1.2f,
                center = Offset(w, size.height / 2),
                blendMode = BlendMode.Plus,
            )
        }
    }
}
