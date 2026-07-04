package com.neongrid.app.di

import android.content.Context
import com.neongrid.app.data.GameStateRepository
import com.neongrid.app.data.ProgressRepository
import com.neongrid.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI — three repos don't justify Hilt. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val gameStateRepository = GameStateRepository(context, appScope)
    val progressRepository = ProgressRepository(context, appScope)
    val settingsRepository = SettingsRepository(context, appScope)
    val musicPlayer = com.neongrid.app.juice.MusicPlayer()
}
