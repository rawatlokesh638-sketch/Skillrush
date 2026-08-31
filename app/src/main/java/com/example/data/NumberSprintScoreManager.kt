package com.example.data

import android.content.Context
import android.content.SharedPreferences

object NumberSprintScoreManager {
    private const val PREFS_NAME = "skillrush_number_sprint_prefs"
    private const val KEY_BEST_SCORE = "number_sprint_best_score"
    private const val KEY_BEST_ACCURACY = "number_sprint_best_accuracy"
    private const val KEY_BEST_COMBO = "number_sprint_best_combo"
    private const val KEY_HIGHEST_STREAK = "number_sprint_highest_streak"
    private const val KEY_TOTAL_XP = "number_sprint_total_xp"
    private const val KEY_GAMES_PLAYED = "number_sprint_games_played"
    private const val KEY_QUESTIONS_SOLVED = "number_sprint_questions_solved"

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

    fun getTotalQuestionsSolved(context: Context): Int {
        return getPrefs(context).getInt(KEY_QUESTIONS_SOLVED, 0)
    }

    data class SaveResult(
        val isNewBestScore: Boolean,
        val previousBestScore: Int,
        val newBestScore: Int,
        val bestAccuracy: Float,
        val xpEarned: Int,
        val coinsEarned: Int,
        val totalCoins: Int
    )

    fun recordGameResult(
        context: Context,
        score: Int,
        accuracy: Float,
        maxCombo: Int,
        solvedQuestions: Int,
        sessionToken: String? = null
    ): SaveResult {
        val prefs = getPrefs(context)
        val prevBestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val prevBestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f)
        val prevTotalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val prevSolved = prefs.getInt(KEY_QUESTIONS_SOLVED, 0)
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)

        val isNewBestScore = score > prevBestScore
        val newBestScore = if (isNewBestScore) score else prevBestScore
        val newBestCombo = if (maxCombo > prevBestCombo) maxCombo else prevBestCombo
        val newBestAccuracy = if (accuracy > prevBestAccuracy) accuracy else prevBestAccuracy

        // Moderate XP
        val baseScoreXp = (score / 10).coerceAtMost(30)
        val comboXp = (maxCombo * 2).coerceAtMost(15)
        val solvedXp = (solvedQuestions * 2).coerceAtMost(20)
        val xpEarned = baseScoreXp + comboXp + solvedXp + 10

        // User Profile Rewards: 10 to 15 coins per game
        val rewardResult = UserProfileManager.awardGameRewards(context, score, accuracy, maxCombo, sessionToken)

        if (DailyChallengeManager.getTodayChallengeGame().first == "number") {
            DailyChallengeManager.completeDailyChallenge(context, score)
        }

        com.example.data.stats.StatsManager.recordGameEnd(
            context = context,
            gameId = "number_sprint",
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
            putInt(KEY_TOTAL_XP, prevTotalXp + xpEarned)
            putInt(KEY_QUESTIONS_SOLVED, prevSolved + solvedQuestions)
            putInt(KEY_GAMES_PLAYED, gamesPlayed + 1)
            apply()
        }

        return SaveResult(
            isNewBestScore = isNewBestScore,
            previousBestScore = prevBestScore,
            newBestScore = newBestScore,
            bestAccuracy = newBestAccuracy,
            xpEarned = xpEarned,
            coinsEarned = rewardResult.coinsEarned,
            totalCoins = rewardResult.totalCoins
        )
    }
}
