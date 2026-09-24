package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * SoundManager utility using the Android MediaPlayer API to play subtle audio cues
 * for correct/incorrect guesses, game milestones, and keypad input to complement
 * tactile haptic feedback.
 */
class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val soundDir = File(appContext.cacheDir, "sounds").apply { mkdirs() }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_GAME)
        .build()

    enum class SoundCue {
        CORRECT_GUESS,
        INCORRECT_GENERIC,
        INCORRECT_WARM,
        INCORRECT_COLD,
        MILESTONE_VICTORY,
        MILESTONE_STREAK,
        MILESTONE_ACHIEVEMENT,
        TAP_CLICK,
        GAME_OVER,
        INPUT_ERROR
    }

    enum class MilestoneType {
        VICTORY,
        STREAK,
        ACHIEVEMENT_UNLOCKED,
        LEVEL_UP
    }

    private val playerMap = mutableMapOf<SoundCue, MediaPlayer>()
    private val soundFiles = mutableMapOf<SoundCue, File>()
    private var isReleased = false

    init {
        try {
            prepareSoundFiles()
            preloadPlayers()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize sound files or players", e)
        }
    }

    /**
     * Plays subtle audio cue for a correct guess.
     */
    fun playCorrectGuess(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(SoundCue.CORRECT_GUESS, volume = 0.45f)
    }

    /**
     * Plays subtle audio cue for an incorrect guess, differentiated by warmth/proximity.
     */
    fun playIncorrectGuess(soundEnabled: Boolean, isWarm: Boolean = false, isCold: Boolean = false) {
        if (!soundEnabled) return
        val cue = when {
            isWarm -> SoundCue.INCORRECT_WARM
            isCold -> SoundCue.INCORRECT_COLD
            else -> SoundCue.INCORRECT_GENERIC
        }
        play(cue, volume = 0.35f)
    }

    /**
     * Plays subtle celebratory audio cue for game milestones.
     */
    fun playMilestone(soundEnabled: Boolean, type: MilestoneType = MilestoneType.VICTORY) {
        if (!soundEnabled) return
        val cue = when (type) {
            MilestoneType.VICTORY -> SoundCue.MILESTONE_VICTORY
            MilestoneType.STREAK -> SoundCue.MILESTONE_STREAK
            MilestoneType.ACHIEVEMENT_UNLOCKED, MilestoneType.LEVEL_UP -> SoundCue.MILESTONE_ACHIEVEMENT
        }
        play(cue, volume = 0.50f)
    }

    /**
     * Plays subtle keypress click audio cue.
     */
    fun playTap(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(SoundCue.TAP_CLICK, volume = 0.20f)
    }

    /**
     * Plays gentle game over audio cue.
     */
    fun playGameOver(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(SoundCue.GAME_OVER, volume = 0.40f)
    }

    /**
     * Plays subtle input error rejection blip.
     */
    fun playInputError(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(SoundCue.INPUT_ERROR, volume = 0.30f)
    }

    /**
     * Internal play method using preloaded MediaPlayer or transient MediaPlayer.
     */
    private fun play(cue: SoundCue, volume: Float = 0.4f) {
        if (isReleased) return

        try {
            val player = playerMap[cue]
            if (player != null) {
                try {
                    if (player.isPlaying) {
                        player.seekTo(0)
                    } else {
                        player.setVolume(volume, volume)
                        player.start()
                    }
                    return
                } catch (e: Exception) {
                    // Preloaded player may be in transient state, recreate below
                    Log.d(TAG, "Preloaded player failed, creating transient player: ${e.message}")
                }
            }

            // Fallback: transient player for this cue
            val file = soundFiles[cue] ?: return
            val transientPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(file.absolutePath)
                setVolume(volume, volume)
                prepare()
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing sound cue $cue", e)
        }
    }

    private fun preloadPlayers() {
        for (cue in SoundCue.values()) {
            val file = soundFiles[cue] ?: continue
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        // Keep player ready for next playback
                        try {
                            it.seekTo(0)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    setOnErrorListener { mp, _, _ ->
                        try {
                            mp.reset()
                        } catch (e: Exception) {
                            // Ignore
                        }
                        true
                    }
                }
                playerMap[cue] = mp
            } catch (e: Exception) {
                Log.w(TAG, "Could not preload player for $cue", e)
            }
        }
    }

    private fun prepareSoundFiles() {
        val sampleRate = 22050

        // 1. Correct guess: ascending cheerful arpeggio (C5 -> E5 -> G5 -> C6)
        soundFiles[SoundCue.CORRECT_GUESS] = getOrCreateWav("correct.wav") {
            generateArpeggio(
                sampleRate = sampleRate,
                durationSeconds = 0.35,
                frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            )
        }

        // 2. Incorrect generic: subtle soft descending tone (260 Hz -> 200 Hz)
        soundFiles[SoundCue.INCORRECT_GENERIC] = getOrCreateWav("incorrect.wav") {
            generateGlideTone(
                sampleRate = sampleRate,
                durationSeconds = 0.22,
                startFreq = 260.0,
                endFreq = 200.0
            )
        }

        // 3. Incorrect warm: two cheerful, hopeful warm chimes (D5 -> F#5)
        soundFiles[SoundCue.INCORRECT_WARM] = getOrCreateWav("warm.wav") {
            generateArpeggio(
                sampleRate = sampleRate,
                durationSeconds = 0.25,
                frequencies = doubleArrayOf(587.33, 739.99)
            )
        }

        // 4. Incorrect cold: soft low frequency drop (180 Hz -> 140 Hz)
        soundFiles[SoundCue.INCORRECT_COLD] = getOrCreateWav("cold.wav") {
            generateGlideTone(
                sampleRate = sampleRate,
                durationSeconds = 0.20,
                startFreq = 180.0,
                endFreq = 135.0
            )
        }

        // 5. Milestone victory: rich celebratory chord with shimmer
        soundFiles[SoundCue.MILESTONE_VICTORY] = getOrCreateWav("victory.wav") {
            generateChord(
                sampleRate = sampleRate,
                durationSeconds = 0.65,
                frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            )
        }

        // 6. Milestone streak: bright three-note celebration (G5 -> B5 -> D6)
        soundFiles[SoundCue.MILESTONE_STREAK] = getOrCreateWav("streak.wav") {
            generateArpeggio(
                sampleRate = sampleRate,
                durationSeconds = 0.45,
                frequencies = doubleArrayOf(783.99, 987.77, 1174.66)
            )
        }

        // 7. Milestone achievement: shimmering bell chime (A5 -> C#6 -> E6)
        soundFiles[SoundCue.MILESTONE_ACHIEVEMENT] = getOrCreateWav("achievement.wav") {
            generateArpeggio(
                sampleRate = sampleRate,
                durationSeconds = 0.50,
                frequencies = doubleArrayOf(880.00, 1108.73, 1318.51)
            )
        }

        // 8. Keypad tap: very subtle soft wooden click (1200 Hz short impulse)
        soundFiles[SoundCue.TAP_CLICK] = getOrCreateWav("tap.wav") {
            generateClick(
                sampleRate = sampleRate,
                durationSeconds = 0.035,
                frequency = 1200.0
            )
        }

        // 9. Game over: gentle descending minor triad (E4 -> C4 -> A3)
        soundFiles[SoundCue.GAME_OVER] = getOrCreateWav("game_over.wav") {
            generateArpeggio(
                sampleRate = sampleRate,
                durationSeconds = 0.45,
                frequencies = doubleArrayOf(329.63, 261.63, 220.00)
            )
        }

        // 10. Input error: double soft wooden knock
        soundFiles[SoundCue.INPUT_ERROR] = getOrCreateWav("input_error.wav") {
            generateDoubleKnock(
                sampleRate = sampleRate,
                durationSeconds = 0.16,
                frequency = 800.0
            )
        }
    }

    private fun getOrCreateWav(fileName: String, sampleGenerator: () -> ShortArray): File {
        val file = File(soundDir, fileName)
        if (!file.exists() || file.length() < 44) {
            val samples = sampleGenerator()
            writeWavFile(file, 22050, samples)
        }
        return file
    }

    private fun writeWavFile(file: File, sampleRate: Int, samples: ShortArray) {
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize
        FileOutputStream(file).use { fos ->
            val header = ByteArray(44)
            // RIFF chunk descriptor
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalSize and 0xff).toByte()
            header[5] = ((totalSize shr 8) and 0xff).toByte()
            header[6] = ((totalSize shr 16) and 0xff).toByte()
            header[7] = ((totalSize shr 24) and 0xff).toByte()
            // Format
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            // "fmt " subchunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            // Subchunk1Size (16 for PCM)
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            // AudioFormat (1 for PCM)
            header[20] = 1
            header[21] = 0
            // NumChannels (1 mono)
            header[22] = 1
            header[23] = 0
            // SampleRate
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            // ByteRate = SampleRate * NumChannels * BitsPerSample/8
            val byteRate = sampleRate * 2
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            // BlockAlign = NumChannels * BitsPerSample/8
            header[32] = 2
            header[33] = 0
            // BitsPerSample
            header[34] = 16
            header[35] = 0
            // "data" subchunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (dataSize and 0xff).toByte()
            header[41] = ((dataSize shr 8) and 0xff).toByte()
            header[42] = ((dataSize shr 16) and 0xff).toByte()
            header[43] = ((dataSize shr 24) and 0xff).toByte()

            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                byteBuffer.putShort(sample)
            }
            fos.write(byteBuffer.array())
        }
    }

    // --- Audio Synthesis Utilities for Subtle Tones ---

    private fun generateArpeggio(
        sampleRate: Int,
        durationSeconds: Double,
        frequencies: DoubleArray
    ): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)
        val noteSamples = totalSamples / frequencies.size

        for (i in 0 until totalSamples) {
            val noteIndex = (i / noteSamples).coerceAtMost(frequencies.size - 1)
            val freq = frequencies[noteIndex]
            val localT = (i % noteSamples).toDouble() / sampleRate
            val envelope = exp(-4.0 * localT) // gentle decay
            val sineVal = sin(2.0 * PI * freq * localT) + 0.25 * sin(4.0 * PI * freq * localT)
            val sample = (sineVal * envelope * 18000.0).toInt().coerceIn(-32000, 32000)
            samples[i] = sample.toShort()
        }
        return samples
    }

    private fun generateGlideTone(
        sampleRate: Int,
        durationSeconds: Double,
        startFreq: Double,
        endFreq: Double
    ): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * currentFreq / sampleRate
            val envelope = exp(-3.5 * progress) * (1.0 - exp(-20.0 * progress)) // Soft attack, gentle decay
            val sineVal = sin(phase)
            val sample = (sineVal * envelope * 16000.0).toInt().coerceIn(-32000, 32000)
            samples[i] = sample.toShort()
        }
        return samples
    }

    private fun generateChord(
        sampleRate: Int,
        durationSeconds: Double,
        frequencies: DoubleArray
    ): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-3.0 * progress) * (1.0 - exp(-30.0 * progress))
            var sum = 0.0
            for (freq in frequencies) {
                sum += sin(2.0 * PI * freq * t)
            }
            sum /= frequencies.size
            val sample = (sum * envelope * 20000.0).toInt().coerceIn(-32000, 32000)
            samples[i] = sample.toShort()
        }
        return samples
    }

    private fun generateClick(
        sampleRate: Int,
        durationSeconds: Double,
        frequency: Double
    ): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-80.0 * t) // fast decay click
            val sineVal = sin(2.0 * PI * frequency * t)
            val sample = (sineVal * envelope * 14000.0).toInt().coerceIn(-32000, 32000)
            samples[i] = sample.toShort()
        }
        return samples
    }

    private fun generateDoubleKnock(
        sampleRate: Int,
        durationSeconds: Double,
        frequency: Double
    ): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val samples = ShortArray(totalSamples)
        val half = totalSamples / 2

        for (i in 0 until totalSamples) {
            val localIndex = if (i < half) i else i - half
            val t = localIndex.toDouble() / sampleRate
            val envelope = exp(-70.0 * t)
            val sineVal = sin(2.0 * PI * frequency * t)
            val sample = (sineVal * envelope * 15000.0).toInt().coerceIn(-32000, 32000)
            samples[i] = sample.toShort()
        }
        return samples
    }

    /**
     * Releases all MediaPlayers and audio resources.
     */
    fun release() {
        isReleased = true
        for ((_, player) in playerMap) {
            try {
                player.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        playerMap.clear()
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
