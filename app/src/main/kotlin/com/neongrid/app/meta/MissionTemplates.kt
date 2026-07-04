package com.neongrid.app.meta

import com.neongrid.app.data.MissionState
import com.neongrid.engine.GameRng

enum class MissionKind { CLEAR_LINES, REACH_SCORE, COMBO_STREAK, PLACE_PIECES, MULTI_CLEAR, PLAY_GAMES }

data class MissionTemplate(
    val id: String,
    val kind: MissionKind,
    /** target given the player's high score (difficulty scaling). */
    val target: (highScore: Long) -> Int,
    val rewardShards: Int,
    val describe: (target: Int) -> String,
)

object MissionTemplates {

    val ALL: List<MissionTemplate> = listOf(
        MissionTemplate(
            "clear_lines", MissionKind.CLEAR_LINES,
            target = { hs -> if (hs < 3_000) 8 else 15 },
            rewardShards = 60,
            describe = { "Complete $it circuits (clear lines)" },
        ),
        MissionTemplate(
            "reach_score", MissionKind.REACH_SCORE,
            target = { hs -> ((hs * 0.6).toInt().coerceAtLeast(800) / 100) * 100 },
            rewardShards = 80,
            describe = { "Score $it in a single run" },
        ),
        MissionTemplate(
            "combo_streak", MissionKind.COMBO_STREAK,
            target = { hs -> if (hs < 5_000) 3 else 5 },
            rewardShards = 100,
            describe = { "Reach a x$it combo" },
        ),
        MissionTemplate(
            "place_pieces", MissionKind.PLACE_PIECES,
            target = { _ -> 60 },
            rewardShards = 40,
            describe = { "Deploy $it energy cells (place pieces)" },
        ),
        MissionTemplate(
            "multi_clear", MissionKind.MULTI_CLEAR,
            target = { _ -> 3 },
            rewardShards = 90,
            describe = { "Trigger $it multi-line surges (2+ lines at once)" },
        ),
        MissionTemplate(
            "play_games", MissionKind.PLAY_GAMES,
            target = { _ -> 3 },
            rewardShards = 40,
            describe = { "Boot the grid $it times (play games)" },
        ),
    )

    val byId: Map<String, MissionTemplate> = ALL.associateBy { it.id }

    /** Deterministic daily pick of 3 distinct templates. */
    fun dailyMissions(epochDay: Long, highScore: Long): List<MissionState> {
        val rng = GameRng(epochDay * 0x9E3779B97F4A7C15uL.toLong() + 1)
        val pool = ALL.toMutableList()
        return List(3) {
            val template = pool.removeAt(rng.nextInt(pool.size))
            MissionState(
                templateId = template.id,
                target = template.target(highScore),
                rewardShards = template.rewardShards,
            )
        }
    }
}
