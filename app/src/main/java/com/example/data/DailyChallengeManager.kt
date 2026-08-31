package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyChallengeInfo(
    val dateKey: String,
    val gameId: String,
    val gameTitle: String,
    val description: String,
    val bonusXp: Int,
    val bonusCoins: Int,
    val isCompleted: Boolean,
    val lastScore: Int,
    val timeRemainingFormatted: String
)

object DailyChallengeManager {
    private const val PREFS_NAME = "skillrush_daily_challenge_prefs"
    private const val KEY_COMPLETED_DATE = "daily_completed_date"
    private const val KEY_LAST_SCORE = "daily_last_score"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getAvailableGames(): List<Triple<String, String, String>> {
        return listOf(
            Triple("tap_rush", "Tap Rush: Speed Trial", "Tap matching targets before time runs out!"),
            Triple("memory", "Memory Flash: Pro", "Memorize grid patterns and recall them instantly."),
            Triple("aim", "Perfect Aim: Bullseye", "Test precision targeting and accuracy."),
            Triple("number", "Number Sprint: Sequence", "Ascend through ordered number grids rapidly."),
            Triple("spot", "Spot Difference: Sharp", "Find subtle visual anomalies under pressure."),
            Triple("speed", "Speed Rush: Reflex", "Lightning-fast reaction time trial.")
        )
    }

    fun getTodayChallengeGame(): Triple<String, String, String> {
        val dateKey = getTodayDateKey()
        val games = getAvailableGames()
        val hash = kotlin.math.abs(dateKey.hashCode())
        val index = hash % games.size
        return games[index]
    }

    fun isCompletedToday(context: Context): Boolean {
        val prefs = getPrefs(context)
        val completedDate = prefs.getString(KEY_COMPLETED_DATE, "")
        return completedDate == getTodayDateKey()
    }

    fun getLastScore(context: Context): Int {
        val prefs = getPrefs(context)
        if (isCompletedToday(context)) {
            return prefs.getInt(KEY_LAST_SCORE, 0)
        }
        return 0
    }

    fun getTimeRemainingFormatted(): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 24)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = (calendar.timeInMillis - now).coerceAtLeast(0L)
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60
        val seconds = (diffMillis / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getDailyChallengeInfo(context: Context): DailyChallengeInfo {
        val game = getTodayChallengeGame()
        val completed = isCompletedToday(context)
        val lastScore = getLastScore(context)
        return DailyChallengeInfo(
            dateKey = getTodayDateKey(),
            gameId = game.first,
            gameTitle = game.second,
            description = game.third,
            bonusXp = 50,
            bonusCoins = 20,
            isCompleted = completed,
            lastScore = lastScore,
            timeRemainingFormatted = getTimeRemainingFormatted()
        )
    }

    @Synchronized
    fun completeDailyChallenge(context: Context, score: Int): Boolean {
        val today = getTodayDateKey()
        val prefs = getPrefs(context)
        val completedDate = prefs.getString(KEY_COMPLETED_DATE, "")

        if (completedDate == today) {
            return false
        }

        prefs.edit()
            .putString(KEY_COMPLETED_DATE, today)
            .putInt(KEY_LAST_SCORE, score)
            .apply()

        // Award daily completion bonus (+50 XP, +20 Coins) via UserProfileManager
        val currentCoins = UserProfileManager.getCoins(context)
        val currentXp = UserProfileManager.getTotalXp(context)
        val sharedPrefs = context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE)
        val newCoins = currentCoins + 20
        val newXp = currentXp + 50
        sharedPrefs.edit()
            .putInt("user_coins_balance", newCoins)
            .putInt("user_total_xp", newXp)
            .apply()

        // Record streak completion (+10 coins, +25 XP streak bonus)
        StreakManager.recordStreakCompletion(context)

        AchievementManager.checkAndUnlockAchievements(context)

        return true
    }
}
