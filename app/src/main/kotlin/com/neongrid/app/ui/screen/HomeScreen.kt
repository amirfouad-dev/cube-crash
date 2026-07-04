package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.data.PlayerProgress
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.Difficulty

@Composable
fun HomeScreen(
    progress: PlayerProgress,
    hasActiveRun: Boolean,
    activeRunDifficulty: Difficulty?,
    selectedDifficulty: Difficulty,
    onSelectDifficulty: (Difficulty) -> Unit,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onHowToPlay: () -> Unit,
    onMissions: () -> Unit,
    onThemes: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NeonTheme.Background, NeonTheme.BackgroundDeep)),
            )
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            "CUBE",
            color = NeonTheme.Cyan,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "CRASH",
            color = NeonTheme.Magenta,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatChip("BEST", "%,d".format(progress.highScore), NeonTheme.Violet)
            Spacer(Modifier.width(16.dp))
            StatChip("STREAK", "${progress.dailyStreak}d", NeonTheme.Amber)
            Spacer(Modifier.width(16.dp))
            StatChip("SHARDS", "${progress.shards}", NeonTheme.Cyan)
        }

        Spacer(Modifier.height(28.dp))

        DifficultySelector(selectedDifficulty, onSelectDifficulty)

        Spacer(Modifier.height(28.dp))

        if (hasActiveRun) {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTheme.Cyan,
                    contentColor = NeonTheme.Background,
                ),
            ) {
                Text(
                    "CONTINUE" + (activeRunDifficulty?.let { " · ${it.name}" } ?: ""),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNewGame,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTheme.Magenta),
            ) {
                Text(
                    "NEW GAME · ${selectedDifficulty.name}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        } else {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonTheme.Cyan,
                    contentColor = NeonTheme.Background,
                ),
            ) {
                Text(
                    "BOOT GRID",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = onHowToPlay,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTheme.Amber),
        ) {
            Text(
                "HOW TO PLAY",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton("MISSIONS", onMissions)
            MenuButton("THEMES", onThemes)
            MenuButton("SETTINGS", onSettings)
        }

        Spacer(Modifier.weight(1f))
        Text(
            "GAMES ${progress.gamesPlayed}  ·  LINES ${progress.lifetimeLines}  ·  MAX COMBO x${progress.bestCombo}",
            color = NeonTheme.HudDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { difficulty ->
                val isSelected = difficulty == selected
                val accent = when (difficulty) {
                    Difficulty.BEGINNER -> NeonTheme.Cyan
                    Difficulty.INTERMEDIATE -> NeonTheme.Amber
                    Difficulty.ADVANCED -> NeonTheme.Magenta
                }
                Text(
                    difficulty.name,
                    color = if (isSelected) accent else NeonTheme.HudDim,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accent else NeonTheme.GridLine,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(difficulty) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (selected) {
                Difficulty.BEGINNER -> "CLASSIC · NO TIMER"
                Difficulty.INTERMEDIATE -> "${Difficulty.TIMER_BASE_SECONDS}S TO PLACE EACH PIECE"
                Difficulty.ADVANCED -> "TIMER DROPS 1S EVERY ${"%,d".format(Difficulty.POINTS_PER_TIMER_STEP)} PTS"
            },
            color = NeonTheme.HudDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = NeonTheme.HudDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MenuButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTheme.HudText),
    ) {
        Text(label, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
