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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.neongrid.app.ui.theme.NeonTheme

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NeonTheme.Background, NeonTheme.BackgroundDeep)))
            .safeDrawingPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader("HOW TO PLAY", "", onBack)
        Spacer(Modifier.height(24.dp))

        Rule("👆", "DRAG", "Drag the active piece onto the 8×8 energy grid. The NEXT slot shows what's coming — plan two moves ahead.", NeonTheme.Cyan)
        Rule("🔄", "ROTATE", "Tap the active piece to rotate it 90°.", NeonTheme.Violet)
        Rule("⏱", "CATEGORIES", "BEGINNER is untimed. INTERMEDIATE eases you in with a 30s clock. ADVANCED starts at 20s, deals trickier shapes with no bailouts, and a careless placement can trap you. INSANE gives you 15s and NO rotation — every piece plays exactly as dealt.", NeonTheme.Amber)
        Rule("⚡", "DISCHARGE", "Fill a complete row or column to clear it in a neon surge. Multiple lines at once score big.", NeonTheme.Magenta)
        Rule("🔥", "COMBO", "Clear again within 3 placements to keep your combo alive — the multiplier climbs to ×3. The heat bar shows how close your streak is to dying.", NeonTheme.Amber)
        Rule("💠", "SHARDS", "Complete daily missions and keep your daily streak to earn shards — spend them on new grid themes.", NeonTheme.Lime)
        Rule("💀", "GRID FAILURE", "The run ends when the active piece no longer fits. BEGINNER, INTERMEDIATE and INSANE never hand you an impossible pair — play carefully and you always have a move. Only ADVANCED can trap you with a bad placement.", NeonTheme.DangerPulse)

        Spacer(Modifier.height(24.dp))
        Text(
            "TIP: leave room for long bars — and an empty board pays a +300 ALL CLEAR bonus.",
            color = NeonTheme.HudDim,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Rule(glyph: String, title: String, body: String, accent: Color) {
    Surface(
        color = Color(0x12FFFFFF),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(glyph, fontSize = 26.sp, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    color = NeonTheme.HudText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
