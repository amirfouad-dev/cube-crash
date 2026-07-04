package com.neongrid.app.meta

import com.neongrid.app.data.MissionState
import com.neongrid.engine.GameEvent

/**
 * Pure fold: applies a game event to mission progress. Collected from the
 * GameViewModel event stream by MetaViewModel.
 */
object MissionEngine {

    /** Score accumulated in the current run (for REACH_SCORE). */
    class RunTracker {
        var runScore: Long = 0
            private set

        fun onEvent(event: GameEvent) {
            when (event) {
                is GameEvent.ScoreAwarded -> runScore += event.delta
                is GameEvent.GameOver -> runScore = 0
                else -> Unit
            }
        }
    }

    fun apply(missions: List<MissionState>, event: GameEvent, runScoreAfter: Long): List<MissionState> {
        if (missions.isEmpty()) return missions
        return missions.map { m ->
            if (m.claimed || m.progress >= m.target) return@map m
            val kind = MissionTemplates.byId[m.templateId]?.kind ?: return@map m
            val newProgress = when (kind) {
                MissionKind.CLEAR_LINES -> if (event is GameEvent.LinesCleared) {
                    m.progress + event.rowIdxs.size + event.colIdxs.size
                } else m.progress
                MissionKind.MULTI_CLEAR -> if (event is GameEvent.LinesCleared &&
                    event.rowIdxs.size + event.colIdxs.size >= 2
                ) m.progress + 1 else m.progress
                MissionKind.COMBO_STREAK -> if (event is GameEvent.ComboChanged) {
                    maxOf(m.progress, event.streak)
                } else m.progress
                MissionKind.PLACE_PIECES -> if (event is GameEvent.PiecePlaced) m.progress + 1 else m.progress
                MissionKind.PLAY_GAMES -> if (event is GameEvent.GameOver) m.progress + 1 else m.progress
                MissionKind.REACH_SCORE -> maxOf(m.progress, runScoreAfter.toInt())
            }
            m.copy(progress = newProgress.coerceAtMost(m.target))
        }
    }
}
