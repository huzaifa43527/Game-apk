package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHaptics
import com.example.data.db.AchievementEntity
import com.example.data.db.AppDatabase
import com.example.data.db.GameRecord
import com.example.data.db.PlayerProfile
import com.example.data.repository.GameRepository
import com.example.game.DistanceWarmth
import com.example.game.ExtraHintInfo
import com.example.game.GameEngine
import com.example.game.GameModeType
import com.example.game.GuessDirection
import com.example.game.GuessRecord
import com.example.game.ScoreBreakdown
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveGameState(
    val mode: GameModeType = GameModeType.MEDIUM,
    val targetNumber: Int = 0,
    val initialMin: Int = 1,
    val initialMax: Int = 200,
    val currentMin: Int = 1,
    val currentMax: Int = 200,
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 10,
    val remainingAttempts: Int = 10,
    val timeLimitSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val elapsedTimeSeconds: Int = 0,
    val guesses: List<GuessRecord> = emptyList(),
    val inputGuess: String = "",
    val isGameActive: Boolean = false,
    val isGameOver: Boolean = false,
    val isWin: Boolean = false,
    val hintsUsed: Int = 0,
    val activeHintMessages: List<String> = emptyList(),
    val availableHints: List<ExtraHintInfo> = emptyList(),
    val validationError: String? = null,
    val scoreBreakdown: ScoreBreakdown? = null,
    val unlockedAchievementsThisGame: List<AchievementEntity> = emptyList()
)

data class ReverseGameState(
    val isActive: Boolean = false,
    val minBound: Int = 1,
    val maxBound: Int = 200,
    val currentGuess: Int = 100,
    val attempts: Int = 0,
    val isComplete: Boolean = false,
    val log: List<String> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = GameRepository(database)
    val soundAndHaptics = SoundAndHaptics(application)

    val profile: StateFlow<PlayerProfile?> = repository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allGames: StateFlow<List<GameRecord>> = repository.allGamesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topScores: StateFlow<List<GameRecord>> = repository.topScoresFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievementsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _gameState = MutableStateFlow(ActiveGameState())
    val gameState: StateFlow<ActiveGameState> = _gameState.asStateFlow()

    private val _reverseState = MutableStateFlow(ReverseGameState())
    val reverseState: StateFlow<ReverseGameState> = _reverseState.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showHowToPlayDialog = MutableStateFlow(false)
    val showHowToPlayDialog: StateFlow<Boolean> = _showHowToPlayDialog.asStateFlow()

    private val _friendChallengeCode = MutableStateFlow("")
    val friendChallengeCode: StateFlow<String> = _friendChallengeCode.asStateFlow()

    private val _generatedFriendCode = MutableStateFlow<String?>(null)
    val generatedFriendCode: StateFlow<String?> = _generatedFriendCode.asStateFlow()

    private var timerJob: Job? = null

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setShowSettings(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowHowToPlay(show: Boolean) {
        _showHowToPlayDialog.value = show
    }

    fun startNewGame(mode: GameModeType, customTarget: Int? = null) {
        timerJob?.cancel()

        val target = when {
            customTarget != null -> customTarget
            mode == GameModeType.DAILY_CHALLENGE -> GameEngine.getDailySecret().first
            else -> GameEngine.generateSecret(mode.minNumber, mode.maxNumber)
        }

        val availableHints = if (mode.allowsExtraHints) {
            GameEngine.generateAvailableHints(target, mode.minNumber, mode.maxNumber)
        } else {
            emptyList()
        }

        _gameState.value = ActiveGameState(
            mode = mode,
            targetNumber = target,
            initialMin = mode.minNumber,
            initialMax = mode.maxNumber,
            currentMin = mode.minNumber,
            currentMax = mode.maxNumber,
            attemptsUsed = 0,
            maxAttempts = mode.maxAttempts,
            remainingAttempts = mode.maxAttempts,
            timeLimitSeconds = mode.timeLimitSeconds,
            timeRemainingSeconds = mode.timeLimitSeconds,
            elapsedTimeSeconds = 0,
            guesses = emptyList(),
            inputGuess = "",
            isGameActive = true,
            isGameOver = false,
            isWin = false,
            hintsUsed = 0,
            activeHintMessages = emptyList(),
            availableHints = availableHints,
            validationError = null,
            scoreBreakdown = null,
            unlockedAchievementsThisGame = emptyList()
        )

        // Start countdown / elapsed timer
        timerJob = viewModelScope.launch {
            while (_gameState.value.isGameActive && !_gameState.value.isGameOver) {
                delay(1000)
                val current = _gameState.value
                val newElapsed = current.elapsedTimeSeconds + 1
                if (current.timeLimitSeconds > 0) {
                    val newRemaining = current.timeRemainingSeconds - 1
                    if (newRemaining <= 0) {
                        // Time's up!
                        handleGameOver(isWin = false, timeSeconds = current.timeLimitSeconds)
                        break
                    } else {
                        _gameState.value = current.copy(
                            elapsedTimeSeconds = newElapsed,
                            timeRemainingSeconds = newRemaining
                        )
                    }
                } else {
                    _gameState.value = current.copy(elapsedTimeSeconds = newElapsed)
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        // Only allow digits up to max length
        val filtered = text.filter { it.isDigit() }.take(6)
        _gameState.value = _gameState.value.copy(inputGuess = filtered, validationError = null)
        val soundOn = profile.value?.soundEnabled ?: true
        val vibOn = profile.value?.vibrationEnabled ?: true
        soundAndHaptics.playTapSound(soundOn)
        soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
    }

    fun onDigitClicked(digit: String) {
        val current = _gameState.value.inputGuess
        if (current.length < 6) {
            _gameState.value = _gameState.value.copy(inputGuess = current + digit, validationError = null)
            val soundOn = profile.value?.soundEnabled ?: true
            val vibOn = profile.value?.vibrationEnabled ?: true
            soundAndHaptics.playTapSound(soundOn)
            soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
        }
    }

    fun onBackspace() {
        val current = _gameState.value.inputGuess
        if (current.isNotEmpty()) {
            _gameState.value = _gameState.value.copy(inputGuess = current.dropLast(1), validationError = null)
            val soundOn = profile.value?.soundEnabled ?: true
            val vibOn = profile.value?.vibrationEnabled ?: true
            soundAndHaptics.playTapSound(soundOn)
            soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
        }
    }

    fun onClearInput() {
        _gameState.value = _gameState.value.copy(inputGuess = "", validationError = null)
        val soundOn = profile.value?.soundEnabled ?: true
        val vibOn = profile.value?.vibrationEnabled ?: true
        soundAndHaptics.playTapSound(soundOn)
        soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
    }

    fun onQuickStep(step: Int) {
        val currentVal = _gameState.value.inputGuess.toIntOrNull() ?: _gameState.value.currentMin
        val newVal = (currentVal + step).coerceIn(_gameState.value.initialMin, _gameState.value.initialMax)
        _gameState.value = _gameState.value.copy(inputGuess = newVal.toString(), validationError = null)
        val soundOn = profile.value?.soundEnabled ?: true
        val vibOn = profile.value?.vibrationEnabled ?: true
        soundAndHaptics.playTapSound(soundOn)
        soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
    }

    fun submitGuess() {
        val state = _gameState.value
        if (!state.isGameActive || state.isGameOver) return

        val soundOn = profile.value?.soundEnabled ?: true
        val vibOn = profile.value?.vibrationEnabled ?: true

        val guessVal = state.inputGuess.toIntOrNull()
        if (guessVal == null) {
            _gameState.value = state.copy(validationError = "Please enter a valid whole number.")
            soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.INPUT_ERROR)
            return
        }

        if (guessVal < state.initialMin || guessVal > state.initialMax) {
            _gameState.value = state.copy(
                validationError = "Number must be between ${state.initialMin} and ${state.initialMax}."
            )
            soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.INPUT_ERROR)
            return
        }

        val attemptsUsed = state.attemptsUsed + 1
        val remainingAttempts = state.maxAttempts - attemptsUsed

        val totalRange = state.initialMax - state.initialMin
        val guessRecord = GameEngine.evaluateGuess(
            guess = guessVal,
            target = state.targetNumber,
            currentMin = state.currentMin,
            currentMax = state.currentMax,
            totalRangeSize = totalRange,
            attemptIndex = attemptsUsed
        )

        val updatedGuesses = state.guesses + guessRecord

        // Update hints with narrowed bounds
        val updatedHints = if (state.mode.allowsExtraHints) {
            GameEngine.generateAvailableHints(
                state.targetNumber,
                guessRecord.remainingRangeMin,
                guessRecord.remainingRangeMax
            )
        } else {
            emptyList()
        }

        if (guessRecord.direction == GuessDirection.CORRECT) {
            // Correct guess: Player won! Trigger celebratory crescendo haptic response & victory audio
            soundAndHaptics.playVictorySound(soundOn)
            soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.GUESS_CORRECT)
            _gameState.value = state.copy(
                attemptsUsed = attemptsUsed,
                remainingAttempts = remainingAttempts,
                guesses = updatedGuesses,
                inputGuess = "",
                currentMin = guessVal,
                currentMax = guessVal,
                availableHints = updatedHints,
                validationError = null
            )
            handleGameOver(isWin = true, timeSeconds = state.elapsedTimeSeconds)
        } else {
            // Incorrect guess: evaluate distance warmth & attempts remaining
            val isHot = guessRecord.warmth == DistanceWarmth.EXTREMELY_CLOSE || guessRecord.warmth == DistanceWarmth.SUPER_HOT
            soundAndHaptics.playWarmthSound(soundOn, isHot)

            if (remainingAttempts <= 0) {
                // Out of attempts! Defeat vibration
                soundAndHaptics.playDefeatSound(soundOn)
                soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.GAME_OVER_DEFEAT)
                _gameState.value = state.copy(
                    attemptsUsed = attemptsUsed,
                    remainingAttempts = 0,
                    guesses = updatedGuesses,
                    inputGuess = "",
                    currentMin = guessRecord.remainingRangeMin,
                    currentMax = guessRecord.remainingRangeMax,
                    availableHints = updatedHints,
                    validationError = null
                )
                handleGameOver(isWin = false, timeSeconds = state.elapsedTimeSeconds)
            } else {
                // Tactile feedback for incorrect guess differentiated based on proximity
                val incorrectVibrationType = when (guessRecord.warmth) {
                    DistanceWarmth.EXTREMELY_CLOSE, DistanceWarmth.SUPER_HOT, DistanceWarmth.HOT -> SoundAndHaptics.VibrateType.GUESS_INCORRECT_WARM
                    DistanceWarmth.VERY_COLD, DistanceWarmth.CHILLY -> SoundAndHaptics.VibrateType.GUESS_INCORRECT_COLD
                    else -> SoundAndHaptics.VibrateType.GUESS_INCORRECT_GENERIC
                }
                soundAndHaptics.vibrate(vibOn, incorrectVibrationType)

                _gameState.value = state.copy(
                    attemptsUsed = attemptsUsed,
                    remainingAttempts = remainingAttempts,
                    guesses = updatedGuesses,
                    inputGuess = "",
                    currentMin = guessRecord.remainingRangeMin,
                    currentMax = guessRecord.remainingRangeMax,
                    availableHints = updatedHints,
                    validationError = null
                )
            }
        }
    }

    private fun handleGameOver(isWin: Boolean, timeSeconds: Int) {
        timerJob?.cancel()
        val state = _gameState.value
        val streak = if (isWin) (profile.value?.currentStreak ?: 0) else 0

        val breakdown = GameEngine.calculateScore(
            mode = state.mode,
            target = state.targetNumber,
            attemptsUsed = state.attemptsUsed,
            maxAttempts = state.maxAttempts,
            timeSeconds = timeSeconds,
            hintsUsed = state.hintsUsed,
            currentStreak = streak
        )

        viewModelScope.launch {
            val newlyUnlocked = repository.saveGameResult(
                mode = state.mode,
                targetNumber = state.targetNumber,
                attemptsUsed = state.attemptsUsed,
                timeSeconds = timeSeconds,
                hintsUsed = state.hintsUsed,
                isWin = isWin,
                scoreBreakdown = breakdown
            )

            if (newlyUnlocked.isNotEmpty()) {
                val soundOn = profile.value?.soundEnabled ?: true
                soundAndHaptics.playMilestoneSound(soundOn, com.example.audio.SoundManager.MilestoneType.ACHIEVEMENT_UNLOCKED)
            } else if (isWin && streak >= 3) {
                val soundOn = profile.value?.soundEnabled ?: true
                soundAndHaptics.playMilestoneSound(soundOn, com.example.audio.SoundManager.MilestoneType.STREAK)
            }

            _gameState.value = _gameState.value.copy(
                isGameActive = false,
                isGameOver = true,
                isWin = isWin,
                scoreBreakdown = breakdown,
                unlockedAchievementsThisGame = newlyUnlocked
            )
        }
    }

    fun purchaseHint(hint: ExtraHintInfo) {
        viewModelScope.launch {
            val success = repository.deductCoins(hint.coinCost)
            if (success) {
                soundAndHaptics.playWarmthSound(profile.value?.soundEnabled ?: true, true)
                soundAndHaptics.vibrate(profile.value?.vibrationEnabled ?: true, SoundAndHaptics.VibrateType.MEDIUM)
                _gameState.value = _gameState.value.copy(
                    hintsUsed = _gameState.value.hintsUsed + 1,
                    activeHintMessages = _gameState.value.activeHintMessages + hint.revealedText
                )
            }
        }
    }

    fun giveUpGame() {
        if (_gameState.value.isGameActive && !_gameState.value.isGameOver) {
            soundAndHaptics.playDefeatSound(profile.value?.soundEnabled ?: true)
            handleGameOver(isWin = false, timeSeconds = _gameState.value.elapsedTimeSeconds)
        }
    }

    fun exitGameScreen() {
        timerJob?.cancel()
        _gameState.value = ActiveGameState(isGameActive = false)
    }

    // --- REVERSE GUESSING MODE ---
    fun startReverseMode(min: Int = 1, max: Int = 200) {
        val initialGuess = (min + max) / 2
        _reverseState.value = ReverseGameState(
            isActive = true,
            minBound = min,
            maxBound = max,
            currentGuess = initialGuess,
            attempts = 1,
            isComplete = false,
            log = listOf("Computer begins: Guessing $initialGuess (Range $min - $max)")
        )
    }

    fun onReverseAnswer(answer: String) {
        val current = _reverseState.value
        if (!current.isActive || current.isComplete) return

        val soundOn = profile.value?.soundEnabled ?: true
        val vibOn = profile.value?.vibrationEnabled ?: true

        when (answer) {
            "HIGHER" -> {
                val newMin = current.currentGuess + 1
                if (newMin > current.maxBound) {
                    // Contradiction
                    _reverseState.value = current.copy(
                        log = current.log + "Wait, that's impossible based on earlier answers!"
                    )
                    soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.INPUT_ERROR)
                    return
                }
                val nextGuess = (newMin + current.maxBound) / 2
                _reverseState.value = current.copy(
                    minBound = newMin,
                    currentGuess = nextGuess,
                    attempts = current.attempts + 1,
                    log = current.log + "Higher than ${current.currentGuess} -> Next Guess: $nextGuess"
                )
                soundAndHaptics.playTapSound(soundOn)
                soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
            }
            "LOWER" -> {
                val newMax = current.currentGuess - 1
                if (newMax < current.minBound) {
                    _reverseState.value = current.copy(
                        log = current.log + "Wait, that's impossible based on earlier answers!"
                    )
                    soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.INPUT_ERROR)
                    return
                }
                val nextGuess = (current.minBound + newMax) / 2
                _reverseState.value = current.copy(
                    maxBound = newMax,
                    currentGuess = nextGuess,
                    attempts = current.attempts + 1,
                    log = current.log + "Lower than ${current.currentGuess} -> Next Guess: $nextGuess"
                )
                soundAndHaptics.playTapSound(soundOn)
                soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.KEYPRESS_TICK)
            }
            "CORRECT" -> {
                soundAndHaptics.playVictorySound(soundOn)
                soundAndHaptics.vibrate(vibOn, SoundAndHaptics.VibrateType.GUESS_CORRECT)
                _reverseState.value = current.copy(
                    isComplete = true,
                    log = current.log + "Computer found your number (${current.currentGuess}) in ${current.attempts} attempts!"
                )
                // Award completion bonus
                viewModelScope.launch {
                    val breakdown = GameEngine.calculateScore(
                        mode = GameModeType.REVERSE,
                        target = current.currentGuess,
                        attemptsUsed = current.attempts,
                        maxAttempts = 10,
                        timeSeconds = 20,
                        hintsUsed = 0,
                        currentStreak = 0
                    )
                    repository.saveGameResult(
                        mode = GameModeType.REVERSE,
                        targetNumber = current.currentGuess,
                        attemptsUsed = current.attempts,
                        timeSeconds = 20,
                        hintsUsed = 0,
                        isWin = true,
                        scoreBreakdown = breakdown
                    )
                }
            }
        }
    }

    fun exitReverseMode() {
        _reverseState.value = ReverseGameState(isActive = false)
    }

    // --- FRIEND CHALLENGE ---
    fun generateFriendChallenge() {
        val target = GameEngine.generateSecret(1, 200)
        val code = GameEngine.encodeChallengeCode(target, 200)
        _generatedFriendCode.value = code
    }

    fun updateFriendCodeInput(code: String) {
        _friendChallengeCode.value = code.uppercase()
    }

    fun playFriendChallenge(code: String): Boolean {
        val target = GameEngine.decodeChallengeCode(code)
        if (target != null) {
            startNewGame(GameModeType.FRIEND_CHALLENGE, target)
            return true
        }
        return false
    }

    // --- PROFILE & SETTINGS ---
    fun updatePlayerName(name: String) {
        viewModelScope.launch { repository.updatePlayerName(name) }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { repository.toggleSound(enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch { repository.toggleVibration(enabled) }
    }

    fun purchaseOrSelectTheme(themeId: String, cost: Int) {
        viewModelScope.launch { repository.purchaseTheme(themeId, cost) }
    }

    fun purchaseOrSelectAvatar(avatarId: String, cost: Int) {
        viewModelScope.launch { repository.purchaseAvatar(avatarId, cost) }
    }

    fun resetAllProgress() {
        viewModelScope.launch { repository.resetAllProgress() }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundAndHaptics.release()
    }
}
