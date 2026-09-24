package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundAndHaptics(context: Context) {
    private val appContext = context.applicationContext
    val soundManager = SoundManager(context)
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.w("SoundAndHaptics", "ToneGenerator init failed", e)
        }
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    fun playTapSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        soundManager.playTap(soundEnabled)
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playWarmthSound(soundEnabled: Boolean, isHot: Boolean) {
        if (!soundEnabled) return
        soundManager.playIncorrectGuess(soundEnabled, isWarm = isHot, isCold = !isHot)
        try {
            val tone = if (isHot) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
            toneGenerator?.startTone(tone, 80)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playVictorySound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        soundManager.playCorrectGuess(soundEnabled)
        soundManager.playMilestone(soundEnabled, SoundManager.MilestoneType.VICTORY)
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playDefeatSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        soundManager.playGameOver(soundEnabled)
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playMilestoneSound(soundEnabled: Boolean, type: SoundManager.MilestoneType) {
        if (!soundEnabled) return
        soundManager.playMilestone(soundEnabled, type)
    }

    /**
     * Triggers tactile haptic feedback matching the given VibrateType.
     */
    fun vibrate(vibrationEnabled: Boolean, type: VibrateType = VibrateType.LIGHT) {
        if (!vibrationEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect: VibrationEffect = when (type) {
                    VibrateType.KEYPRESS_TICK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                            } catch (e: Exception) {
                                VibrationEffect.createOneShot(18, 120)
                            }
                        } else {
                            VibrationEffect.createOneShot(18, 120)
                        }
                    }
                    VibrateType.LIGHT -> VibrationEffect.createOneShot(25, 100)
                    VibrateType.MEDIUM -> VibrationEffect.createOneShot(65, 180)
                    VibrateType.GUESS_CORRECT, VibrateType.SUCCESS -> {
                        // Celebratory ascending crescendo waveform: tap -> pause -> pulse -> pause -> long victorious burst
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 45, 50, 65, 45, 130),
                            intArrayOf(0, 160, 0, 210, 0, 255),
                            -1
                        )
                    }
                    VibrateType.GUESS_INCORRECT_WARM -> {
                        // Dynamic tactile feedback: rapid energetic dual pulse for warm / extremely close guesses
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 35, 45, 45),
                            intArrayOf(0, 210, 0, 190),
                            -1
                        )
                    }
                    VibrateType.GUESS_INCORRECT_COLD -> {
                        // Low dull pulse indicating cold / far off
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 75),
                            intArrayOf(0, 150),
                            -1
                        )
                    }
                    VibrateType.GUESS_INCORRECT_GENERIC -> {
                        // Crisp dual-beat thud for incorrect guesses
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 50, 60, 65),
                            intArrayOf(0, 190, 0, 190),
                            -1
                        )
                    }
                    VibrateType.GAME_OVER_DEFEAT -> {
                        // Heavy descending three-beat pulse
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 80, 60, 110, 60, 160),
                            intArrayOf(0, 230, 0, 180, 0, 130),
                            -1
                        )
                    }
                    VibrateType.INPUT_ERROR, VibrateType.ERROR -> {
                        // Snappy warning double-buzz
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 40, 50, 45),
                            intArrayOf(0, 255, 0, 255),
                            -1
                        )
                    }
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val legacyTimings: LongArray = when (type) {
                    VibrateType.KEYPRESS_TICK -> longArrayOf(0, 15)
                    VibrateType.LIGHT -> longArrayOf(0, 25)
                    VibrateType.MEDIUM -> longArrayOf(0, 65)
                    VibrateType.GUESS_CORRECT, VibrateType.SUCCESS -> longArrayOf(0, 45, 50, 65, 45, 130)
                    VibrateType.GUESS_INCORRECT_WARM -> longArrayOf(0, 35, 45, 45)
                    VibrateType.GUESS_INCORRECT_COLD -> longArrayOf(0, 75)
                    VibrateType.GUESS_INCORRECT_GENERIC -> longArrayOf(0, 50, 60, 65)
                    VibrateType.GAME_OVER_DEFEAT -> longArrayOf(0, 80, 60, 110, 60, 160)
                    VibrateType.INPUT_ERROR, VibrateType.ERROR -> longArrayOf(0, 40, 50, 45)
                }
                @Suppress("DEPRECATION")
                v.vibrate(legacyTimings, -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    enum class VibrateType {
        KEYPRESS_TICK,
        LIGHT,
        MEDIUM,
        GUESS_CORRECT,
        GUESS_INCORRECT_WARM,
        GUESS_INCORRECT_COLD,
        GUESS_INCORRECT_GENERIC,
        GAME_OVER_DEFEAT,
        INPUT_ERROR,
        SUCCESS,
        ERROR
    }

    fun release() {
        soundManager.release()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
