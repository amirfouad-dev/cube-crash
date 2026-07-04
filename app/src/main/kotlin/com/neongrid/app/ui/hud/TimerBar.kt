package com.neongrid.app.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.ui.theme.NeonTheme
import kotlin.math.ceil

/**
 * Timed-mode countdown: seconds remaining plus a draining neon bar.
 * Cyan → amber below half → danger red in the final 5 seconds.
 */
@Composable
fun TimerBar(
    secondsLeft: Float,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (totalSeconds > 0) (secondsLeft / totalSeconds).coerceIn(0f, 1f) else 0f
    val color = when {
        secondsLeft <= 5f -> NeonTheme.DangerPulse
        fraction < 0.5f -> NeonTheme.Amber
        else -> NeonTheme.Cyan
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "%02d".format(ceil(secondsLeft.toDouble()).toInt()),
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NeonTheme.GridLine),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
