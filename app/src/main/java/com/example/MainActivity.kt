package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.data.AdManager
import com.example.ui.game.GamePlayScreen
import com.example.ui.game.GameSelectionScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.MyApplicationTheme

sealed class Screen {
    data object UsernameSetup : Screen()
    data object Home : Screen()
    data object GameSelection : Screen()
    data object Leaderboard : Screen()
    data object Tournaments : Screen()
    data object Profile : Screen()
    data object Settings : Screen()
    data object Statistics : Screen()
    data class GamePlay(
        val gameId: String,
        val tournamentId: String? = null,
        val sessionToken: String? = null
    ) : Screen()
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(applicationContext)
        com.example.data.CloudSyncManager.initialize(applicationContext)
        com.example.audio.AudioManager.init(applicationContext)
        AdManager.showAppOpenIfReady(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SkillRushApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AdManager.showAppOpenIfReady(this)
        com.example.audio.AudioManager.resumeBackgroundMusic(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        com.example.audio.AudioManager.pauseBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.audio.AudioManager.release()
    }
}

@Composable
fun SkillRushApp() {
    val context = LocalContext.current
    val isOnlineState = com.example.util.rememberIsOnline()
    val isOnline = isOnlineState.value

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (!com.example.data.UserProfileManager.hasSetupUsername(context)) {
                Screen.UsernameSetup
            } else {
                Screen.Home
            }
        )
    }

    if (!isOnline) {
        com.example.ui.components.NetworkOfflineScreen(
            onRetry = {
                // NetworkObserver will update automatically when connection changes
            }
        )
        return
    }

    BackHandler(enabled = currentScreen !is Screen.Home && currentScreen !is Screen.UsernameSetup) {
        currentScreen = when (val screen = currentScreen) {
            is Screen.GamePlay -> if (screen.tournamentId != null) Screen.Tournaments else Screen.GameSelection
            is Screen.GameSelection -> Screen.Home
            is Screen.Leaderboard -> Screen.Home
            is Screen.Tournaments -> Screen.Home
            is Screen.Profile -> Screen.Home
            is Screen.Settings -> Screen.Profile
            is Screen.Statistics -> Screen.Profile
            else -> Screen.Home
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                initialState is Screen.Home && targetState is Screen.GameSelection -> {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                }
                initialState is Screen.GameSelection && targetState is Screen.Home -> {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
                targetState is Screen.GamePlay -> {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                }
                else -> {
                    fadeIn().togetherWith(fadeOut())
                }
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.UsernameSetup -> {
                com.example.ui.auth.UsernameSetupScreen(
                    onSetupComplete = {
                        currentScreen = Screen.Home
                    }
                )
            }
            is Screen.Home -> {
                HomeScreen(
                    onNavigateToGames = { currentScreen = Screen.GameSelection },
                    onPlayGame = { gameId -> currentScreen = Screen.GamePlay(gameId) },
                    onDailyChallenge = { currentScreen = Screen.GamePlay(com.example.data.DailyChallengeManager.getTodayChallengeGame().first) },
                    onNavigateToTournaments = { currentScreen = Screen.Tournaments },
                    onNavigateToProfile = { currentScreen = Screen.Profile },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            1 -> currentScreen = Screen.GameSelection
                            2 -> currentScreen = Screen.Leaderboard
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.GameSelection -> {
                GameSelectionScreen(
                    onNavigateBack = { currentScreen = Screen.Home },
                    onPlayGame = { gameId -> currentScreen = Screen.GamePlay(gameId) },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            2 -> currentScreen = Screen.Leaderboard
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.Leaderboard -> {
                com.example.ui.leaderboard.LeaderboardScreen(
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToGames = { currentScreen = Screen.GameSelection },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            1 -> currentScreen = Screen.GameSelection
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.Tournaments -> {
                com.example.ui.tournament.TournamentScreen(
                    onNavigateBack = { currentScreen = Screen.Home },
                    onStartTournamentGame = { gameId, tId, sToken ->
                        currentScreen = Screen.GamePlay(
                            gameId = gameId,
                            tournamentId = tId,
                            sessionToken = sToken
                        )
                    },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            1 -> currentScreen = Screen.GameSelection
                            2 -> currentScreen = Screen.Leaderboard
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.Profile -> {
                ProfileScreen(
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            1 -> currentScreen = Screen.GameSelection
                            2 -> currentScreen = Screen.Leaderboard
                        }
                    },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToStats = { currentScreen = Screen.Statistics }
                )
            }
            is Screen.Statistics -> {
                com.example.ui.stats.StatisticsScreen(
                    onNavigateBack = { currentScreen = Screen.Profile },
                    onNavigateToGames = { currentScreen = Screen.GameSelection },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            1 -> currentScreen = Screen.GameSelection
                            2 -> currentScreen = Screen.Leaderboard
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.Settings -> {
                com.example.ui.settings.SettingsScreen(
                    onNavigateBack = { currentScreen = Screen.Profile },
                    onNavigateToNavTab = { index ->
                        when (index) {
                            0 -> currentScreen = Screen.Home
                            1 -> currentScreen = Screen.GameSelection
                            2 -> currentScreen = Screen.Leaderboard
                            3 -> currentScreen = Screen.Profile
                        }
                    }
                )
            }
            is Screen.GamePlay -> {
                val activity = androidx.activity.compose.LocalActivity.current
                val returnTarget = if (screen.tournamentId != null) Screen.Tournaments else Screen.GameSelection
                GamePlayScreen(
                    gameId = screen.gameId,
                    tournamentId = screen.tournamentId,
                    sessionToken = screen.sessionToken,
                    onBack = {
                        if (activity != null) {
                            AdManager.showInterstitialIfNeeded(activity) {
                                currentScreen = returnTarget
                            }
                        } else {
                            currentScreen = returnTarget
                        }
                    }
                )
            }
        }
    }
}
