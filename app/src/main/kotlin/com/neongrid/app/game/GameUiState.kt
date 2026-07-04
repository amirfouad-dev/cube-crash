package com.neongrid.app.game

import com.neongrid.engine.Difficulty
import com.neongrid.engine.GameState

sealed interface GameUiState {
    data object Loading : GameUiState
    data class Playing(
        val gameState: GameState,
        val bestScore: Long,
        val isNewBest: Boolean,
        val difficulty: Difficulty = Difficulty.BEGINNER,
        /** True when the run ended because the placement timer expired. */
        val timedOut: Boolean = false,
    ) : GameUiState
}
