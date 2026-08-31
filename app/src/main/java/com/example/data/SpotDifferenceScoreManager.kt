package com.example.data

import android.content.Context
import android.content.SharedPreferences

object SpotDifferenceScoreManager {
    private const val PREFS_NAME = "spot_difference_game_prefs"
    private const val KEY_BEST_SCORE = "spot_diff_best_score"
    private const val KEY_BEST_ACCURACY = "spot_diff_best_accuracy"
    private const val KEY_HIGHEST_ROUND = "spot_diff_highest_round"
    private const val KEY_TOTAL_DIFFERENCES_FOUND = "spot_diff_total_found"
    private const val KEY_GAMES_PLAYED = "spot_diff_games_played"
    private const val KEY_BEST_COMBO = "spot_diff_best_combo"

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

    fun getHighestRound(context: Context): Int {
        return getPrefs(context).getInt(KEY_HIGHEST_ROUND, 1)
    }

    fun getTotalDifferencesFound(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_DIFFERENCES_FOUND, 0)
    }

    fun getGamesPlayed(context: Context): Int {
        return getPrefs(context).getInt(KEY_GAMES_PLAYED, 0)
    }

    fun getBestCombo(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_COMBO, 0)
    }

    data class GameSaveResult(
        val isNewBestScore: Boolean,
        val previousBestScore: Int,
        val newBestScore: Int,
        val bestAccuracy: Float,
        val highestRound: Int,
        val xpEarned: Int,
        val coinsEarned: Int,
        val totalCoins: Int
    )

    fun recordGameResult(
        context: Context,
        score: Int,
        accuracy: Float,
        maxCombo: Int,
        differencesFound: Int,
        roundReached: Int,
        sessionToken: String? = null
    ): GameSaveResult {
        val prefs = getPrefs(context)
        val prevBestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        val prevBestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f)
        val prevHighestRound = prefs.getInt(KEY_HIGHEST_ROUND, 1)
        val prevTotalFound = prefs.getInt(KEY_TOTAL_DIFFERENCES_FOUND, 0)
        val prevGamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)

        val isNewBestScore = score > prevBestScore
        val newBestScore = if (isNewBestScore) score else prevBestScore
        val newBestCombo = if (maxCombo > prevBestCombo) maxCombo else prevBestCombo
        val newBestAccuracy = if (accuracy > prevBestAccuracy) accuracy else prevBestAccuracy
        val newHighestRound = if (roundReached > prevHighestRound) roundReached else prevHighestRound

        // Moderate XP
        val baseScoreXp = (score / 20).coerceAtMost(30)
        val diffXp = (differencesFound * 3).coerceAtMost(25)
        val comboXp = (maxCombo * 2).coerceAtMost(15)
        val xpEarned = baseScoreXp + diffXp + comboXp + 10

        // User Profile Rewards: 10 to 15 coins per game
        val rewardResult = UserProfileManager.awardGameRewards(context, score, accuracy, maxCombo, sessionToken)

        if (DailyChallengeManager.getTodayChallengeGame().first == "spot") {
            DailyChallengeManager.completeDailyChallenge(context, score)
        }

        com.example.data.stats.StatsManager.recordGameEnd(
            context = context,
            gameId = "spot_difference",
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            playTimeSeconds = 30,
            sessionToken = sessionToken
        )

        prefs.edit().apply {
            putInt(KEY_BEST_SCORE, newBestScore)
            putFloat(KEY_BEST_ACCURACY, newBestAccuracy)
            putInt(KEY_HIGHEST_ROUND, newHighestRound)
            putInt(KEY_TOTAL_DIFFERENCES_FOUND, prevTotalFound + differencesFound)
            putInt(KEY_GAMES_PLAYED, prevGamesPlayed + 1)
            putInt(KEY_BEST_COMBO, newBestCombo)
            apply()
        }

        return GameSaveResult(
            isNewBestScore = isNewBestScore,
            previousBestScore = prevBestScore,
            newBestScore = newBestScore,
            bestAccuracy = newBestAccuracy,
            highestRound = newHighestRound,
            xpEarned = xpEarned,
            coinsEarned = rewardResult.coinsEarned,
            totalCoins = rewardResult.totalCoins
        )
    }
}
