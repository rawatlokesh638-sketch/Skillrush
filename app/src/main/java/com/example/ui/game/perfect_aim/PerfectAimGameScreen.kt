package com.example.ui.game.perfect_aim

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PerfectAimScoreManager
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.random.Random
import java.util.Locale

enum class PerfectAimGameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class PerfectAimHitType {
    BULLSEYE,
    GREAT,
    EDGE,
    MISS
}

data class FloatingHitFeedback(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val isBullseye: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfectAimGameScreen(
    onBack: () -> Unit,
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(PerfectAimGameState.READY) }
    var countdownNumber by remember { mutableIntStateOf(3) }
    var timeLeftMillis by remember { mutableLongStateOf(30000L) }
    val totalTimeMillis = 30000L

    var score by remember { mutableIntStateOf(0) }
    var currentCombo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var totalShots by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var bullseyes by remember { mutableIntStateOf(0) }

    // Arena dimensions
    var arenaSize by remember { mutableStateOf(IntSize.Zero) }

    // Target Physics: center (x, y), velocity (vx, vy), radius
    var targetX by remember { mutableFloatStateOf(200f) }
    var targetY by remember { mutableFloatStateOf(300f) }
    var targetVx by remember { mutableFloatStateOf(3.5f) }
    var targetVy by remember { mutableFloatStateOf(2.5f) }
    var targetBaseRadius by remember { mutableFloatStateOf(60f) } // pixels, dynamically scaled
    var targetLifetimeMillis by remember { mutableLongStateOf(0L) }

    // Hit feedback list
    var floatingFeedbacks by remember { mutableStateOf(listOf<FloatingHitFeedback>()) }

    // Best record tracking
    var bestScore by remember { mutableIntStateOf(PerfectAimScoreManager.getBestScore(context)) }
    var bestAccuracy by remember { mutableFloatStateOf(PerfectAimScoreManager.getBestAccuracy(context)) }
    var saveResult by remember { mutableStateOf<PerfectAimScoreManager.SaveResult?>(null) }

    // Jobs
    var gameLoopJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHaptic(type: HapticFeedbackType) {
        try {
            hapticFeedback.performHapticFeedback(type)
        } catch (_: Exception) {}
    }

    // Reposition target with new velocity
    fun relocateTarget() {
        if (arenaSize.width <= 0 || arenaSize.height <= 0) return
        val currentProgress = (timeLeftMillis.toFloat() / totalTimeMillis).coerceIn(0f, 1f)
        // Target shrinks as time elapses: 80f down to 38f
        val radius = 38f + (42f * currentProgress)
        targetBaseRadius = radius

        val margin = radius + 20f
        val minX = margin
        val maxX = (arenaSize.width - margin).coerceAtLeast(minX + 10f)
        val minY = margin
        val maxY = (arenaSize.height - margin).coerceAtLeast(minY + 10f)

        targetX = Random.nextFloat() * (maxX - minX) + minX
        targetY = Random.nextFloat() * (maxY - minY) + minY

        // Target speed increases as time elapses (from speed 3.0 up to 8.5)
        val speedMultiplier = 1.0f + ((1.0f - currentProgress) * 1.8f)
        val baseSpeed = 3.2f * speedMultiplier
        val angle = Random.nextFloat() * (2 * Math.PI).toFloat()
        targetVx = kotlin.math.cos(angle) * baseSpeed
        targetVy = kotlin.math.sin(angle) * baseSpeed
        targetLifetimeMillis = 0L
    }

    // End Game
    fun endGame() {
        gameLoopJob?.cancel()
        gameState = PerfectAimGameState.GAME_OVER

        val accuracy = if (totalShots > 0) (hits.toFloat() / totalShots * 100f) else 0f
        val result = PerfectAimScoreManager.recordGameResult(
            context = context,
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            bullseyes = bullseyes
        )
        saveResult = result
        bestScore = result.newBestScore
        bestAccuracy = result.bestAccuracy

        com.example.audio.AudioManager.playGameEndSound(context)
        com.example.audio.HapticManager.vibrateGameEnd(context)

        if (tournamentId != null && sessionToken != null) {
            coroutineScope.launch {
                try {
                    val repo = com.example.data.tournament.TournamentRepository(context)
                    val submitRes = repo.submitTournamentScore(
                        tournamentId = tournamentId,
                        tournamentTitle = "Perfect Aim Masters Arena",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("PerfectAim", "Tournament submit error: ${e.message}")
                }
            }
        }
    }

    // Main Game Loop (Physics + Timer)
    fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = coroutineScope.launch {
            val frameTime = 20L // ~50 fps loop
            while (isActive && timeLeftMillis > 0) {
                delay(frameTime)
                timeLeftMillis = (timeLeftMillis - frameTime).coerceAtLeast(0L)
                targetLifetimeMillis += frameTime

                // Update target physics if arena size is known
                if (arenaSize.width > 0 && arenaSize.height > 0) {
                    val radius = targetBaseRadius
                    var newX = targetX + targetVx
                    var newY = targetY + targetVy
                    var newVx = targetVx
                    var newVy = targetVy

                    val minX = radius
                    val maxX = arenaSize.width - radius
                    val minY = radius
                    val maxY = arenaSize.height - radius

                    // Bounce off walls
                    if (newX <= minX) {
                        newX = minX
                        newVx = -newVx
                    } else if (newX >= maxX) {
                        newX = maxX
                        newVx = -newVx
                    }

                    if (newY <= minY) {
                        newY = minY
                        newVy = -newVy
                    } else if (newY >= maxY) {
                        newY = maxY
                        newVy = -newVy
                    }

                    targetX = newX
                    targetY = newY
                    targetVx = newVx
                    targetVy = newVy

                    // Auto-relocate if target drifts for too long (e.g., 2.5s) without hit
                    val maxLifetime = (1500L + (timeLeftMillis / 30000f * 1500L)).toLong()
                    if (targetLifetimeMillis > maxLifetime) {
                        // Unhit target counts as combo break
                        if (currentCombo > 0) {
                            currentCombo = 0
                        }
                        relocateTarget()
                    }
                }

                // Cleanup old floating feedbacks
                val now = System.currentTimeMillis()
                floatingFeedbacks = floatingFeedbacks.filter { now - it.id < 900 }
            }

            if (isActive && timeLeftMillis <= 0) {
                endGame()
            }
        }
    }

    // Start 3-2-1 Countdown
    fun startCountdown() {
        gameLoopJob?.cancel()
        gameState = PerfectAimGameState.COUNTDOWN
        countdownNumber = 3
        timeLeftMillis = totalTimeMillis
        score = 0
        currentCombo = 0
        maxCombo = 0
        totalShots = 0
        hits = 0
        bullseyes = 0
        floatingFeedbacks = emptyList()
        saveResult = null

        coroutineScope.launch {
            com.example.audio.AudioManager.playTapSound(context)
            delay(800)
            countdownNumber = 2
            com.example.audio.AudioManager.playTapSound(context)
            delay(800)
            countdownNumber = 1
            com.example.audio.AudioManager.playTapSound(context)
            delay(800)
            gameState = PerfectAimGameState.PLAYING
            com.example.audio.AudioManager.playGameStartSound(context)
            relocateTarget()
            startGameLoop()
        }
    }

    // Pause Game
    fun pauseGame() {
        if (gameState == PerfectAimGameState.PLAYING) {
            gameLoopJob?.cancel()
            gameState = PerfectAimGameState.PAUSED
        }
    }

    // Resume Game
    fun resumeGame() {
        if (gameState == PerfectAimGameState.PAUSED) {
            gameState = PerfectAimGameState.PLAYING
            startGameLoop()
        }
    }

    // Restart Game
    fun restartGame() {
        gameLoopJob?.cancel()
        startCountdown()
    }

    // Handle Tap in Arena
    fun handleArenaTap(tapOffset: Offset) {
        if (gameState != PerfectAimGameState.PLAYING) return

        totalShots++
        val distance = hypot(tapOffset.x - targetX, tapOffset.y - targetY)
        val radius = targetBaseRadius

        val now = System.currentTimeMillis()

        if (distance <= radius) {
            // Hit!
            hits++
            currentCombo++
            if (currentCombo > maxCombo) maxCombo = currentCombo

            val relativeDist = distance / radius
            val hitType: PerfectAimHitType
            val basePoints: Int
            val hitText: String
            val hitColor: Color

            when {
                relativeDist <= 0.35f -> {
                    // Bullseye (Center Hit)
                    hitType = PerfectAimHitType.BULLSEYE
                    bullseyes++
                    basePoints = 100
                    hitText = "🎯 BULLSEYE! +100"
                    hitColor = SkillRushCoinGold
                    triggerHaptic(HapticFeedbackType.TextHandleMove)
                }
                relativeDist <= 0.70f -> {
                    // Great Hit (Middle Ring)
                    hitType = PerfectAimHitType.GREAT
                    basePoints = 50
                    hitText = "⚡ GREAT! +50"
                    hitColor = SkillRushPrimary
                    triggerHaptic(HapticFeedbackType.TextHandleMove)
                }
                else -> {
                    // Edge Hit (Outer Ring)
                    hitType = PerfectAimHitType.EDGE
                    basePoints = 25
                    hitText = "EDGE HIT +25"
                    hitColor = DifficultyMedium
                    triggerHaptic(HapticFeedbackType.TextHandleMove)
                }
            }

            val comboMultiplier = (1.0f + (currentCombo - 1) * 0.1f).coerceAtMost(3.0f)
            val finalPoints = (basePoints * comboMultiplier).toInt()
            score += finalPoints

            com.example.audio.AudioManager.playCorrectSound(context)
            if (currentCombo % 5 == 0) {
                com.example.audio.AudioManager.playScoreComboSound(context, currentCombo)
            }

            // Add floating feedback
            floatingFeedbacks = floatingFeedbacks + FloatingHitFeedback(
                id = now,
                text = if (currentCombo > 1) "$hitText (x$currentCombo)" else hitText,
                x = tapOffset.x,
                y = tapOffset.y,
                color = hitColor,
                isBullseye = hitType == PerfectAimHitType.BULLSEYE
            )

            // Relocate immediately upon successful hit
            relocateTarget()
        } else {
            // Miss: Tap outside target
            currentCombo = 0
            score = (score - 10).coerceAtLeast(0)
            com.example.audio.AudioManager.playWrongSound(context)
            com.example.audio.HapticManager.vibrateWrong(context)

            floatingFeedbacks = floatingFeedbacks + FloatingHitFeedback(
                id = now,
                text = "MISS! -10",
                x = tapOffset.x,
                y = tapOffset.y,
                color = DifficultyHard,
                isBullseye = false
            )
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
                            text = "PERFECT AIM",
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
                                text = "ACCURACY",
                                style = MaterialTheme.typography.labelSmall,
                                color = DifficultyMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            gameLoopJob?.cancel()
                            onBack()
                        },
                        modifier = Modifier.testTag("perfect_aim_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    if (gameState == PerfectAimGameState.PLAYING) {
                        IconButton(
                            onClick = { pauseGame() },
                            modifier = Modifier.testTag("perfect_aim_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = SkillRushPrimary
                            )
                        }
                    } else if (gameState == PerfectAimGameState.PAUSED) {
                        IconButton(
                            onClick = { resumeGame() },
                            modifier = Modifier.testTag("perfect_aim_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = SkillRushPrimary
                            )
                        }
                    }

                    if (gameState != PerfectAimGameState.READY && gameState != PerfectAimGameState.COUNTDOWN) {
                        IconButton(
                            onClick = { restartGame() },
                            modifier = Modifier.testTag("perfect_aim_restart_button")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live HUD: Score, Timer, Accuracy, Combo
            PerfectAimHud(
                score = score,
                timeLeftMillis = timeLeftMillis,
                totalTimeMillis = totalTimeMillis,
                hits = hits,
                totalShots = totalShots,
                combo = currentCombo,
                bestScore = bestScore
            )

            // Interactive Aim Arena
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        BorderStroke(
                            2.dp,
                            if (currentCombo >= 5) SkillRushCoinGold else SkillRushPrimary.copy(alpha = 0.25f)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .onSizeChanged { size ->
                        arenaSize = size
                    }
                    .pointerInput(gameState) {
                        detectTapGestures { offset ->
                            handleArenaTap(offset)
                        }
                    }
                    .testTag("perfect_aim_arena"),
                contentAlignment = Alignment.Center
            ) {
                // Background Crosshair & Grid Guidelines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeColor = Color(0xFF6750A4).copy(alpha = 0.08f)
                    // Grid lines
                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2f
                    )
                    // Concentric background rings
                    drawCircle(
                        color = strokeColor,
                        radius = size.minDimension / 4,
                        center = Offset(size.width / 2, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }

                // Active Dynamic Target
                if (gameState == PerfectAimGameState.PLAYING || gameState == PerfectAimGameState.PAUSED) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(targetX, targetY)
                        val radius = targetBaseRadius

                        // Outer Glow Ring
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF6750A4).copy(alpha = 0.4f), Color.Transparent),
                                center = center,
                                radius = radius * 1.3f
                            ),
                            radius = radius * 1.3f,
                            center = center
                        )

                        // Outer Edge Ring (Red / Purple)
                        drawCircle(
                            color = Color(0xFF6750A4),
                            radius = radius,
                            center = center
                        )

                        // Middle Ring (White / Light)
                        drawCircle(
                            color = Color(0xFFF3EDF7),
                            radius = radius * 0.70f,
                            center = center
                        )

                        // Inner Bullseye Ring (Gold / Red)
                        drawCircle(
                            color = Color(0xFFE8590C),
                            radius = radius * 0.35f,
                            center = center
                        )

                        // Bullseye Pin Point
                        drawCircle(
                            color = Color(0xFFFFD700),
                            radius = radius * 0.15f,
                            center = center
                        )

                        // Crosshair tick marks on target
                        val tickLen = radius * 0.3f
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(center.x - radius - tickLen, center.y),
                            end = Offset(center.x - radius + tickLen, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(center.x + radius - tickLen, center.y),
                            end = Offset(center.x + radius + tickLen, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(center.x, center.y - radius - tickLen),
                            end = Offset(center.x, center.y - radius + tickLen),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(center.x, center.y + radius - tickLen),
                            end = Offset(center.x, center.y + radius + tickLen),
                            strokeWidth = 2f
                        )
                    }
                }

                // Floating Text Feedbacks (Scores / Misses)
                floatingFeedbacks.forEach { feedback ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = (feedback.x / 3f).coerceIn(0f, 250f).dp,
                                top = (feedback.y / 3f).coerceIn(0f, 400f).dp
                            )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = feedback.color.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = feedback.text,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Ready Overlay
                if (gameState == PerfectAimGameState.READY) {
                    PerfectAimReadyOverlay(
                        bestScore = bestScore,
                        bestAccuracy = bestAccuracy,
                        onStart = { startCountdown() }
                    )
                }

                // Countdown Overlay
                if (gameState == PerfectAimGameState.COUNTDOWN) {
                    PerfectAimCountdownOverlay(number = countdownNumber)
                }

                // Paused Overlay
                if (gameState == PerfectAimGameState.PAUSED) {
                    PerfectAimPausedOverlay(
                        onResume = { resumeGame() },
                        onRestart = { restartGame() },
                        onQuit = { onBack() }
                    )
                }

                // Game Over Overlay
                if (gameState == PerfectAimGameState.GAME_OVER) {
                    PerfectAimGameOverOverlay(
                        score = score,
                        hits = hits,
                        totalShots = totalShots,
                        bullseyes = bullseyes,
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

            // Bottom Quick Action Bar
            PerfectAimBottomControls(
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
fun PerfectAimHud(
    score: Int,
    timeLeftMillis: Long,
    totalTimeMillis: Long,
    hits: Int,
    totalShots: Int,
    combo: Int,
    bestScore: Int
) {
    val secondsRemaining = (timeLeftMillis / 1000f)
    val timeProgress = (timeLeftMillis.toFloat() / totalTimeMillis).coerceIn(0f, 1f)
    val accuracy = if (totalShots > 0) (hits.toFloat() / totalShots * 100f) else 0f

    val timerColor = when {
        timeLeftMillis < 6000L -> DifficultyHard
        timeLeftMillis < 12000L -> SkillRushCoinGold
        else -> SkillRushPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        modifier = Modifier.testTag("perfect_aim_live_score")
                    )
                }

                // 30s Countdown Clock
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIME LEFT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1fs", secondsRemaining),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = timerColor,
                        modifier = Modifier.testTag("perfect_aim_timer")
                    )
                }

                // Combo & Accuracy
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ACCURACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.0f%%", accuracy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (accuracy >= 85f) DifficultyEasy else MaterialTheme.colorScheme.onSurface
                    )
                    if (combo > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (combo >= 5) SkillRushCoinGold else SkillRushPrimaryContainer
                        ) {
                            Text(
                                text = "STREAK x$combo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (combo >= 5) Color.White else SkillRushOnPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Linear Progress for Timer
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun PerfectAimReadyOverlay(
    bestScore: Int,
    bestAccuracy: Float,
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
                            imageVector = Icons.Default.AdsClick,
                            contentDescription = null,
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = "PERFECT AIM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = SkillRushPrimary
                )

                Text(
                    text = "30-second target frenzy! Tap moving targets: Bullseye (+100 pts), Inner Ring (+50 pts), Edge (+25 pts). Targets shrink and speed up over time!",
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
                            Text("Best Accuracy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f%%", bestAccuracy), fontWeight = FontWeight.Black, color = SkillRushCoinGold)
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
                        .testTag("perfect_aim_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START 30s AIM SESSION", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun PerfectAimCountdownOverlay(number: Int) {
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
                text = "LOCK ON TARGETS!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SkillRushCoinGold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun PerfectAimPausedOverlay(
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
                    text = "AIM PAUSED",
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
fun PerfectAimGameOverOverlay(
    score: Int,
    hits: Int,
    totalShots: Int,
    bullseyes: Int,
    maxCombo: Int,
    saveResult: PerfectAimScoreManager.SaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalShots > 0) (hits.toFloat() / totalShots * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 50).coerceAtMost(30) + (maxCombo * 2).coerceAtMost(15) + (bullseyes * 3).coerceAtMost(20) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        accuracy >= 92f && score >= 1200 -> "S"
        accuracy >= 80f && score >= 800 -> "A"
        accuracy >= 65f && score >= 400 -> "B"
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
                            text = "🏆 NEW AIM RECORD!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "TIME'S UP!",
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
                            modifier = Modifier.testTag("perfect_aim_final_score")
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
                        StatItem("Accuracy", String.format(Locale.getDefault(), "%.1f%% (%d/%d)", accuracy, hits, totalShots), Icons.Default.AdsClick, DifficultyEasy)
                        StatItem("Bullseyes", "$bullseyes center hits", Icons.Default.Adjust, SkillRushCoinGold)
                        StatItem("Max Combo", "x$maxCombo streak", Icons.Default.LocalFireDepartment, SkillRushStreakFire)
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
                        .testTag("perfect_aim_play_again_button")
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
fun PerfectAimBottomControls(
    gameState: PerfectAimGameState,
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
            PerfectAimGameState.READY -> {
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
                    Text("START 30s AIM CHALLENGE", fontWeight = FontWeight.Black)
                }
            }
            PerfectAimGameState.PLAYING -> {
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
            PerfectAimGameState.PAUSED -> {
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
            else -> {}
        }
    }
}
