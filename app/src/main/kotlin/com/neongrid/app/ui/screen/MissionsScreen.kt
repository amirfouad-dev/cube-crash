package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.data.MissionState
import com.neongrid.app.meta.MissionTemplates
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun MissionsScreen(
    missions: List<MissionState>,
    shards: Int,
    onClaim: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NeonTheme.Background, NeonTheme.BackgroundDeep)))
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        ScreenHeader("DAILY MISSIONS", "SHARDS $shards", onBack)
        Spacer(Modifier.height(24.dp))

        missions.forEachIndexed { index, mission ->
            MissionCard(mission, onClaim = { onClaim(index) })
            Spacer(Modifier.height(14.dp))
        }
        if (missions.isEmpty()) {
            Text(
                "Missions recalibrating…",
                color = NeonTheme.HudDim,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun MissionCard(mission: MissionState, onClaim: () -> Unit) {
    val template = MissionTemplates.byId[mission.templateId] ?: return
    val complete = mission.progress >= mission.target
    Surface(
        color = Color(0x14FFFFFF),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                template.describe(mission.target),
                color = NeonTheme.HudText,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (mission.progress.toFloat() / mission.target).coerceIn(0f, 1f) },
                    color = if (complete) NeonTheme.Lime else NeonTheme.Cyan,
                    trackColor = Color(0x22FFFFFF),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${mission.progress}/${mission.target}",
                    color = NeonTheme.HudDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "+${mission.rewardShards} SHARDS",
                    color = NeonTheme.Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                when {
                    mission.claimed -> Text(
                        "CLAIMED",
                        color = NeonTheme.HudDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    complete -> Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonTheme.Lime,
                            contentColor = NeonTheme.Background,
                        ),
                    ) {
                        Text("CLAIM", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "< $title",
            color = NeonTheme.Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickableNoRipple(onBack),
        )
        Text(subtitle, color = NeonTheme.Amber, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
