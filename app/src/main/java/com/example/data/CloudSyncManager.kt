package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.validation.ScoreValidator
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            
            // Disable offline persistence to enforce online-only requirement
            FirebaseDatabase.getInstance().setPersistenceEnabled(false)
            
            signInAnonymously(context)
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }

    private fun signInAnonymously(context: Context) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInAnonymously:success")
                        syncData(context)
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.exception)
                    }
                }
        } else {
            Log.d(TAG, "Already signed in anonymously: ${auth.currentUser?.uid}")
            syncData(context)
        }
    }

    private fun syncData(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users/${user.uid}")
        dbRef.keepSynced(true) // Keep data synced for offline use

        // Run transaction to safely merge
        dbRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val cloudTimestamp = currentData.child("lastUpdated").getValue(Long::class.java) ?: 0L
                val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)
                
                if (cloudTimestamp > localTimestamp) {
                    // Cloud is newer, we will update local after transaction
                    // But we don't modify cloud data
                    return Transaction.success(currentData)
                } else if (localTimestamp > cloudTimestamp) {
                    // Local is newer, update cloud data
                    val coins = UserProfileManager.getCoins(context)
                    val xp = UserProfileManager.getTotalXp(context)
                    val games = UserProfileManager.getTotalGamesPlayed(context)
                    val streak = UserProfileManager.getStreakDays(context)

                    val profileValidation = ScoreValidator.validateProfileData(coins, xp, games, streak)
                    if (!profileValidation.isValid) {
                        Log.e(TAG, "Local profile data failed validation during sync: ${profileValidation.reason}")
                        return Transaction.abort()
                    }
                    
                    currentData.child("coins").value = coins
                    currentData.child("xp").value = xp
                    currentData.child("gamesPlayed").value = games
                    currentData.child("streak").value = streak
                    currentData.child("lastUpdated").value = localTimestamp
                    
                    // Also sync best scores
                    currentData.child("bestScores").child("tapRush").value = TapRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                    currentData.child("bestScores").child("memoryFlash").value = MemoryFlashScoreManager.getBestScore(context).coerceIn(0, 10000)
                    currentData.child("bestScores").child("perfectAim").value = PerfectAimScoreManager.getBestScore(context).coerceIn(0, 10000)
                    currentData.child("bestScores").child("numberSprint").value = NumberSprintScoreManager.getBestScore(context).coerceIn(0, 10000)
                    currentData.child("bestScores").child("spotDifference").value = SpotDifferenceScoreManager.getBestScore(context).coerceIn(0, 10000)
                    currentData.child("bestScores").child("speedRush").value = SpeedRushScoreManager.getBestScore(context).coerceIn(0, 10000)

                    // Sync statistics
                    val statsMap = com.example.data.stats.StatsManager.exportStatsForCloud(context)
                    currentData.child("stats").value = statsMap
                }
                
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed.", error.toException())
                } else if (currentData != null) {
                    // Update local if cloud was newer
                    val cloudTimestamp = currentData.child("lastUpdated").getValue(Long::class.java) ?: 0L
                    val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)
                    
                    if (cloudTimestamp > localTimestamp) {
                        Log.d(TAG, "Cloud data is newer, updating local preferences.")
                        val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                        val xp = currentData.child("xp").getValue(Int::class.java) ?: 0
                        val games = currentData.child("gamesPlayed").getValue(Int::class.java) ?: 0
                        val streak = currentData.child("streak").getValue(Int::class.java) ?: 0

                        val validation = ScoreValidator.validateProfileData(coins, xp, games, streak)
                        if (validation.isValid) {
                            UserProfileManager.overwriteLocalDataFromCloud(context, coins, xp, games, streak, cloudTimestamp)
                            
                            // Overwrite best scores with validated bounds
                            TapRushScoreManager.saveBestScore(context, (currentData.child("bestScores").child("tapRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            MemoryFlashScoreManager.saveBestScore(context, (currentData.child("bestScores").child("memoryFlash").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            PerfectAimScoreManager.saveBestScore(context, (currentData.child("bestScores").child("perfectAim").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            NumberSprintScoreManager.saveBestScore(context, (currentData.child("bestScores").child("numberSprint").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpotDifferenceScoreManager.saveBestScore(context, (currentData.child("bestScores").child("spotDifference").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpeedRushScoreManager.saveBestScore(context, (currentData.child("bestScores").child("speedRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))

                            // Merge cloud stats into local
                            (currentData.child("stats").value as? Map<String, Any?>)?.let { cloudStatsMap ->
                                com.example.data.stats.StatsManager.mergeCloudStats(context, cloudStatsMap)
                            }
                        } else {
                            Log.e(TAG, "Cloud data failed validation, ignoring overwrite: ${validation.reason}")
                        }
                    }
                }
            }
        })
    }

    /**
     * Call this whenever local data changes significantly to trigger an upload.
     */
    fun onLocalDataUpdated(context: Context) {
        if (!isInitialized) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)
        val dbRef = FirebaseDatabase.getInstance().getReference("users/${user.uid}")

        val coins = UserProfileManager.getCoins(context)
        val xp = UserProfileManager.getTotalXp(context)
        val games = UserProfileManager.getTotalGamesPlayed(context)
        val streak = UserProfileManager.getStreakDays(context)

        val validation = ScoreValidator.validateProfileData(coins, xp, games, streak)
        if (!validation.isValid) {
            Log.e(TAG, "Cannot sync local profile update: ${validation.reason}")
            return
        }

        dbRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                currentData.child("coins").value = coins
                currentData.child("xp").value = xp
                currentData.child("gamesPlayed").value = games
                currentData.child("streak").value = streak
                currentData.child("lastUpdated").value = localTimestamp

                currentData.child("bestScores").child("tapRush").value = TapRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                currentData.child("bestScores").child("memoryFlash").value = MemoryFlashScoreManager.getBestScore(context).coerceIn(0, 10000)
                currentData.child("bestScores").child("perfectAim").value = PerfectAimScoreManager.getBestScore(context).coerceIn(0, 10000)
                currentData.child("bestScores").child("numberSprint").value = NumberSprintScoreManager.getBestScore(context).coerceIn(0, 10000)
                currentData.child("bestScores").child("spotDifference").value = SpotDifferenceScoreManager.getBestScore(context).coerceIn(0, 10000)
                currentData.child("bestScores").child("speedRush").value = SpeedRushScoreManager.getBestScore(context).coerceIn(0, 10000)

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e(TAG, "Failed to update cloud data via transaction", error.toException())
                }
            }
        })
    }
}
