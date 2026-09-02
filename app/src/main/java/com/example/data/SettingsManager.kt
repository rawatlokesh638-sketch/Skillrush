package com.example.data

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "skillrush_settings_prefs"
    private const val KEY_SOUND = "settings_sound"
    private const val KEY_MUSIC = "settings_music"
    private const val KEY_VIBRATION = "settings_vibration"
    private const val KEY_NOTIFICATIONS = "settings_notifications"
    private const val KEY_REDUCED_MOTION = "settings_reduced_motion"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isMusicEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MUSIC, true)
    }

    fun setMusicEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MUSIC, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun isReducedMotionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REDUCED_MOTION, false)
    }

    fun setReducedMotionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        CloudSyncManager.onLocalDataUpdated(context)
    }

    fun exportSettingsForCloud(context: Context): Map<String, Boolean> {
        return mapOf(
            "soundEnabled" to isSoundEnabled(context),
            "musicEnabled" to isMusicEnabled(context),
            "vibrationEnabled" to isVibrationEnabled(context),
            "notificationsEnabled" to isNotificationsEnabled(context),
            "reducedMotionEnabled" to isReducedMotionEnabled(context)
        )
    }

    fun mergeCloudSettings(context: Context, cloudData: Map<String, Any?>) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        if (cloudData.containsKey("soundEnabled")) {
            editor.putBoolean(KEY_SOUND, cloudData["soundEnabled"] as? Boolean ?: true)
        }
        if (cloudData.containsKey("musicEnabled")) {
            editor.putBoolean(KEY_MUSIC, cloudData["musicEnabled"] as? Boolean ?: true)
        }
        if (cloudData.containsKey("vibrationEnabled")) {
            editor.putBoolean(KEY_VIBRATION, cloudData["vibrationEnabled"] as? Boolean ?: true)
        }
        if (cloudData.containsKey("notificationsEnabled")) {
            editor.putBoolean(KEY_NOTIFICATIONS, cloudData["notificationsEnabled"] as? Boolean ?: true)
        }
        if (cloudData.containsKey("reducedMotionEnabled")) {
            editor.putBoolean(KEY_REDUCED_MOTION, cloudData["reducedMotionEnabled"] as? Boolean ?: false)
        }
        editor.apply()
    }

    fun resetGameProgress(context: Context) {
        val prefFiles = listOf(
            "skillrush_tap_rush_prefs",
            "skillrush_memory_flash_prefs",
            "skillrush_perfect_aim_prefs",
            "skillrush_number_sprint_prefs",
            "skillrush_spot_diff_prefs",
            "skillrush_speed_rush_prefs",
            "skillrush_user_profile_prefs",
            "skillrush_daily_challenge_prefs",
            "skillrush_streak_prefs",
            "skillrush_achievements_prefs"
        )
        for (file in prefFiles) {
            context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
