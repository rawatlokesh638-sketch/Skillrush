package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.stats.StatsManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // INFO, WARNING, EVENT, UPDATE
    val active: Boolean = true,
    val timestamp: Long = 0L
)

data class GameConfig(
    val xpMultiplier: Float = 1.0f,
    val coinMultiplier: Float = 1.0f,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "SkillRush is undergoing maintenance. Please check back soon!"
)

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private var isInitialized = false
    private var userListenerAttached = false
    private var globalListenersAttached = false

    // Real-time state flows for UI observation
    private val _isUserBanned = MutableStateFlow(false)
    val isUserBanned: StateFlow<Boolean> = _isUserBanned.asStateFlow()

    private val _banReason = MutableStateFlow("")
    val banReason: StateFlow<String> = _banReason.asStateFlow()

    private val _activeAnnouncement = MutableStateFlow<Announcement?>(null)
    val activeAnnouncement: StateFlow<Announcement?> = _activeAnnouncement.asStateFlow()

    private val _gameConfig = MutableStateFlow(GameConfig())
    val gameConfig: StateFlow<GameConfig> = _gameConfig.asStateFlow()

    private val _syncState = MutableStateFlow("Synced")
    val syncState: StateFlow<String> = _syncState.asStateFlow()

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

            FirebaseDatabase.getInstance().setPersistenceEnabled(false)

            signInAnonymously(context)
            attachGlobalListeners(context)
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
                        Log.d(TAG, "signInAnonymously:success UID: ${task.result?.user?.uid}")
                        attachUserListener(context)
                        syncData(context)
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.exception)
                    }
                }
        } else {
            Log.d(TAG, "Already signed in: ${auth.currentUser?.uid}")
            attachUserListener(context)
            syncData(context)
        }
    }

    /**
     * Attaches a real-time listener to the user's Firebase node.
     * When Admin updates Coins, XP, Level, Ban status, or Inventory in Admin Panel,
     * the app receives it live and updates local state.
     */
    fun attachUserListener(context: Context) {
        if (!UserProfileManager.hasSetupUsername(context)) return
        val uid = UserProfileManager.getUserUid(context)
        if (uid.isBlank()) return
        if (userListenerAttached) return

        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                // Check Ban status
                val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java) ?: false
                val reason = snapshot.child("banReason").getValue(String::class.java) ?: "Your account has been suspended by an administrator."
                _isUserBanned.value = isBanned
                _banReason.value = reason

                if (isBanned) {
                    Log.w(TAG, "User is marked as BANNED in Firebase: $reason")
                    return
                }

                // Check for updates from Admin Panel (if lastUpdated in cloud is newer)
                val cloudTimestamp = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
                val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)

                if (cloudTimestamp > localTimestamp) {
                    val coins = snapshot.child("coins").getValue(Int::class.java) ?: 0
                    val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    val games = snapshot.child("gamesPlayed").getValue(Int::class.java) ?: 0
                    val streak = snapshot.child("streak").getValue(Int::class.java) ?: 0

                    val validation = ScoreValidator.validateProfileData(coins, xp, games, streak)
                    if (validation.isValid) {
                        Log.d(TAG, "Live update from Firebase: Coins=$coins, XP=$xp, Games=$games, Streak=$streak")
                        UserProfileManager.overwriteLocalDataFromCloud(context, coins, xp, games, streak, cloudTimestamp)

                        // Update best scores
                        snapshot.child("bestScores").let { best ->
                            TapRushScoreManager.saveBestScore(context, (best.child("tapRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            MemoryFlashScoreManager.saveBestScore(context, (best.child("memoryFlash").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            PerfectAimScoreManager.saveBestScore(context, (best.child("perfectAim").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            NumberSprintScoreManager.saveBestScore(context, (best.child("numberSprint").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpotDifferenceScoreManager.saveBestScore(context, (best.child("spotDifference").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpeedRushScoreManager.saveBestScore(context, (best.child("speedRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                        }

                        // Update Power-ups
                        (snapshot.child("powerUps").value as? Map<String, Any?>)?.let { powerUpsMap ->
                            DailyRewardManager.mergeCloudPowerUps(context, powerUpsMap)
                        }

                        // Update Daily Reward
                        (snapshot.child("dailyReward").value as? Map<String, Any?>)?.let { dailyMap ->
                            DailyRewardManager.mergeCloudDailyReward(context, dailyMap)
                        }

                        // Update Achievements
                        (snapshot.child("achievements").value as? Map<String, Any?>)?.let { achMap ->
                            AchievementManager.mergeCloudAchievements(context, achMap)
                        }

                        // Update Stats
                        (snapshot.child("stats").value as? Map<String, Any?>)?.let { statsMap ->
                            StatsManager.mergeCloudStats(context, statsMap)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "User listener cancelled: ${error.message}")
            }
        })
        userListenerAttached = true
    }

    /**
     * Attaches listeners for global broadcasts and game configs.
     */
    private fun attachGlobalListeners(context: Context) {
        if (globalListenersAttached) return

        val db = FirebaseDatabase.getInstance()

        // 1. Announcements Listener
        db.getReference("announcements/active").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val id = snapshot.child("id").getValue(String::class.java) ?: "ann_1"
                    val title = snapshot.child("title").getValue(String::class.java) ?: ""
                    val message = snapshot.child("message").getValue(String::class.java) ?: ""
                    val type = snapshot.child("type").getValue(String::class.java) ?: "INFO"
                    val active = snapshot.child("active").getValue(Boolean::class.java) ?: true
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (active && title.isNotEmpty()) {
                        _activeAnnouncement.value = Announcement(id, title, message, type, active, timestamp)
                    } else {
                        _activeAnnouncement.value = null
                    }
                } else {
                    _activeAnnouncement.value = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Announcements listener error: ${error.message}")
            }
        })

        // 2. System Game Config Listener
        db.getReference("system").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val maintenanceSnap = snapshot.child("maintenance")
                    val isMaint = maintenanceSnap.child("enabled").getValue(Boolean::class.java) ?: false
                    val maintMsg = maintenanceSnap.child("message").getValue(String::class.java) 
                        ?: "SkillRush is undergoing maintenance. Please check back soon!"

                    val configSnap = snapshot.child("game_config")
                    val xpMult = (configSnap.child("xpMultiplier").getValue(Number::class.java))?.toFloat() ?: 1.0f
                    val coinMult = (configSnap.child("coinMultiplier").getValue(Number::class.java))?.toFloat() ?: 1.0f

                    _gameConfig.value = GameConfig(
                        xpMultiplier = xpMult,
                        coinMultiplier = coinMult,
                        maintenanceMode = isMaint,
                        maintenanceMessage = maintMsg
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "System config listener error: ${error.message}")
            }
        })

        globalListenersAttached = true
    }

    /**
     * Complete synchronization of all user data between Firebase and local storage.
     */
    fun syncData(context: Context) {
        if (!UserProfileManager.hasSetupUsername(context)) return
        val uid = UserProfileManager.getUserUid(context)
        if (uid.isBlank()) return
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")
        dbRef.keepSynced(true)

        attachUserListener(context)

        dbRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val cloudTimestamp = currentData.child("lastUpdated").getValue(Long::class.java) ?: 0L
                val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)

                if (cloudTimestamp > localTimestamp) {
                    return Transaction.success(currentData)
                } else {
                    val coins = UserProfileManager.getCoins(context)
                    val xp = UserProfileManager.getTotalXp(context)
                    val games = UserProfileManager.getTotalGamesPlayed(context)
                    val streak = UserProfileManager.getStreakDays(context)
                    val level = UserProfileManager.getLevel(context)
                    val levelTitle = UserProfileManager.getTitleForLevel(level)

                    val profileValidation = ScoreValidator.validateProfileData(coins, xp, games, streak)
                    if (!profileValidation.isValid) {
                        Log.e(TAG, "Local profile data failed validation: ${profileValidation.reason}")
                        return Transaction.abort()
                    }

                    currentData.child("uid").value = uid
                    currentData.child("username").value = UserProfileManager.getPlayerName(context)
                    currentData.child("role").value = currentData.child("role").getValue(String::class.java) ?: "user"
                    currentData.child("coins").value = coins
                    currentData.child("xp").value = xp
                    currentData.child("level").value = level
                    currentData.child("levelTitle").value = levelTitle
                    currentData.child("gamesPlayed").value = games
                    currentData.child("streak").value = streak
                    currentData.child("lastActive").value = System.currentTimeMillis()
                    currentData.child("lastUpdated").value = if (localTimestamp > 0) localTimestamp else System.currentTimeMillis()
                    
                    if (currentData.child("createdAt").getValue(Long::class.java) == null) {
                        currentData.child("createdAt").value = System.currentTimeMillis()
                    }
                    if (currentData.child("isBanned").getValue(Boolean::class.java) == null) {
                        currentData.child("isBanned").value = false
                    }

                    // Device info for Admin Panel
                    currentData.child("deviceInfo").child("manufacturer").value = Build.MANUFACTURER
                    currentData.child("deviceInfo").child("model").value = Build.MODEL
                    currentData.child("deviceInfo").child("androidVersion").value = Build.VERSION.RELEASE
                    currentData.child("deviceInfo").child("sdkVersion").value = Build.VERSION.SDK_INT
                    currentData.child("deviceInfo").child("appVersion").value = "1.0.0"

                    // Best scores
                    val tapBest = TapRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val memBest = MemoryFlashScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val aimBest = PerfectAimScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val numBest = NumberSprintScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val spotBest = SpotDifferenceScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val speedBest = SpeedRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                    val overallHighest = maxOf(tapBest, memBest, aimBest, numBest, spotBest, speedBest)

                    currentData.child("bestScores").child("tapRush").value = tapBest
                    currentData.child("bestScores").child("memoryFlash").value = memBest
                    currentData.child("bestScores").child("perfectAim").value = aimBest
                    currentData.child("bestScores").child("numberSprint").value = numBest
                    currentData.child("bestScores").child("spotDifference").value = spotBest
                    currentData.child("bestScores").child("speedRush").value = speedBest
                    currentData.child("bestScores").child("overallHighest").value = overallHighest

                    // Power-ups
                    currentData.child("powerUps").value = DailyRewardManager.exportPowerUpsForCloud(context)

                    // Daily Reward
                    currentData.child("dailyReward").value = DailyRewardManager.exportDailyRewardForCloud(context)

                    // Daily Challenge
                    currentData.child("dailyChallenge").value = DailyChallengeManager.exportDailyChallengeForCloud(context)

                    // Achievements
                    currentData.child("achievements").value = AchievementManager.exportAchievementsForCloud(context)

                    // Settings
                    currentData.child("settings").value = SettingsManager.exportSettingsForCloud(context)

                    // Statistics
                    val statsMap = StatsManager.exportStatsForCloud(context)
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
                    _syncState.value = "Error"
                } else if (currentData != null) {
                    _syncState.value = "Synced"
                    val cloudTimestamp = currentData.child("lastUpdated").getValue(Long::class.java) ?: 0L
                    val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)

                    if (cloudTimestamp > localTimestamp) {
                        Log.d(TAG, "Cloud data is newer, updating local data.")
                        val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                        val xp = currentData.child("xp").getValue(Int::class.java) ?: 0
                        val games = currentData.child("gamesPlayed").getValue(Int::class.java) ?: 0
                        val streak = currentData.child("streak").getValue(Int::class.java) ?: 0

                        val validation = ScoreValidator.validateProfileData(coins, xp, games, streak)
                        if (validation.isValid) {
                            UserProfileManager.overwriteLocalDataFromCloud(context, coins, xp, games, streak, cloudTimestamp)

                            TapRushScoreManager.saveBestScore(context, (currentData.child("bestScores").child("tapRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            MemoryFlashScoreManager.saveBestScore(context, (currentData.child("bestScores").child("memoryFlash").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            PerfectAimScoreManager.saveBestScore(context, (currentData.child("bestScores").child("perfectAim").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            NumberSprintScoreManager.saveBestScore(context, (currentData.child("bestScores").child("numberSprint").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpotDifferenceScoreManager.saveBestScore(context, (currentData.child("bestScores").child("spotDifference").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                            SpeedRushScoreManager.saveBestScore(context, (currentData.child("bestScores").child("speedRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))

                            (currentData.child("powerUps").value as? Map<String, Any?>)?.let {
                                DailyRewardManager.mergeCloudPowerUps(context, it)
                            }
                            (currentData.child("dailyReward").value as? Map<String, Any?>)?.let {
                                DailyRewardManager.mergeCloudDailyReward(context, it)
                            }
                            (currentData.child("dailyChallenge").value as? Map<String, Any?>)?.let {
                                DailyChallengeManager.mergeCloudDailyChallenge(context, it)
                            }
                            (currentData.child("achievements").value as? Map<String, Any?>)?.let {
                                AchievementManager.mergeCloudAchievements(context, it)
                            }
                            (currentData.child("settings").value as? Map<String, Any?>)?.let {
                                SettingsManager.mergeCloudSettings(context, it)
                            }
                            (currentData.child("stats").value as? Map<String, Any?>)?.let {
                                StatsManager.mergeCloudStats(context, it)
                            }
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
        if (!isInitialized) {
            initialize(context)
            return
        }
        if (!UserProfileManager.hasSetupUsername(context)) return
        val uid = UserProfileManager.getUserUid(context)
        if (uid.isBlank()) return
        val localTimestamp = UserProfileManager.getLastUpdatedTimestamp(context)
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        val coins = UserProfileManager.getCoins(context)
        val xp = UserProfileManager.getTotalXp(context)
        val games = UserProfileManager.getTotalGamesPlayed(context)
        val streak = UserProfileManager.getStreakDays(context)
        val level = UserProfileManager.getLevel(context)
        val levelTitle = UserProfileManager.getTitleForLevel(level)

        val validation = ScoreValidator.validateProfileData(coins, xp, games, streak)
        if (!validation.isValid) {
            Log.e(TAG, "Cannot sync local profile update: ${validation.reason}")
            return
        }

        dbRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                currentData.child("uid").value = uid
                currentData.child("username").value = UserProfileManager.getPlayerName(context)
                currentData.child("coins").value = coins
                currentData.child("xp").value = xp
                currentData.child("level").value = level
                currentData.child("levelTitle").value = levelTitle
                currentData.child("gamesPlayed").value = games
                currentData.child("streak").value = streak
                currentData.child("lastActive").value = System.currentTimeMillis()
                currentData.child("lastUpdated").value = localTimestamp

                // Best scores
                val tapBest = TapRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                val memBest = MemoryFlashScoreManager.getBestScore(context).coerceIn(0, 10000)
                val aimBest = PerfectAimScoreManager.getBestScore(context).coerceIn(0, 10000)
                val numBest = NumberSprintScoreManager.getBestScore(context).coerceIn(0, 10000)
                val spotBest = SpotDifferenceScoreManager.getBestScore(context).coerceIn(0, 10000)
                val speedBest = SpeedRushScoreManager.getBestScore(context).coerceIn(0, 10000)
                val overallHighest = maxOf(tapBest, memBest, aimBest, numBest, spotBest, speedBest)

                currentData.child("bestScores").child("tapRush").value = tapBest
                currentData.child("bestScores").child("memoryFlash").value = memBest
                currentData.child("bestScores").child("perfectAim").value = aimBest
                currentData.child("bestScores").child("numberSprint").value = numBest
                currentData.child("bestScores").child("spotDifference").value = spotBest
                currentData.child("bestScores").child("speedRush").value = speedBest
                currentData.child("bestScores").child("overallHighest").value = overallHighest

                // Power-ups
                currentData.child("powerUps").value = DailyRewardManager.exportPowerUpsForCloud(context)

                // Daily Reward
                currentData.child("dailyReward").value = DailyRewardManager.exportDailyRewardForCloud(context)

                // Daily Challenge
                currentData.child("dailyChallenge").value = DailyChallengeManager.exportDailyChallengeForCloud(context)

                // Achievements
                currentData.child("achievements").value = AchievementManager.exportAchievementsForCloud(context)

                // Settings
                currentData.child("settings").value = SettingsManager.exportSettingsForCloud(context)

                // Stats
                currentData.child("stats").value = StatsManager.exportStatsForCloud(context)

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

    /**
     * Logs every completed game session to Firebase.
     * Accessible in Admin Panel under `game_logs` for live monitoring & cheat analysis.
     */
    fun logGameSession(
        context: Context,
        gameId: String,
        gameTitle: String,
        score: Int,
        accuracy: Float,
        maxCombo: Int,
        xpEarned: Int,
        coinsEarned: Int,
        sessionToken: String? = null,
        durationSeconds: Long = 30L
    ) {
        if (!isInitialized) return
        val uid = UserProfileManager.getUserUid(context)
        val username = UserProfileManager.getPlayerName(context)
        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val logData = mapOf(
            "logId" to logId,
            "uid" to uid,
            "username" to username,
            "gameId" to gameId,
            "gameTitle" to gameTitle,
            "score" to score,
            "accuracy" to accuracy,
            "maxCombo" to maxCombo,
            "xpEarned" to xpEarned,
            "coinsEarned" to coinsEarned,
            "durationSeconds" to durationSeconds,
            "timestamp" to System.currentTimeMillis(),
            "dateFormatted" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "sessionToken" to (sessionToken ?: "")
        )

        try {
            val db = FirebaseDatabase.getInstance()
            // 1. Global log
            db.getReference("game_logs/$logId").setValue(logData)
            // 2. User specific game logs (last 50)
            db.getReference("users/$uid/game_logs/$logId").setValue(logData)
            // 3. Update last active timestamp
            db.getReference("users/$uid/lastActive").setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to push game session log: ${e.message}")
        }
    }

    /**
     * Submits a player report / feedback directly to the Admin Panel.
     */
    fun submitReport(
        context: Context,
        category: String,
        message: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (message.isBlank()) {
            onComplete(false)
            return
        }
        val uid = UserProfileManager.getUserUid(context)
        val username = UserProfileManager.getPlayerName(context)
        val reportId = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val reportData = mapOf(
            "reportId" to reportId,
            "uid" to uid,
            "username" to username,
            "category" to category,
            "message" to message.trim(),
            "timestamp" to System.currentTimeMillis(),
            "dateFormatted" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "status" to "OPEN",
            "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        )

        try {
            FirebaseDatabase.getInstance().getReference("reports/$reportId")
                .setValue(reportData)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit report", e)
            onComplete(false)
        }
    }
}
