package com.neongrid.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neongrid.app.data.GameStateRepository
import com.neongrid.app.data.ProgressRepository
import com.neongrid.app.data.SettingsRepository
import com.neongrid.app.data.toGameState
import com.neongrid.app.data.toSnapshot
import com.neongrid.engine.Difficulty
import com.neongrid.engine.GameAction
import com.neongrid.engine.GameEngine
import com.neongrid.engine.GameEvent
import com.neongrid.engine.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameStateRepo: GameStateRepository,
    private val progressRepo: ProgressRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /** Seconds left to place the active piece; null in untimed play. */
    private val _timerSecondsLeft = MutableStateFlow<Float?>(null)
    val timerSecondsLeft: StateFlow<Float?> = _timerSecondsLeft.asStateFlow()

    /** True while the game screen is visible and the activity is started. */
    private val timerGateOpen = MutableStateFlow(false)

    private var timerJob: Job? = null
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            val snapshot = gameStateRepo.read()
            val best = progressRepo.read().highScore
            // Old snapshots (pre-difficulty 3-piece trays) can't be resumed.
            if (snapshot.exists && !snapshot.isGameOver && snapshot.tray.size == GameState.TRAY_SIZE) {
                val difficulty = Difficulty.fromId(snapshot.difficulty)
                // Live run may already exceed the persisted (game-over) best.
                _uiState.value = GameUiState.Playing(
                    snapshot.toGameState(),
                    maxOf(best, snapshot.score),
                    isNewBest = false,
                    difficulty = difficulty,
                )
                // The saved deadline is gone; give the piece a fresh clock.
                restartTimer(difficulty, snapshot.score)
            } else {
                startNewGame()
            }
        }
    }

    fun startNewGame(difficulty: Difficulty? = null) {
        viewModelScope.launch {
            val best = progressRepo.read().highScore
            val chosen = difficulty
                ?: Difficulty.fromId(settingsRepo.settings.first().preferredDifficulty)
            val t = GameEngine.newGame(System.nanoTime(), chosen)
            _uiState.value = GameUiState.Playing(t.state, best, isNewBest = false, difficulty = chosen)
            t.events.forEach { _events.tryEmit(it) }
            restartTimer(chosen, t.state.score)
            persist(t.state, chosen, immediate = true)
        }
    }

    fun dispatch(action: GameAction) {
        val current = _uiState.value as? GameUiState.Playing ?: return
        if (action is GameAction.NewGame) { startNewGame(); return }
        val t = GameEngine.reduce(current.gameState, action, current.difficulty)
        if (t.state === current.gameState) return

        val newBest = maxOf(current.bestScore, t.state.score)
        _uiState.value = current.copy(
            gameState = t.state,
            bestScore = newBest,
            isNewBest = t.state.score > 0 && t.state.score >= newBest && current.bestScore > 0,
            timedOut = current.timedOut || (action is GameAction.TimeUp && t.state.isGameOver),
        )
        t.events.forEach { _events.tryEmit(it) }

        if (t.state.isGameOver) {
            stopTimer()
            onGameOver(t.state)
            persist(t.state, current.difficulty, immediate = true)
        } else {
            // A successful placement dealt a new active piece: fresh clock.
            if (t.state.piecesPlaced != current.gameState.piecesPlaced) {
                restartTimer(current.difficulty, t.state.score)
            }
            persist(t.state, current.difficulty, immediate = false)
        }
    }

    /** Gate from the UI: countdown runs only while the game is on screen. */
    fun setTimerGateOpen(open: Boolean) {
        timerGateOpen.value = open
    }

    private fun restartTimer(difficulty: Difficulty, score: Long) {
        timerJob?.cancel()
        val total = difficulty.timerSecondsAt(score)
        if (total == null) {
            _timerSecondsLeft.value = null
            return
        }
        _timerSecondsLeft.value = total.toFloat()
        timerJob = viewModelScope.launch {
            var last = System.nanoTime()
            while (true) {
                if (!timerGateOpen.value) {
                    timerGateOpen.first { it } // suspend while paused
                    last = System.nanoTime()
                }
                delay(TICK_MS)
                val now = System.nanoTime()
                val dt = (now - last) / 1_000_000_000f
                last = now
                val left = ((_timerSecondsLeft.value ?: return@launch) - dt)
                if (left <= 0f) {
                    _timerSecondsLeft.value = 0f
                    dispatch(GameAction.TimeUp)
                    return@launch
                }
                _timerSecondsLeft.value = left
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onGameOver(state: GameState) {
        viewModelScope.launch {
            progressRepo.update { p ->
                p.copy(
                    highScore = maxOf(p.highScore, state.score),
                    gamesPlayed = p.gamesPlayed + 1,
                    bestCombo = maxOf(p.bestCombo, state.maxCombo),
                    lifetimeLines = p.lifetimeLines + state.linesClearedTotal,
                )
            }
        }
    }

    /** Debounced persistence; immediate on game over. */
    private fun persist(state: GameState, difficulty: Difficulty, immediate: Boolean) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            if (!immediate) delay(300)
            gameStateRepo.save(state.toSnapshot(difficulty.name))
        }
    }

    /** Called from ON_STOP so a swipe-kill can't lose the run. */
    fun persistNow() {
        val current = _uiState.value as? GameUiState.Playing ?: return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            gameStateRepo.save(current.gameState.toSnapshot(current.difficulty.name))
        }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
