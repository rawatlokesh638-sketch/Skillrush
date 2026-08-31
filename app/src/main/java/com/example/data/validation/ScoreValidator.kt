package com.example.data.validation

import android.util.Log

data class ScoreValidationResult(
    val isValid: Boolean,
    val reason: String = ""
)

object ScoreValidator {
    private const val TAG = "ScoreValidator"

    private fun logWarn(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (e: Throwable) {
            println("[$tag] $msg")
        }
    }

    // Maximum plausible scores per single mini-game session
    private const val MAX_SINGLE_GAME_SCORE = 3000
    private const val MAX_COMPOSITE_SCORE = 18000

    /**
     * Validates mini-game result payload metrics.
     */
    fun validateGameScore(
        gameCategory: String,
        score: Int,
        accuracyPercent: Float = 100f,
        maxCombo: Int = 0,
        sessionToken: String? = null
    ): ScoreValidationResult {
        if (score < 0) {
            logWarn(TAG, "Rejected negative score ($score) for game $gameCategory")
            return ScoreValidationResult(false, "Score cannot be negative")
        }

        val maxAllowed = if (gameCategory == "global") MAX_COMPOSITE_SCORE else MAX_SINGLE_GAME_SCORE
        if (score > maxAllowed) {
            logWarn(TAG, "Rejected impossible high score ($score > $maxAllowed) for game $gameCategory")
            return ScoreValidationResult(false, "Score exceeds maximum plausible limit")
        }

        if (accuracyPercent !in 0f..100f) {
            logWarn(TAG, "Rejected invalid accuracy ($accuracyPercent%) for game $gameCategory")
            return ScoreValidationResult(false, "Accuracy must be between 0% and 100%")
        }

        if (maxCombo < 0 || maxCombo > 500) {
            logWarn(TAG, "Rejected invalid combo ($maxCombo) for game $gameCategory")
            return ScoreValidationResult(false, "Max combo out of plausible range")
        }

        if (sessionToken != null && (sessionToken.length < 5 || sessionToken.length > 128)) {
            logWarn(TAG, "Rejected invalid session token length (${sessionToken.length})")
            return ScoreValidationResult(false, "Invalid session token structure")
        }

        return ScoreValidationResult(true)
    }

    /**
     * Validates profile metrics before cloud synchronization or local save.
     */
    fun validateProfileData(
        coins: Int,
        xp: Int,
        gamesPlayed: Int,
        streak: Int
    ): ScoreValidationResult {
        if (coins < 0 || coins > 1_000_000) {
            logWarn(TAG, "Invalid coins amount: $coins")
            return ScoreValidationResult(false, "Coins balance out of valid range (0..1,000,000)")
        }

        if (xp < 0 || xp > 1_000_000) {
            logWarn(TAG, "Invalid XP amount: $xp")
            return ScoreValidationResult(false, "Total XP out of valid range (0..1,000,000)")
        }

        if (gamesPlayed < 0 || gamesPlayed > 100_000) {
            logWarn(TAG, "Invalid games played: $gamesPlayed")
            return ScoreValidationResult(false, "Games played count out of valid range")
        }

        if (streak < 0 || streak > 3650) {
            logWarn(TAG, "Invalid streak: $streak")
            return ScoreValidationResult(false, "Streak days out of valid range")
        }

        return ScoreValidationResult(true)
    }

    /**
     * Validates leaderboard entry fields before sending to Firebase RTDB.
     */
    fun validateLeaderboardUpload(
        score: Int,
        name: String,
        level: Int,
        badge: String
    ): ScoreValidationResult {
        if (score <= 0 || score > MAX_COMPOSITE_SCORE) {
            logWarn(TAG, "Leaderboard score out of range: $score")
            return ScoreValidationResult(false, "Leaderboard score must be between 1 and $MAX_COMPOSITE_SCORE")
        }

        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 30) {
            logWarn(TAG, "Leaderboard player name length invalid: '${trimmedName}'")
            return ScoreValidationResult(false, "Player name must be between 1 and 30 characters")
        }

        if (level < 1 || level > 100) {
            logWarn(TAG, "Leaderboard level out of range: $level")
            return ScoreValidationResult(false, "Player level must be between 1 and 100")
        }

        if (badge.length > 30) {
            logWarn(TAG, "Leaderboard badge text too long: '${badge}'")
            return ScoreValidationResult(false, "Badge string too long")
        }

        return ScoreValidationResult(true)
    }

    /**
     * Validates rewarded ad bonus amount.
     */
    fun validateRewardedAdBonus(amount: Int): ScoreValidationResult {
        if (amount <= 0 || amount > 100) {
            logWarn(TAG, "Invalid rewarded ad bonus amount: $amount")
            return ScoreValidationResult(false, "Rewarded ad bonus must be between 1 and 100 coins")
        }
        return ScoreValidationResult(true)
    }
}
