package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val difficultyTitle: String,
    val targetNumber: Int,
    val attemptsUsed: Int,
    val maxAttempts: Int,
    val timeSeconds: Int,
    val score: Int,
    val isWin: Boolean,
    val hintsUsed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Huzaifa",
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 150,
    val totalGames: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val bestScore: Int = 0,
    val fastestWinSeconds: Int = 0,
    val unlockedThemes: String = "INDIGO",
    val activeTheme: String = "INDIGO",
    val unlockedAvatars: String = "ROOKIE",
    val activeAvatar: String = "ROOKIE",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastDailyCompletedDate: String = ""
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconType: String,
    val coinReward: Int,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
