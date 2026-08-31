package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val rewardXp: Int,
    val rewardCoins: Int,
    val targetValue: Int
)

data class AchievementStatus(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val currentValue: Int,
    val progressFraction: Float
)

object AchievementManager {
    private const val PREFS_NAME = "skillrush_achievements_prefs"

    val achievementsList = listOf(
        Achievement("first_game", "First Steps", "Play your first mini-game", 30, 10, 1),
        Achievement("games_10", "Arcade Regular", "Complete 10 mini-games", 50, 20, 10),
        Achievement("games_100", "Dedicated Gamer", "Complete 100 mini-games", 150, 50, 100),
        Achievement("score_100", "Century Club", "Score 100 or higher in any game", 40, 15, 100),
        Achievement("score_500", "High Scorer", "Score 500 or higher in any game", 100, 35, 500),
        Achievement("perfect_accuracy", "Dead Eye", "Achieve 100% accuracy in any game", 75, 25, 100),
        Achievement("combo_5", "Combo Chain", "Reach a 5+ combo in any game", 50, 20, 5),
        Achievement("level_5", "Rising Star", "Reach Level 5", 80, 30, 5),
        Achievement("level_10", "Elite Reflexer", "Reach Level 10", 150, 50, 10),
        Achievement("streak_7", "Week Warrior", "Maintain a 7-day daily streak", 120, 40, 7),
        Achievement("daily_challenge", "Daily Champion", "Complete a Daily Challenge", 50, 20, 1),
        Achievement("master_tap", "Tap Master", "Score 300+ in Tap Rush", 100, 30, 300),
        Achievement("master_memory", "Memory Master", "Score 300+ in Memory Flash", 100, 30, 300),
        Achievement("master_aim", "Aim Master", "Score 300+ in Perfect Aim", 100, 30, 300),
        Achievement("master_number", "Sprint Master", "Score 300+ in Number Sprint", 100, 30, 300),
        Achievement("master_spot", "Spot Master", "Score 300+ in Spot Difference", 100, 30, 300),
        Achievement("master_speed", "Speed Master", "Score 300+ in Speed Rush", 100, 30, 300)
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isUnlocked(context: Context, achievementId: String): Boolean {
        return getPrefs(context).getBoolean("unlocked_$achievementId", false)
    }

    private fun unlockAchievement(context: Context, achievement: Achievement): Boolean {
        val prefs = getPrefs(context)
        val key = "unlocked_${achievement.id}"
        if (prefs.getBoolean(key, false)) return false

        // Mark unlocked
        prefs.edit().putBoolean(key, true).apply()

        // Award XP and Coins via UserProfileManager SharedPreferences directly or rewards
        val userPrefs = context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE)
        val currentCoins = userPrefs.getInt("user_coins_balance", 0)
        val currentXp = userPrefs.getInt("user_total_xp", 0)
        userPrefs.edit()
            .putInt("user_coins_balance", currentCoins + achievement.rewardCoins)
            .putInt("user_total_xp", currentXp + achievement.rewardXp)
            .apply()

        return true
    }

    fun getCurrentValueForAchievement(context: Context, achievementId: String): Int {
        val gamesPlayed = UserProfileManager.getTotalGamesPlayed(context)
        val totalXp = UserProfileManager.getTotalXp(context)
        val level = UserProfileManager.getLevel(context)
        val streak = UserProfileManager.getStreakDays(context)

        val tapScore = TapRushScoreManager.getBestScore(context)
        val tapCombo = TapRushScoreManager.getBestCombo(context)
        val memScore = MemoryFlashScoreManager.getBestScore(context)
        val aimScore = PerfectAimScoreManager.getBestScore(context)
        val aimAcc = PerfectAimScoreManager.getBestAccuracy(context).toInt()
        val aimCombo = PerfectAimScoreManager.getBestCombo(context)
        val numScore = NumberSprintScoreManager.getBestScore(context)
        val numAcc = NumberSprintScoreManager.getBestAccuracy(context).toInt()
        val numCombo = NumberSprintScoreManager.getBestCombo(context)
        val spotScore = SpotDifferenceScoreManager.getBestScore(context)
        val spotAcc = SpotDifferenceScoreManager.getBestAccuracy(context).toInt()
        val speedScore = SpeedRushScoreManager.getBestScore(context)
        val speedAcc = SpeedRushScoreManager.getBestAccuracy(context).toInt()

        val maxScore = maxOf(tapScore, memScore, aimScore, numScore, spotScore, speedScore)
        val maxAcc = maxOf(aimAcc, numAcc, spotAcc, speedAcc)
        val maxCombo = maxOf(tapCombo, aimCombo, numCombo)
        val dailyCompleted = if (DailyChallengeManager.isCompletedToday(context) || prefs(context).getBoolean("daily_completed_ever", false)) 1 else 0

        return when (achievementId) {
            "first_game" -> gamesPlayed.coerceAtMost(1)
            "games_10" -> gamesPlayed.coerceAtMost(10)
            "games_100" -> gamesPlayed.coerceAtMost(100)
            "score_100" -> maxScore.coerceAtMost(100)
            "score_500" -> maxScore.coerceAtMost(500)
            "perfect_accuracy" -> maxAcc.coerceAtMost(100)
            "combo_5" -> maxCombo.coerceAtMost(5)
            "level_5" -> level.coerceAtMost(5)
            "level_10" -> level.coerceAtMost(10)
            "streak_7" -> streak.coerceAtMost(7)
            "daily_challenge" -> dailyCompleted
            "master_tap" -> tapScore.coerceAtMost(300)
            "master_memory" -> memScore.coerceAtMost(300)
            "master_aim" -> aimScore.coerceAtMost(300)
            "master_number" -> numScore.coerceAtMost(300)
            "master_spot" -> spotScore.coerceAtMost(300)
            "master_speed" -> speedScore.coerceAtMost(300)
            else -> 0
        }
    }

    private fun prefs(context: Context): SharedPreferences = getPrefs(context)

    /**
     * Checks all achievements, unlocks any newly qualified ones, awards their bonuses,
     * and returns a list of newly unlocked achievements for toasts/animations.
     */
    @Synchronized
    fun checkAndUnlockAchievements(context: Context): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()

        if (DailyChallengeManager.isCompletedToday(context)) {
            getPrefs(context).edit().putBoolean("daily_completed_ever", true).apply()
        }

        for (achievement in achievementsList) {
            if (isUnlocked(context, achievement.id)) continue

            val current = getCurrentValueForAchievement(context, achievement.id)
            if (current >= achievement.targetValue) {
                if (unlockAchievement(context, achievement)) {
                    newlyUnlocked.add(achievement)
                }
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            com.example.audio.AudioManager.playAchievementSound(context)
        }

        return newlyUnlocked
    }

    fun getAchievementStatuses(context: Context): List<AchievementStatus> {
        checkAndUnlockAchievements(context)
        return achievementsList.map { achievement ->
            val unlocked = isUnlocked(context, achievement.id)
            val current = getCurrentValueForAchievement(context, achievement.id)
            val fraction = if (achievement.targetValue > 0) {
                (current.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)
            } else {
                if (unlocked) 1f else 0f
            }
            AchievementStatus(
                achievement = achievement,
                isUnlocked = unlocked,
                currentValue = current,
                progressFraction = fraction
            )
        }
    }

    fun getUnlockedCount(context: Context): Int {
        val statuses = getAchievementStatuses(context)
        return statuses.count { it.isUnlocked }
    }
}
