package com.example.data

import android.content.Context
import android.content.SharedPreferences

object MemoryFlashScoreManager {
    private const val PREFS_NAME = "skillrush_memory_flash_prefs"
    private const val KEY_BEST_SCORE = "memory_flash_best_score"
    private const val KEY_HIGHEST_LEVEL = "memory_flash_highest_level"
    private const val KEY_BEST_ACCURACY = "memory_flash_best_accuracy"
    private const val KEY_BEST_COMBO = "memory_flash_best_combo"
    private const val KEY_TOTAL_XP = "memory_flash_total_xp"
    private const val KEY_GAMES_PLAYED = "memory_flash_games_played"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveBestScore(context: Context, score: Int) {
        getPrefs(context).edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    fun getBestScore(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_SCORE, 0)
    }

    fun getHighestLevel(context: Context): Int {
        return getPrefs(context).getInt(KEY_HIGHEST_LEVEL, 0)
    }

    fun getBestAccuracy(context: Context): Float {
        return getPrefs(context).getFloat(KEY_BEST_ACCURACY, 0f)
    }

    fun getBestCombo(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_COMBO, 0)
    }

    fun getTotalXp(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_XP, 0)
    }

    fun getGamesPlayed(context: Context): Int {
        return getPrefs(context).getInt(KEY_GAMES_PLAYED, 0)
    }

    data class SaveResult(
        val isNewBestScore: Boolean,
        val isNewHighestLevel: Boolean,
        val previousBestScore: Int,
        val newBestScore: Int,
        val highestLevel: Int,
        val xpEarned: Int,
        val coinsEarned: Int,
        val totalCoins: Int,
        val didLevelUp: Boolean,
        val previousLevel: Int,
        val newLevel: Int,
        val levelProgress: LevelProgress,
        val xpBreakdown: XpBreakdown
    )

    fun recordGameResult(
        context: Context,
        score: Int,
        level: Int,
        accuracy: Float,
        maxCombo: Int,
        sessionToken: String? = null
    ): SaveResult {
        val prefs = getPrefs(context)
        val prevBestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        val prevHighestLevel = prefs.getInt(KEY_HIGHEST_LEVEL, 0)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val prevBestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f)
        val prevTotalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)

        val isNewBestScore = score > prevBestScore
        val isNewHighestLevel = level > prevHighestLevel
        val newBestScore = if (isNewBestScore) score else prevBestScore
        val newHighestLevel = if (isNewHighestLevel) level else prevHighestLevel
        val newBestCombo = if (maxCombo > prevBestCombo) maxCombo else prevBestCombo
        val newBestAccuracy = if (accuracy > prevBestAccuracy) accuracy else prevBestAccuracy

        // User Profile Rewards
        val rewardResult = UserProfileManager.awardGameRewards(
            context = context,
            score = score,
            accuracyPercent = accuracy,
            maxCombo = maxCombo,
            sessionToken = sessionToken
        )

        if (DailyChallengeManager.getTodayChallengeGame().first == "memory") {
            DailyChallengeManager.completeDailyChallenge(context, score)
        }

        com.example.data.stats.StatsManager.recordGameEnd(
            context = context,
            gameId = "memory_flash",
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            playTimeSeconds = 30,
            sessionToken = sessionToken
        )

        prefs.edit().apply {
            putInt(KEY_BEST_SCORE, newBestScore)
            putInt(KEY_HIGHEST_LEVEL, newHighestLevel)
            putInt(KEY_BEST_COMBO, newBestCombo)
            putFloat(KEY_BEST_ACCURACY, newBestAccuracy)
            putInt(KEY_TOTAL_XP, prevTotalXp + rewardResult.xpEarned)
            putInt(KEY_GAMES_PLAYED, gamesPlayed + 1)
            apply()
        }

        return SaveResult(
            isNewBestScore = isNewBestScore,
            isNewHighestLevel = isNewHighestLevel,
            previousBestScore = prevBestScore,
            newBestScore = newBestScore,
            highestLevel = newHighestLevel,
            xpEarned = rewardResult.xpEarned,
            coinsEarned = rewardResult.coinsEarned,
            totalCoins = rewardResult.totalCoins,
            didLevelUp = rewardResult.didLevelUp,
            previousLevel = rewardResult.previousLevel,
            newLevel = rewardResult.currentLevel,
            levelProgress = rewardResult.levelProgress,
            xpBreakdown = rewardResult.xpBreakdown
        )
    }
}
