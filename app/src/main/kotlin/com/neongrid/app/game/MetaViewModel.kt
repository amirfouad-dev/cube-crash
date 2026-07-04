package com.neongrid.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neongrid.app.data.PlayerProgress
import com.neongrid.app.data.ProgressRepository
import com.neongrid.app.data.Settings
import com.neongrid.app.data.SettingsRepository
import com.neongrid.app.meta.DailyStreakManager
import com.neongrid.app.meta.GameTheme
import com.neongrid.app.meta.MissionEngine
import com.neongrid.app.meta.MissionTemplates
import com.neongrid.app.meta.ThemeCatalog
import com.neongrid.app.meta.UnlockRule
import com.neongrid.engine.GameEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MetaViewModel(
    private val progressRepo: ProgressRepository,
    private val settingsRepo: SettingsRepository,
    gameEvents: Flow<GameEvent>,
) : ViewModel() {

    val progress = progressRepo.progress.stateIn(
        viewModelScope, SharingStarted.Eagerly, PlayerProgress(),
    )

    val settings = settingsRepo.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, Settings(),
    )

    val activeTheme = progressRepo.progress
        .map { ThemeCatalog.get(it.activeTheme) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeCatalog.DEFAULT)

    private val runTracker = MissionEngine.RunTracker()

    init {
        // Daily streak + mission rotation, once per app open.
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            progressRepo.update { p ->
                var next = DailyStreakManager.onAppOpened(p, today).progress
                if (next.missionsEpochDay != today) {
                    next = next.copy(
                        missionsEpochDay = today,
                        dailyMissions = MissionTemplates.dailyMissions(today, next.highScore),
                    )
                }
                next
            }
        }

        // Mission progress from live game events. The fold runs inside the
        // atomic updateData lambda — computing from the stateIn snapshot
        // would race and drop increments when events arrive back-to-back.
        viewModelScope.launch {
            gameEvents.collect { event ->
                runTracker.onEvent(event)
                val runScore = runTracker.runScore
                progressRepo.update { p ->
                    val updated = MissionEngine.apply(p.dailyMissions, event, runScore)
                    if (updated != p.dailyMissions) p.copy(dailyMissions = updated) else p
                }
            }
        }
    }

    fun claimMission(index: Int) {
        viewModelScope.launch {
            progressRepo.update { p ->
                val m = p.dailyMissions.getOrNull(index) ?: return@update p
                if (m.claimed || m.progress < m.target) return@update p
                p.copy(
                    shards = p.shards + m.rewardShards,
                    dailyMissions = p.dailyMissions.mapIndexed { i, ms ->
                        if (i == index) ms.copy(claimed = true) else ms
                    },
                )
            }
        }
    }

    fun isUnlocked(theme: GameTheme, p: PlayerProgress): Boolean =
        theme.id in p.unlockedThemes || when (val rule = theme.unlock) {
            is UnlockRule.Free -> true
            is UnlockRule.Streak -> p.bestDailyStreak >= rule.days
            is UnlockRule.Score -> p.highScore >= rule.points
            is UnlockRule.Shards -> false // must be purchased
        }

    fun selectTheme(theme: GameTheme) {
        viewModelScope.launch {
            progressRepo.update { p ->
                if (isUnlocked(theme, p)) p.copy(activeTheme = theme.id) else p
            }
        }
    }

    fun purchaseTheme(theme: GameTheme) {
        viewModelScope.launch {
            progressRepo.update { p ->
                val rule = theme.unlock
                if (rule is UnlockRule.Shards && theme.id !in p.unlockedThemes && p.shards >= rule.cost) {
                    p.copy(
                        shards = p.shards - rule.cost,
                        unlockedThemes = p.unlockedThemes + theme.id,
                        activeTheme = theme.id,
                    )
                } else {
                    p
                }
            }
        }
    }

    fun setSound(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.update { it.copy(soundEnabled = enabled) }
    }

    fun setMusic(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.update { it.copy(musicEnabled = enabled) }
    }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.update { it.copy(hapticsEnabled = enabled) }
    }

    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.update { it.copy(reduceMotion = enabled) }
    }

    fun setDifficulty(difficulty: com.neongrid.engine.Difficulty) = viewModelScope.launch {
        settingsRepo.update { it.copy(preferredDifficulty = difficulty.name) }
    }
}
