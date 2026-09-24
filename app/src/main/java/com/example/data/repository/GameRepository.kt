package com.example.data.repository

import com.example.data.db.AchievementEntity
import com.example.data.db.AppDatabase
import com.example.data.db.GameRecord
import com.example.data.db.PlayerProfile
import com.example.game.GameModeType
import com.example.game.ScoreBreakdown
import kotlinx.coroutines.flow.Flow

/**
 * GameRepository abstracts all local database access for the UI and ViewModel layers.
 * It provides clean, reactive flows and suspend functions to:
 * - Fetch and update player statistics, economy, and preferences.
 * - Insert and query game records and match history.
 * - Observe, unlock, and update achievement progress.
 */
class GameRepository(private val database: AppDatabase) {
    private val gameRecordDao = database.gameRecordDao()
    private val playerProfileDao = database.playerProfileDao()
    private val achievementDao = database.achievementDao()

    // ==========================================
    // Reactive Streams for UI Observation
    // ==========================================
    val profileFlow: Flow<PlayerProfile?> = playerProfileDao.getProfileFlow()
    val allGamesFlow: Flow<List<GameRecord>> = gameRecordDao.getAllGames()
    val topScoresFlow: Flow<List<GameRecord>> = gameRecordDao.getTopScores(30)
    val achievementsFlow: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    val unlockedAchievementsFlow: Flow<List<AchievementEntity>> = achievementDao.getUnlockedAchievements()

    // ==========================================
    // Player Statistics & Profile Methods
    // ==========================================

    /**
     * Observe the player's lifetime statistics, level, and economy.
     */
    fun getPlayerStats(): Flow<PlayerProfile?> = profileFlow

    /**
     * Retrieve the player profile synchronously within a coroutine.
     */
    suspend fun getPlayerProfileSync(): PlayerProfile? = playerProfileDao.getProfileSync()

    /**
     * Directly update or insert the player profile entity.
     */
    suspend fun updatePlayerStats(profile: PlayerProfile) {
        playerProfileDao.insertOrUpdate(profile)
    }

    /**
     * Update the display name of the player.
     */
    suspend fun updatePlayerName(newName: String) {
        val profile = playerProfileDao.getProfileSync() ?: return
        playerProfileDao.insertOrUpdate(profile.copy(playerName = newName.trim().ifEmpty { "Player" }))
    }

    /**
     * Update the player's virtual coin balance.
     */
    suspend fun updateCoins(coins: Int) {
        playerProfileDao.updateCoins(coins)
    }

    /**
     * Deduct coins for in-game purchases like hints or cosmetic items.
     * Returns true if the player had enough coins and the transaction succeeded.
     */
    suspend fun deductCoins(amount: Int): Boolean {
        val profile = playerProfileDao.getProfileSync() ?: return false
        if (profile.coins >= amount) {
            playerProfileDao.insertOrUpdate(profile.copy(coins = profile.coins - amount))
            return true
        }
        return false
    }

    /**
     * Toggle sound effects setting.
     */
    suspend fun toggleSound(enabled: Boolean) {
        val profile = playerProfileDao.getProfileSync() ?: return
        playerProfileDao.insertOrUpdate(profile.copy(soundEnabled = enabled))
    }

    /**
     * Toggle haptic vibration feedback setting.
     */
    suspend fun toggleVibration(enabled: Boolean) {
        val profile = playerProfileDao.getProfileSync() ?: return
        playerProfileDao.insertOrUpdate(profile.copy(vibrationEnabled = enabled))
    }

    /**
     * Purchase and activate a visual theme with virtual coins.
     */
    suspend fun purchaseTheme(themeId: String, cost: Int): Boolean {
        val profile = playerProfileDao.getProfileSync() ?: return false
        val unlockedList = profile.unlockedThemes.split(",").toMutableSet()
        if (unlockedList.contains(themeId)) {
            playerProfileDao.insertOrUpdate(profile.copy(activeTheme = themeId))
            return true
        }
        if (profile.coins >= cost) {
            unlockedList.add(themeId)
            playerProfileDao.insertOrUpdate(
                profile.copy(
                    coins = profile.coins - cost,
                    unlockedThemes = unlockedList.joinToString(","),
                    activeTheme = themeId
                )
            )
            return true
        }
        return false
    }

    /**
     * Purchase and equip a player avatar with virtual coins.
     */
    suspend fun purchaseAvatar(avatarId: String, cost: Int): Boolean {
        val profile = playerProfileDao.getProfileSync() ?: return false
        val unlockedList = profile.unlockedAvatars.split(",").toMutableSet()
        if (unlockedList.contains(avatarId)) {
            playerProfileDao.insertOrUpdate(profile.copy(activeAvatar = avatarId))
            return true
        }
        if (profile.coins >= cost) {
            unlockedList.add(avatarId)
            playerProfileDao.insertOrUpdate(
                profile.copy(
                    coins = profile.coins - cost,
                    unlockedAvatars = unlockedList.joinToString(","),
                    activeAvatar = avatarId
                )
            )
            return true
        }
        return false
    }

    // ==========================================
    // Game History & Results Methods
    // ==========================================

    /**
     * Directly insert a raw GameRecord into the database.
     */
    suspend fun insertGameResult(record: GameRecord): Long {
        return gameRecordDao.insertGame(record)
    }

    /**
     * Fetch complete game history ordered by most recent.
     */
    fun getGameHistory(): Flow<List<GameRecord>> = allGamesFlow

    /**
     * Fetch recent game records up to the specified limit.
     */
    fun getRecentGames(limit: Int = 10): Flow<List<GameRecord>> = gameRecordDao.getRecentGames(limit)

    /**
     * Fetch the top winning scores for the leaderboard.
     */
    fun getTopScores(limit: Int = 30): Flow<List<GameRecord>> = gameRecordDao.getTopScores(limit)

    /**
     * Fetch game records filtered by game mode.
     */
    fun getGamesByMode(mode: String): Flow<List<GameRecord>> = gameRecordDao.getGamesByMode(mode)

    /**
     * Get reactive count of total games played.
     */
    fun getTotalGamesCount(): Flow<Int> = gameRecordDao.getTotalGamesCount()

    /**
     * Get reactive count of total games won.
     */
    fun getTotalWinsCount(): Flow<Int> = gameRecordDao.getTotalWinsCount()

    /**
     * Delete an individual game record from history.
     */
    suspend fun deleteGameRecord(id: Long) {
        gameRecordDao.deleteGame(id)
    }

    /**
     * Clear all recorded matches from history.
     */
    suspend fun clearGameHistory() {
        gameRecordDao.clearAll()
    }

    /**
     * Comprehensive end-of-game transaction:
     * 1. Inserts the match record into [game_records].
     * 2. Updates player stats (total games, wins, win streaks, XP, level, fastest win, best score).
     * 3. Checks milestones and unlocks corresponding achievements, granting bonus rewards.
     * 4. Returns the list of achievements newly unlocked this game.
     */
    suspend fun saveGameResult(
        mode: GameModeType,
        targetNumber: Int,
        attemptsUsed: Int,
        timeSeconds: Int,
        hintsUsed: Int,
        isWin: Boolean,
        scoreBreakdown: ScoreBreakdown
    ): List<AchievementEntity> {
        val newlyUnlockedAchievements = mutableListOf<AchievementEntity>()

        // 1. Record game in database
        val record = GameRecord(
            mode = mode.name,
            difficultyTitle = mode.title,
            targetNumber = targetNumber,
            attemptsUsed = attemptsUsed,
            maxAttempts = mode.maxAttempts,
            timeSeconds = timeSeconds,
            score = if (isWin) scoreBreakdown.finalScore else 0,
            isWin = isWin,
            hintsUsed = hintsUsed
        )
        gameRecordDao.insertGame(record)

        // 2. Fetch current profile
        var currentProfile = playerProfileDao.getProfileSync() ?: PlayerProfile(
            id = 1,
            playerName = "Player",
            xp = 0,
            level = 1,
            coins = 150
        )

        val totalGames = currentProfile.totalGames + 1
        val totalWins = currentProfile.totalWins + if (isWin) 1 else 0
        val totalLosses = currentProfile.totalLosses + if (!isWin) 1 else 0
        val newStreak = if (isWin) currentProfile.currentStreak + 1 else 0
        val highestStreak = maxOf(currentProfile.highestStreak, newStreak)
        val bestScore = if (isWin) maxOf(currentProfile.bestScore, scoreBreakdown.finalScore) else currentProfile.bestScore
        val fastestWin = if (isWin) {
            if (currentProfile.fastestWinSeconds == 0) timeSeconds else minOf(currentProfile.fastestWinSeconds, timeSeconds)
        } else {
            currentProfile.fastestWinSeconds
        }

        var newXp = currentProfile.xp + if (isWin) scoreBreakdown.xpEarned else 25
        var newCoins = currentProfile.coins + if (isWin) scoreBreakdown.coinsEarned else 10

        val lastDailyDate = if (mode == GameModeType.DAILY_CHALLENGE && isWin) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        } else {
            currentProfile.lastDailyCompletedDate
        }

        // 3. Check and unlock achievements
        val now = System.currentTimeMillis()
        val checkAndUnlock: suspend (String) -> Unit = { id ->
            val rows = achievementDao.unlock(id, now)
            if (rows > 0) {
                achievementDao.getAchievement(id)?.let { ach ->
                    newCoins += ach.coinReward
                    newXp += ach.xpReward
                    newlyUnlockedAchievements.add(ach)
                }
            }
        }

        if (isWin) {
            checkAndUnlock("first_win")
            if (timeSeconds <= 15) checkAndUnlock("speed_master")
            if (attemptsUsed <= 2) checkAndUnlock("sniper")
            if (hintsUsed == 0) checkAndUnlock("no_hint")
            if (newStreak >= 3) checkAndUnlock("streak_3")
            if (newStreak >= 5) checkAndUnlock("streak_5")
            if (mode == GameModeType.HARD) checkAndUnlock("hard_mode")
            if (mode == GameModeType.EXTREME) checkAndUnlock("extreme_mode")
            if (mode == GameModeType.TIME_CHALLENGE) checkAndUnlock("time_attack")
            if (mode == GameModeType.DAILY_CHALLENGE) checkAndUnlock("daily_challenger")
            if (mode == GameModeType.REVERSE) checkAndUnlock("reverse_guesser")
        }
        if (newCoins >= 500) {
            checkAndUnlock("coin_collector")
        }

        // Recalculate level after potential achievement XP
        val finalLevel = 1 + (newXp / 500)

        // 4. Update profile in database
        val updatedProfile = currentProfile.copy(
            xp = newXp,
            level = finalLevel,
            coins = newCoins,
            totalGames = totalGames,
            totalWins = totalWins,
            totalLosses = totalLosses,
            currentStreak = newStreak,
            highestStreak = highestStreak,
            bestScore = bestScore,
            fastestWinSeconds = fastestWin,
            lastDailyCompletedDate = lastDailyDate
        )
        playerProfileDao.insertOrUpdate(updatedProfile)

        return newlyUnlockedAchievements
    }

    // ==========================================
    // Achievement Methods
    // ==========================================

    /**
     * Observe all available achievements.
     */
    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementsFlow

    /**
     * Observe only unlocked achievements.
     */
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> = unlockedAchievementsFlow

    /**
     * Retrieve a specific achievement by ID.
     */
    suspend fun getAchievement(id: String): AchievementEntity? = achievementDao.getAchievement(id)

    /**
     * Update an achievement's data.
     */
    suspend fun updateAchievement(achievement: AchievementEntity) {
        achievementDao.updateAchievement(achievement)
    }

    /**
     * Manually unlock an achievement and grant its coin and XP rewards.
     * Returns true if the achievement was newly unlocked.
     */
    suspend fun unlockAchievement(id: String): Boolean {
        val now = System.currentTimeMillis()
        val rows = achievementDao.unlock(id, now)
        if (rows > 0) {
            achievementDao.getAchievement(id)?.let { ach ->
                val profile = playerProfileDao.getProfileSync() ?: return true
                val newCoins = profile.coins + ach.coinReward
                val newXp = profile.xp + ach.xpReward
                val newLevel = 1 + (newXp / 500)
                playerProfileDao.insertOrUpdate(
                    profile.copy(
                        coins = newCoins,
                        xp = newXp,
                        level = newLevel
                    )
                )
            }
            return true
        }
        return false
    }

    /**
     * Reset all achievement progress.
     */
    suspend fun resetAchievements() {
        achievementDao.resetAll()
    }

    // ==========================================
    // Global Reset
    // ==========================================

    /**
     * Reset all progress, history, stats, and achievements back to initial defaults.
     */
    suspend fun resetAllProgress() {
        gameRecordDao.clearAll()
        achievementDao.resetAll()
        playerProfileDao.insertOrUpdate(
            PlayerProfile(
                id = 1,
                playerName = "Player",
                xp = 0,
                level = 1,
                coins = 150,
                totalGames = 0,
                totalWins = 0,
                totalLosses = 0,
                currentStreak = 0,
                highestStreak = 0,
                bestScore = 0,
                fastestWinSeconds = 0,
                unlockedThemes = "INDIGO",
                activeTheme = "INDIGO",
                unlockedAvatars = "ROOKIE",
                activeAvatar = "ROOKIE",
                soundEnabled = true,
                vibrationEnabled = true,
                lastDailyCompletedDate = ""
            )
        )
    }
}
