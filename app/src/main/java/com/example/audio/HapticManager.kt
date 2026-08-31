package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.SettingsManager

object HapticManager {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun shouldVibrate(context: Context): Boolean {
        if (!SettingsManager.isVibrationEnabled(context)) return false
        val vibrator = getVibrator(context) ?: return false
        return vibrator.hasVibrator()
    }

    fun vibrateTap(context: Context) {
        if (!shouldVibrate(context)) return
        val isReducedMotion = SettingsManager.isReducedMotionEnabled(context)
        if (isReducedMotion) return // Skip subtle click vibration if reduced motion is ON

        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15L)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCorrect(context: Context) {
        if (!shouldVibrate(context)) return
        val vibrator = getVibrator(context) ?: return
        val isReducedMotion = SettingsManager.isReducedMotionEnabled(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isReducedMotion) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    fun vibrateWrong(context: Context) {
        if (!shouldVibrate(context)) return
        val vibrator = getVibrator(context) ?: return
        val isReducedMotion = SettingsManager.isReducedMotionEnabled(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isReducedMotion) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80L)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCombo(context: Context, comboLevel: Int = 1) {
        if (!shouldVibrate(context)) return
        val vibrator = getVibrator(context) ?: return
        if (SettingsManager.isReducedMotionEnabled(context)) {
            vibrateCorrect(context)
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intensity = (comboLevel * 20).coerceAtMost(255)
                val timings = longArrayOf(0, 25, 35, 45)
                val amplitudes = intArrayOf(0, 80, 150, intensity)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 25, 35, 45), -1)
            }
        } catch (_: Exception) {}
    }

    fun vibrateLevelUp(context: Context) {
        if (!shouldVibrate(context)) return
        val vibrator = getVibrator(context) ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 40, 60, 40, 100)
                val amplitudes = intArrayOf(0, 120, 0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(180L)
            }
        } catch (_: Exception) {}
    }

    fun vibrateAchievement(context: Context) {
        if (!shouldVibrate(context)) return
        val vibrator = getVibrator(context) ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 30, 30, 30, 70)
                val amplitudes = intArrayOf(0, 140, 0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120L)
            }
        } catch (_: Exception) {}
    }

    fun vibrateReward(context: Context) {
        vibrateCombo(context, 5)
    }

    fun vibrateGameStart(context: Context) {
        vibrateCorrect(context)
    }

    fun vibrateGameEnd(context: Context) {
        vibrateWrong(context)
    }
}
