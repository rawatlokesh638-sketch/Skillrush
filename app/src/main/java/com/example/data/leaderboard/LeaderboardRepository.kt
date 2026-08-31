package com.example.data.leaderboard

import android.content.Context
import com.example.data.MemoryFlashScoreManager
import com.example.data.NumberSprintScoreManager
import com.example.data.PerfectAimScoreManager
import com.example.data.SpeedRushScoreManager
import com.example.data.SpotDifferenceScoreManager
import com.example.data.TapRushScoreManager
import com.example.data.UserProfileManager
import kotlinx.coroutines.delay

enum class LeaderboardTimeframe(val label: String, val description: String) {
    DAILY("Daily", "Resets every 24h at midnight UTC"),
    WEEKLY("Weekly", "Current competitive weekly season"),
    ALL_TIME("All-Time", "Hall of Fame lifetime records")
}

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val name: String,
    val score: Int,
    val isCurrentPlayer: Boolean,
    val level: Int,
    val avatarColorHex: Long = 0xFF3D5AFE,
    val badge: String = "Contender",
    val gamesPlayed: Int = 1,
    val streakDays: Int = 1
)

sealed class LeaderboardUiState {
    data object Loading : LeaderboardUiState()
    data class Success(
        val entries: List<LeaderboardEntry>,
        val currentUserEntry: LeaderboardEntry?,
        val timeframe: LeaderboardTimeframe,
        val lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
    data object Empty : LeaderboardUiState()
}

/**
 * Clean data layer abstraction.
 * This interface can be swapped or backed with Firebase Firestore / Cloud Functions
 * without changing the Leaderboard UI layer.
 */
interface LeaderboardRepository {
    suspend fun getLeaderboard(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory = LeaderboardGameCategory.GLOBAL): List<LeaderboardEntry>
    suspend fun getCurrentUserEntry(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory = LeaderboardGameCategory.GLOBAL): LeaderboardEntry
}

class LocalLeaderboardRepository(
    private val context: Context
) : LeaderboardRepository {

    /**
     * Calculates the local player's composite score across all completed micro-games.
     */
    fun calculatePlayerCompositeScore(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory): Int {
        val tapRush = TapRushScoreManager.getBestScore(context)
        val memoryFlash = MemoryFlashScoreManager.getBestScore(context)
        val perfectAim = PerfectAimScoreManager.getBestScore(context)
        val numberSprint = NumberSprintScoreManager.getBestScore(context)
        val spotDiff = SpotDifferenceScoreManager.getBestScore(context)
        val speedRush = SpeedRushScoreManager.getBestScore(context)
        val totalXp = UserProfileManager.getTotalXp(context)

        val totalGameScores = tapRush + memoryFlash + perfectAim + numberSprint + spotDiff + speedRush

        // If player has played any games, baseline composite score
        val baseScore = if (totalGameScores > 0) {
            totalGameScores + (totalXp / 2)
        } else {
            0
        }

        return when (timeframe) {
            LeaderboardTimeframe.DAILY -> (baseScore * 0.45f).toInt()
            LeaderboardTimeframe.WEEKLY -> (baseScore * 0.75f).toInt()
            LeaderboardTimeframe.ALL_TIME -> baseScore
        }
    }

    override suspend fun getCurrentUserEntry(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory ): LeaderboardEntry {
        val playerName = UserProfileManager.getPlayerName(context)
        val level = UserProfileManager.getLevel(context)
        val gamesPlayed = UserProfileManager.getTotalGamesPlayed(context)
        val streak = UserProfileManager.getStreakDays(context)
        val score = calculatePlayerCompositeScore(timeframe, category)
        val userUid = UserProfileManager.getUserUid(context)

        val badge = when {
            level >= 20 || score >= 8000 -> "Grandmaster"
            level >= 10 || score >= 4000 -> "Elite Reflex"
            level >= 5 || score >= 1500 -> "Challenger"
            else -> "Novice"
        }

        return LeaderboardEntry(
            rank = 1,
            userId = userUid,
            name = playerName,
            score = score,
            isCurrentPlayer = true,
            level = level,
            avatarColorHex = 0xFF3D5AFE,
            badge = badge,
            gamesPlayed = gamesPlayed,
            streakDays = streak
        )
    }

    override suspend fun getLeaderboard(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory ): List<LeaderboardEntry> {
        delay(150L)

        val userEntry = getCurrentUserEntry(timeframe, category)
        if (userEntry.score <= 0) {
            return emptyList()
        }

        return listOf(userEntry.copy(rank = 1))
    }
}
