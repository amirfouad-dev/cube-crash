package com.neongrid.app.meta

import androidx.compose.ui.graphics.Color

sealed interface UnlockRule {
    data object Free : UnlockRule
    data class Shards(val cost: Int) : UnlockRule
    data class Streak(val days: Int) : UnlockRule
    data class Score(val points: Long) : UnlockRule
}

/**
 * A cosmetic theme: 5 block colors (indexed by the engine's colorId 1..5)
 * plus background/grid tints.
 */
data class GameTheme(
    val id: String,
    val name: String,
    val blocks: List<Color>,
    val background: Color = Color(0xFF0A0A12),
    val backgroundDeep: Color = Color(0xFF12081F),
    val gridLine: Color = Color(0x2600F0FF),
    val unlock: UnlockRule,
) {
    fun blockColor(colorId: Byte): Color =
        blocks.getOrElse(colorId.toInt() - 1) { blocks[0] }
}

object ThemeCatalog {

    val DEFAULT = GameTheme(
        id = "neon",
        name = "Neon Grid",
        blocks = listOf(
            Color(0xFF00F0FF), Color(0xFFFF2BD6), Color(0xFF9D4DFF),
            Color(0xFFB4FF39), Color(0xFFFFB800),
        ),
        unlock = UnlockRule.Free,
    )

    val ALL: List<GameTheme> = listOf(
        DEFAULT,
        GameTheme(
            id = "vaporwave",
            name = "Vaporwave",
            blocks = listOf(
                Color(0xFFFF71CE), Color(0xFF01CDFE), Color(0xFFB967FF),
                Color(0xFF05FFA1), Color(0xFFFFFB96),
            ),
            background = Color(0xFF120E1E),
            backgroundDeep = Color(0xFF1D0B2E),
            gridLine = Color(0x26FF71CE),
            unlock = UnlockRule.Streak(7),
        ),
        GameTheme(
            id = "matrix",
            name = "Matrix",
            blocks = listOf(
                Color(0xFF00FF41), Color(0xFF7FFF9E), Color(0xFF00B32D),
                Color(0xFFCCFFCC), Color(0xFF39FF14),
            ),
            background = Color(0xFF050A05),
            backgroundDeep = Color(0xFF0A140A),
            gridLine = Color(0x2600FF41),
            unlock = UnlockRule.Shards(400),
        ),
        GameTheme(
            id = "hologram",
            name = "Hologram",
            blocks = listOf(
                Color(0xFF7DF9FF), Color(0xFFB3E5FF), Color(0xFF80DEEA),
                Color(0xFFE0F7FF), Color(0xFF4DD0E1),
            ),
            background = Color(0xFF06121A),
            backgroundDeep = Color(0xFF0A1D2B),
            gridLine = Color(0x267DF9FF),
            unlock = UnlockRule.Shards(600),
        ),
        GameTheme(
            id = "sunset_drive",
            name = "Sunset Drive",
            blocks = listOf(
                Color(0xFFFF5F6D), Color(0xFFFFC371), Color(0xFFFF2BD6),
                Color(0xFFFF9E00), Color(0xFFC471ED),
            ),
            background = Color(0xFF16060F),
            backgroundDeep = Color(0xFF250A18),
            gridLine = Color(0x26FF5F6D),
            unlock = UnlockRule.Shards(600),
        ),
        GameTheme(
            id = "reactor_core",
            name = "Reactor Core",
            blocks = listOf(
                Color(0xFFFFE600), Color(0xFFFF6B00), Color(0xFFFF2200),
                Color(0xFFFFA200), Color(0xFFFFF176),
            ),
            background = Color(0xFF130B02),
            backgroundDeep = Color(0xFF201004),
            gridLine = Color(0x26FFA200),
            unlock = UnlockRule.Shards(1200),
        ),
        GameTheme(
            id = "deep_space",
            name = "Deep Space",
            blocks = listOf(
                Color(0xFF6C8CFF), Color(0xFFB388FF), Color(0xFF448AFF),
                Color(0xFF8C9EFF), Color(0xFFEA80FC),
            ),
            background = Color(0xFF05060F),
            backgroundDeep = Color(0xFF0A0C1E),
            gridLine = Color(0x266C8CFF),
            unlock = UnlockRule.Score(25_000),
        ),
        GameTheme(
            id = "ghost_protocol",
            name = "Ghost Protocol",
            blocks = listOf(
                Color(0xFFE0E0E0), Color(0xFFB0BEC5), Color(0xFF90A4AE),
                Color(0xFFF5F5F5), Color(0xFFCFD8DC),
            ),
            background = Color(0xFF0A0A0D),
            backgroundDeep = Color(0xFF121218),
            gridLine = Color(0x26E0E0E0),
            unlock = UnlockRule.Streak(30),
        ),
    )

    val byId: Map<String, GameTheme> = ALL.associateBy { it.id }

    fun get(id: String): GameTheme = byId[id] ?: DEFAULT
}
