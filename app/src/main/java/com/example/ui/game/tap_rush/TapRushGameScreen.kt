package com.example.ui.game.tap_rush

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TapRushScoreManager
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import java.util.Locale

enum class TapRushGameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class TapRushSoundEvent {
    GAME_START,
    COUNTDOWN_TICK,
    TARGET_HIT,
    MISS,
    COMBO_STREAK,
    GAME_OVER
}

data class FloatingIndicator(
    val id: Long,
    val text: String,
    val xOffset: Float,
    val yOffset: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapRushGameScreen(
    onBack: () -> Unit,
    onSoundEvent: (TapRushSoundEvent) -> Unit = {},
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(TapRushGameState.READY) }
    var countdownNumber by remember { mutableIntStateOf(3) }
    var timeRemainingMillis by remember { mutableLongStateOf(30_000L) }
    val totalGameDurationMillis = 30_000L

    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var currentCombo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }

    // Target properties
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.5f) }
    var targetSpawnToken by remember { mutableLongStateOf(1L) }
    var isTargetActive by remember { mutableStateOf(false) }

    // Floating text popups for hits/misses
    var floatingIndicators by remember { mutableStateOf(listOf<FloatingIndicator>()) }

    // Best scores
    var bestScore by remember { mutableIntStateOf(TapRushScoreManager.getBestScore(context)) }
    var saveResult by remember { mutableStateOf<TapRushScoreManager.SaveResult?>(null) }

    val handleSoundEvent: (TapRushSoundEvent) -> Unit = { event ->
        onSoundEvent(event)
        when (event) {
            TapRushSoundEvent.COUNTDOWN_TICK -> com.example.audio.AudioManager.playTapSound(context)
            TapRushSoundEvent.GAME_START -> com.example.audio.AudioManager.playGameStartSound(context)
            TapRushSoundEvent.TARGET_HIT -> com.example.audio.AudioManager.playCorrectSound(context)
            TapRushSoundEvent.MISS -> com.example.audio.AudioManager.playWrongSound(context)
            TapRushSoundEvent.COMBO_STREAK -> com.example.audio.AudioManager.playScoreComboSound(context, currentCombo)
            TapRushSoundEvent.GAME_OVER -> com.example.audio.AudioManager.playGameEndSound(context)
        }
    }

    // Active timer loop job
    var gameLoopJob by remember { mutableStateOf<Job?>(null) }
    var targetLifecycleJob by remember { mutableStateOf<Job?>(null) }

    // Helper to spawn floating feedback
    fun addIndicator(text: String, x: Float, y: Float, color: Color) {
        val indicator = FloatingIndicator(
            id = System.currentTimeMillis() + Random.nextInt(1000),
            text = text,
            xOffset = x,
            yOffset = y,
            color = color
        )
        floatingIndicators = (floatingIndicators + indicator).takeLast(6)
    }

    // Spawn a new target at a random location
    fun spawnTarget() {
        targetLifecycleJob?.cancel()
        targetX = Random.nextFloat().coerceIn(0.12f, 0.88f)
        targetY = Random.nextFloat().coerceIn(0.15f, 0.85f)
        val newToken = targetSpawnToken + 1
        targetSpawnToken = newToken
        isTargetActive = true

        // Dynamic speed calculation: Target stays shorter as time goes on
        val speedFactor = (timeRemainingMillis.toFloat() / totalGameDurationMillis).coerceIn(0f, 1f)
        val targetLifetime = (550L + (speedFactor * 750L)).toLong() // 1300ms down to 550ms

        targetLifecycleJob = coroutineScope.launch {
            delay(targetLifetime)
            if (isActive && gameState == TapRushGameState.PLAYING && targetSpawnToken == newToken && isTargetActive) {
                // Target timed out without tap -> auto shift & reset combo
                if (currentCombo > 0) {
                    currentCombo = 0
                }
                spawnTarget()
            }
        }
    }

    // End Game Handler
    fun endGame() {
        gameLoopJob?.cancel()
        targetLifecycleJob?.cancel()
        isTargetActive = false
        gameState = TapRushGameState.GAME_OVER

        val totalTaps = hits + misses
        val accuracy = if (totalTaps > 0) (hits.toFloat() / totalTaps * 100f) else 0f

        val result = TapRushScoreManager.recordGameResult(
            context = context,
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo
        )
        saveResult = result
        bestScore = result.newBestScore

        handleSoundEvent(TapRushSoundEvent.GAME_OVER)
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}

        if (tournamentId != null && sessionToken != null) {
            coroutineScope.launch {
                try {
                    val repo = com.example.data.tournament.TournamentRepository(context)
                    val submitRes = repo.submitTournamentScore(
                        tournamentId = tournamentId,
                        tournamentTitle = "Tap Rush Daily Clash",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("TapRush", "Failed submitting tournament score: ${e.message}")
                }
            }
        }
    }

    // Start 30-second main game loop
    fun launchGameLoop() {
        gameLoopJob?.cancel()
        isTargetActive = true
        spawnTarget()

        gameLoopJob = coroutineScope.launch {
            val tickInterval = 50L
            while (isActive && timeRemainingMillis > 0) {
                delay(tickInterval)
                timeRemainingMillis = (timeRemainingMillis - tickInterval).coerceAtLeast(0L)
                if (timeRemainingMillis <= 0L) {
                    endGame()
                    break
                }
            }
        }
    }

    // Start 3-2-1 countdown
    fun startCountdown() {
        gameLoopJob?.cancel()
        targetLifecycleJob?.cancel()
        gameState = TapRushGameState.COUNTDOWN
        countdownNumber = 3
        timeRemainingMillis = 30_000L
        score = 0
        hits = 0
        misses = 0
        currentCombo = 0
        maxCombo = 0
        floatingIndicators = emptyList()
        saveResult = null

        coroutineScope.launch {
            handleSoundEvent(TapRushSoundEvent.COUNTDOWN_TICK)
            delay(800)
            countdownNumber = 2
            handleSoundEvent(TapRushSoundEvent.COUNTDOWN_TICK)
            delay(800)
            countdownNumber = 1
            handleSoundEvent(TapRushSoundEvent.COUNTDOWN_TICK)
            delay(800)
            gameState = TapRushGameState.PLAYING
            handleSoundEvent(TapRushSoundEvent.GAME_START)
            launchGameLoop()
        }
    }

    // Resume from Pause
    fun resumeGame() {
        if (gameState == TapRushGameState.PAUSED) {
            gameState = TapRushGameState.PLAYING
            launchGameLoop()
        }
    }

    // Pause Game
    fun pauseGame() {
        if (gameState == TapRushGameState.PLAYING) {
            gameLoopJob?.cancel()
            targetLifecycleJob?.cancel()
            gameState = TapRushGameState.PAUSED
        }
    }

    // Restart Game
    fun restartGame() {
        gameLoopJob?.cancel()
        targetLifecycleJob?.cancel()
        startCountdown()
    }

    // Handle Successful Target Hit
    fun handleTargetHit(token: Long) {
        if (gameState != TapRushGameState.PLAYING || !isTargetActive || targetSpawnToken != token) {
            return
        }
        // Immediately lock target to prevent duplicate hit events
        isTargetActive = false
        targetLifecycleJob?.cancel()

        score += 1
        hits += 1
        currentCombo += 1
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo
        }

        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}

        handleSoundEvent(TapRushSoundEvent.TARGET_HIT)
        if (currentCombo % 5 == 0) {
            handleSoundEvent(TapRushSoundEvent.COMBO_STREAK)
        }

        addIndicator(
            text = if (currentCombo >= 5) "+1 (x$currentCombo!)" else "+1",
            x = targetX,
            y = targetY,
            color = if (currentCombo >= 5) SkillRushCoinGold else DifficultyEasy
        )

        // Spawn next target
        spawnTarget()
    }

    // Handle Miss (tap outside the target)
    fun handleMiss(missX: Float, missY: Float) {
        if (gameState != TapRushGameState.PLAYING) return

        misses += 1
        score = (score - 1).coerceAtLeast(0)
        currentCombo = 0

        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}

        handleSoundEvent(TapRushSoundEvent.MISS)
        addIndicator("-1", missX, missY, DifficultyHard)
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
                            text = "TAP RUSH",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = DifficultyEasyContainer
                        ) {
                            Text(
                                text = "REFLEX",
                                style = MaterialTheme.typography.labelSmall,
                                color = DifficultyEasy,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            gameLoopJob?.cancel()
                            targetLifecycleJob?.cancel()
                            onBack()
                        },
                        modifier = Modifier.testTag("tap_rush_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    if (gameState == TapRushGameState.PLAYING) {
                        IconButton(
                            onClick = { pauseGame() },
                            modifier = Modifier.testTag("tap_rush_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = SkillRushPrimary
                            )
                        }
                    } else if (gameState == TapRushGameState.PAUSED) {
                        IconButton(
                            onClick = { resumeGame() },
                            modifier = Modifier.testTag("tap_rush_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = SkillRushPrimary
                            )
                        }
                    }

                    if (gameState == TapRushGameState.PLAYING || gameState == TapRushGameState.PAUSED) {
                        IconButton(
                            onClick = { restartGame() },
                            modifier = Modifier.testTag("tap_rush_restart_button")
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
            // Top HUD: Timer, Score, Combo, Accuracy
            TapRushHud(
                timeRemainingMillis = timeRemainingMillis,
                totalDurationMillis = totalGameDurationMillis,
                score = score,
                combo = currentCombo,
                hits = hits,
                misses = misses,
                bestScore = bestScore
            )

            // Game Play Arena
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        BorderStroke(
                            2.dp,
                            if (currentCombo >= 10) SkillRushCoinGold else SkillRushPrimary.copy(alpha = 0.25f)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Tapping background counts as miss during active play
                        if (gameState == TapRushGameState.PLAYING) {
                            handleMiss(targetX, targetY)
                        }
                    }
                    .testTag("tap_rush_arena")
            ) {
                val arenaWidth = maxWidth
                val arenaHeight = maxHeight
                val targetSizeDp = 72.dp

                // Calculate target pixel offsets
                val currentToken = targetSpawnToken
                val targetOffsetX = (arenaWidth - targetSizeDp) * targetX
                val targetOffsetY = (arenaHeight - targetSizeDp) * targetY

                // Active Moving Target
                if (gameState == TapRushGameState.PLAYING && isTargetActive) {
                    TapRushTarget(
                        modifier = Modifier
                            .offset(x = targetOffsetX, y = targetOffsetY)
                            .size(targetSizeDp),
                        token = currentToken,
                        combo = currentCombo,
                        onHit = { handleTargetHit(currentToken) }
                    )
                }

                // Floating score / penalty indicators
                floatingIndicators.forEach { indicator ->
                    key(indicator.id) {
                        FloatingFeedbackPopup(
                            indicator = indicator,
                            arenaWidth = arenaWidth,
                            arenaHeight = arenaHeight
                        )
                    }
                }

                // Ready / Start Screen Overlay
                if (gameState == TapRushGameState.READY) {
                    TapRushReadyOverlay(
                        bestScore = bestScore,
                        onStart = { startCountdown() }
                    )
                }

                // 3-2-1 Countdown Overlay
                if (gameState == TapRushGameState.COUNTDOWN) {
                    TapRushCountdownOverlay(number = countdownNumber)
                }

                // Paused Overlay
                if (gameState == TapRushGameState.PAUSED) {
                    TapRushPausedOverlay(
                        onResume = { resumeGame() },
                        onRestart = { restartGame() },
                        onQuit = { onBack() }
                    )
                }

                // Game Over / Results Overlay
                if (gameState == TapRushGameState.GAME_OVER) {
                    TapRushGameOverOverlay(
                        score = score,
                        hits = hits,
                        misses = misses,
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
            TapRushBottomControls(
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
fun TapRushHud(
    timeRemainingMillis: Long,
    totalDurationMillis: Long,
    score: Int,
    combo: Int,
    hits: Int,
    misses: Int,
    bestScore: Int
) {
    val secondsRemaining = (timeRemainingMillis / 1000f)
    val progress = (timeRemainingMillis.toFloat() / totalDurationMillis).coerceIn(0f, 1f)
    val isUrgent = timeRemainingMillis <= 5000L && timeRemainingMillis > 0L

    val timerColor by animateColorAsState(
        targetValue = when {
            isUrgent -> DifficultyHard
            timeRemainingMillis <= 10000L -> DifficultyMedium
            else -> SkillRushPrimary
        },
        label = "timer_color"
    )

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
                        modifier = Modifier.testTag("tap_rush_live_score")
                    )
                }

                // Combo Badge
                AnimatedVisibility(
                    visible = combo > 1,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (combo >= 10) SkillRushCoinGold else SkillRushPrimaryContainer,
                        border = BorderStroke(1.dp, if (combo >= 10) Color.White else SkillRushPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Combo",
                                tint = if (combo >= 10) Color.White else SkillRushStreakFire,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "x$combo COMBO",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (combo >= 10) Color.White else SkillRushOnPrimaryContainer
                            )
                        }
                    }
                }

                // Best Score Box
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BEST SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Best",
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(16.dp)
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

            // Countdown Progress Bar & Timer text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape),
                    color = timerColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = String.format("%.1fs", secondsRemaining),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = timerColor,
                    modifier = Modifier
                        .width(54.dp)
                        .testTag("tap_rush_timer")
                )
            }
        }
    }
}

@Composable
fun TapRushTarget(
    modifier: Modifier = Modifier,
    token: Long,
    combo: Int,
    onHit: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "target_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "target_pulse"
    )

    val targetColor = if (combo >= 10) SkillRushCoinGold else SkillRushPrimary

    Box(
        modifier = modifier
            .scale(pulseScale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        targetColor.copy(alpha = 0.9f),
                        targetColor
                    )
                )
            )
            .border(3.dp, Color.White, CircleShape)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 40.dp)
            ) {
                onHit()
            }
            .testTag("tap_rush_active_target"),
        contentAlignment = Alignment.Center
    ) {
        // Inner bullseye ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Target",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FloatingFeedbackPopup(
    indicator: FloatingIndicator,
    arenaWidth: androidx.compose.ui.unit.Dp,
    arenaHeight: androidx.compose.ui.unit.Dp
) {
    var isVisible by remember { mutableStateOf(true) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else -30f,
        animationSpec = tween(600),
        label = "offset"
    )

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = false
    }

    val posX = arenaWidth * indicator.xOffset
    val posY = arenaHeight * indicator.yOffset

    Box(
        modifier = Modifier
            .offset(x = posX, y = posY + animatedOffset.dp)
    ) {
        Text(
            text = indicator.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = indicator.color.copy(alpha = animatedAlpha),
            fontSize = 18.sp
        )
    }
}

@Composable
fun TapRushReadyOverlay(
    bestScore: Int,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SkillRushPrimaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "TAP RUSH",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = SkillRushPrimary
                )

                Text(
                    text = "Tap appearing reflex targets as rapidly as possible in 30 seconds. Avoid misses to keep your combo streak alive!",
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Record", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SkillRushCoinGold, modifier = Modifier.size(18.dp))
                            Text("$bestScore pts", fontWeight = FontWeight.Black, color = SkillRushPrimary)
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("tap_rush_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START CHALLENGE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun TapRushCountdownOverlay(number: Int) {
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
                text = "GET READY!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SkillRushCoinGold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun TapRushPausedOverlay(
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
                    Text("RESTART ROUND", fontWeight = FontWeight.Bold)
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
fun TapRushGameOverOverlay(
    score: Int,
    hits: Int,
    misses: Int,
    maxCombo: Int,
    saveResult: TapRushScoreManager.SaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val totalTaps = hits + misses
    val accuracy = if (totalTaps > 0) (hits.toFloat() / totalTaps * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 2).coerceAtMost(30) + (maxCombo * 2).coerceAtMost(20) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        score >= 35 && accuracy >= 90f -> "S"
        score >= 25 && accuracy >= 80f -> "A"
        score >= 18 -> "B"
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
                            text = "🏆 NEW RECORD ACHIEVED!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "ROUND COMPLETE",
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
                            modifier = Modifier.testTag("tap_rush_final_score")
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
                        StatRow("Accuracy", String.format(Locale.getDefault(), "%.1f%%", accuracy), Icons.Default.AdsClick, SkillRushPrimary)
                        StatRow("Max Combo", "x$maxCombo", Icons.Default.LocalFireDepartment, SkillRushStreakFire)
                        StatRow("Hits / Misses", "$hits / $misses", Icons.Default.CheckCircle, DifficultyEasy)
                        StatRow("Coins Earned", "+$coinsEarned Coins", Icons.Default.MonetizationOn, SkillRushCoinGold)
                        StatRow("XP Earned", "+$xpEarned XP", Icons.Default.Stars, SkillRushPrimary)
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
                        .testTag("tap_rush_play_again_button")
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
fun StatRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color) {
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
fun TapRushBottomControls(
    gameState: TapRushGameState,
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
            TapRushGameState.READY -> {
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
                    Text("START GAME (30S)", fontWeight = FontWeight.Black)
                }
            }
            TapRushGameState.PLAYING -> {
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
            TapRushGameState.PAUSED -> {
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
            TapRushGameState.COUNTDOWN, TapRushGameState.GAME_OVER -> {
                // Controls handled by overlays
            }
        }
    }
}
