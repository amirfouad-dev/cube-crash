package com.neongrid.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.neongrid.app.game.DragController
import com.neongrid.app.game.GameUiState
import com.neongrid.app.game.GameViewModel
import com.neongrid.app.juice.HapticsManager
import com.neongrid.app.juice.JuiceController
import com.neongrid.app.juice.SoundEngine
import com.neongrid.app.ui.board.BoardCanvas
import com.neongrid.app.ui.board.DraggedPieceOverlay
import com.neongrid.app.ui.board.TrayRow
import com.neongrid.app.ui.fx.FxOverlay
import com.neongrid.app.ui.fx.ScorePopups
import com.neongrid.app.ui.hud.ComboMeter
import com.neongrid.app.ui.hud.ScoreHud
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.GameAction
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    metaViewModel: com.neongrid.app.game.MetaViewModel,
    onHome: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme by metaViewModel.activeTheme.collectAsState()
    val settings by metaViewModel.settings.collectAsState()
    val dragController = remember { DragController() }
    val context = LocalContext.current
    val juice = remember { JuiceController(HapticsManager(context), SoundEngine()) }

    // Anchor-change tick haptic.
    LaunchedEffect(Unit) {
        dragController.onAnchorChanged = { juice.anchorTick() }
    }

    // The placement countdown only runs while this screen is visible and
    // the activity is started (backgrounding pauses the clock).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> viewModel.setTimerGateOpen(true)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> viewModel.setTimerGateOpen(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setTimerGateOpen(
            lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED),
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setTimerGateOpen(false)
        }
    }

    // Apply user settings to the juice layer.
    LaunchedEffect(settings) {
        juice.haptics.enabled = settings.hapticsEnabled
        juice.sound.enabled = settings.soundEnabled
        juice.reduceMotion = settings.reduceMotion
    }

    // Event pipeline: engine events -> juice.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            juice.cellPx = dragController.cellSizePx
            juice.onEvent(event)
        }
    }

    // Single FX frame loop; suspends completely while idle.
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            val stillActive = withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                val active = juice.update(dt)
                juice.frameTick.longValue = now
                active
            }
            if (!stillActive) {
                last = 0L
                juice.awaitActive()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.background, theme.backgroundDeep),
                ),
            ),
    ) {
        when (val state = uiState) {
            is GameUiState.Loading -> Unit
            is GameUiState.Playing -> {
                val game = state.gameState
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .graphicsLayer {
                            // Trauma shake; reads frame tick in the layer
                            // block so idle frames cost nothing.
                            @Suppress("UNUSED_EXPRESSION")
                            juice.frameTick.longValue
                            translationX = juice.shake.offsetX
                            translationY = juice.shake.offsetY
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))
                    ScoreHud(
                        score = game.score,
                        bestScore = state.bestScore,
                        comboStreak = game.comboStreak,
                    )
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Text(
                            text = "⌂",
                            color = com.neongrid.app.ui.theme.NeonTheme.Cyan,
                            fontSize = androidx.compose.ui.unit.TextUnit(
                                22f, androidx.compose.ui.unit.TextUnitType.Sp,
                            ),
                            modifier = Modifier.clickable { onHome() },
                        )
                        Spacer(Modifier.width(14.dp))
                        ComboMeter(
                            comboStreak = game.comboStreak,
                            placementsSinceClear = game.placementsSinceClear,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(14.dp))
                        androidx.compose.material3.Text(
                            text = if (settings.musicEnabled) "♪" else "♪̶",
                            color = if (settings.musicEnabled) {
                                com.neongrid.app.ui.theme.NeonTheme.Cyan
                            } else {
                                com.neongrid.app.ui.theme.NeonTheme.HudDim
                            },
                            fontSize = androidx.compose.ui.unit.TextUnit(
                                20f, androidx.compose.ui.unit.TextUnitType.Sp,
                            ),
                            modifier = Modifier.clickable {
                                metaViewModel.setMusic(!settings.musicEnabled)
                            },
                        )
                    }
                    if (state.difficulty != com.neongrid.engine.Difficulty.BEGINNER) {
                        val secondsLeft by viewModel.timerSecondsLeft.collectAsState()
                        val total = state.difficulty.timerSecondsAt(game.score)
                        if (secondsLeft != null && total != null) {
                            Spacer(Modifier.height(10.dp))
                            com.neongrid.app.ui.hud.TimerBar(
                                secondsLeft = secondsLeft!!,
                                totalSeconds = total,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        BoardCanvas(
                            board = game.board,
                            cellColors = game.cellColors,
                            dragController = dragController,
                            theme = theme,
                        )
                        FxOverlay(juice, theme)
                        ScorePopups(juice)
                    }
                    Spacer(Modifier.height(24.dp))
                    TrayRow(
                        tray = game.tray,
                        board = game.board,
                        dragController = dragController,
                        theme = theme,
                        onDrop = { slot, row, col ->
                            viewModel.dispatch(GameAction.Place(slot, row, col))
                        },
                        onRotate = { slot ->
                            viewModel.dispatch(GameAction.Rotate(slot))
                        },
                    )
                }

                // Full-screen overlay so the dragged piece floats over
                // both the board and the tray.
                DraggedPieceOverlay(dragController, theme, modifier = Modifier.zIndex(2f))

                if (game.isGameOver) {
                    GameOverOverlay(
                        score = game.score,
                        bestScore = state.bestScore,
                        maxCombo = game.maxCombo,
                        linesCleared = game.linesClearedTotal,
                        timedOut = state.timedOut,
                        onRestart = { viewModel.startNewGame(state.difficulty) },
                        onHome = onHome,
                        modifier = Modifier.zIndex(3f),
                    )
                }
            }
        }
    }
}
