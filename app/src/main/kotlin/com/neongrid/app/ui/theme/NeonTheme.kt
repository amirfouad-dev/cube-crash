package com.neongrid.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Neon cyberpunk palette. Block colors are indexed by the engine's colorId
 * (1-based; 0 = empty cell).
 */
object NeonTheme {
    val Background = Color(0xFF0A0A12)
    val BackgroundDeep = Color(0xFF12081F)
    val GridLine = Color(0x2600F0FF)      // cyan @ 15%
    val GridCellFill = Color(0x0DFFFFFF)  // barely-there cell tint
    val DangerPulse = Color(0xFFFF2B4E)
    val HudText = Color(0xFFB8F6FF)
    val HudDim = Color(0xFF5A7A8C)

    val Cyan = Color(0xFF00F0FF)
    val Magenta = Color(0xFFFF2BD6)
    val Violet = Color(0xFF9D4DFF)
    val Lime = Color(0xFFB4FF39)
    val Amber = Color(0xFFFFB800)

    /** colorId (1..5) → block color. Index 0 unused. */
    val blockColors = arrayOf(Color.Transparent, Cyan, Magenta, Violet, Lime, Amber)

    fun blockColor(colorId: Byte): Color =
        blockColors.getOrElse(colorId.toInt()) { Cyan }
}
