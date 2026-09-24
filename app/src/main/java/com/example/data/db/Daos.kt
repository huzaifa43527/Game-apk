package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(record: GameRecord): Long

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records WHERE isWin = 1 ORDER BY score DESC LIMIT :limit")
    fun getTopScores(limit: Int = 30): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGames(limit: Int = 10): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records WHERE mode = :mode ORDER BY timestamp DESC")
    fun getGamesByMode(mode: String): Flow<List<GameRecord>>

    @Query("SELECT COUNT(*) FROM game_records")
    fun getTotalGamesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_records WHERE isWin = 1")
    fun getTotalWinsCount(): Flow<Int>

    @Query("DELETE FROM game_records WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("DELETE FROM game_records")
    suspend fun clearAll()
}

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: PlayerProfile)

    @Query("UPDATE player_profile SET coins = :coins WHERE id = 1")
    suspend fun updateCoins(coins: Int)

    @Query("UPDATE player_profile SET soundEnabled = :sound, vibrationEnabled = :vibration WHERE id = 1")
    suspend fun updateSoundVibration(sound: Boolean, vibration: Boolean)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id AND isUnlocked = 0")
    suspend fun unlock(id: String, timestamp: Long): Int

    @Query("UPDATE achievements SET isUnlocked = 0, unlockedAt = NULL")
    suspend fun resetAll()
}
