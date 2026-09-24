package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameModeType
import com.example.ui.screens.ChallengesScreen
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayDialog
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ProfileShopScreen
import com.example.ui.screens.ReverseGameScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val profile by viewModel.profile.collectAsStateWithLifecycle()
            val gameState by viewModel.gameState.collectAsStateWithLifecycle()
            val reverseState by viewModel.reverseState.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val topScores by viewModel.topScores.collectAsStateWithLifecycle()
            val allGames by viewModel.allGames.collectAsStateWithLifecycle()
            val achievements by viewModel.achievements.collectAsStateWithLifecycle()
            val generatedFriendCode by viewModel.generatedFriendCode.collectAsStateWithLifecycle()
            val showSettings by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
            val showHowToPlay by viewModel.showHowToPlayDialog.collectAsStateWithLifecycle()

            MyApplicationTheme(themeName = profile?.activeTheme ?: "INDIGO") {
                when {
                    gameState.isGameActive || gameState.isGameOver -> {
                        BackHandler {
                            if (gameState.isGameOver) {
                                viewModel.exitGameScreen()
                            } else {
                                viewModel.giveUpGame()
                            }
                        }
                        GamePlayScreen(
                            state = gameState,
                            profile = profile,
                            onInputChanged = viewModel::onInputChanged,
                            onDigitClicked = viewModel::onDigitClicked,
                            onBackspace = viewModel::onBackspace,
                            onClearInput = viewModel::onClearInput,
                            onQuickStep = viewModel::onQuickStep,
                            onSubmitGuess = viewModel::submitGuess,
                            onPurchaseHint = viewModel::purchaseHint,
                            onGiveUp = viewModel::giveUpGame,
                            onPlayAgain = { viewModel.startNewGame(gameState.mode) },
                            onExitGame = viewModel::exitGameScreen
                        )
                    }

                    reverseState.isActive -> {
                        BackHandler {
                            viewModel.exitReverseMode()
                        }
                        ReverseGameScreen(
                            state = reverseState,
                            onAnswer = viewModel::onReverseAnswer,
                            onRestart = { viewModel.startReverseMode() },
                            onExit = viewModel::exitReverseMode
                        )
                    }

                    else -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { viewModel.selectTab(0) },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                                        label = { Text("Play") },
                                        modifier = Modifier.testTag("nav_tab_play")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { viewModel.selectTab(1) },
                                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Challenges", modifier = Modifier.size(22.dp)) },
                                        label = { Text("Challenges") },
                                        modifier = Modifier.testTag("nav_tab_challenges")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { viewModel.selectTab(2) },
                                        icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Scores", modifier = Modifier.size(22.dp)) },
                                        label = { Text("Scores") },
                                        modifier = Modifier.testTag("nav_tab_scores")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = { viewModel.selectTab(3) },
                                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Shop", modifier = Modifier.size(22.dp)) },
                                        label = { Text("Shop") },
                                        modifier = Modifier.testTag("nav_tab_shop")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        profile = profile,
                                        onSelectMode = { mode -> viewModel.startNewGame(mode) },
                                        onStartReverseMode = { viewModel.startReverseMode() },
                                        onOpenSettings = { viewModel.setShowSettings(true) },
                                        onOpenHowToPlay = { viewModel.setShowHowToPlay(true) }
                                    )
                                    1 -> ChallengesScreen(
                                        profile = profile,
                                        generatedFriendCode = generatedFriendCode,
                                        onGenerateFriendCode = viewModel::generateFriendChallenge,
                                        onPlayFriendCode = viewModel::playFriendChallenge,
                                        onStartDailyChallenge = { viewModel.startNewGame(GameModeType.DAILY_CHALLENGE) }
                                    )
                                    2 -> LeaderboardScreen(
                                        topScores = topScores,
                                        allGames = allGames,
                                        profile = profile
                                    )
                                    3 -> ProfileShopScreen(
                                        profile = profile,
                                        achievements = achievements,
                                        onUpdateName = viewModel::updatePlayerName,
                                        onSelectTheme = viewModel::purchaseOrSelectTheme,
                                        onSelectAvatar = viewModel::purchaseOrSelectAvatar
                                    )
                                }
                            }
                        }

                        // Dialogs
                        if (showSettings) {
                            SettingsDialog(
                                profile = profile,
                                onToggleSound = viewModel::toggleSound,
                                onToggleVibration = viewModel::toggleVibration,
                                onResetProgress = viewModel::resetAllProgress,
                                onDismiss = { viewModel.setShowSettings(false) }
                            )
                        }

                        if (showHowToPlay) {
                            HowToPlayDialog(onDismiss = { viewModel.setShowHowToPlay(false) })
                        }
                    }
                }
            }
        }
    }
}
