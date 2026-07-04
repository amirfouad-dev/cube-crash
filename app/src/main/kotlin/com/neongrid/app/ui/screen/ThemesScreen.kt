package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.neongrid.app.data.PlayerProgress
import com.neongrid.app.meta.GameTheme
import com.neongrid.app.meta.ThemeCatalog
import com.neongrid.app.meta.UnlockRule
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun ThemesScreen(
    progress: PlayerProgress,
    isUnlocked: (GameTheme) -> Boolean,
    onSelect: (GameTheme) -> Unit,
    onPurchase: (GameTheme) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NeonTheme.Background, NeonTheme.BackgroundDeep)))
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        ScreenHeader("THEMES", "SHARDS ${progress.shards}", onBack)
        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ThemeCatalog.ALL, key = { it.id }) { theme ->
                val unlocked = isUnlocked(theme)
                val active = progress.activeTheme == theme.id
                ThemeCard(
                    theme = theme,
                    unlocked = unlocked,
                    active = active,
                    onClick = {
                        when {
                            unlocked -> onSelect(theme)
                            theme.unlock is UnlockRule.Shards -> onPurchase(theme)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(theme: GameTheme, unlocked: Boolean, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) Color(0x2200F0FF) else Color(0x14FFFFFF),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    theme.name.uppercase(),
                    color = if (unlocked) NeonTheme.HudText else NeonTheme.HudDim,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    theme.blocks.forEach { c ->
                        Surface(
                            color = if (unlocked) c else c.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.size(22.dp),
                        ) {}
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
            Text(
                text = when {
                    active -> "ACTIVE"
                    unlocked -> "SELECT"
                    else -> when (val rule = theme.unlock) {
                        is UnlockRule.Shards -> "${rule.cost} SHARDS"
                        is UnlockRule.Streak -> "${rule.days}-DAY STREAK"
                        is UnlockRule.Score -> "SCORE %,d".format(rule.points)
                        is UnlockRule.Free -> "SELECT"
                    }
                },
                color = when {
                    active -> NeonTheme.Cyan
                    unlocked -> NeonTheme.Lime
                    else -> NeonTheme.Amber
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
