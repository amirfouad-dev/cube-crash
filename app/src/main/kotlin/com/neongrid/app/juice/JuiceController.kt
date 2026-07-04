package com.neongrid.app.juice

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import com.neongrid.app.ui.fx.ClearAnimator
import com.neongrid.app.ui.fx.ParticleSystem
import com.neongrid.app.ui.fx.ScreenShake
import com.neongrid.engine.Board
import com.neongrid.engine.GameEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

data class ScorePop(val text: String, val cellX: Float, val cellY: Float, val id: Long)

/**
 * Owns all FX state and translates GameEvents into juice: particles, shake,
 * clear ghosts, haptics, sound, slow-mo, score pops. Driven by a single
 * frame loop that suspends when nothing is animating.
 */
class JuiceController(
    val haptics: HapticsManager,
    val sound: SoundEngine,
) {
    val particles = ParticleSystem()
    val shake = ScreenShake()
    val clears = ClearAnimator()

    /** Invalidates the FX overlay each frame while active. */
    val frameTick = mutableLongStateOf(0L)

    val scorePops = mutableStateListOf<ScorePop>()
    private var popId = 0L

    private var slowMoRemaining = 0f
    private val fxActive = MutableStateFlow(false)
    private val flashScratch = ArrayList<Pair<Int, Byte>>()

    /** Board geometry, set by the screen before events flow. */
    var cellPx = 0f

    /** Accessibility: disables screen shake and slow-mo. */
    var reduceMotion = false

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.PiecePlaced -> {
                haptics.place()
                sound.place()
            }
            is GameEvent.LinesCleared -> {
                val lines = event.rowIdxs.size + event.colIdxs.size
                haptics.clear(lines, event.combo)
                sound.clear(lines, event.combo)
                if (!reduceMotion) shake.addTrauma(0.2f + 0.15f * lines)
                val snapshot = if (event.colorSnapshot.size == Board.CELLS) {
                    event.colorSnapshot
                } else {
                    ByteArray(Board.CELLS) { 1 }
                }
                clears.push(event.clearedCells, snapshot, event.originCell, big = lines >= 3)
                if (lines >= 3 && !reduceMotion) slowMoRemaining = 0.4f
                wake()
            }
            is GameEvent.ComboChanged -> {
                if (event.streak == 0) sound.comboBreak()
            }
            is GameEvent.PieceRotated -> {
                haptics.tick()
                sound.rotate()
            }
            is GameEvent.AllClear -> {
                sound.allClear()
                // Full-board radial burst.
                val center = Board.SIZE / 2f * cellPx
                particles.emit(center, center, 150, 5, 200f, 900f, cellPx * 0.12f, lifeSec = 1.0f)
                if (!reduceMotion) shake.addTrauma(0.5f)
                wake()
            }
            is GameEvent.GameOver -> {
                haptics.gameOver()
                sound.gameOver()
            }
            is GameEvent.ScoreAwarded -> {
                if (event.delta >= 100 && event.atCells != 0L) {
                    val i = java.lang.Long.numberOfTrailingZeros(event.atCells)
                    scorePops.add(
                        ScorePop(
                            text = "+${event.delta}",
                            cellX = (i % Board.SIZE + 0.5f),
                            cellY = (i / Board.SIZE + 0.5f),
                            id = popId++,
                        ),
                    )
                }
            }
            is GameEvent.TrayRefilled -> Unit
        }
    }

    fun anchorTick() = haptics.tick()

    /** One FX step; returns true while anything is still animating. */
    fun update(dtSec: Float): Boolean {
        val dt = if (slowMoRemaining > 0f) {
            slowMoRemaining -= dtSec
            dtSec * 0.5f
        } else {
            dtSec
        }
        // Emit shatter particles for cells hitting their flash moment.
        flashScratch.clear()
        clears.collectFlashingCells(dt, flashScratch)
        for ((cell, color) in flashScratch) {
            val cx = (cell % Board.SIZE + 0.5f) * cellPx
            val cy = (cell / Board.SIZE + 0.5f) * cellPx
            particles.emit(cx, cy, 8, color, 60f, 340f, cellPx * 0.10f)
        }
        clears.update(dt)
        particles.update(dt)
        shake.update(dtSec, maxOffsetPx = cellPx * 0.28f)
        val active = particles.alive > 0 || clears.active || shake.trauma > 0f || slowMoRemaining > 0f
        if (!active) fxActive.value = false
        return active
    }

    suspend fun awaitActive() {
        fxActive.first { it }
    }

    private fun wake() {
        fxActive.value = true
    }
}
