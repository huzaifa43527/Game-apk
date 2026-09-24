package com.example.game

enum class GameModeType(
    val title: String,
    val subtitle: String,
    val minNumber: Int,
    val maxNumber: Int,
    val maxAttempts: Int,
    val timeLimitSeconds: Int,
    val baseScore: Int,
    val allowsExtraHints: Boolean = true
) {
    EASY("Easy", "1 – 50 • 10 attempts", 1, 50, 10, 0, 2000, true),
    MEDIUM("Medium", "1 – 200 • 10 attempts", 1, 200, 10, 0, 5000, true),
    HARD("Hard", "1 – 1,000 • 10 attempts", 1, 1000, 10, 0, 10000, true),
    EXTREME("Extreme", "1 – 10,000 • 12 attempts", 1, 10000, 12, 0, 20000, false),
    TIME_CHALLENGE("Time Challenge", "1 – 200 • 30s Countdown", 1, 200, 12, 30, 8000, true),
    DAILY_CHALLENGE("Daily Challenge", "1 – 500 • 7 attempts", 1, 500, 7, 0, 7500, true),
    REVERSE("Reverse Guessing", "Computer guesses your secret number", 1, 200, 10, 0, 3500, false),
    FRIEND_CHALLENGE("Friend Challenge", "Custom secret challenge code", 1, 200, 10, 0, 6000, true)
}

enum class GuessDirection {
    TOO_LOW,
    TOO_HIGH,
    CORRECT,
    INVALID
}

enum class DistanceWarmth(val label: String, val emoji: String, val colorHex: Long) {
    MATCH("Correct!", "🎯", 0xFF00E676),
    EXTREMELY_CLOSE("Extremely Close – 1 number away!", "🔥", 0xFFFF3D00),
    SUPER_HOT("Super Hot – You are right on top of it!", "♨️", 0xFFFF6D00),
    HOT("Hot – Very close!", "🌡️", 0xFFFF9100),
    WARM("Warm – Getting closer!", "☀️", 0xFFFFAB00),
    CHILLY("Chilly – Moving in the direction", "❄️", 0xFF00B0FF),
    VERY_COLD("Very Cold – You are far away", "🧊", 0xFF2979FF);

    companion object {
        fun fromDifference(diff: Int, rangeSize: Int): DistanceWarmth {
            val relativePercent = (diff.toDouble() / rangeSize.toDouble()) * 100.0
            return when {
                diff == 0 -> MATCH
                diff == 1 -> EXTREMELY_CLOSE
                diff <= 3 || relativePercent <= 2.0 -> SUPER_HOT
                diff <= 10 || relativePercent <= 6.0 -> HOT
                diff <= 25 || relativePercent <= 15.0 -> WARM
                relativePercent <= 35.0 -> CHILLY
                else -> VERY_COLD
            }
        }
    }
}

data class GuessRecord(
    val guessNumber: Int,
    val attemptIndex: Int,
    val direction: GuessDirection,
    val warmth: DistanceWarmth,
    val remainingRangeMin: Int,
    val remainingRangeMax: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExtraHintInfo(
    val typeId: String,
    val title: String,
    val description: String,
    val coinCost: Int,
    val revealedText: String
)

data class ScoreBreakdown(
    val baseScore: Int,
    val attemptsBonus: Int,
    val timeBonus: Int,
    val noHintBonus: Int,
    val streakMultiplier: Double,
    val finalScore: Int,
    val xpEarned: Int,
    val coinsEarned: Int
)
