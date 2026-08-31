package com.example.data.tournament

import android.content.Context
import android.util.Log
import com.example.data.UserProfileManager
import com.example.data.validation.ScoreValidator
import com.example.model.Tournament
import com.example.model.TournamentEntry
import com.example.model.TournamentStatus
import com.example.model.TournamentSubmitResult
import com.example.model.TournamentType
import com.example.util.NetworkObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TournamentRepository(private val context: Context) {

    private val TAG = "TournamentRepository"
    private val database = FirebaseDatabase.getInstance()
    private val prefs = context.getSharedPreferences("tournament_session_prefs", Context.MODE_PRIVATE)

    /**
     * Get list of currently active and recent daily/weekly tournaments.
     * Generates standard tournaments dynamically anchored to UTC dates while checking Firebase for any server catalog updates.
     */
    suspend fun getAvailableTournaments(): List<Tournament> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        // Daily Time Range
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dailyStartMs = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dailyEndMs = calendar.timeInMillis - 1L

        // Weekly Time Range
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weeklyStartMs = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weeklyEndMs = calendar.timeInMillis - 1L

        val dateSuffix = getCurrentDateString()
        val weekSuffix = getCurrentWeekString()

        val generatedTournaments = mutableListOf(
            Tournament(
                id = "daily_tap_rush_$dateSuffix",
                title = "Tap Rush Daily Clash",
                gameId = "tap_rush",
                gameTitle = "Tap Rush",
                type = TournamentType.DAILY,
                description = "Fastest fingers win! Score as many high-speed precision taps in 30 seconds.",
                startTimeMs = dailyStartMs,
                endTimeMs = dailyEndMs,
                virtualPrizePoolCoins = 5000,
                status = if (now > dailyEndMs) TournamentStatus.EXPIRED else TournamentStatus.ACTIVE
            ),
            Tournament(
                id = "daily_speed_rush_$dateSuffix",
                title = "Speed Rush Daily Frenzy",
                gameId = "speed_rush",
                gameTitle = "Speed Rush",
                type = TournamentType.DAILY,
                description = "Rapid response frenzy. Hit targets before time runs out!",
                startTimeMs = dailyStartMs,
                endTimeMs = dailyEndMs,
                virtualPrizePoolCoins = 5000,
                status = if (now > dailyEndMs) TournamentStatus.EXPIRED else TournamentStatus.ACTIVE
            ),
            Tournament(
                id = "weekly_perfect_aim_$weekSuffix",
                title = "Perfect Aim Weekly Grand Prix",
                gameId = "perfect_aim",
                gameTitle = "Perfect Aim",
                type = TournamentType.WEEKLY,
                description = "The ultimate precision competition. Climb the weekly global leaderboard!",
                startTimeMs = weeklyStartMs,
                endTimeMs = weeklyEndMs,
                virtualPrizePoolCoins = 25000,
                status = if (now > weeklyEndMs) TournamentStatus.EXPIRED else TournamentStatus.ACTIVE
            ),
            Tournament(
                id = "weekly_memory_flash_$weekSuffix",
                title = "Memory Flash Weekly Showdown",
                gameId = "memory",
                gameTitle = "Memory Flash",
                type = TournamentType.WEEKLY,
                description = "Test pattern recall against global competitors. Master the sequence!",
                startTimeMs = weeklyStartMs,
                endTimeMs = weeklyEndMs,
                virtualPrizePoolCoins = 25000,
                status = if (now > weeklyEndMs) TournamentStatus.EXPIRED else TournamentStatus.ACTIVE
            )
        )

        // Try to fetch custom tournaments from Firebase
        try {
            val snapshot = database.getReference("tournaments_catalog").get().await()
            for (child in snapshot.children) {
                val id = child.key ?: continue
                val title = child.child("title").getValue(String::class.java) ?: "Tournament"
                val gameId = child.child("gameId").getValue(String::class.java) ?: "tap_rush"
                val gameTitle = child.child("gameTitle").getValue(String::class.java) ?: "Mini Game"
                val typeStr = child.child("type").getValue(String::class.java) ?: "DAILY"
                val desc = child.child("description").getValue(String::class.java) ?: "Compete for virtual rewards."
                val start = child.child("startTimeMs").getValue(Long::class.java) ?: dailyStartMs
                val end = child.child("endTimeMs").getValue(Long::class.java) ?: dailyEndMs
                val prize = child.child("virtualPrizePoolCoins").getValue(Int::class.java) ?: 10000

                val status = when {
                    now < start -> TournamentStatus.UPCOMING
                    now > end -> TournamentStatus.EXPIRED
                    else -> TournamentStatus.ACTIVE
                }

                val custom = Tournament(
                    id = id,
                    title = title,
                    gameId = gameId,
                    gameTitle = gameTitle,
                    type = if (typeStr.equals("WEEKLY", ignoreCase = true)) TournamentType.WEEKLY else TournamentType.DAILY,
                    description = desc,
                    startTimeMs = start,
                    endTimeMs = end,
                    virtualPrizePoolCoins = prize,
                    status = status
                )

                val existingIdx = generatedTournaments.indexOfFirst { it.id == id }
                if (existingIdx >= 0) {
                    generatedTournaments[existingIdx] = custom
                } else {
                    generatedTournaments.add(0, custom)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom tournament catalog fetch skipped: ${e.message}")
        }

        return generatedTournaments
    }

    /**
     * Realtime Flow observing live scores for a specific tournament.
     */
    fun observeTournamentLeaderboard(tournamentId: String): Flow<List<TournamentEntry>> = callbackFlow {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val dbRef = database.getReference("tournaments/$tournamentId/scores")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<TournamentEntry>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val score = child.child("score").getValue(Int::class.java) ?: 0
                    if (score <= 0) continue

                    val name = child.child("name").getValue(String::class.java) ?: "Anonymous"
                    val level = child.child("level").getValue(Int::class.java) ?: 1
                    val badge = child.child("badge").getValue(String::class.java) ?: "Contender"
                    val avatarColorHex = child.child("avatarColorHex").getValue(Long::class.java) ?: 0xFF3D5AFE
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                    entries.add(
                        TournamentEntry(
                            userId = uid,
                            name = name,
                            score = score,
                            level = level,
                            badge = badge,
                            avatarColorHex = avatarColorHex,
                            timestamp = timestamp,
                            isCurrentPlayer = (uid == currentUid)
                        )
                    )
                }

                entries.sortByDescending { it.score }
                val rankedList = entries.mapIndexed { index, entry -> entry.copy(rank = index + 1) }
                trySend(rankedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to tournament leaderboard $tournamentId: ${error.message}")
                close(error.toException())
            }
        }

        dbRef.addValueEventListener(listener)
        awaitClose { dbRef.removeEventListener(listener) }
    }

    /**
     * Check if a session token has already been submitted for score posting.
     */
    fun isSessionTokenUsed(sessionToken: String): Boolean {
        return prefs.getBoolean("token_used_$sessionToken", false)
    }

    /**
     * Mark a session token as used.
     */
    private fun markSessionTokenUsed(sessionToken: String) {
        prefs.edit().putBoolean("token_used_$sessionToken", true).apply()
    }

    /**
     * Submit score for an active tournament using Firebase transaction.
     */
    suspend fun submitTournamentScore(
        tournamentId: String,
        tournamentTitle: String,
        score: Int,
        sessionToken: String
    ): TournamentSubmitResult {
        if (!com.example.util.NetworkObserver(context).isCurrentlyConnected()) {
            throw IllegalStateException("Online network connection required to submit tournament score.")
        }

        if (sessionToken.isNotBlank() && isSessionTokenUsed(sessionToken)) {
            throw IllegalStateException("This attempt score has already been submitted to the tournament.")
        }

        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("User must be authenticated to submit tournament score.")

        val localProfile = UserProfileManager.getPlayerName(context)
        val levelProgress = UserProfileManager.getLevelProgress(context)
        val currentLevel = levelProgress.level
        val currentBadge = levelProgress.levelTitle

        val validation = ScoreValidator.validateLeaderboardUpload(
            score = score,
            name = localProfile,
            level = currentLevel,
            badge = currentBadge
        )

        if (!validation.isValid) {
            throw IllegalArgumentException("Score validation failed: ${validation.reason}")
        }

        val scoreRef = database.getReference("tournaments/$tournamentId/scores").child(user.uid)

        var isNewBest = false
        var updatedScore = score

        suspendCoroutine<Unit> { continuation ->
            scoreRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val existingScore = currentData.child("score").getValue(Int::class.java) ?: 0
                    if (score > existingScore) {
                        isNewBest = true
                        updatedScore = score
                        currentData.child("score").value = score
                        currentData.child("name").value = localProfile
                        currentData.child("level").value = currentLevel
                        currentData.child("badge").value = currentBadge
                        currentData.child("avatarColorHex").value = 0xFF3D5AFE
                        currentData.child("timestamp").value = System.currentTimeMillis()
                    } else {
                        isNewBest = false
                        updatedScore = existingScore
                    }
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        continuation.resumeWith(Result.failure(error.toException()))
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }

        // Mark token as used
        if (sessionToken.isNotBlank()) {
            markSessionTokenUsed(sessionToken)
        }

        // Award participation XP & Virtual Coins
        val bonusCoins = if (isNewBest) 150 else 50
        val bonusXp = if (isNewBest) 100 else 40
        UserProfileManager.addCoins(context, bonusCoins)
        UserProfileManager.addXp(context, bonusXp)

        // Query current rank in tournament
        val snapshot = database.getReference("tournaments/$tournamentId/scores").get().await()
        val allEntries = mutableListOf<Pair<String, Int>>()
        for (child in snapshot.children) {
            val uid = child.key ?: continue
            val sc = child.child("score").getValue(Int::class.java) ?: 0
            if (sc > 0) {
                allEntries.add(Pair(uid, sc))
            }
        }
        allEntries.sortByDescending { it.second }
        val rank = allEntries.indexOfFirst { it.first == user.uid } + 1
        val finalRank = if (rank <= 0) allEntries.size + 1 else rank

        return TournamentSubmitResult(
            tournamentId = tournamentId,
            tournamentTitle = tournamentTitle,
            score = updatedScore,
            isNewTournamentBest = isNewBest,
            currentRank = finalRank,
            totalParticipants = allEntries.size,
            coinsEarned = bonusCoins,
            xpEarned = bonusXp
        )
    }

    private fun getCurrentDateString(): String {
        val format = SimpleDateFormat("yyyy_MM_dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun getCurrentWeekString(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return "${cal.get(Calendar.YEAR)}_W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }
}
