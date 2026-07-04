package com.neongrid.app.meta

import com.neongrid.app.data.PlayerProgress

data class StreakResult(
    val progress: PlayerProgress,
    /** Non-null when today extended the streak: shards granted. */
    val rewardShards: Int?,
)

object DailyStreakManager {

    /** Shards granted when the streak reaches these milestones. */
    private val MILESTONES = mapOf(3 to 50, 7 to 150, 14 to 300, 30 to 800)
    private const val DAILY_BONUS = 20

    /** Call once on first app-open of a session. Idempotent per day. */
    fun onAppOpened(progress: PlayerProgress, todayEpochDay: Long): StreakResult {
        if (progress.lastPlayedEpochDay == todayEpochDay) {
            return StreakResult(progress, null)
        }
        val newStreak = if (progress.lastPlayedEpochDay == todayEpochDay - 1) {
            progress.dailyStreak + 1
        } else {
            1
        }
        val reward = DAILY_BONUS + (MILESTONES[newStreak] ?: 0)
        return StreakResult(
            progress = progress.copy(
                dailyStreak = newStreak,
                bestDailyStreak = maxOf(progress.bestDailyStreak, newStreak),
                lastPlayedEpochDay = todayEpochDay,
                shards = progress.shards + reward,
            ),
            rewardShards = reward,
        )
    }
}
