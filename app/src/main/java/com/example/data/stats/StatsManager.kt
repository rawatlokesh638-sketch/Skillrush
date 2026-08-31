package com.example.data.stats

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.MemoryFlashScoreManager
import com.example.data.NumberSprintScoreManager
import com.example.data.PerfectAimScoreManager
import com.example.data.SpeedRushScoreManager
import com.example.data.SpotDifferenceScoreManager
import com.example.data.TapRushScoreManager
import com.example.data.UserProfileManager
import com.example.data.validation.ScoreValidator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

data class OverallStats(
    val totalGamesPlayed: Int,
    val totalGamesCompleted: Int,
    val totalScoreSum: Long,
    val highestScore: Int,
    val averageScore: Float,
    val overallAccuracy: Float,
    val bestCombo: Int,
    val totalXpEarned: Int,
    val totalCoinsEarned: Int,
    val totalPlayTimeSeconds: Long
)

data class GameStatItem(
    val gameId: String,
    val title: String,
    val gamesPlayed: Int,
    val gamesCompleted: Int,
    val bestScore: Int,
    val bestAccuracy: Float,
    val bestCombo: Int,
    val totalScore: Long,
    val totalPlayTimeSeconds: Long,
    val averageScore: Float
)

data class DailyActivityItem(
    val dateString: String,
    val dayLabel: String,
    val gamesPlayed: Int,
    val totalScore: Int,
    val playTimeSeconds: Long,
    val isToday: Boolean
)

object StatsManager {
    private const val TAG = "StatsManager"
    private const val PREFS_NAME = "skillrush_stats_prefs"

    // Key Constants
    private const val KEY_TOTAL_GAMES_PLAYED = "stats_total_games_played"
    private const val KEY_TOTAL_GAMES_COMPLETED = "stats_total_games_completed"
    private const val KEY_TOTAL_SCORE_SUM = "stats_total_score_sum"
    private const val KEY_HIGHEST_SCORE = "stats_highest_score"
    private const val KEY_ACCURACY_SUM = "stats_total_accuracy_sum"
    private const val KEY_BEST_COMBO = "stats_best_combo"
    private const val KEY_PLAY_TIME_SECONDS = "stats_total_play_time_seconds"
    private const val KEY_PROCESSED_SESSIONS = "stats_processed_session_tokens"
    private const val KEY_INITIALIZED_MIGRATION = "stats_initialized_migration"

    private val memoryProcessedSessions = Collections.synchronizedSet(HashSet<String>())

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val GAME_CONFIGS = mapOf(
        "tap_rush" to "Tap Rush",
        "memory_flash" to "Memory Flash",
        "perfect_aim" to "Perfect Aim",
        "number_sprint" to "Number Sprint",
        "spot_difference" to "Spot Difference",
        "speed_rush" to "Speed Rush"
    )

    /**
     * Checks if initial migration from legacy score managers is needed and executes it once.
     */
    @Synchronized
    fun ensureInitialMigration(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_INITIALIZED_MIGRATION, false)) return

        val profilePlayed = UserProfileManager.getTotalGamesPlayed(context)
        val tapRushPlayed = TapRushScoreManager.getGamesPlayed(context)
        val memoryPlayed = MemoryFlashScoreManager.getGamesPlayed(context)
        val aimPlayed = PerfectAimScoreManager.getGamesPlayed(context)
        val numberPlayed = NumberSprintScoreManager.getGamesPlayed(context)
        val spotPlayed = SpotDifferenceScoreManager.getGamesPlayed(context)
        val speedPlayed = SpeedRushScoreManager.getGamesPlayed(context)

        val totalLegacyPlayed = (tapRushPlayed + memoryPlayed + aimPlayed + numberPlayed + spotPlayed + speedPlayed).coerceAtLeast(profilePlayed)

        val tapRushBest = TapRushScoreManager.getBestScore(context)
        val memoryBest = MemoryFlashScoreManager.getBestScore(context)
        val aimBest = PerfectAimScoreManager.getBestScore(context)
        val numberBest = NumberSprintScoreManager.getBestScore(context)
        val spotBest = SpotDifferenceScoreManager.getBestScore(context)
        val speedBest = SpeedRushScoreManager.getBestScore(context)

        val highest = listOf(tapRushBest, memoryBest, aimBest, numberBest, spotBest, speedBest).maxOrNull() ?: 0
        val sumScores = (tapRushBest + memoryBest + aimBest + numberBest + spotBest + speedBest).toLong()

        val tapRushCombo = TapRushScoreManager.getBestCombo(context)
        val memoryCombo = MemoryFlashScoreManager.getBestCombo(context)
        val aimCombo = PerfectAimScoreManager.getBestCombo(context)
        val numberCombo = NumberSprintScoreManager.getBestCombo(context)
        val spotCombo = SpotDifferenceScoreManager.getBestCombo(context)
        val speedCombo = SpeedRushScoreManager.getBestCombo(context)
        val bestCombo = listOf(tapRushCombo, memoryCombo, aimCombo, numberCombo, spotCombo, speedCombo).maxOrNull() ?: 0

        val tapRushAcc = TapRushScoreManager.getBestAccuracy(context)
        val memoryAcc = MemoryFlashScoreManager.getBestAccuracy(context)
        val aimAcc = PerfectAimScoreManager.getBestAccuracy(context)
        val numberAcc = NumberSprintScoreManager.getBestAccuracy(context)
        val spotAcc = SpotDifferenceScoreManager.getBestAccuracy(context)
        val speedAcc = SpeedRushScoreManager.getBestAccuracy(context)
        val accList = listOf(tapRushAcc, memoryAcc, aimAcc, numberAcc, spotAcc, speedAcc).filter { it > 0f }
        val avgAcc = if (accList.isNotEmpty()) accList.average().toFloat() else 90f

        prefs.edit().apply {
            putInt(KEY_TOTAL_GAMES_PLAYED, totalLegacyPlayed)
            putInt(KEY_TOTAL_GAMES_COMPLETED, totalLegacyPlayed)
            putLong(KEY_TOTAL_SCORE_SUM, sumScores)
            putInt(KEY_HIGHEST_SCORE, highest)
            putFloat(KEY_ACCURACY_SUM, avgAcc * totalLegacyPlayed)
            putInt(KEY_BEST_COMBO, bestCombo)
            putLong(KEY_PLAY_TIME_SECONDS, (totalLegacyPlayed * 30L))

            // Per-game migration
            saveGameStatsInternal(this, "tap_rush", tapRushPlayed, tapRushPlayed, tapRushBest, tapRushAcc, tapRushCombo, tapRushBest.toLong(), tapRushPlayed * 30L)
            saveGameStatsInternal(this, "memory_flash", memoryPlayed, memoryPlayed, memoryBest, memoryAcc, memoryCombo, memoryBest.toLong(), memoryPlayed * 30L)
            saveGameStatsInternal(this, "perfect_aim", aimPlayed, aimPlayed, aimBest, aimAcc, aimCombo, aimBest.toLong(), aimPlayed * 30L)
            saveGameStatsInternal(this, "number_sprint", numberPlayed, numberPlayed, numberBest, numberAcc, numberCombo, numberBest.toLong(), numberPlayed * 30L)
            saveGameStatsInternal(this, "spot_difference", spotPlayed, spotPlayed, spotBest, spotAcc, spotCombo, spotBest.toLong(), spotPlayed * 30L)
            saveGameStatsInternal(this, "speed_rush", speedPlayed, speedPlayed, speedBest, speedAcc, speedCombo, speedBest.toLong(), speedPlayed * 30L)

            putBoolean(KEY_INITIALIZED_MIGRATION, true)
            apply()
        }
    }

    private fun saveGameStatsInternal(
        editor: SharedPreferences.Editor,
        gameId: String,
        played: Int,
        completed: Int,
        bestScore: Int,
        bestAccuracy: Float,
        bestCombo: Int,
        totalScore: Long,
        playTimeSeconds: Long
    ) {
        editor.putInt("game_${gameId}_played", played)
        editor.putInt("game_${gameId}_completed", completed)
        editor.putInt("game_${gameId}_best_score", bestScore)
        editor.putFloat("game_${gameId}_best_accuracy", bestAccuracy)
        editor.putInt("game_${gameId}_best_combo", bestCombo)
        editor.putLong("game_${gameId}_total_score", totalScore)
        editor.putLong("game_${gameId}_play_time", playTimeSeconds)
    }

    /**
     * Records a completed mini-game result into overall, per-game, and daily statistics.
     * Guaranteed to prevent duplicate additions when the same sessionToken is processed multiple times.
     */
    @Synchronized
    fun recordGameEnd(
        context: Context,
        gameId: String,
        score: Int,
        accuracy: Float,
        maxCombo: Int,
        playTimeSeconds: Int = 30,
        sessionToken: String? = null
    ) {
        ensureInitialMigration(context)

        val prefs = getPrefs(context)
        val validCheck = ScoreValidator.validateGameScore(gameId, score, accuracy, maxCombo, sessionToken)
        if (!validCheck.isValid) {
            Log.w(TAG, "Skipping stat update due to score validation failure: ${validCheck.reason}")
            return
        }

        // Duplicate prevention check
        if (!sessionToken.isNullOrBlank()) {
            if (memoryProcessedSessions.contains(sessionToken) || isSessionProcessedInPrefs(prefs, sessionToken)) {
                Log.d(TAG, "Ignoring duplicate stat recording for session token: $sessionToken")
                return
            }
            memoryProcessedSessions.add(sessionToken)
            recordSessionInPrefs(prefs, sessionToken)
        }

        val prevPlayed = prefs.getInt(KEY_TOTAL_GAMES_PLAYED, 0)
        val prevCompleted = prefs.getInt(KEY_TOTAL_GAMES_COMPLETED, 0)
        val prevScoreSum = prefs.getLong(KEY_TOTAL_SCORE_SUM, 0L)
        val prevHighest = prefs.getInt(KEY_HIGHEST_SCORE, 0)
        val prevAccSum = prefs.getFloat(KEY_ACCURACY_SUM, 0f)
        val prevBestCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val prevPlayTime = prefs.getLong(KEY_PLAY_TIME_SECONDS, 0L)

        val safeScore = score.coerceAtLeast(0)
        val safeAccuracy = accuracy.coerceIn(0f, 100f)
        val safeCombo = maxCombo.coerceAtLeast(0)
        val safeDuration = playTimeSeconds.coerceIn(1, 600).toLong()

        val isCompleted = safeScore > 0

        val newPlayed = prevPlayed + 1
        val newCompleted = if (isCompleted) prevCompleted + 1 else prevCompleted
        val newScoreSum = prevScoreSum + safeScore
        val newHighest = if (safeScore > prevHighest) safeScore else prevHighest
        val newAccSum = prevAccSum + safeAccuracy
        val newBestCombo = if (safeCombo > prevBestCombo) safeCombo else prevBestCombo
        val newPlayTime = prevPlayTime + safeDuration

        // Per-game updates
        val gPlayed = prefs.getInt("game_${gameId}_played", 0) + 1
        val gCompleted = prefs.getInt("game_${gameId}_completed", 0) + (if (isCompleted) 1 else 0)
        val gBestScore = prefs.getInt("game_${gameId}_best_score", 0).let { if (safeScore > it) safeScore else it }
        val gBestAcc = prefs.getFloat("game_${gameId}_best_accuracy", 0f).let { if (safeAccuracy > it) safeAccuracy else it }
        val gBestCombo = prefs.getInt("game_${gameId}_best_combo", 0).let { if (safeCombo > it) safeCombo else it }
        val gTotalScore = prefs.getLong("game_${gameId}_total_score", 0L) + safeScore
        val gPlayTime = prefs.getLong("game_${gameId}_play_time", 0L) + safeDuration

        // Daily activity update
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dGames = prefs.getInt("daily_${todayStr}_games", 0) + 1
        val dScore = prefs.getInt("daily_${todayStr}_score", 0) + safeScore
        val dPlayTime = prefs.getLong("daily_${todayStr}_playtime", 0L) + safeDuration

        prefs.edit().apply {
            putInt(KEY_TOTAL_GAMES_PLAYED, newPlayed)
            putInt(KEY_TOTAL_GAMES_COMPLETED, newCompleted)
            putLong(KEY_TOTAL_SCORE_SUM, newScoreSum)
            putInt(KEY_HIGHEST_SCORE, newHighest)
            putFloat(KEY_ACCURACY_SUM, newAccSum)
            putInt(KEY_BEST_COMBO, newBestCombo)
            putLong(KEY_PLAY_TIME_SECONDS, newPlayTime)

            putInt("game_${gameId}_played", gPlayed)
            putInt("game_${gameId}_completed", gCompleted)
            putInt("game_${gameId}_best_score", gBestScore)
            putFloat("game_${gameId}_best_accuracy", gBestAcc)
            putInt("game_${gameId}_best_combo", gBestCombo)
            putLong("game_${gameId}_total_score", gTotalScore)
            putLong("game_${gameId}_play_time", gPlayTime)

            putInt("daily_${todayStr}_games", dGames)
            putInt("daily_${todayStr}_score", dScore)
            putLong("daily_${todayStr}_playtime", dPlayTime)

            apply()
        }

        // Notify cloud sync
        UserProfileManager.getCoins(context) // refresh context
        com.example.data.CloudSyncManager.onLocalDataUpdated(context)
    }

    fun getOverallStats(context: Context): OverallStats {
        ensureInitialMigration(context)
        val prefs = getPrefs(context)

        val played = prefs.getInt(KEY_TOTAL_GAMES_PLAYED, 0)
        val completed = prefs.getInt(KEY_TOTAL_GAMES_COMPLETED, 0)
        val scoreSum = prefs.getLong(KEY_TOTAL_SCORE_SUM, 0L)
        val highest = prefs.getInt(KEY_HIGHEST_SCORE, 0)
        val accSum = prefs.getFloat(KEY_ACCURACY_SUM, 0f)
        val combo = prefs.getInt(KEY_BEST_COMBO, 0)
        val playTime = prefs.getLong(KEY_PLAY_TIME_SECONDS, 0L)

        val totalXp = UserProfileManager.getTotalXp(context)
        val coins = UserProfileManager.getCoins(context)

        val divisor = if (played > 0) played.toFloat() else 1f
        val avgScore = if (played > 0) scoreSum.toFloat() / divisor else 0f
        val overallAcc = if (played > 0) (accSum / divisor).coerceIn(0f, 100f) else 0f

        return OverallStats(
            totalGamesPlayed = played,
            totalGamesCompleted = completed,
            totalScoreSum = scoreSum,
            highestScore = highest,
            averageScore = avgScore,
            overallAccuracy = overallAcc,
            bestCombo = combo,
            totalXpEarned = totalXp,
            totalCoinsEarned = coins,
            totalPlayTimeSeconds = playTime
        )
    }

    fun getGameStats(context: Context, gameId: String): GameStatItem {
        ensureInitialMigration(context)
        val prefs = getPrefs(context)
        val title = GAME_CONFIGS[gameId] ?: gameId

        val played = prefs.getInt("game_${gameId}_played", 0)
        val completed = prefs.getInt("game_${gameId}_completed", 0)
        val bestScore = prefs.getInt("game_${gameId}_best_score", 0)
        val bestAcc = prefs.getFloat("game_${gameId}_best_accuracy", 0f)
        val bestCombo = prefs.getInt("game_${gameId}_best_combo", 0)
        val totalScore = prefs.getLong("game_${gameId}_total_score", 0L)
        val playTime = prefs.getLong("game_${gameId}_play_time", 0L)

        val divisor = if (played > 0) played.toFloat() else 1f
        val avgScore = if (played > 0) totalScore.toFloat() / divisor else 0f

        return GameStatItem(
            gameId = gameId,
            title = title,
            gamesPlayed = played,
            gamesCompleted = completed,
            bestScore = bestScore,
            bestAccuracy = bestAcc,
            bestCombo = bestCombo,
            totalScore = totalScore,
            totalPlayTimeSeconds = playTime,
            averageScore = avgScore
        )
    }

    fun getAllGameStats(context: Context): List<GameStatItem> {
        return GAME_CONFIGS.keys.map { gameId -> getGameStats(context, gameId) }
    }

    fun getDailyActivityHistory(context: Context, days: Int = 7): List<DailyActivityItem> {
        val prefs = getPrefs(context)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val result = mutableListOf<DailyActivityItem>()
        val todayStr = dateFormat.format(calendar.time)

        for (i in (days - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val dayLabel = dayLabelFormat.format(cal.time)

            val games = prefs.getInt("daily_${dateStr}_games", 0)
            val score = prefs.getInt("daily_${dateStr}_score", 0)
            val playTime = prefs.getLong("daily_${dateStr}_playtime", 0L)

            result.add(
                DailyActivityItem(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    gamesPlayed = games,
                    totalScore = score,
                    playTimeSeconds = playTime,
                    isToday = (dateStr == todayStr)
                )
            )
        }
        return result
    }

    fun exportStatsForCloud(context: Context): Map<String, Any> {
        ensureInitialMigration(context)
        val overall = getOverallStats(context)
        val gamesMap = mutableMapOf<String, Any>()

        GAME_CONFIGS.keys.forEach { gameId ->
            val stat = getGameStats(context, gameId)
            gamesMap[gameId] = mapOf(
                "gamesPlayed" to stat.gamesPlayed,
                "gamesCompleted" to stat.gamesCompleted,
                "bestScore" to stat.bestScore,
                "bestAccuracy" to stat.bestAccuracy,
                "bestCombo" to stat.bestCombo,
                "totalScore" to stat.totalScore,
                "totalPlayTimeSeconds" to stat.totalPlayTimeSeconds
            )
        }

        val activityList = getDailyActivityHistory(context, 7)
        val activityMap = mutableMapOf<String, Any>()
        activityList.forEach { act ->
            activityMap[act.dateString] = mapOf(
                "gamesPlayed" to act.gamesPlayed,
                "totalScore" to act.totalScore,
                "playTimeSeconds" to act.playTimeSeconds
            )
        }

        return mapOf(
            "totalGamesPlayed" to overall.totalGamesPlayed,
            "totalGamesCompleted" to overall.totalGamesCompleted,
            "totalScoreSum" to overall.totalScoreSum,
            "highestScore" to overall.highestScore,
            "overallAccuracy" to overall.overallAccuracy,
            "bestCombo" to overall.bestCombo,
            "totalPlayTimeSeconds" to overall.totalPlayTimeSeconds,
            "games" to gamesMap,
            "activity" to activityMap
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun mergeCloudStats(context: Context, cloudData: Map<String, Any?>) {
        val prefs = getPrefs(context)

        val cPlayed = (cloudData["totalGamesPlayed"] as? Number)?.toInt() ?: 0
        val cCompleted = (cloudData["totalGamesCompleted"] as? Number)?.toInt() ?: 0
        val cScoreSum = (cloudData["totalScoreSum"] as? Number)?.toLong() ?: 0L
        val cHighest = (cloudData["highestScore"] as? Number)?.toInt() ?: 0
        val cAcc = (cloudData["overallAccuracy"] as? Number)?.toFloat() ?: 0f
        val cCombo = (cloudData["bestCombo"] as? Number)?.toInt() ?: 0
        val cPlayTime = (cloudData["totalPlayTimeSeconds"] as? Number)?.toLong() ?: 0L

        val lPlayed = prefs.getInt(KEY_TOTAL_GAMES_PLAYED, 0)
        val lCompleted = prefs.getInt(KEY_TOTAL_GAMES_COMPLETED, 0)
        val lScoreSum = prefs.getLong(KEY_TOTAL_SCORE_SUM, 0L)
        val lHighest = prefs.getInt(KEY_HIGHEST_SCORE, 0)
        val lAccSum = prefs.getFloat(KEY_ACCURACY_SUM, 0f)
        val lCombo = prefs.getInt(KEY_BEST_COMBO, 0)
        val lPlayTime = prefs.getLong(KEY_PLAY_TIME_SECONDS, 0L)

        val mergedPlayed = maxOf(lPlayed, cPlayed)
        val mergedCompleted = maxOf(lCompleted, cCompleted)
        val mergedScoreSum = maxOf(lScoreSum, cScoreSum)
        val mergedHighest = maxOf(lHighest, cHighest)
        val mergedCombo = maxOf(lCombo, cCombo)
        val mergedPlayTime = maxOf(lPlayTime, cPlayTime)
        val mergedAccSum = maxOf(lAccSum, cAcc * cPlayed)

        val editor = prefs.edit()
        editor.putInt(KEY_TOTAL_GAMES_PLAYED, mergedPlayed)
        editor.putInt(KEY_TOTAL_GAMES_COMPLETED, mergedCompleted)
        editor.putLong(KEY_TOTAL_SCORE_SUM, mergedScoreSum)
        editor.putInt(KEY_HIGHEST_SCORE, mergedHighest)
        editor.putFloat(KEY_ACCURACY_SUM, mergedAccSum)
        editor.putInt(KEY_BEST_COMBO, mergedCombo)
        editor.putLong(KEY_PLAY_TIME_SECONDS, mergedPlayTime)

        // Merge game stats
        val cloudGamesMap = cloudData["games"] as? Map<String, Map<String, Any?>>
        GAME_CONFIGS.keys.forEach { gameId ->
            val cGame = cloudGamesMap?.get(gameId)
            val cgPlayed = (cGame?.get("gamesPlayed") as? Number)?.toInt() ?: 0
            val cgCompleted = (cGame?.get("gamesCompleted") as? Number)?.toInt() ?: 0
            val cgBestScore = (cGame?.get("bestScore") as? Number)?.toInt() ?: 0
            val cgBestAcc = (cGame?.get("bestAccuracy") as? Number)?.toFloat() ?: 0f
            val cgBestCombo = (cGame?.get("bestCombo") as? Number)?.toInt() ?: 0
            val cgTotalScore = (cGame?.get("totalScore") as? Number)?.toLong() ?: 0L
            val cgPlayTime = (cGame?.get("totalPlayTimeSeconds") as? Number)?.toLong() ?: 0L

            val lgPlayed = prefs.getInt("game_${gameId}_played", 0)
            val lgCompleted = prefs.getInt("game_${gameId}_completed", 0)
            val lgBestScore = prefs.getInt("game_${gameId}_best_score", 0)
            val lgBestAcc = prefs.getFloat("game_${gameId}_best_accuracy", 0f)
            val lgBestCombo = prefs.getInt("game_${gameId}_best_combo", 0)
            val lgTotalScore = prefs.getLong("game_${gameId}_total_score", 0L)
            val lgPlayTime = prefs.getLong("game_${gameId}_play_time", 0L)

            saveGameStatsInternal(
                editor = editor,
                gameId = gameId,
                played = maxOf(lgPlayed, cgPlayed),
                completed = maxOf(lgCompleted, cgCompleted),
                bestScore = maxOf(lgBestScore, cgBestScore),
                bestAccuracy = maxOf(lgBestAcc, cgBestAcc),
                bestCombo = maxOf(lgBestCombo, cgBestCombo),
                totalScore = maxOf(lgTotalScore, cgTotalScore),
                playTimeSeconds = maxOf(lgPlayTime, cgPlayTime)
            )
        }

        // Merge activity stats
        val cloudActivityMap = cloudData["activity"] as? Map<String, Map<String, Any?>>
        cloudActivityMap?.forEach { (dateStr, actData) ->
            val cdGames = (actData["gamesPlayed"] as? Number)?.toInt() ?: 0
            val cdScore = (actData["totalScore"] as? Number)?.toInt() ?: 0
            val cdPlayTime = (actData["playTimeSeconds"] as? Number)?.toLong() ?: 0L

            val ldGames = prefs.getInt("daily_${dateStr}_games", 0)
            val ldScore = prefs.getInt("daily_${dateStr}_score", 0)
            val ldPlayTime = prefs.getLong("daily_${dateStr}_playtime", 0L)

            editor.putInt("daily_${dateStr}_games", maxOf(ldGames, cdGames))
            editor.putInt("daily_${dateStr}_score", maxOf(ldScore, cdScore))
            editor.putLong("daily_${dateStr}_playtime", maxOf(ldPlayTime, cdPlayTime))
        }

        editor.apply()
    }

    private fun isSessionProcessedInPrefs(prefs: SharedPreferences, token: String): Boolean {
        val stored = prefs.getString(KEY_PROCESSED_SESSIONS, "") ?: ""
        return stored.split(",").contains(token)
    }

    private fun recordSessionInPrefs(prefs: SharedPreferences, token: String) {
        val stored = prefs.getString(KEY_PROCESSED_SESSIONS, "") ?: ""
        val list = if (stored.isBlank()) mutableListOf() else stored.split(",").toMutableList()
        list.add(token)
        val trimmed = list.takeLast(50)
        prefs.edit().putString(KEY_PROCESSED_SESSIONS, trimmed.joinToString(",")).apply()
    }
}
