package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

object AudioManager {
    private const val TAG = "SkillRushAudioManager"
    private const val SAMPLE_RATE = 44100
    private const val DEBOUNCE_COOLDOWN_MS = 35L

    enum class SoundType {
        TAP,
        CORRECT,
        WRONG,
        COMBO,
        GAME_START,
        GAME_END,
        LEVEL_UP,
        ACHIEVEMENT,
        REWARD
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val soundTracks = ConcurrentHashMap<SoundType, AudioTrack>()
    private var musicTrack: AudioTrack? = null
    private val lastPlayTimes = ConcurrentHashMap<SoundType, Long>()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isMusicPlaying = false

    fun init(context: Context) {
        if (isInitialized) return
        scope.launch {
            try {
                prepareAllSoundEffects()
                prepareBackgroundMusicLoop()
                isInitialized = true
                Log.d(TAG, "Audio System initialized successfully with synthesized PCM tracks.")
                if (SettingsManager.isMusicEnabled(context)) {
                    startBackgroundMusic(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize audio system", e)
            }
        }
    }

    private fun createStaticAudioTrack(pcmBuffer: ShortArray, volume: Float = 0.8f): AudioTrack? {
        return try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcmBuffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmBuffer, 0, pcmBuffer.size)
            track.setVolume(volume)
            track
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AudioTrack", e)
            null
        }
    }

    private fun prepareAllSoundEffects() {
        // TAP Sound Effect
        val tapPcm = generateFrequencySweep(startFreq = 880.0, endFreq = 440.0, durationMs = 35.0, volume = 0.5)
        createStaticAudioTrack(tapPcm, 0.6f)?.let { soundTracks[SoundType.TAP] = it }

        // CORRECT Sound Effect
        val correctPcm = generateMelody(
            freqs = doubleArrayOf(523.25, 659.25),
            durationsMs = doubleArrayOf(50.0, 70.0),
            volume = 0.6
        )
        createStaticAudioTrack(correctPcm, 0.7f)?.let { soundTracks[SoundType.CORRECT] = it }

        // WRONG Sound Effect
        val wrongPcm = generateDisharmony(freq1 = 185.0, freq2 = 196.0, durationMs = 120.0, volume = 0.6)
        createStaticAudioTrack(wrongPcm, 0.7f)?.let { soundTracks[SoundType.WRONG] = it }

        // COMBO Sound Effect
        val comboPcm = generateMelody(
            freqs = doubleArrayOf(659.25, 783.99, 1046.50),
            durationsMs = doubleArrayOf(45.0, 45.0, 65.0),
            volume = 0.7
        )
        createStaticAudioTrack(comboPcm, 0.8f)?.let { soundTracks[SoundType.COMBO] = it }

        // GAME_START Sound Effect
        val startPcm = generateMelody(
            freqs = doubleArrayOf(261.63, 329.63, 392.00, 523.25),
            durationsMs = doubleArrayOf(50.0, 50.0, 50.0, 90.0),
            volume = 0.65
        )
        createStaticAudioTrack(startPcm, 0.8f)?.let { soundTracks[SoundType.GAME_START] = it }

        // GAME_END Sound Effect
        val endPcm = generateMelody(
            freqs = doubleArrayOf(392.00, 329.63, 261.63, 196.00),
            durationsMs = doubleArrayOf(70.0, 70.0, 70.0, 110.0),
            volume = 0.65
        )
        createStaticAudioTrack(endPcm, 0.8f)?.let { soundTracks[SoundType.GAME_END] = it }

        // LEVEL_UP Fanfare Sound Effect
        val levelUpPcm = generateMelody(
            freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51),
            durationsMs = doubleArrayOf(45.0, 45.0, 45.0, 60.0, 120.0),
            volume = 0.75
        )
        createStaticAudioTrack(levelUpPcm, 0.85f)?.let { soundTracks[SoundType.LEVEL_UP] = it }

        // ACHIEVEMENT Sound Effect
        val achievementPcm = generateMelody(
            freqs = doubleArrayOf(783.99, 1046.50, 1318.51, 1567.98, 2093.00),
            durationsMs = doubleArrayOf(35.0, 35.0, 35.0, 35.0, 100.0),
            volume = 0.7
        )
        createStaticAudioTrack(achievementPcm, 0.85f)?.let { soundTracks[SoundType.ACHIEVEMENT] = it }

        // REWARD Sound Effect
        val rewardPcm = generateMelody(
            freqs = doubleArrayOf(1318.51, 1760.00),
            durationsMs = doubleArrayOf(50.0, 110.0),
            volume = 0.75
        )
        createStaticAudioTrack(rewardPcm, 0.85f)?.let { soundTracks[SoundType.REWARD] = it }
    }

    private fun prepareBackgroundMusicLoop() {
        // Generate a smooth 8-second ambient music chord loop (Cmaj7 -> Am7 -> Fmaj7 -> G7)
        val chordFreqs = arrayOf(
            doubleArrayOf(261.63, 329.63, 392.00, 493.88), // Cmaj7
            doubleArrayOf(220.00, 261.63, 329.63, 392.00), // Am7
            doubleArrayOf(174.61, 220.00, 261.63, 329.63), // Fmaj7
            doubleArrayOf(196.00, 246.94, 293.66, 349.23)  // G7
        )
        val chordDurationMs = 2000.0 // 2s per chord = 8s loop

        val totalSamples = (SAMPLE_RATE * (4 * chordDurationMs / 1000.0)).toInt()
        val pcmBuffer = ShortArray(totalSamples)

        var sampleIndex = 0
        for (chord in chordFreqs) {
            val chordSamples = (SAMPLE_RATE * (chordDurationMs / 1000.0)).toInt()
            for (i in 0 until chordSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Envelope for each 2s chord
                val attack = (i.toDouble() / (SAMPLE_RATE * 0.1)).coerceAtMost(1.0)
                val release = ((chordSamples - i).toDouble() / (SAMPLE_RATE * 0.2)).coerceIn(0.0, 1.0)
                val env = attack * release * 0.12 // Low ambient volume

                var sampleVal = 0.0
                for (freq in chord) {
                    sampleVal += sin(2.0 * PI * freq * t)
                }
                sampleVal = (sampleVal / chord.size) * env
                pcmBuffer[sampleIndex++] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        musicTrack = createStaticAudioTrack(pcmBuffer, 0.14f)
    }

    private fun playSoundEffect(context: Context, type: SoundType) {
        if (!SettingsManager.isSoundEnabled(context)) return

        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[type] ?: 0L
        if (now - lastTime < DEBOUNCE_COOLDOWN_MS) return
        lastPlayTimes[type] = now

        scope.launch {
            try {
                val track = soundTracks[type]
                if (track != null) {
                    track.stop()
                    track.setPlaybackHeadPosition(0)
                    track.play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error playing sound $type: ${e.message}")
            }
        }
    }

    fun playTapSound(context: Context) {
        playSoundEffect(context, SoundType.TAP)
        HapticManager.vibrateTap(context)
    }

    fun playCorrectSound(context: Context) {
        playSoundEffect(context, SoundType.CORRECT)
        HapticManager.vibrateCorrect(context)
    }

    fun playWrongSound(context: Context) {
        playSoundEffect(context, SoundType.WRONG)
        HapticManager.vibrateWrong(context)
    }

    fun playScoreComboSound(context: Context, comboLevel: Int = 1) {
        playSoundEffect(context, SoundType.COMBO)
        HapticManager.vibrateCombo(context, comboLevel)
    }

    fun playGameStartSound(context: Context) {
        playSoundEffect(context, SoundType.GAME_START)
        HapticManager.vibrateGameStart(context)
    }

    fun playGameEndSound(context: Context) {
        playSoundEffect(context, SoundType.GAME_END)
        HapticManager.vibrateGameEnd(context)
    }

    fun playLevelUpSound(context: Context) {
        playSoundEffect(context, SoundType.LEVEL_UP)
        HapticManager.vibrateLevelUp(context)
    }

    fun playAchievementSound(context: Context) {
        playSoundEffect(context, SoundType.ACHIEVEMENT)
        HapticManager.vibrateAchievement(context)
    }

    fun playRewardSound(context: Context) {
        playSoundEffect(context, SoundType.REWARD)
        HapticManager.vibrateReward(context)
    }

    fun startBackgroundMusic(context: Context) {
        if (!SettingsManager.isMusicEnabled(context)) return
        if (isMusicPlaying) return

        scope.launch {
            try {
                val track = musicTrack ?: return@launch
                track.stop()
                track.setPlaybackHeadPosition(0)
                track.setLoopPoints(0, track.bufferCapacityInFrames, -1)
                track.play()
                isMusicPlaying = true
                Log.d(TAG, "Background music loop started.")
            } catch (e: Exception) {
                Log.w(TAG, "Error starting background music: ${e.message}")
            }
        }
    }

    fun pauseBackgroundMusic() {
        if (!isMusicPlaying) return
        try {
            musicTrack?.pause()
            isMusicPlaying = false
            Log.d(TAG, "Background music paused.")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing background music: ${e.message}")
        }
    }

    fun resumeBackgroundMusic(context: Context) {
        if (SettingsManager.isMusicEnabled(context)) {
            startBackgroundMusic(context)
        }
    }

    fun stopBackgroundMusic() {
        try {
            musicTrack?.stop()
            isMusicPlaying = false
            Log.d(TAG, "Background music stopped.")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping background music: ${e.message}")
        }
    }

    fun release() {
        try {
            stopBackgroundMusic()
            musicTrack?.release()
            musicTrack = null

            for ((_, track) in soundTracks) {
                track.stop()
                track.release()
            }
            soundTracks.clear()
            isInitialized = false
            Log.d(TAG, "All audio resources released successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio resources: ${e.message}")
        }
    }

    // Synthesis Waveform Helpers
    private fun generateFrequencySweep(startFreq: Double, endFreq: Double, durationMs: Double, volume: Double): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * currentFreq / SAMPLE_RATE

            // Fast exponential decay envelope
            val env = Math.exp(-4.0 * progress) * volume
            val valDouble = sin(phase) * env
            buffer[i] = (valDouble * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateMelody(freqs: DoubleArray, durationsMs: DoubleArray, volume: Double): ShortArray {
        var totalMs = 0.0
        for (d in durationsMs) totalMs += d
        val totalSamples = (SAMPLE_RATE * (totalMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        var sampleOffset = 0
        for (idx in freqs.indices) {
            val freq = freqs[idx]
            val duration = durationsMs[idx]
            val noteSamples = (SAMPLE_RATE * (duration / 1000.0)).toInt()

            for (i in 0 until noteSamples) {
                if (sampleOffset + i >= totalSamples) break
                val t = i.toDouble() / SAMPLE_RATE
                val noteProgress = i.toDouble() / noteSamples

                // Envelope: 5ms attack, smooth decay
                val attack = (i.toDouble() / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
                val decay = (1.0 - noteProgress).coerceAtLeast(0.0)
                val env = attack * decay * volume

                val valDouble = sin(2.0 * PI * freq * t) * env
                buffer[sampleOffset + i] = (valDouble * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            sampleOffset += noteSamples
        }
        return buffer
    }

    private fun generateDisharmony(freq1: Double, freq2: Double, durationMs: Double, volume: Double): ShortArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples

            // Envelope: quick attack, linear decay
            val attack = (i.toDouble() / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
            val decay = (1.0 - progress).coerceAtLeast(0.0)
            val env = attack * decay * volume

            val valDouble = (sin(2.0 * PI * freq1 * t) + sin(2.0 * PI * freq2 * t)) * 0.5 * env
            buffer[i] = (valDouble * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
