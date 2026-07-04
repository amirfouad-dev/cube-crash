package com.neongrid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neongrid.app.game.GameViewModel
import com.neongrid.app.game.MetaViewModel
import com.neongrid.app.ui.nav.AppNav

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as NeonGridApp).container

        setContent {
            val gameViewModel: GameViewModel = viewModel(
                factory = factoryFor {
                    GameViewModel(
                        container.gameStateRepository,
                        container.progressRepository,
                        container.settingsRepository,
                    )
                },
            )
            val metaViewModel: MetaViewModel = viewModel(
                factory = factoryFor {
                    MetaViewModel(
                        container.progressRepository,
                        container.settingsRepository,
                        gameViewModel.events,
                    )
                },
            )

            // Persist on background (swipe-kill never loses the run) and
            // pause/resume the music with the activity.
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            gameViewModel.persistNow()
                            container.musicPlayer.pause()
                        }
                        Lifecycle.Event.ON_START -> container.musicPlayer.play()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            // Music on/off follows the settings toggle live.
            val settings by metaViewModel.settings.collectAsState()
            LaunchedEffect(settings.musicEnabled) {
                container.musicPlayer.enabled = settings.musicEnabled
            }

            AppNav(gameViewModel, metaViewModel)
        }
    }
}

private inline fun <reified VM : ViewModel> factoryFor(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
