package com.neongrid.app.ui.hud

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun ScoreHud(
    score: Long,
    bestScore: Long,
    comboStreak: Int,
    modifier: Modifier = Modifier,
) {
    val animatedScore by animateIntAsState(
        targetValue = score.toInt(),
        animationSpec = tween(durationMillis = 350),
        label = "score",
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("SCORE", color = NeonTheme.HudDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "%,d".format(animatedScore),
                color = NeonTheme.HudText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (comboStreak > 1) {
            Text(
                text = "x$comboStreak COMBO",
                color = NeonTheme.Amber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("BEST", color = NeonTheme.HudDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "%,d".format(bestScore),
                color = NeonTheme.Violet,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
