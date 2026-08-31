package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

import com.example.data.validation.ScoreValidator

data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    val currentLevelXp: Int,
    val xpRequiredForNextLevel: Int,
    val progressFraction: Float,
    val xpRemainingForNextLevel: Int,
    val levelTitle: String,
    val nextLevelTitle: String
)

data class XpBreakdown(
    val baseXp: Int,
    val scoreXp: Int,
    val accuracyBonusXp: Int,
    val comboBonusXp: Int,
    val totalEarned: Int
)

data class RewardResult(
    val coinsEarned: Int,
    val totalCoins: Int,
    val xpEarned: Int,
    val previousTotalXp: Int,
    val totalXp: Int,
    val previousLevel: Int,
    val currentLevel: Int,
    val didLevelUp: Boolean,
    val levelsGained: Int,
    val levelProgress: LevelProgress,
    val xpBreakdown: XpBreakdown,
    val streakDays: Int,
    val isDuplicateIgnored: Boolean = false
)

object UserProfileManager {
    private const val PREFS_NAME = "skillrush_user_profile_prefs"
    private const val KEY_COINS = "user_coins_balance"
    private const val KEY_TOTAL_XP = "user_total_xp"
    private const val KEY_CURRENT_STREAK = "user_current_streak"
    private const val KEY_LAST_PLAYED_DATE = "user_last_played_date"
    private const val KEY_PLAYER_NAME = "user_player_name"
    private const val KEY_HAS_SETUP_USERNAME = "user_has_setup_username"
    private const val KEY_TOTAL_GAMES_PLAYED = "user_total_games_played"
    private const val KEY_PROCESSED_SESSIONS = "user_processed_session_tokens"

    // In-memory set of recent session IDs to prevent duplicate rewards within the same session
    private val memoryProcessedSessions = Collections.synchronizedSet(LinkedHashSet<String>())
    // Cache the latest reward result for a session token so idempotent re-requests get the exact same result
    private val sessionRewardCache = Collections.synchronizedMap(HashMap<String, RewardResult>())

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Progressive XP requirement for advancing from [level] to [level + 1].
     * Level 1: 100 XP
     * Level 2: 150 XP
     * Level 3: 200 XP
     * Level 4: 250 XP
     * Level N: 100 + (N - 1) * 50
     */
    fun getXpRequiredForLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        return 100 + (safeLevel - 1) * 50
    }

    /**
     * Cumulative XP required from 0 to reach [level].
     * Level 1: 0 XP
     * Level 2: 100 XP
     * Level 3: 250 XP (100 + 150)
     * Level 4: 450 XP (100 + 150 + 200)
     * Level 5: 700 XP (100 + 150 + 200 + 250)
     */
    fun getCumulativeXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        var cumulative = 0
        for (lvl in 1 until level) {
            cumulative += getXpRequiredForLevel(lvl)
        }
        return cumulative
    }

    /**
     * Calculates the player level directly from [totalXp].
     * Baseline is Level 1.
     */
    fun getLevelFromTotalXp(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        var level = 1
        var remaining = totalXp
        while (true) {
            val needed = getXpRequiredForLevel(level)
            if (remaining >= needed) {
                remaining -= needed
                level++
            } else {
                break
            }
        }
        return level
    }

    /**
     * Title / Rank Tier based on player level.
     */
    fun getTitleForLevel(level: Int): String {
        return when {
            level >= 25 -> "Apex Titan"
            level >= 20 -> "Grandmaster"
            level >= 15 -> "Focus Master"
            level >= 10 -> "Precision Expert"
            level >= 6 -> "Skill Challenger"
            level >= 3 -> "Speed Apprentice"
            else -> "Novice Reflexer"
        }
    }

    /**
     * Returns the full LevelProgress information for the given total XP.
     */
    fun getLevelProgress(totalXp: Int): LevelProgress {
        val safeXp = totalXp.coerceAtLeast(0)
        val level = getLevelFromTotalXp(safeXp)
        val cumulativeCurrentLevelStart = getCumulativeXpForLevel(level)
        val xpRequiredForNext = getXpRequiredForLevel(level)
        val currentLevelXp = (safeXp - cumulativeCurrentLevelStart).coerceIn(0, xpRequiredForNext)
        val progressFraction = if (xpRequiredForNext > 0) {
            (currentLevelXp.toFloat() / xpRequiredForNext.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val xpRemaining = (xpRequiredForNext - currentLevelXp).coerceAtLeast(0)

        return LevelProgress(
            level = level,
            totalXp = safeXp,
            currentLevelXp = currentLevelXp,
            xpRequiredForNextLevel = xpRequiredForNext,
            progressFraction = progressFraction,
            xpRemainingForNextLevel = xpRemaining,
            levelTitle = getTitleForLevel(level),
            nextLevelTitle = getTitleForLevel(level + 1)
        )
    }

    fun getCoins(context: Context): Int {
        return getPrefs(context).getInt(KEY_COINS, 0)
    }

    fun getTotalXp(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_XP, 0)
    }

    fun getLevel(context: Context): Int {
        return getLevelFromTotalXp(getTotalXp(context))
    }

    fun getLevelProgress(context: Context): LevelProgress {
        return getLevelProgress(getTotalXp(context))
    }

    fun getStreakDays(context: Context): Int {
        return getPrefs(context).getInt(KEY_CURRENT_STREAK, 0)
    }

    fun getPlayerName(context: Context): String {
        return getPrefs(context).getString(KEY_PLAYER_NAME, "Player 1") ?: "Player 1"
    }

    fun setPlayerName(context: Context, name: String) {
        val trimmed = name.trim().ifEmpty { "Player 1" }
        getPrefs(context).edit().putString(KEY_PLAYER_NAME, trimmed).apply()
    }

    fun hasSetupUsername(context: Context): Boolean {
        val name = getPlayerName(context)
        val isExplicitlySet = getPrefs(context).getBoolean(KEY_HAS_SETUP_USERNAME, false)
        return isExplicitlySet && name != "Player 1" && name.isNotEmpty()
    }

    fun markUsernameSetupComplete(context: Context, name: String) {
        setPlayerName(context, name)
        getPrefs(context).edit().putBoolean(KEY_HAS_SETUP_USERNAME, true).apply()
        CloudSyncManager.onLocalDataUpdated(context)
    }

    fun getUserUid(context: Context): String {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        return if (!authUid.isNullOrEmpty()) {
            authUid
        } else {
            val cachedUid = getPrefs(context).getString("user_cached_uid", null)
            if (cachedUid != null) {
                cachedUid
            } else {
                val newUid = "UID_" + java.util.UUID.randomUUID().toString().take(8)
                getPrefs(context).edit().putString("user_cached_uid", newUid).apply()
                newUid
            }
        }
    }

    fun getTotalGamesPlayed(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_GAMES_PLAYED, 0)
    }

    fun getHighestOverallScore(context: Context): Int {
        val tapRush = TapRushScoreManager.getBestScore(context)
        val memoryFlash = MemoryFlashScoreManager.getBestScore(context)
        val perfectAim = PerfectAimScoreManager.getBestScore(context)
        val numberSprint = NumberSprintScoreManager.getBestScore(context)
        val spotDiff = SpotDifferenceScoreManager.getBestScore(context)
        val speedRush = SpeedRushScoreManager.getBestScore(context)
        return maxOf(tapRush, memoryFlash, perfectAim, numberSprint, spotDiff, speedRush)
    }

    /**
     * Calculates XP based on score, accuracy and performance parameters.
     */
    fun calculateGameXp(
        score: Int,
        accuracyPercent: Float = 100f,
        maxCombo: Int = 0
    ): XpBreakdown {
        val baseXp = 20
        val scoreXp = (score / 15).coerceIn(0, 50)
        val accuracyBonusXp = when {
            accuracyPercent >= 98f -> 30
            accuracyPercent >= 90f -> 20
            accuracyPercent >= 75f -> 15
            accuracyPercent >= 50f -> 10
            else -> 5
        }
        val comboBonusXp = when {
            maxCombo >= 20 -> 25
            maxCombo >= 10 -> 15
            maxCombo >= 5 -> 10
            maxCombo >= 3 -> 5
            else -> 0
        }
        val totalEarned = baseXp + scoreXp + accuracyBonusXp + comboBonusXp
        return XpBreakdown(
            baseXp = baseXp,
            scoreXp = scoreXp,
            accuracyBonusXp = accuracyBonusXp,
            comboBonusXp = comboBonusXp,
            totalEarned = totalEarned
        )
    }

    /**
     * Awards XP, Coins, and Level progression after every completed game.
     * Prevents duplicate awards for the same game session using [sessionToken].
     * Fully persistent across app restarts.
     */
    @Synchronized
    fun awardGameRewards(
        context: Context,
        score: Int,
        accuracyPercent: Float = 100f,
        maxCombo: Int = 0,
        sessionToken: String? = null
    ): RewardResult {
        val prefs = getPrefs(context)

        // Validate score metrics
        val validation = ScoreValidator.validateGameScore("mini_game", score, accuracyPercent, maxCombo, sessionToken)
        if (!validation.isValid) {
            val totalCoins = prefs.getInt(KEY_COINS, 0)
            val totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
            val streak = prefs.getInt(KEY_CURRENT_STREAK, 0)
            val levelProgress = getLevelProgress(totalXp)
            return RewardResult(
                coinsEarned = 0,
                totalCoins = totalCoins,
                xpEarned = 0,
                previousTotalXp = totalXp,
                totalXp = totalXp,
                previousLevel = levelProgress.level,
                currentLevel = levelProgress.level,
                didLevelUp = false,
                levelsGained = 0,
                levelProgress = levelProgress,
                xpBreakdown = XpBreakdown(0, 0, 0, 0, 0),
                streakDays = streak,
                isDuplicateIgnored = true
            )
        }

        // Check for duplicate session reward
        if (!sessionToken.isNullOrBlank()) {
            if (memoryProcessedSessions.contains(sessionToken) || isSessionAlreadyProcessedInPrefs(prefs, sessionToken)) {
                sessionRewardCache[sessionToken]?.let { return it }

                // Fallback: return current state without re-awarding
                val totalCoins = prefs.getInt(KEY_COINS, 0)
                val totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
                val streak = prefs.getInt(KEY_CURRENT_STREAK, 0)
                val levelProgress = getLevelProgress(totalXp)
                return RewardResult(
                    coinsEarned = 0,
                    totalCoins = totalCoins,
                    xpEarned = 0,
                    previousTotalXp = totalXp,
                    totalXp = totalXp,
                    previousLevel = levelProgress.level,
                    currentLevel = levelProgress.level,
                    didLevelUp = false,
                    levelsGained = 0,
                    levelProgress = levelProgress,
                    xpBreakdown = XpBreakdown(0, 0, 0, 0, 0),
                    streakDays = streak,
                    isDuplicateIgnored = true
                )
            }
        }

        val prevCoins = prefs.getInt(KEY_COINS, 0)
        val prevXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val prevGames = prefs.getInt(KEY_TOTAL_GAMES_PLAYED, 0)
        val prevStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_PLAYED_DATE, "")

        val prevLevel = getLevelFromTotalXp(prevXp)

        // Award 10 to 15 coins strictly per game
        val bonusCoins = when {
            accuracyPercent >= 90f || score >= 500 -> 5
            accuracyPercent >= 75f || score >= 300 -> 3
            accuracyPercent >= 50f || score >= 100 -> 2
            else -> 0
        }
        val coinsEarned = 10 + bonusCoins

        // Calculate performance-based XP
        val xpBreakdown = calculateGameXp(score, accuracyPercent, maxCombo)
        val xpEarned = xpBreakdown.totalEarned

        // Update streak based on date
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var newStreak = prevStreak
        if (lastDate != todayStr) {
            newStreak = if (prevStreak == 0) 1 else prevStreak + 1
        }

        val newCoins = prevCoins + coinsEarned
        val newXp = prevXp + xpEarned
        val newGames = prevGames + 1

        val newLevel = getLevelFromTotalXp(newXp)
        val didLevelUp = newLevel > prevLevel
        val levelsGained = (newLevel - prevLevel).coerceAtLeast(0)

        // Record session token to prevent duplicates
        if (!sessionToken.isNullOrBlank()) {
            memoryProcessedSessions.add(sessionToken)
            recordSessionInPrefs(prefs, sessionToken)
        }

        prefs.edit().apply {
            putInt(KEY_COINS, newCoins)
            putInt(KEY_TOTAL_XP, newXp)
            putInt(KEY_TOTAL_GAMES_PLAYED, newGames)
            putInt(KEY_CURRENT_STREAK, newStreak)
            putString(KEY_LAST_PLAYED_DATE, todayStr)
            apply()
        }
        updateTimestamp(context)

        AchievementManager.checkAndUnlockAchievements(context)
        if (didLevelUp) {
            com.example.audio.AudioManager.playLevelUpSound(context)
        }

        val newLevelProgress = getLevelProgress(newXp)

        val result = RewardResult(
            coinsEarned = coinsEarned,
            totalCoins = newCoins,
            xpEarned = xpEarned,
            previousTotalXp = prevXp,
            totalXp = newXp,
            previousLevel = prevLevel,
            currentLevel = newLevel,
            didLevelUp = didLevelUp,
            levelsGained = levelsGained,
            levelProgress = newLevelProgress,
            xpBreakdown = xpBreakdown,
            streakDays = newStreak,
            isDuplicateIgnored = false
        )

        if (!sessionToken.isNullOrBlank()) {
            sessionRewardCache[sessionToken] = result
        }

        return result
    }

    private fun isSessionAlreadyProcessedInPrefs(prefs: SharedPreferences, sessionToken: String): Boolean {
        val stored = prefs.getString(KEY_PROCESSED_SESSIONS, "") ?: ""
        return stored.split(",").contains(sessionToken)
    }

    private fun recordSessionInPrefs(prefs: SharedPreferences, sessionToken: String) {
        val stored = prefs.getString(KEY_PROCESSED_SESSIONS, "") ?: ""
        val list = if (stored.isBlank()) mutableListOf() else stored.split(",").toMutableList()
        list.add(sessionToken)
        // Keep the last 50 session tokens to prevent unbounded growth
        val trimmed = list.takeLast(50)
        prefs.edit().putString(KEY_PROCESSED_SESSIONS, trimmed.joinToString(",")).apply()
    }

    fun addRewardedBonus(context: Context, amount: Int) {
        val validation = ScoreValidator.validateRewardedAdBonus(amount)
        if (!validation.isValid) return

        val prefs = getPrefs(context)
        val currentCoins = prefs.getInt(KEY_COINS, 0)
        val currentXp = prefs.getInt(KEY_TOTAL_XP, 0)
        prefs.edit()
            .putInt(KEY_COINS, currentCoins + amount)
            .putInt(KEY_TOTAL_XP, currentXp + (amount * 2))
            .apply()
        updateTimestamp(context)
        AchievementManager.checkAndUnlockAchievements(context)
        com.example.audio.AudioManager.playRewardSound(context)
    }

    @Synchronized
    fun addCoins(context: Context, amount: Int) {
        if (amount <= 0) return
        val prefs = getPrefs(context)
        val currentCoins = prefs.getInt(KEY_COINS, 0)
        prefs.edit().putInt(KEY_COINS, currentCoins + amount).apply()
        updateTimestamp(context)
    }

    @Synchronized
    fun addXp(context: Context, amount: Int) {
        if (amount <= 0) return
        val prefs = getPrefs(context)
        val currentXp = prefs.getInt(KEY_TOTAL_XP, 0)
        prefs.edit().putInt(KEY_TOTAL_XP, currentXp + amount).apply()
        updateTimestamp(context)
    }

    fun getLastUpdatedTimestamp(context: Context): Long {
        return getPrefs(context).getLong("last_updated_timestamp", 0L)
    }

    private fun updateTimestamp(context: Context) {
        getPrefs(context).edit().putLong("last_updated_timestamp", System.currentTimeMillis()).apply()
        // Notify sync manager
        CloudSyncManager.onLocalDataUpdated(context)
    }

    fun overwriteLocalDataFromCloud(
        context: Context,
        coins: Int,
        xp: Int,
        gamesPlayed: Int,
        streak: Int,
        timestamp: Long
    ) {
        val validation = ScoreValidator.validateProfileData(coins, xp, gamesPlayed, streak)
        if (!validation.isValid) return

        getPrefs(context).edit().apply {
            putInt(KEY_COINS, coins)
            putInt(KEY_TOTAL_XP, xp)
            putInt(KEY_TOTAL_GAMES_PLAYED, gamesPlayed)
            putInt(KEY_CURRENT_STREAK, streak)
            putLong("last_updated_timestamp", timestamp)
            apply()
        }
        AchievementManager.checkAndUnlockAchievements(context)
    }
}
