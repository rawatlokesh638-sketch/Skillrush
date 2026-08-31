package com.example.data

import android.content.Context
import android.content.SharedPreferences

object SpeedRushScoreManager {
    private const val PREFS_NAME = "speed_rush_game_prefs"
    private const val KEY_BEST_SCORE = "speed_rush_best_score"
    private const val KEY_BEST_ACCURACY = "speed_rush_best_accuracy"
    private const val KEY_BEST_REACTION_MS = "speed_rush_best_reaction_ms"
    private const val KEY_BEST_COMBO = "speed_rush_best_combo"
    private const val KEY_TOTAL_TARGETS_HIT = "speed_rush_total_hits"
    private const val KEY_GAMES_PLAYED = "speed_rush_games_played"

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

    fun getBestReactionMs(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_REACTION_MS, 0)
    }

    fun getBestCombo(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_COMBO, 0)
    }

    fun getTotalTargetsHit(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_TARGETS_HIT, 0)
    }

    fun getGamesPlayed(context: Context): Int {
        return getPrefs(context).getInt(KEY_GAMES_PLAYED, 0)
    }

    data class GameSaveResult(
        val isNewBestScore: Boolean,
        val previousBestScore: Int,
        val newBestScore: Int,
        val bestAccuracy: Float,
        val bestReactionMs: Int,
        val xpEarned: Int,
        val coinsEarned: Int,
        val totalCoins: Int
    )

    fun recordGameResult(
        context: Context,
        score: Int,
        accuracy: Float,
        maxCombo: Int,
        avgReactionMs: Int,
        targetsHit: Int,
        sessionToken: String? = null
    ): GameSaveResult {
        val prefs = getPrefs(context)
        val prevBestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        val prevBestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f)
        val prevBestReactionMs = prefs.getInt(KEY_BEST_REACTION_MS, 0)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val prevTotalHits = prefs.getInt(KEY_TOTAL_TARGETS_HIT, 0)
        val prevGamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)

        val isNewBestScore = score > prevBestScore
        val newBestScore = if (isNewBestScore) score else prevBestScore
        val newBestCombo = if (maxCombo > prevBestCombo) maxCombo else prevBestCombo
        val newBestAccuracy = if (accuracy > prevBestAccuracy) accuracy else prevBestAccuracy
        val newBestReactionMs = if (prevBestReactionMs == 0 || (avgReactionMs in 1 until prevBestReactionMs)) {
            avgReactionMs
        } else {
            prevBestReactionMs
        }

        // Standardized XP calculation
        val baseScoreXp = (score / 25).coerceAtMost(30)
        val comboXp = (maxCombo * 2).coerceAtMost(15)
        val accuracyXp = (accuracy * 0.2f).toInt().coerceAtMost(20)
        val xpEarned = baseScoreXp + comboXp + accuracyXp + 10

        // User Profile Rewards: 10 to 15 coins per game
        val rewardResult = UserProfileManager.awardGameRewards(context, score, accuracy, maxCombo, sessionToken)

        if (DailyChallengeManager.getTodayChallengeGame().first == "speed") {
            DailyChallengeManager.completeDailyChallenge(context, score)
        }

        com.example.data.stats.StatsManager.recordGameEnd(
            context = context,
            gameId = "speed_rush",
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            playTimeSeconds = 30,
            sessionToken = sessionToken
        )

        prefs.edit().apply {
            putInt(KEY_BEST_SCORE, newBestScore)
            putFloat(KEY_BEST_ACCURACY, newBestAccuracy)
            putInt(KEY_BEST_REACTION_MS, newBestReactionMs)
            putInt(KEY_BEST_COMBO, newBestCombo)
            putInt(KEY_TOTAL_TARGETS_HIT, prevTotalHits + targetsHit)
            putInt(KEY_GAMES_PLAYED, prevGamesPlayed + 1)
            apply()
        }

        return GameSaveResult(
            isNewBestScore = isNewBestScore,
            previousBestScore = prevBestScore,
            newBestScore = newBestScore,
            bestAccuracy = newBestAccuracy,
            bestReactionMs = newBestReactionMs,
            xpEarned = xpEarned,
            coinsEarned = rewardResult.coinsEarned,
            totalCoins = rewardResult.totalCoins
        )
    }
}
