package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GameRecord::class, PlayerProfile::class, AchievementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "number_guessing_game.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default profile and achievements on creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.playerProfileDao().insertOrUpdate(
                                    PlayerProfile(
                                        id = 1,
                                        playerName = "Huzaifa",
                                        xp = 0,
                                        level = 1,
                                        coins = 150
                                    )
                                )
                                database.achievementDao().insertAll(INITIAL_ACHIEVEMENTS)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val INITIAL_ACHIEVEMENTS = listOf(
            AchievementEntity(
                id = "first_win",
                title = "First Victory",
                description = "Successfully guess your first secret number",
                iconType = "TROPHY",
                coinReward = 50,
                xpReward = 100
            ),
            AchievementEntity(
                id = "speed_master",
                title = "Speed Master",
                description = "Win a game in under 15 seconds",
                iconType = "LIGHTNING",
                coinReward = 75,
                xpReward = 150
            ),
            AchievementEntity(
                id = "sniper",
                title = "Sniper Accuracy",
                description = "Guess the secret number on your 1st or 2nd attempt",
                iconType = "TARGET",
                coinReward = 100,
                xpReward = 200
            ),
            AchievementEntity(
                id = "no_hint",
                title = "Pure Intuition",
                description = "Win a game without purchasing any extra hints",
                iconType = "STAR",
                coinReward = 50,
                xpReward = 100
            ),
            AchievementEntity(
                id = "streak_3",
                title = "On Fire!",
                description = "Achieve a consecutive 3-win streak",
                iconType = "FIRE",
                coinReward = 100,
                xpReward = 200
            ),
            AchievementEntity(
                id = "streak_5",
                title = "Unstoppable",
                description = "Reach a consecutive 5-win streak",
                iconType = "CROWN",
                coinReward = 200,
                xpReward = 400
            ),
            AchievementEntity(
                id = "hard_mode",
                title = "High Roller",
                description = "Conquer Hard Mode (1 to 1,000 range)",
                iconType = "SHIELD",
                coinReward = 150,
                xpReward = 300
            ),
            AchievementEntity(
                id = "extreme_mode",
                title = "Number Savant",
                description = "Master Extreme Mode (1 to 10,000 range)",
                iconType = "DIAMOND",
                coinReward = 300,
                xpReward = 600
            ),
            AchievementEntity(
                id = "time_attack",
                title = "Tick-Tock Champ",
                description = "Win a Time Challenge game before time expires",
                iconType = "TIMER",
                coinReward = 100,
                xpReward = 200
            ),
            AchievementEntity(
                id = "daily_challenger",
                title = "Daily Champion",
                description = "Complete a Daily Challenge puzzle",
                iconType = "CALENDAR",
                coinReward = 150,
                xpReward = 250
            ),
            AchievementEntity(
                id = "reverse_guesser",
                title = "Mind Reader",
                description = "Guide the computer to guess your number in Reverse Mode",
                iconType = "BRAIN",
                coinReward = 75,
                xpReward = 150
            ),
            AchievementEntity(
                id = "coin_collector",
                title = "Coin Hoarder",
                description = "Accumulate 500 or more virtual coins",
                iconType = "COINS",
                coinReward = 100,
                xpReward = 200
            )
        )
    }
}
