package com.example.game

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

object GameEngine {

    fun generateSecret(min: Int, max: Int): Int {
        val rand = Random()
        return min + rand.nextInt(max - min + 1)
    }

    fun getDailySecret(): Pair<Int, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(Date())
        // Deterministic pseudo-random seed based on today's date string
        val seed = todayStr.hashCode().toLong()
        val rand = Random(seed)
        val target = 1 + rand.nextInt(500)
        return Pair(target, todayStr)
    }

    fun encodeChallengeCode(target: Int, maxNumber: Int): String {
        // Encode into a 5-6 char string like "NG" + base36
        val payload = (target * 97 + maxNumber * 13) % 46656
        val base36 = payload.toString(36).uppercase(Locale.US).padStart(3, '0')
        val checksum = ((target + 7) % 26 + 'A'.code).toChar()
        val targetEncoded = ((target % 36).toString(36) + (target / 36).toString(36)).uppercase(Locale.US)
        return "NG$targetEncoded$checksum"
    }

    fun decodeChallengeCode(code: String): Int? {
        val clean = code.trim().uppercase(Locale.US)
        if (clean.length < 5 || !clean.startsWith("NG")) return null
        return try {
            val c1 = clean[2].toString().toInt(36)
            val c2 = clean[3].toString().toInt(36)
            val target = c1 + c2 * 36
            if (target in 1..1000) target else null
        } catch (e: Exception) {
            null
        }
    }

    fun evaluateGuess(
        guess: Int,
        target: Int,
        currentMin: Int,
        currentMax: Int,
        totalRangeSize: Int,
        attemptIndex: Int
    ): GuessRecord {
        val direction = when {
            guess < target -> GuessDirection.TOO_LOW
            guess > target -> GuessDirection.TOO_HIGH
            else -> GuessDirection.CORRECT
        }

        val diff = Math.abs(guess - target)
        val warmth = if (direction == GuessDirection.CORRECT) {
            DistanceWarmth.MATCH
        } else {
            DistanceWarmth.fromDifference(diff, totalRangeSize)
        }

        val newMin = if (direction == GuessDirection.TOO_LOW) maxOf(currentMin, guess + 1) else currentMin
        val newMax = if (direction == GuessDirection.TOO_HIGH) minOf(currentMax, guess - 1) else currentMax

        return GuessRecord(
            guessNumber = guess,
            attemptIndex = attemptIndex,
            direction = direction,
            warmth = warmth,
            remainingRangeMin = newMin,
            remainingRangeMax = newMax
        )
    }

    fun calculateScore(
        mode: GameModeType,
        target: Int,
        attemptsUsed: Int,
        maxAttempts: Int,
        timeSeconds: Int,
        hintsUsed: Int,
        currentStreak: Int
    ): ScoreBreakdown {
        val base = mode.baseScore
        val remainingAttempts = maxOf(0, maxAttempts - attemptsUsed)
        val attemptsBonus = remainingAttempts * 350

        // Time bonus: up to +1200 if finished fast (within 25s)
        val timeBonus = if (timeSeconds < 25) {
            (25 - timeSeconds) * 48
        } else {
            0
        }

        val noHintBonus = if (hintsUsed == 0) 600 else 0

        // Streak multiplier: 1.0 up to 2.0x (10% per consecutive win up to 10 streak)
        val streakMult = 1.0 + (minOf(currentStreak, 10) * 0.10)
        val rawTotal = (base + attemptsBonus + timeBonus + noHintBonus) * streakMult
        val finalScore = rawTotal.toInt()

        val xpEarned = (finalScore / 30).coerceAtLeast(60)
        val coinsEarned = (finalScore / 120).coerceIn(20, 300) + if (mode == GameModeType.DAILY_CHALLENGE) 100 else 0

        return ScoreBreakdown(
            baseScore = base,
            attemptsBonus = attemptsBonus,
            timeBonus = timeBonus,
            noHintBonus = noHintBonus,
            streakMultiplier = streakMult,
            finalScore = finalScore,
            xpEarned = xpEarned,
            coinsEarned = coinsEarned
        )
    }

    fun generateAvailableHints(target: Int, currentMin: Int, currentMax: Int): List<ExtraHintInfo> {
        val hints = mutableListOf<ExtraHintInfo>()

        // 1. Even or Odd
        val isEven = target % 2 == 0
        hints.add(
            ExtraHintInfo(
                typeId = "EVEN_ODD",
                title = "Parity Check",
                description = "Reveals whether the secret number is even or odd",
                coinCost = 15,
                revealedText = "The secret number is ${if (isEven) "EVEN (divisible by 2)" else "ODD (not divisible by 2)"}."
            )
        )

        // 2. Divisibility
        val divText = when {
            target % 10 == 0 -> "The secret number ends in 0 (divisible by 10)!"
            target % 5 == 0 -> "The secret number is a multiple of 5 (ends in 5 or 0)!"
            target % 3 == 0 -> "The secret number is a multiple of 3!"
            target % 7 == 0 -> "The secret number is a multiple of 7!"
            else -> "The secret number is NOT divisible by 2, 3, or 5."
        }
        hints.add(
            ExtraHintInfo(
                typeId = "DIVISIBILITY",
                title = "Divisibility Clue",
                description = "Checks for special divisor properties (3, 5, 7, 10)",
                coinCost = 25,
                revealedText = divText
            )
        )

        // 3. Digit Sum
        val digitSum = target.toString().map { it.digitToInt() }.sum()
        hints.add(
            ExtraHintInfo(
                typeId = "DIGIT_SUM",
                title = "Digit Sum",
                description = "Reveals the sum of all digits in the secret number",
                coinCost = 30,
                revealedText = "The sum of the digits in the secret number is $digitSum."
            )
        )

        // 4. Narrow Bounds
        val span = currentMax - currentMin
        if (span > 8) {
            val cut = (span * 0.3).toInt()
            val narrowedMin = if (target - currentMin > cut) currentMin + cut else currentMin
            val narrowedMax = if (currentMax - target > cut) currentMax - cut else currentMax
            hints.add(
                ExtraHintInfo(
                    typeId = "NARROW_BOUNDS",
                    title = "Range Radar",
                    description = "Eliminates outer zones away from the target",
                    coinCost = 40,
                    revealedText = "Safe zone narrowed down! The number is definitely between $narrowedMin and $narrowedMax."
                )
            )
        }

        return hints
    }
}
