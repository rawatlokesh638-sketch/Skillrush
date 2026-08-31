package com.example.data

import android.content.Context
import android.content.SharedPreferences

object PerfectAimScoreManager {
    private const val PREFS_NAME = "skillrush_perfect_aim_prefs"
    private const val KEY_BEST_SCORE = "perfect_aim_best_score"
    private const val KEY_BEST_ACCURACY = "perfect_aim_best_accuracy"
    private const val KEY_BEST_COMBO = "perfect_aim_best_combo"
    private const val KEY_TOTAL_XP = "perfect_aim_total_xp"
    private const val KEY_GAMES_PLAYED = "perfect_aim_games_played"
    private const val KEY_BULLSEYES_HIT = "perfect_aim_bullseyes_hit"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveBestScore(context: Context, score: Int) {
        getPrefs(context).edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    fun getBestScore(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_SCORE, 0)
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

    fun getTotalBullseyes(context: Context): Int {
        return getPrefs(context).getInt(KEY_BULLSEYES_HIT, 0)
    }

    data class SaveResult(
        val isNewBestScore: Boolean,
        val previousBestScore: Int,
        val newBestScore: Int,
        val bestAccuracy: Float,
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
        accuracy: Float,
        maxCombo: Int,
        bullseyes: Int,
        sessionToken: String? = null
    ): SaveResult {
        val prefs = getPrefs(context)
        val prevBestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val prevBestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f)
        val prevTotalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val prevBullseyes = prefs.getInt(KEY_BULLSEYES_HIT, 0)
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)

        val isNewBestScore = score > prevBestScore
        val newBestScore = if (isNewBestScore) score else prevBestScore
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

        if (DailyChallengeManager.getTodayChallengeGame().first == "aim") {
            DailyChallengeManager.completeDailyChallenge(context, score)
        }

        com.example.data.stats.StatsManager.recordGameEnd(
            context = context,
            gameId = "perfect_aim",
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            playTimeSeconds = 30,
            sessionToken = sessionToken
        )

        prefs.edit().apply {
            putInt(KEY_BEST_SCORE, newBestScore)
            putInt(KEY_BEST_COMBO, newBestCombo)
            putFloat(KEY_BEST_ACCURACY, newBestAccuracy)
            putInt(KEY_TOTAL_XP, prevTotalXp + rewardResult.xpEarned)
            putInt(KEY_BULLSEYES_HIT, prevBullseyes + bullseyes)
            putInt(KEY_GAMES_PLAYED, gamesPlayed + 1)
            apply()
        }

        return SaveResult(
            isNewBestScore = isNewBestScore,
            previousBestScore = prevBestScore,
            newBestScore = newBestScore,
            bestAccuracy = newBestAccuracy,
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
