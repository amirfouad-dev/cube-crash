package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun GameOverOverlay(
    score: Long,
    bestScore: Long,
    maxCombo: Int,
    linesCleared: Int,
    onRestart: () -> Unit,
    onHome: () -> Unit = {},
    timedOut: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE60A0A12)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (timedOut) "TIME OUT" else "GRID FAILURE",
            color = NeonTheme.DangerPulse,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "%,d".format(score),
            color = NeonTheme.HudText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        if (score >= bestScore && score > 0) {
            Text("NEW BEST", color = NeonTheme.Amber, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        } else {
            Text(
                "BEST %,d".format(bestScore),
                color = NeonTheme.HudDim,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "MAX COMBO x$maxCombo   LINES $linesCleared",
            color = NeonTheme.HudDim,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonTheme.Cyan,
                contentColor = NeonTheme.Background,
            ),
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Text(
                "REBOOT GRID",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "HOME",
            color = NeonTheme.HudDim,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clickable { onHome() }
                .padding(12.dp),
        )
    }
}
