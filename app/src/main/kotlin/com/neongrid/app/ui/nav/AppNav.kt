package com.neongrid.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neongrid.app.game.GameUiState
import com.neongrid.app.game.GameViewModel
import com.neongrid.app.game.MetaViewModel
import com.neongrid.app.ui.screen.GameScreen
import com.neongrid.app.ui.screen.HomeScreen
import com.neongrid.app.ui.screen.HowToPlayScreen
import com.neongrid.app.ui.screen.MissionsScreen
import com.neongrid.app.ui.screen.SettingsScreen
import com.neongrid.app.ui.screen.ThemesScreen

@Composable
fun AppNav(gameViewModel: GameViewModel, metaViewModel: MetaViewModel) {
    val nav = rememberNavController()
    val progress by metaViewModel.progress.collectAsState()
    val settings by metaViewModel.settings.collectAsState()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val gameState by gameViewModel.uiState.collectAsState()
            val activeRun = (gameState as? GameUiState.Playing)
                ?.takeIf { !it.gameState.isGameOver && it.gameState.score > 0 }
            val selectedDifficulty = com.neongrid.engine.Difficulty.fromId(settings.preferredDifficulty)
            HomeScreen(
                progress = progress,
                hasActiveRun = activeRun != null,
                activeRunDifficulty = activeRun?.difficulty,
                selectedDifficulty = selectedDifficulty,
                onSelectDifficulty = metaViewModel::setDifficulty,
                onContinue = { nav.navigate("game") },
                onNewGame = {
                    gameViewModel.startNewGame(selectedDifficulty)
                    nav.navigate("game")
                },
                onHowToPlay = { nav.navigate("howtoplay") },
                onMissions = { nav.navigate("missions") },
                onThemes = { nav.navigate("themes") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("howtoplay") {
            HowToPlayScreen(onBack = { nav.popBackStack() })
        }
        composable("game") {
            GameScreen(gameViewModel, metaViewModel, onHome = { nav.popBackStack() })
        }
        composable("missions") {
            MissionsScreen(
                missions = progress.dailyMissions,
                shards = progress.shards,
                onClaim = metaViewModel::claimMission,
                onBack = { nav.popBackStack() },
            )
        }
        composable("themes") {
            ThemesScreen(
                progress = progress,
                isUnlocked = { metaViewModel.isUnlocked(it, progress) },
                onSelect = metaViewModel::selectTheme,
                onPurchase = metaViewModel::purchaseTheme,
                onBack = { nav.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                settings = settings,
                onSound = metaViewModel::setSound,
                onMusic = metaViewModel::setMusic,
                onHaptics = metaViewModel::setHaptics,
                onReduceMotion = metaViewModel::setReduceMotion,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
