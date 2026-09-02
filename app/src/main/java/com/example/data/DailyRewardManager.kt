package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.audio.AudioManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PowerUpType(
    val id: String,
    val displayName: String,
    val description: String
) {
    TIME_FREEZE("powerup_time_freeze", "Time Freeze", "Freezes countdown timer for 4 extra seconds"),
    SCORE_BOOST("powerup_score_boost", "2x Score Surge", "Doubles your score multiplier during gameplay"),
    SHIELD("powerup_shield", "Reflex Shield", "Protects against 1 penalty or mistake"),
    FOCUS_SURGE("powerup_focus_surge", "Focus Surge", "Slows target velocity and sharpens reflexes")
}

data class PowerUpReward(
    val type: PowerUpType,
    val count: Int
)

data class DailyRewardTier(
    val dayNumber: Int,
    val coins: Int,
    val xp: Int,
    val powerUp: PowerUpReward? = null,
    val isJackpotDay: Boolean = false
)

data class DailyRewardClaimResult(
    val dayNumber: Int,
    val baseCoins: Int,
    val finalCoins: Int,
    val baseXp: Int,
    val finalXp: Int,
    val powerUpsGranted: List<PowerUpReward>,
    val didLevelUp: Boolean,
    val currentLevel: Int,
    val newStreak: Int,
    val isDoubledWithAd: Boolean
)

object DailyRewardManager {
    private const val TAG = "DailyRewardManager"
    private const val PREFS_NAME = "skillrush_daily_reward_prefs"
    private const val KEY_LAST_CLAIM_DATE = "daily_reward_last_claim_date"
    private const val KEY_CURRENT_STREAK_DAY = "daily_reward_streak_day"
    private const val KEY_TOTAL_CLAIMS = "daily_reward_total_claims_count"
    private const val POWERUP_PREFIX = "powerup_inventory_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    /**
     * Checks if a daily reward is ready to be claimed today.
     */
    fun isDailyRewardAvailable(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastClaimDate = prefs.getString(KEY_LAST_CLAIM_DATE, "") ?: ""
        val today = getTodayDateString()
        return lastClaimDate != today
    }

    /**
     * Gets the last claim date string (yyyy-MM-dd).
     */
    fun getLastClaimDate(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_CLAIM_DATE, "") ?: ""
    }

    /**
     * Gets the active day of the 7-day reward cycle (1 to 7).
     * If the user skipped a day, returns 1.
     * If available today, returns the day they will claim today.
     */
    fun getCurrentRewardDay(context: Context): Int {
        val prefs = getPrefs(context)
        val lastClaimDate = prefs.getString(KEY_LAST_CLAIM_DATE, "") ?: ""
        val storedStreakDay = prefs.getInt(KEY_CURRENT_STREAK_DAY, 0)

        if (lastClaimDate.isEmpty()) {
            return 1
        }

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()

        return if (lastClaimDate == today) {
            // Already claimed today, return the current completed day
            if (storedStreakDay in 1..7) storedStreakDay else 1
        } else if (lastClaimDate == yesterday) {
            // Consecutive day: advance to next day in 1..7 cycle
            val nextDay = storedStreakDay + 1
            if (nextDay > 7) 1 else nextDay
        } else {
            // Broken streak, reset to Day 1
            1
        }
    }

    /**
     * Pre-defined 7-day reward calendar with escalating Coins, XP, and Power-Ups.
     */
    fun getRewardTiers(): List<DailyRewardTier> {
        return listOf(
            DailyRewardTier(
                dayNumber = 1,
                coins = 100,
                xp = 50,
                powerUp = null
            ),
            DailyRewardTier(
                dayNumber = 2,
                coins = 150,
                xp = 75,
                powerUp = PowerUpReward(PowerUpType.SHIELD, 1)
            ),
            DailyRewardTier(
                dayNumber = 3,
                coins = 220,
                xp = 120,
                powerUp = PowerUpReward(PowerUpType.SCORE_BOOST, 1)
            ),
            DailyRewardTier(
                dayNumber = 4,
                coins = 320,
                xp = 180,
                powerUp = PowerUpReward(PowerUpType.TIME_FREEZE, 1)
            ),
            DailyRewardTier(
                dayNumber = 5,
                coins = 450,
                xp = 250,
                powerUp = PowerUpReward(PowerUpType.FOCUS_SURGE, 1)
            ),
            DailyRewardTier(
                dayNumber = 6,
                coins = 650,
                xp = 350,
                powerUp = PowerUpReward(PowerUpType.SHIELD, 2)
            ),
            DailyRewardTier(
                dayNumber = 7,
                coins = 1500,
                xp = 800,
                powerUp = PowerUpReward(PowerUpType.SCORE_BOOST, 3),
                isJackpotDay = true
            )
        )
    }

    /**
     * Claims today's daily reward.
     */
    @Synchronized
    fun claimDailyReward(context: Context, doubleWithAd: Boolean = false): DailyRewardClaimResult? {
        if (!isDailyRewardAvailable(context)) {
            Log.w(TAG, "Daily reward already claimed today.")
            return null
        }

        val claimDay = getCurrentRewardDay(context)
        val tier = getRewardTiers().find { it.dayNumber == claimDay } ?: getRewardTiers().first()

        val multiplier = if (doubleWithAd) 2 else 1
        val finalCoins = tier.coins * multiplier
        val finalXp = tier.xp * multiplier

        val grantedPowerUps = mutableListOf<PowerUpReward>()
        if (tier.powerUp != null) {
            val count = tier.powerUp.count * multiplier
            addPowerUp(context, tier.powerUp.type, count)
            grantedPowerUps.add(PowerUpReward(tier.powerUp.type, count))
        }

        // On Day 7 jackpot, grant an extra bonus power-up
        if (tier.isJackpotDay) {
            val freezeCount = 2 * multiplier
            addPowerUp(context, PowerUpType.TIME_FREEZE, freezeCount)
            grantedPowerUps.add(PowerUpReward(PowerUpType.TIME_FREEZE, freezeCount))
        }

        // Apply Coins and XP to UserProfile
        val prevLevel = UserProfileManager.getLevel(context)

        UserProfileManager.addCoins(context, finalCoins)
        UserProfileManager.addXp(context, finalXp)

        // Save Daily Reward claim state
        val today = getTodayDateString()
        val rewardPrefs = getPrefs(context)
        val totalClaims = rewardPrefs.getInt(KEY_TOTAL_CLAIMS, 0) + 1

        rewardPrefs.edit()
            .putString(KEY_LAST_CLAIM_DATE, today)
            .putInt(KEY_CURRENT_STREAK_DAY, claimDay)
            .putInt(KEY_TOTAL_CLAIMS, totalClaims)
            .apply()

        // Also record streak completion in StreakManager
        val streakResult = StreakManager.recordStreakCompletion(context)

        // Cloud sync
        CloudSyncManager.onLocalDataUpdated(context)

        val newLevel = UserProfileManager.getLevel(context)
        val didLevelUp = newLevel > prevLevel

        // Play sounds & haptics
        if (didLevelUp) {
            AudioManager.playLevelUpSound(context)
        } else {
            AudioManager.playRewardSound(context)
        }

        return DailyRewardClaimResult(
            dayNumber = claimDay,
            baseCoins = tier.coins,
            finalCoins = finalCoins,
            baseXp = tier.xp,
            finalXp = finalXp,
            powerUpsGranted = grantedPowerUps,
            didLevelUp = didLevelUp,
            currentLevel = newLevel,
            newStreak = streakResult.first,
            isDoubledWithAd = doubleWithAd
        )
    }

    // Power-up Inventory Management
    fun getPowerUpCount(context: Context, type: PowerUpType): Int {
        return getPrefs(context).getInt(POWERUP_PREFIX + type.id, 0)
    }

    fun addPowerUp(context: Context, type: PowerUpType, amount: Int) {
        if (amount <= 0) return
        val current = getPowerUpCount(context, type)
        getPrefs(context).edit().putInt(POWERUP_PREFIX + type.id, current + amount).apply()
        CloudSyncManager.onLocalDataUpdated(context)
    }

    fun usePowerUp(context: Context, type: PowerUpType): Boolean {
        val current = getPowerUpCount(context, type)
        return if (current > 0) {
            getPrefs(context).edit().putInt(POWERUP_PREFIX + type.id, current - 1).apply()
            CloudSyncManager.onLocalDataUpdated(context)
            true
        } else {
            false
        }
    }

    fun exportPowerUpsForCloud(context: Context): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        PowerUpType.entries.forEach { type ->
            map[type.id] = getPowerUpCount(context, type)
        }
        return map
    }

    fun mergeCloudPowerUps(context: Context, cloudData: Map<String, Any?>) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        PowerUpType.entries.forEach { type ->
            val cloudVal = (cloudData[type.id] as? Number)?.toInt() ?: 0
            val localVal = prefs.getInt(POWERUP_PREFIX + type.id, 0)
            editor.putInt(POWERUP_PREFIX + type.id, maxOf(localVal, cloudVal))
        }
        editor.apply()
    }

    fun exportDailyRewardForCloud(context: Context): Map<String, Any?> {
        val prefs = getPrefs(context)
        return mapOf(
            "lastClaimDate" to (prefs.getString(KEY_LAST_CLAIM_DATE, "") ?: ""),
            "streakDay" to prefs.getInt(KEY_CURRENT_STREAK_DAY, 0),
            "totalClaims" to prefs.getInt(KEY_TOTAL_CLAIMS, 0)
        )
    }

    fun mergeCloudDailyReward(context: Context, cloudData: Map<String, Any?>) {
        val prefs = getPrefs(context)
        val cLastDate = cloudData["lastClaimDate"] as? String ?: ""
        val cStreakDay = (cloudData["streakDay"] as? Number)?.toInt() ?: 0
        val cTotalClaims = (cloudData["totalClaims"] as? Number)?.toInt() ?: 0

        val lLastDate = prefs.getString(KEY_LAST_CLAIM_DATE, "") ?: ""
        val lStreakDay = prefs.getInt(KEY_CURRENT_STREAK_DAY, 0)
        val lTotalClaims = prefs.getInt(KEY_TOTAL_CLAIMS, 0)

        val editor = prefs.edit()
        if (cLastDate.isNotEmpty() && cLastDate >= lLastDate) {
            editor.putString(KEY_LAST_CLAIM_DATE, cLastDate)
            editor.putInt(KEY_CURRENT_STREAK_DAY, maxOf(lStreakDay, cStreakDay))
        }
        editor.putInt(KEY_TOTAL_CLAIMS, maxOf(lTotalClaims, cTotalClaims))
        editor.apply()
    }
}
