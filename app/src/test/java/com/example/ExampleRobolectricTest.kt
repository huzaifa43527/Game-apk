package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundAndHaptics
import com.example.data.db.AchievementEntity
import com.example.data.db.AppDatabase
import com.example.data.db.GameRecord
import com.example.data.db.PlayerProfile
import com.example.data.repository.GameRepository
import com.example.game.DistanceWarmth
import com.example.game.GameEngine
import com.example.game.GameModeType
import com.example.game.GuessDirection
import com.example.game.ScoreBreakdown
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Number Guessing Game", appName)
    }

    @Test
    fun `secret generation is within specified range`() {
        for (i in 1..20) {
            val secret = GameEngine.generateSecret(1, 200)
            assertTrue(secret in 1..200)
        }
    }

    @Test
    fun `guess evaluation correctly narrows range and evaluates direction`() {
        val target = 142

        // Guess 100 (Too low)
        val recordLow = GameEngine.evaluateGuess(
            guess = 100,
            target = target,
            currentMin = 1,
            currentMax = 200,
            totalRangeSize = 200,
            attemptIndex = 1
        )
        assertEquals(GuessDirection.TOO_LOW, recordLow.direction)
        assertEquals(101, recordLow.remainingRangeMin)
        assertEquals(200, recordLow.remainingRangeMax)

        // Guess 160 (Too high)
        val recordHigh = GameEngine.evaluateGuess(
            guess = 160,
            target = target,
            currentMin = recordLow.remainingRangeMin,
            currentMax = recordLow.remainingRangeMax,
            totalRangeSize = 200,
            attemptIndex = 2
        )
        assertEquals(GuessDirection.TOO_HIGH, recordHigh.direction)
        assertEquals(101, recordHigh.remainingRangeMin)
        assertEquals(159, recordHigh.remainingRangeMax)

        // Guess 142 (Correct)
        val recordCorrect = GameEngine.evaluateGuess(
            guess = 142,
            target = target,
            currentMin = recordHigh.remainingRangeMin,
            currentMax = recordHigh.remainingRangeMax,
            totalRangeSize = 200,
            attemptIndex = 3
        )
        assertEquals(GuessDirection.CORRECT, recordCorrect.direction)
        assertEquals(DistanceWarmth.MATCH, recordCorrect.warmth)
    }

    @Test
    fun `scoring calculates properly with streak bonus`() {
        val scoreNormal = GameEngine.calculateScore(
            mode = GameModeType.MEDIUM,
            target = 100,
            attemptsUsed = 3,
            maxAttempts = 10,
            timeSeconds = 12,
            hintsUsed = 0,
            currentStreak = 0
        )
        assertTrue(scoreNormal.finalScore > 1000)

        val scoreWithStreak = GameEngine.calculateScore(
            mode = GameModeType.MEDIUM,
            target = 100,
            attemptsUsed = 3,
            maxAttempts = 10,
            timeSeconds = 12,
            hintsUsed = 0,
            currentStreak = 3
        )
        assertTrue(scoreWithStreak.finalScore > scoreNormal.finalScore)
    }

    @Test
    fun `friend challenge encode and decode works seamlessly`() {
        val target = 137
        val code = GameEngine.encodeChallengeCode(target, 200)
        assertTrue(code.startsWith("NG"))

        val decoded = GameEngine.decodeChallengeCode(code)
        assertNotNull(decoded)
        assertEquals(target, decoded)
    }

    @Test
    fun `game record dao persists and retrieves game history`() = runBlocking {
        val dao = db.gameRecordDao()
        val record = GameRecord(
            mode = "MEDIUM",
            difficultyTitle = "Medium (1–200)",
            targetNumber = 142,
            attemptsUsed = 4,
            maxAttempts = 10,
            timeSeconds = 18,
            score = 1850,
            isWin = true,
            hintsUsed = 0
        )

        val id = dao.insertGame(record)
        assertTrue(id > 0)

        val allGames = dao.getAllGames().first()
        assertEquals(1, allGames.size)
        assertEquals(1850, allGames.first().score)
        assertEquals(142, allGames.first().targetNumber)

        val topScores = dao.getTopScores(5).first()
        assertEquals(1, topScores.size)
    }

    @Test
    fun `player profile dao stores and updates statistics and economy`() = runBlocking {
        val profileDao = db.playerProfileDao()
        val profile = PlayerProfile(
            id = 1,
            playerName = "Huzaifa",
            xp = 1200,
            level = 3,
            coins = 350,
            totalGames = 10,
            totalWins = 8,
            totalLosses = 2,
            currentStreak = 5,
            highestStreak = 5,
            bestScore = 2400,
            fastestWinSeconds = 9
        )

        profileDao.insertOrUpdate(profile)
        val loaded = profileDao.getProfileSync()
        assertNotNull(loaded)
        assertEquals("Huzaifa", loaded?.playerName)
        assertEquals(350, loaded?.coins)
        assertEquals(8, loaded?.totalWins)

        profileDao.updateCoins(500)
        val updated = profileDao.getProfileFlow().first()
        assertEquals(500, updated?.coins)
    }

    @Test
    fun `achievement dao inserts and unlocks achievements`() = runBlocking {
        val achDao = db.achievementDao()
        val ach = AchievementEntity(
            id = "speed_master",
            title = "Speed Master",
            description = "Win in under 15 seconds",
            iconType = "LIGHTNING",
            coinReward = 75,
            xpReward = 150,
            isUnlocked = false
        )

        achDao.insertAll(listOf(ach))
        val initial = achDao.getAchievement("speed_master")
        assertNotNull(initial)
        assertEquals(false, initial?.isUnlocked)

        val timestamp = System.currentTimeMillis()
        val rows = achDao.unlock("speed_master", timestamp)
        assertEquals(1, rows)

        val unlocked = achDao.getUnlockedAchievements().first()
        assertEquals(1, unlocked.size)
        assertEquals(true, unlocked.first().isUnlocked)
        assertEquals(timestamp, unlocked.first().unlockedAt)
    }

    @Test
    fun `game repository saves game result, updates stats, and unlocks achievements`() = runBlocking {
        val repo = GameRepository(db)

        // Seed an achievement
        db.achievementDao().insertAll(
            listOf(
                AchievementEntity(
                    id = "first_win",
                    title = "First Victory",
                    description = "Win your first game",
                    iconType = "STAR",
                    coinReward = 50,
                    xpReward = 100,
                    isUnlocked = false
                )
            )
        )

        val breakdown = ScoreBreakdown(
            baseScore = 1000,
            attemptsBonus = 500,
            timeBonus = 300,
            noHintBonus = 200,
            streakMultiplier = 1.0,
            finalScore = 2000,
            coinsEarned = 100,
            xpEarned = 250
        )

        val unlocked = repo.saveGameResult(
            mode = GameModeType.MEDIUM,
            targetNumber = 120,
            attemptsUsed = 3,
            timeSeconds = 18,
            hintsUsed = 0,
            isWin = true,
            scoreBreakdown = breakdown
        )

        // first_win should be unlocked
        assertTrue(unlocked.any { it.id == "first_win" })

        // Check player stats
        val stats = repo.getPlayerStats().first()
        assertNotNull(stats)
        assertEquals(1, stats?.totalGames)
        assertEquals(1, stats?.totalWins)
        assertEquals(1, stats?.currentStreak)
        assertEquals(2000, stats?.bestScore)
        // Initial 150 + 100 earned + 50 from first_win = 300
        assertEquals(300, stats?.coins)

        // Check game history
        val games = repo.getGameHistory().first()
        assertEquals(1, games.size)
        assertEquals(120, games.first().targetNumber)
        assertEquals(2000, games.first().score)
    }

    @Test
    fun `game repository handles economy and purchases`() = runBlocking {
        val repo = GameRepository(db)

        val initialProfile = PlayerProfile(
            id = 1,
            playerName = "Hero",
            coins = 200,
            unlockedThemes = "INDIGO",
            activeTheme = "INDIGO"
        )
        repo.updatePlayerStats(initialProfile)

        val deducted = repo.deductCoins(50)
        assertTrue(deducted)
        assertEquals(150, repo.getPlayerProfileSync()?.coins)

        // Purchase theme
        val purchased = repo.purchaseTheme("CYBERPUNK", 100)
        assertTrue(purchased)
        val profileAfter = repo.getPlayerProfileSync()
        assertEquals(50, profileAfter?.coins)
        assertEquals("CYBERPUNK", profileAfter?.activeTheme)
        assertTrue(profileAfter?.unlockedThemes?.contains("CYBERPUNK") == true)
    }

    @Test
    fun `sound and haptics executes all tactile feedback patterns without failure`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundAndHaptics = SoundAndHaptics(context)

        // Test all vibrate types with vibration enabled
        val allTypes = listOf(
            SoundAndHaptics.VibrateType.KEYPRESS_TICK,
            SoundAndHaptics.VibrateType.LIGHT,
            SoundAndHaptics.VibrateType.MEDIUM,
            SoundAndHaptics.VibrateType.GUESS_CORRECT,
            SoundAndHaptics.VibrateType.GUESS_INCORRECT_WARM,
            SoundAndHaptics.VibrateType.GUESS_INCORRECT_COLD,
            SoundAndHaptics.VibrateType.GUESS_INCORRECT_GENERIC,
            SoundAndHaptics.VibrateType.GAME_OVER_DEFEAT,
            SoundAndHaptics.VibrateType.INPUT_ERROR,
            SoundAndHaptics.VibrateType.SUCCESS,
            SoundAndHaptics.VibrateType.ERROR
        )

        for (type in allTypes) {
            soundAndHaptics.vibrate(vibrationEnabled = true, type = type)
            soundAndHaptics.vibrate(vibrationEnabled = false, type = type)
        }

        // Test audio cues
        soundAndHaptics.playTapSound(soundEnabled = true)
        soundAndHaptics.playTapSound(soundEnabled = false)
        soundAndHaptics.playWarmthSound(soundEnabled = true, isHot = true)
        soundAndHaptics.playWarmthSound(soundEnabled = true, isHot = false)
        soundAndHaptics.playVictorySound(soundEnabled = true)
        soundAndHaptics.playDefeatSound(soundEnabled = true)
        soundAndHaptics.playMilestoneSound(soundEnabled = true, com.example.audio.SoundManager.MilestoneType.ACHIEVEMENT_UNLOCKED)
        soundAndHaptics.playMilestoneSound(soundEnabled = true, com.example.audio.SoundManager.MilestoneType.STREAK)

        // Test SoundManager directly
        val soundManager = com.example.audio.SoundManager(context)
        soundManager.playCorrectGuess(soundEnabled = true)
        soundManager.playIncorrectGuess(soundEnabled = true, isWarm = true)
        soundManager.playIncorrectGuess(soundEnabled = true, isCold = true)
        soundManager.playMilestone(soundEnabled = true, com.example.audio.SoundManager.MilestoneType.VICTORY)
        soundManager.playMilestone(soundEnabled = true, com.example.audio.SoundManager.MilestoneType.LEVEL_UP)
        soundManager.playTap(soundEnabled = true)
        soundManager.playGameOver(soundEnabled = true)
        soundManager.playInputError(soundEnabled = true)
        soundManager.release()

        soundAndHaptics.release()
    }
}
