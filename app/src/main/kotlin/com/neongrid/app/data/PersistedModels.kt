package com.neongrid.app.data

import com.neongrid.engine.GameState
import com.neongrid.engine.TrayPiece
import kotlinx.serialization.Serializable

@Serializable
data class TrayPieceSnap(val pieceId: Int, val colorId: Byte)

@Serializable
data class GameSnapshot(
    val board: Long = 0L,
    val cellColors: ByteArray = ByteArray(64),
    val tray: List<TrayPieceSnap?> = emptyList(),
    val score: Long = 0,
    val comboStreak: Int = 0,
    val placementsSinceClear: Int = 0,
    val piecesPlaced: Int = 0,
    val linesClearedTotal: Int = 0,
    val maxCombo: Int = 0,
    val rngState: Long = 0,
    val pressure: Int = 0,
    val isGameOver: Boolean = false,
    /** [com.neongrid.engine.Difficulty] name this run was started with. */
    val difficulty: String = "BEGINNER",
    /** False for the pristine default instance — nothing to resume. */
    val exists: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        other is GameSnapshot && other.board == board && other.score == score &&
            other.rngState == rngState && other.tray == tray && other.exists == exists &&
            other.cellColors.contentEquals(cellColors) && other.isGameOver == isGameOver &&
            other.difficulty == difficulty
    override fun hashCode(): Int = 31 * board.hashCode() + score.hashCode()
}

fun GameState.toSnapshot(difficulty: String) = GameSnapshot(
    difficulty = difficulty,
    board = board,
    cellColors = cellColors,
    tray = tray.map { it?.let { p -> TrayPieceSnap(p.pieceId, p.colorId) } },
    score = score,
    comboStreak = comboStreak,
    placementsSinceClear = placementsSinceClear,
    piecesPlaced = piecesPlaced,
    linesClearedTotal = linesClearedTotal,
    maxCombo = maxCombo,
    rngState = rngState,
    pressure = pressure,
    isGameOver = isGameOver,
    exists = true,
)

fun GameSnapshot.toGameState() = GameState(
    board = board,
    cellColors = cellColors,
    tray = tray.map { it?.let { p -> TrayPiece(p.pieceId, p.colorId) } },
    score = score,
    comboStreak = comboStreak,
    placementsSinceClear = placementsSinceClear,
    piecesPlaced = piecesPlaced,
    linesClearedTotal = linesClearedTotal,
    maxCombo = maxCombo,
    rngState = rngState,
    pressure = pressure,
    isGameOver = isGameOver,
)

@Serializable
data class MissionState(
    val templateId: String,
    val target: Int,
    val progress: Int = 0,
    val rewardShards: Int,
    val claimed: Boolean = false,
)

@Serializable
data class PlayerProgress(
    val highScore: Long = 0,
    val gamesPlayed: Int = 0,
    val bestCombo: Int = 0,
    val lifetimeLines: Long = 0,
    // Daily streak
    val dailyStreak: Int = 0,
    val lastPlayedEpochDay: Long = 0,
    val bestDailyStreak: Int = 0,
    // Earned-only currency (no IAP)
    val shards: Int = 0,
    // Themes
    val unlockedThemes: Set<String> = setOf("neon"),
    val activeTheme: String = "neon",
    // Daily missions
    val missionsEpochDay: Long = 0,
    val dailyMissions: List<MissionState> = emptyList(),
)

@Serializable
data class Settings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    /** [com.neongrid.engine.Difficulty] name used for new games. */
    val preferredDifficulty: String = "BEGINNER",
)
