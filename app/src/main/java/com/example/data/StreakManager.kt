package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayStreakStatus(
    val dateKey: String,
    val dayLabel: String,
    val dayNumber: String,
    val isCompleted: Boolean,
    val isToday: Boolean
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStreakDate: String,
    val last7Days: List<DayStreakStatus>
)

object StreakManager {
    private const val PREFS_NAME = "skillrush_streak_prefs"
    private const val KEY_CURRENT_STREAK = "streak_current"
    private const val KEY_LONGEST_STREAK = "streak_longest"
    private const val KEY_LAST_STREAK_DATE = "streak_last_date"
    private const val KEY_COMPLETED_DATES = "streak_completed_dates"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    }

    private fun getYesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    fun getCurrentStreak(context: Context): Int {
        val prefs = getPrefs(context)
        val lastDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)

        if (lastDate.isBlank()) return currentStreak

        val today = getTodayDateKey()
        val yesterday = getYesterdayDateKey()

        if (lastDate != today && lastDate != yesterday) {
            return 0
        }
        return currentStreak
    }

    fun getLongestStreak(context: Context): Int {
        val prefs = getPrefs(context)
        val current = getCurrentStreak(context)
        val storedLongest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        return maxOf(current, storedLongest)
    }

    fun getLast7DaysStatus(context: Context): List<DayStreakStatus> {
        val prefs = getPrefs(context)
        val completedStored = prefs.getString(KEY_COMPLETED_DATES, "") ?: ""
        val completedSet = completedStored.split(",").filter { it.isNotBlank() }.toSet()

        val today = getTodayDateKey()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())

        val result = mutableListOf<DayStreakStatus>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateKey = dateFormat.format(cal.time)
            val dayLabel = dayNameFormat.format(cal.time).uppercase()
            val dayNumber = dayNumFormat.format(cal.time)
            val isToday = (dateKey == today)
            val isCompleted = completedSet.contains(dateKey)

            result.add(
                DayStreakStatus(
                    dateKey = dateKey,
                    dayLabel = dayLabel,
                    dayNumber = dayNumber,
                    isCompleted = isCompleted,
                    isToday = isToday
                )
            )
        }
        return result
    }

    fun getStreakInfo(context: Context): StreakInfo {
        val prefs = getPrefs(context)
        val rawCurrent = getCurrentStreak(context)
        val longest = getLongestStreak(context)
        val lastDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        val status = getLast7DaysStatus(context)

        return StreakInfo(
            currentStreak = rawCurrent,
            longestStreak = longest,
            lastStreakDate = lastDate,
            last7Days = status
        )
    }

    @Synchronized
    fun recordStreakCompletion(context: Context): Pair<Int, Boolean> {
        val prefs = getPrefs(context)
        val today = getTodayDateKey()
        val yesterday = getYesterdayDateKey()
        val lastDate = prefs.getString(KEY_LAST_STREAK_DATE, "") ?: ""
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)

        if (lastDate == today) {
            return Pair(currentStreak, false)
        }

        if (lastDate == yesterday || lastDate.isBlank()) {
            currentStreak += 1
        } else {
            currentStreak = 1
        }

        longestStreak = maxOf(longestStreak, currentStreak)

        val completedStored = prefs.getString(KEY_COMPLETED_DATES, "") ?: ""
        val completedList = if (completedStored.isBlank()) mutableListOf() else completedStored.split(",").toMutableList()
        if (!completedList.contains(today)) {
            completedList.add(today)
        }
        val trimmedCompleted = completedList.takeLast(30)

        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putString(KEY_LAST_STREAK_DATE, today)
            .putString(KEY_COMPLETED_DATES, trimmedCompleted.joinToString(","))
            .apply()

        val userPrefs = context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE)
        userPrefs.edit().putInt("user_current_streak", currentStreak).apply()

        val currentCoins = UserProfileManager.getCoins(context)
        val currentXp = UserProfileManager.getTotalXp(context)
        userPrefs.edit()
            .putInt("user_coins_balance", currentCoins + 10)
            .putInt("user_total_xp", currentXp + 25)
            .apply()

        return Pair(currentStreak, true)
    }
}
