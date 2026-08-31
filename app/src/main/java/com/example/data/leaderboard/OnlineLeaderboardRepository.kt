package com.example.data.leaderboard

import android.content.Context
import android.util.Log
import com.example.data.UserProfileManager
import com.example.data.validation.ScoreValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OnlineLeaderboardRepository(
    private val context: Context,
    private val localRepository: LocalLeaderboardRepository
) : LeaderboardRepository {

    private val TAG = "OnlineLeaderboardRepo"
    
    // We get Firebase RTDB instance. Offline persistence is enabled in CloudSyncManager.
    private val database = FirebaseDatabase.getInstance()

    override suspend fun getLeaderboard(
        timeframe: LeaderboardTimeframe,
        category: LeaderboardGameCategory
    ): List<LeaderboardEntry> {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Fallback to local if not authenticated yet
            return localRepository.getLeaderboard(timeframe, category)
        }

        // Trigger an automatic score upload for the user first so latest local scores are synced online
        try {
            uploadScore(timeframe, category)
        } catch (e: Exception) {
            Log.w(TAG, "Pre-fetch score upload skipped or failed: ${e.message}")
        }

        return try {
            val dbRef = database.getReference(getPath(timeframe, category))
            
            // Query for top 100 scores descending
            val snapshot = dbRef.orderByChild("score").limitToLast(100).get().await()
            
            val entries = mutableListOf<LeaderboardEntry>()
            for (child in snapshot.children) {
                val uid = child.key ?: continue
                val score = child.child("score").getValue(Int::class.java) ?: 0
                if (score <= 0) continue // Skip invalid or zero scores
                
                val name = child.child("name").getValue(String::class.java) ?: "Anonymous"
                val level = child.child("level").getValue(Int::class.java) ?: 1
                val badge = child.child("badge").getValue(String::class.java) ?: "Contender"
                val gamesPlayed = child.child("gamesPlayed").getValue(Int::class.java) ?: 0
                val streak = child.child("streakDays").getValue(Int::class.java) ?: 0
                val avatarColorHex = child.child("avatarColorHex").getValue(Long::class.java) ?: 0xFF3D5AFE
                
                entries.add(
                    LeaderboardEntry(
                        rank = 0, // Assigned after sorting
                        userId = uid,
                        name = name,
                        score = score,
                        isCurrentPlayer = (uid == user.uid),
                        level = level,
                        avatarColorHex = avatarColorHex,
                        badge = badge,
                        gamesPlayed = gamesPlayed,
                        streakDays = streak
                    )
                )
            }
            
            // Firebase orderByChild sorts ascending, so we reverse it
            entries.sortByDescending { it.score }
            
            // Assign ranks
            entries.mapIndexed { index, entry -> entry.copy(rank = index + 1) }.let {
                // Ensure current user is in the list (or at least their score is up to date)
                ensureCurrentUserIncluded(it, timeframe, category, user.uid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch online leaderboard, falling back to local.", e)
            localRepository.getLeaderboard(timeframe, category)
        }
    }

    private suspend fun ensureCurrentUserIncluded(
        list: List<LeaderboardEntry>,
        timeframe: LeaderboardTimeframe,
        category: LeaderboardGameCategory,
        uid: String
    ): List<LeaderboardEntry> {
        val mutableList = list.toMutableList()
        val existingIndex = mutableList.indexOfFirst { it.userId == uid }
        
        val localUserEntry = getCurrentUserEntry(timeframe, category).copy(userId = uid, isCurrentPlayer = true)
        
        if (existingIndex >= 0) {
            // Update with local if local score is higher (hasn't synced yet)
            if (localUserEntry.score > mutableList[existingIndex].score) {
                mutableList[existingIndex] = localUserEntry
            } else {
                // Use online score but update details
                mutableList[existingIndex] = mutableList[existingIndex].copy(
                    isCurrentPlayer = true,
                    name = localUserEntry.name,
                    level = localUserEntry.level,
                    badge = localUserEntry.badge
                )
            }
        } else {
            // Append and resort
            if (localUserEntry.score > 0) {
                mutableList.add(localUserEntry)
            }
        }
        
        mutableList.sortByDescending { it.score }
        return mutableList.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    override suspend fun getCurrentUserEntry(
        timeframe: LeaderboardTimeframe,
        category: LeaderboardGameCategory
    ): LeaderboardEntry {
        // Compute the user's entry using local repository logic for the actual current state
        return localRepository.getCurrentUserEntry(timeframe, category)
    }

    suspend fun uploadScore(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val localEntry = getCurrentUserEntry(timeframe, category)
        
        val validation = ScoreValidator.validateLeaderboardUpload(
            score = localEntry.score,
            name = localEntry.name,
            level = localEntry.level,
            badge = localEntry.badge
        )

        if (!validation.isValid) {
            Log.w(TAG, "Skipping leaderboard upload due to validation error: ${validation.reason}")
            return
        }
        
        val dbRef = database.getReference(getPath(timeframe, category)).child(user.uid)

        try {
            suspendCoroutine<Unit> { continuation ->
                dbRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val existingScore = currentData.child("score").getValue(Int::class.java) ?: 0
                        // Only update if new score is strictly higher or equal
                        if (localEntry.score >= existingScore) {
                            currentData.child("score").value = localEntry.score
                            currentData.child("name").value = localEntry.name
                            currentData.child("level").value = localEntry.level
                            currentData.child("badge").value = localEntry.badge
                            currentData.child("gamesPlayed").value = localEntry.gamesPlayed
                            currentData.child("streakDays").value = localEntry.streakDays
                            currentData.child("avatarColorHex").value = localEntry.avatarColorHex
                            currentData.child("timestamp").value = System.currentTimeMillis()
                        }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            Log.e(TAG, "Leaderboard upload transaction failed", error.toException())
                            continuation.resumeWithException(error.toException())
                        } else {
                            Log.d(TAG, "Uploaded score to $category / $timeframe via transaction: ${localEntry.score}")
                            continuation.resume(Unit)
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload leaderboard score", e)
        }
    }

    private fun getPath(timeframe: LeaderboardTimeframe, category: LeaderboardGameCategory): String {
        // Format path: leaderboards/{category}/{timeframe}
        // Since we can't easily rely on orderByChild timestamp indexing for the whole "daily" bucket without rules,
        // we'll just write directly to timeframe nodes. We could suffix with current date, but let's stick to the enums.
        // Actually, to make daily reset automatically, we should use the actual date in the path.
        val timeframeKey = when (timeframe) {
            LeaderboardTimeframe.DAILY -> "daily_" + getCurrentDateString()
            LeaderboardTimeframe.WEEKLY -> "weekly_" + getCurrentWeekString()
            LeaderboardTimeframe.ALL_TIME -> "all_time"
        }
        return "leaderboards/${category.id}/$timeframeKey"
    }
    
    private fun getCurrentDateString(): String {
        val format = java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(java.util.Date())
    }
    
    private fun getCurrentWeekString(): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        return "${cal.get(java.util.Calendar.YEAR)}_W${cal.get(java.util.Calendar.WEEK_OF_YEAR)}"
    }
}
