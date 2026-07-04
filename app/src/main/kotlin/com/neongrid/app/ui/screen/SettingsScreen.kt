package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neongrid.app.data.Settings
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun SettingsScreen(
    settings: Settings,
    onSound: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NeonTheme.Background, NeonTheme.BackgroundDeep)))
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        ScreenHeader("SETTINGS", "", onBack)
        Spacer(Modifier.height(28.dp))

        SettingRow("SOUND FX", settings.soundEnabled, onSound)
        SettingRow("MUSIC", settings.musicEnabled, onMusic)
        SettingRow("HAPTICS", settings.hapticsEnabled, onHaptics)
        SettingRow("REDUCE MOTION", settings.reduceMotion, onReduceMotion)

        Spacer(Modifier.height(32.dp))
        Text(
            "NeonGrid stores everything on this device.\nNo ads. No tracking. No network.",
            color = NeonTheme.HudDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = NeonTheme.HudText,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NeonTheme.Cyan,
                checkedThumbColor = NeonTheme.Background,
            ),
        )
    }
}
