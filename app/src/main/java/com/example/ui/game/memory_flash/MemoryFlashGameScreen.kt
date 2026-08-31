package com.example.ui.game.memory_flash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryFlashScoreManager
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.Locale

enum class MemoryFlashGameState {
    READY,
    COUNTDOWN,
    FLASHING,    // Showing sequence for 3 seconds
    PLAYER_TURN, // Player tapping sequence
    ROUND_SUCCESS,
    WRONG_FEEDBACK,
    PAUSED,
    GAME_OVER
}

enum class MemoryFlashSoundEvent {
    GAME_START,
    COUNTDOWN_TICK,
    TILE_FLASH,
    CORRECT_TAP,
    WRONG_TAP,
    ROUND_COMPLETE,
    LIFE_LOST,
    COMBO_STREAK,
    GAME_OVER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryFlashGameScreen(
    onBack: () -> Unit,
    onSoundEvent: (MemoryFlashSoundEvent) -> Unit = {},
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(MemoryFlashGameState.READY) }
    var countdownNumber by remember { mutableIntStateOf(3) }
    var level by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var currentCombo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var totalTaps by remember { mutableIntStateOf(0) }
    var correctTaps by remember { mutableIntStateOf(0) }

    // Sequence for current round (indices 0..8)
    var currentSequence by remember { mutableStateOf(listOf<Int>()) }
    var userTappedIndices by remember { mutableStateOf(listOf<Int>()) }
    var wrongTileIndex by remember { mutableIntStateOf(-1) }

    // 3-second flash timer tracking
    var flashTimeRemainingMillis by remember { mutableLongStateOf(3000L) }
    val totalFlashDurationMillis = 3000L

    // Best records
    var bestScore by remember { mutableIntStateOf(MemoryFlashScoreManager.getBestScore(context)) }
    var highestLevelRecord by remember { mutableIntStateOf(MemoryFlashScoreManager.getHighestLevel(context)) }
    var saveResult by remember { mutableStateOf<MemoryFlashScoreManager.SaveResult?>(null) }

    // Jobs for async timers & transitions
    var flashTimerJob by remember { mutableStateOf<Job?>(null) }
    var statusTransitionJob by remember { mutableStateOf<Job?>(null) }

    // Status message shown under HUD
    var statusMessage by remember { mutableStateOf("Memorize the sequence!") }

    val handleSoundEvent: (MemoryFlashSoundEvent) -> Unit = { event ->
        onSoundEvent(event)
        when (event) {
            MemoryFlashSoundEvent.COUNTDOWN_TICK -> com.example.audio.AudioManager.playTapSound(context)
            MemoryFlashSoundEvent.TILE_FLASH -> com.example.audio.AudioManager.playTapSound(context)
            MemoryFlashSoundEvent.CORRECT_TAP -> com.example.audio.AudioManager.playCorrectSound(context)
            MemoryFlashSoundEvent.WRONG_TAP -> com.example.audio.AudioManager.playWrongSound(context)
            MemoryFlashSoundEvent.ROUND_COMPLETE -> com.example.audio.AudioManager.playScoreComboSound(context, level)
            MemoryFlashSoundEvent.LIFE_LOST -> com.example.audio.AudioManager.playWrongSound(context)
            MemoryFlashSoundEvent.COMBO_STREAK -> com.example.audio.AudioManager.playScoreComboSound(context, currentCombo)
            MemoryFlashSoundEvent.GAME_START -> com.example.audio.AudioManager.playGameStartSound(context)
            MemoryFlashSoundEvent.GAME_OVER -> com.example.audio.AudioManager.playGameEndSound(context)
        }
    }

    fun triggerHaptic(type: HapticFeedbackType) {
        try {
            hapticFeedback.performHapticFeedback(type)
        } catch (_: Exception) {}
    }

    // End Game Handler
    fun endGame() {
        flashTimerJob?.cancel()
        statusTransitionJob?.cancel()
        gameState = MemoryFlashGameState.GAME_OVER

        val accuracy = if (totalTaps > 0) (correctTaps.toFloat() / totalTaps * 100f) else 0f

        val result = MemoryFlashScoreManager.recordGameResult(
            context = context,
            score = score,
            level = level,
            accuracy = accuracy,
            maxCombo = maxCombo
        )
        saveResult = result
        bestScore = result.newBestScore
        highestLevelRecord = result.highestLevel

        handleSoundEvent(MemoryFlashSoundEvent.GAME_OVER)
        triggerHaptic(HapticFeedbackType.LongPress)

        if (tournamentId != null && sessionToken != null) {
            coroutineScope.launch {
                try {
                    val repo = com.example.data.tournament.TournamentRepository(context)
                    val submitRes = repo.submitTournamentScore(
                        tournamentId = tournamentId,
                        tournamentTitle = "Memory Flash Weekly Showdown",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("MemoryFlash", "Tournament submit error: ${e.message}")
                }
            }
        }
    }

    // Generate and show sequence for the given round
    fun startFlashPhase(forLevel: Int) {
        flashTimerJob?.cancel()
        statusTransitionJob?.cancel()
        gameState = MemoryFlashGameState.FLASHING
        userTappedIndices = emptyList()
        wrongTileIndex = -1
        flashTimeRemainingMillis = totalFlashDurationMillis
        statusMessage = "Memorize the sequence (3s)..."

        // Determine sequence length based on level (3 tiles for Level 1, up to 7 tiles)
        val seqLength = (forLevel + 2).coerceIn(3, 8)
        val newSeq = mutableListOf<Int>()
        while (newSeq.size < seqLength) {
            val nextTile = Random.nextInt(0, 9)
            // Ensure no direct consecutive identical tile to make the sequence crisp
            if (newSeq.isEmpty() || newSeq.last() != nextTile) {
                newSeq.add(nextTile)
            }
        }
        currentSequence = newSeq

        handleSoundEvent(MemoryFlashSoundEvent.TILE_FLASH)

        // 3-second countdown loop for showing sequence
        flashTimerJob = coroutineScope.launch {
            val tick = 50L
            while (isActive && flashTimeRemainingMillis > 0) {
                delay(tick)
                flashTimeRemainingMillis = (flashTimeRemainingMillis - tick).coerceAtLeast(0L)
            }
            if (isActive && gameState == MemoryFlashGameState.FLASHING) {
                gameState = MemoryFlashGameState.PLAYER_TURN
                statusMessage = "Your turn! Tap the tiles in exact order."
            }
        }
    }

    // Start 3-2-1 Countdown
    fun startCountdown() {
        flashTimerJob?.cancel()
        statusTransitionJob?.cancel()
        gameState = MemoryFlashGameState.COUNTDOWN
        countdownNumber = 3
        level = 1
        score = 0
        lives = 3
        currentCombo = 0
        maxCombo = 0
        totalTaps = 0
        correctTaps = 0
        userTappedIndices = emptyList()
        wrongTileIndex = -1
        saveResult = null

        coroutineScope.launch {
            handleSoundEvent(MemoryFlashSoundEvent.COUNTDOWN_TICK)
            delay(800)
            countdownNumber = 2
            handleSoundEvent(MemoryFlashSoundEvent.COUNTDOWN_TICK)
            delay(800)
            countdownNumber = 1
            handleSoundEvent(MemoryFlashSoundEvent.COUNTDOWN_TICK)
            delay(800)
            handleSoundEvent(MemoryFlashSoundEvent.GAME_START)
            startFlashPhase(forLevel = 1)
        }
    }

    // Pause game
    fun pauseGame() {
        if (gameState == MemoryFlashGameState.FLASHING || gameState == MemoryFlashGameState.PLAYER_TURN) {
            flashTimerJob?.cancel()
            gameState = MemoryFlashGameState.PAUSED
        }
    }

    // Resume game
    fun resumeGame() {
        if (gameState == MemoryFlashGameState.PAUSED) {
            if (flashTimeRemainingMillis > 0 && userTappedIndices.isEmpty()) {
                // Resume flash countdown
                gameState = MemoryFlashGameState.FLASHING
                flashTimerJob = coroutineScope.launch {
                    val tick = 50L
                    while (isActive && flashTimeRemainingMillis > 0) {
                        delay(tick)
                        flashTimeRemainingMillis = (flashTimeRemainingMillis - tick).coerceAtLeast(0L)
                    }
                    if (isActive && gameState == MemoryFlashGameState.FLASHING) {
                        gameState = MemoryFlashGameState.PLAYER_TURN
                        statusMessage = "Your turn! Tap the tiles in exact order."
                    }
                }
            } else {
                gameState = MemoryFlashGameState.PLAYER_TURN
            }
        }
    }

    // Restart game
    fun restartGame() {
        flashTimerJob?.cancel()
        statusTransitionJob?.cancel()
        startCountdown()
    }

    // Handle tile tap during PLAYER_TURN
    fun handleTileTap(tileIndex: Int) {
        if (gameState != MemoryFlashGameState.PLAYER_TURN) return
        val currentTapPosition = userTappedIndices.size
        if (currentTapPosition >= currentSequence.size) return

        totalTaps++
        val expectedTile = currentSequence[currentTapPosition]

        if (tileIndex == expectedTile) {
            // Correct tap
            correctTaps++
            val nextTapped = userTappedIndices + tileIndex
            userTappedIndices = nextTapped
            currentCombo++
            if (currentCombo > maxCombo) maxCombo = currentCombo

            triggerHaptic(HapticFeedbackType.TextHandleMove)
            handleSoundEvent(MemoryFlashSoundEvent.CORRECT_TAP)

            if (currentCombo % 5 == 0) {
                handleSoundEvent(MemoryFlashSoundEvent.COMBO_STREAK)
            }

            // Check if level sequence is fully completed
            if (nextTapped.size == currentSequence.size) {
                val roundPoints = (level * 20) + (currentCombo * 5)
                score += roundPoints
                gameState = MemoryFlashGameState.ROUND_SUCCESS
                statusMessage = "Round Cleared! +$roundPoints pts"
                handleSoundEvent(MemoryFlashSoundEvent.ROUND_COMPLETE)

                statusTransitionJob = coroutineScope.launch {
                    delay(900)
                    level++
                    startFlashPhase(forLevel = level)
                }
            }
        } else {
            // Wrong tap
            wrongTileIndex = tileIndex
            currentCombo = 0
            val remainingLives = lives - 1
            lives = remainingLives

            triggerHaptic(HapticFeedbackType.LongPress)
            onSoundEvent(MemoryFlashSoundEvent.WRONG_TAP)
            onSoundEvent(MemoryFlashSoundEvent.LIFE_LOST)

            if (remainingLives <= 0) {
                endGame()
            } else {
                gameState = MemoryFlashGameState.WRONG_FEEDBACK
                statusMessage = "Wrong Tile! -1 Life. Memorize again..."
                statusTransitionJob = coroutineScope.launch {
                    delay(1000)
                    // Replay current sequence so player can recover
                    startFlashPhase(forLevel = level)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MEMORY FLASH",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = DifficultyMediumContainer
                        ) {
                            Text(
                                text = "LVL $level",
                                style = MaterialTheme.typography.labelSmall,
                                color = DifficultyMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .testTag("memory_flash_level_badge"),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            flashTimerJob?.cancel()
                            statusTransitionJob?.cancel()
                            onBack()
                        },
                        modifier = Modifier.testTag("memory_flash_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    if (gameState == MemoryFlashGameState.FLASHING || gameState == MemoryFlashGameState.PLAYER_TURN) {
                        IconButton(
                            onClick = { pauseGame() },
                            modifier = Modifier.testTag("memory_flash_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = SkillRushPrimary
                            )
                        }
                    } else if (gameState == MemoryFlashGameState.PAUSED) {
                        IconButton(
                            onClick = { resumeGame() },
                            modifier = Modifier.testTag("memory_flash_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = SkillRushPrimary
                            )
                        }
                    }

                    if (gameState != MemoryFlashGameState.READY && gameState != MemoryFlashGameState.COUNTDOWN) {
                        IconButton(
                            onClick = { restartGame() },
                            modifier = Modifier.testTag("memory_flash_restart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live HUD: Score, Lives, Combo, Level
            MemoryFlashHud(
                score = score,
                level = level,
                lives = lives,
                combo = currentCombo,
                bestScore = bestScore,
                gameState = gameState,
                flashTimeRemainingMillis = flashTimeRemainingMillis,
                totalFlashDurationMillis = totalFlashDurationMillis,
                statusMessage = statusMessage
            )

            // 3x3 Grid Game Arena
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        BorderStroke(
                            2.dp,
                            if (currentCombo >= 5) SkillRushCoinGold else SkillRushPrimary.copy(alpha = 0.25f)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
                    .testTag("memory_flash_arena"),
                contentAlignment = Alignment.Center
            ) {
                // The 3x3 Matrix
                Column(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (col in 0 until 3) {
                                val tileIndex = row * 3 + col
                                MemoryFlashTile(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    tileIndex = tileIndex,
                                    sequence = currentSequence,
                                    userTappedIndices = userTappedIndices,
                                    wrongTileIndex = wrongTileIndex,
                                    gameState = gameState,
                                    onTap = { handleTileTap(tileIndex) }
                                )
                            }
                        }
                    }
                }

                // Ready Overlay
                if (gameState == MemoryFlashGameState.READY) {
                    MemoryFlashReadyOverlay(
                        bestScore = bestScore,
                        highestLevel = highestLevelRecord,
                        onStart = { startCountdown() }
                    )
                }

                // 3-2-1 Countdown Overlay
                if (gameState == MemoryFlashGameState.COUNTDOWN) {
                    MemoryFlashCountdownOverlay(number = countdownNumber)
                }

                // Paused Overlay
                if (gameState == MemoryFlashGameState.PAUSED) {
                    MemoryFlashPausedOverlay(
                        onResume = { resumeGame() },
                        onRestart = { restartGame() },
                        onQuit = { onBack() }
                    )
                }

                // Game Over Overlay
                if (gameState == MemoryFlashGameState.GAME_OVER) {
                    MemoryFlashGameOverOverlay(
                        score = score,
                        level = level,
                        correctTaps = correctTaps,
                        totalTaps = totalTaps,
                        maxCombo = maxCombo,
                        saveResult = saveResult,
                        onPlayAgain = { startCountdown() },
                        onExit = { onBack() }
                    )
                }

                tournamentResult?.let { res ->
                    com.example.ui.tournament.TournamentResultDialog(
                        result = res,
                        onPlayAgain = {
                            tournamentResult = null
                            startCountdown()
                        },
                        onViewStandings = {
                            tournamentResult = null
                            onBack()
                        },
                        onDismiss = {
                            tournamentResult = null
                        }
                    )
                }
            }

            // Bottom Quick Controls Bar
            MemoryFlashBottomControls(
                gameState = gameState,
                onStart = { startCountdown() },
                onPause = { pauseGame() },
                onResume = { resumeGame() },
                onRestart = { restartGame() }
            )
        }
    }
}

@Composable
fun MemoryFlashHud(
    score: Int,
    level: Int,
    lives: Int,
    combo: Int,
    bestScore: Int,
    gameState: MemoryFlashGameState,
    flashTimeRemainingMillis: Long,
    totalFlashDurationMillis: Long,
    statusMessage: String
) {
    val flashProgress = (flashTimeRemainingMillis.toFloat() / totalFlashDurationMillis).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Score, Lives Hearts, Combo Badge, Best
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Column {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = SkillRushPrimary,
                        modifier = Modifier.testTag("memory_flash_live_score")
                    )
                }

                // 3 Hearts for Lives
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("memory_flash_lives_container")
                ) {
                    for (i in 1..3) {
                        val isAlive = i <= lives
                        Icon(
                            imageVector = if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isAlive) "Active Life" else "Lost Life",
                            tint = if (isAlive) DifficultyHard else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Combo Badge
                AnimatedVisibility(
                    visible = combo > 1,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (combo >= 5) SkillRushCoinGold else SkillRushPrimaryContainer,
                        border = BorderStroke(1.dp, if (combo >= 5) Color.White else SkillRushPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Combo",
                                tint = if (combo >= 5) Color.White else SkillRushStreakFire,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "x$combo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (combo >= 5) Color.White else SkillRushOnPrimaryContainer
                            )
                        }
                    }
                }

                // Best Score Box
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RECORD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Best",
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$bestScore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3-Second Flash Progress Bar during FLASHING phase
            if (gameState == MemoryFlashGameState.FLASHING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { flashProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = SkillRushCoinGold,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        text = String.format("%.1fs", flashTimeRemainingMillis / 1000f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = SkillRushCoinGold
                    )
                }
            }

            // Live status prompt
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (gameState) {
                    MemoryFlashGameState.FLASHING -> SkillRushCoinGold
                    MemoryFlashGameState.ROUND_SUCCESS -> DifficultyEasy
                    MemoryFlashGameState.WRONG_FEEDBACK -> DifficultyHard
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_flash_status_text")
            )
        }
    }
}

@Composable
fun MemoryFlashTile(
    modifier: Modifier = Modifier,
    tileIndex: Int,
    sequence: List<Int>,
    userTappedIndices: List<Int>,
    wrongTileIndex: Int,
    gameState: MemoryFlashGameState,
    onTap: () -> Unit
) {
    // Check if tile is in the sequence and what order position it appears
    val sequencePositions = remember(sequence, tileIndex) {
        sequence.mapIndexedNotNull { index, item -> if (item == tileIndex) index + 1 else null }
    }
    val isPartOfSequence = sequencePositions.isNotEmpty()

    // Flashing state during demonstration
    val isFlashing = gameState == MemoryFlashGameState.FLASHING && isPartOfSequence

    // Tapped state during player turn
    val tappedCount = userTappedIndices.count { it == tileIndex }
    val isTappedCorrectly = tappedCount > 0 && gameState == MemoryFlashGameState.PLAYER_TURN

    // Wrong tap error state
    val isWrong = wrongTileIndex == tileIndex && gameState == MemoryFlashGameState.WRONG_FEEDBACK

    val isSuccessRound = gameState == MemoryFlashGameState.ROUND_SUCCESS && isPartOfSequence

    // Animated colors and scaling
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWrong -> DifficultyHard
            isSuccessRound -> DifficultyEasy
            isFlashing -> SkillRushPrimary
            isTappedCorrectly -> SkillRushPrimary.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "tile_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isWrong -> DifficultyHard
            isSuccessRound -> DifficultyEasy
            isFlashing -> SkillRushCoinGold
            isTappedCorrectly -> SkillRushPrimary
            else -> SkillRushPrimary.copy(alpha = 0.2f)
        },
        label = "tile_border"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFlashing || isTappedCorrectly || isSuccessRound) 1.04f else 1.0f,
        label = "tile_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(18.dp))
            .shadow(
                elevation = if (isFlashing || isTappedCorrectly) 6.dp else 2.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                enabled = gameState == MemoryFlashGameState.PLAYER_TURN,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) {
                onTap()
            }
            .testTag("memory_flash_tile_$tileIndex"),
        contentAlignment = Alignment.Center
    ) {
        when {
            // During flashing phase, display the step number badges (e.g. 1, 2, 3...)
            isFlashing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = sequencePositions.joinToString("/"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            // During player turn, show check if already tapped in this turn
            isTappedCorrectly -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Tapped",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Wrong tile feedback
            isWrong -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Wrong",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            // Success round feedback
            isSuccessRound -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            else -> {
                // Default subtle dot indicator in center
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                )
            }
        }
    }
}

@Composable
fun MemoryFlashReadyOverlay(
    bestScore: Int,
    highestLevel: Int,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SkillRushPrimaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = "MEMORY FLASH",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = SkillRushPrimary
                )

                Text(
                    text = "Memorize the illuminated sequence during the 3-second flash, then replay the exact sequence. 3 lives — don't miss!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Best Record", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$bestScore pts", fontWeight = FontWeight.Black, color = SkillRushPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Highest Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Level ${if (highestLevel > 0) highestLevel else 1}", fontWeight = FontWeight.Black, color = SkillRushCoinGold)
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("memory_flash_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START FLASH ROUND", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun MemoryFlashCountdownOverlay(number: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 84.sp
            )
            Text(
                text = "GET READY TO MEMORIZE!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SkillRushCoinGold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun MemoryFlashPausedOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = SkillRushPrimary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "GAME PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = SkillRushPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onQuit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("QUIT TO ARENA", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MemoryFlashGameOverOverlay(
    score: Int,
    level: Int,
    correctTaps: Int,
    totalTaps: Int,
    maxCombo: Int,
    saveResult: MemoryFlashScoreManager.SaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalTaps > 0) (correctTaps.toFloat() / totalTaps * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 10).coerceAtMost(30) + (level * 3).coerceAtMost(20) + (maxCombo * 2).coerceAtMost(15) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        level >= 7 && accuracy >= 90f -> "S"
        level >= 5 && accuracy >= 80f -> "A"
        level >= 3 -> "B"
        else -> "C"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header badge
                if (isNewBest) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SkillRushCoinGold
                    ) {
                        Text(
                            text = "🏆 NEW HIGH SCORE!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "GAME OVER",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                // Final Score & Grade
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = SkillRushPrimary,
                            modifier = Modifier.testTag("memory_flash_final_score")
                        )
                        Text(
                            text = "FINAL SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SkillRushPrimaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = grade,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = SkillRushOnPrimaryContainer
                            )
                        }
                    }
                }

                // Stats Matrix
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatItem("Highest Level", "Level $level", Icons.Default.Psychology, SkillRushPrimary)
                        StatItem("Accuracy", String.format(Locale.getDefault(), "%.1f%%", accuracy), Icons.Default.AdsClick, DifficultyEasy)
                        StatItem("Max Combo", "x$maxCombo", Icons.Default.LocalFireDepartment, SkillRushStreakFire)
                        StatItem("Coins Earned", "+$coinsEarned Coins", Icons.Default.MonetizationOn, SkillRushCoinGold)
                        StatItem("XP Earned", "+$xpEarned XP", Icons.Default.Stars, SkillRushPrimary)
                    }
                }

                // Action Buttons
                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("memory_flash_play_again_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY AGAIN", fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RETURN TO ARENA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MemoryFlashBottomControls(
    gameState: MemoryFlashGameState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (gameState) {
            MemoryFlashGameState.READY -> {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START FLASH (3 LIVES)", fontWeight = FontWeight.Black)
                }
            }
            MemoryFlashGameState.FLASHING, MemoryFlashGameState.PLAYER_TURN -> {
                Button(
                    onClick = onPause,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PAUSE", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold)
                }
            }
            MemoryFlashGameState.PAUSED -> {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold)
                }
            }
            MemoryFlashGameState.COUNTDOWN, MemoryFlashGameState.ROUND_SUCCESS, MemoryFlashGameState.WRONG_FEEDBACK, MemoryFlashGameState.GAME_OVER -> {
                // Controlled by animated overlays
            }
        }
    }
}
