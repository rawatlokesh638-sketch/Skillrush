package com.example.ui.game.speed_rush

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SpeedRushScoreManager
import com.example.ui.theme.DifficultyEasy
import com.example.ui.theme.DifficultyHard
import com.example.ui.theme.DifficultyMedium
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushStreakFire
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

enum class SpeedRushGameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class SpeedRushSymbol(val icon: ImageVector, val label: String) {
    BOLT(Icons.Default.Bolt, "Lightning"),
    STAR(Icons.Default.Star, "Star"),
    DIAMOND(Icons.Default.Diamond, "Diamond"),
    ROCKET(Icons.Default.RocketLaunch, "Rocket"),
    SHIELD(Icons.Default.Shield, "Shield"),
    FIRE(Icons.Default.LocalFireDepartment, "Flame"),
    HEART(Icons.Default.Favorite, "Heart"),
    HEXAGON(Icons.Default.Hexagon, "Hexagon"),
    KEY(Icons.Default.VpnKey, "Key")
}

data class SpeedRushNode(
    val id: Int,
    val symbol: SpeedRushSymbol,
    val color: Color,
    val isTarget: Boolean,
    val pulseScale: Float = 1.0f
)

private val SPEED_PALETTE = listOf(
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFF0055), // Neon Red/Pink
    Color(0xFFFFD600), // Electric Gold
    Color(0xFF00E676), // Lime Green
    Color(0xFFD500F9), // Purple Neon
    Color(0xFFFF6D00), // Blaze Orange
    Color(0xFF2979FF)  // Electric Blue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedRushGameScreen(
    onBack: () -> Unit,
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(SpeedRushGameState.READY) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var timeRemainingMs by remember { mutableLongStateOf(30_000L) }
    val totalTimeMs by remember { mutableLongStateOf(30_000L) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var totalAttempts by remember { mutableIntStateOf(0) }
    var hitsCount by remember { mutableIntStateOf(0) }
    var missesCount by remember { mutableIntStateOf(0) }

    // Reaction speed metrics
    var targetSpawnTimeMs by remember { mutableLongStateOf(0L) }
    var latestReactionMs by remember { mutableIntStateOf(0) }
    val reactionTimesList = remember { mutableStateListOf<Int>() }
    val averageReactionMs by remember {
        androidx.compose.runtime.derivedStateOf {
            if (reactionTimesList.isNotEmpty()) {
                reactionTimesList.average().roundToInt()
            } else {
                0
            }
        }
    }

    // Grid nodes (3x3 grid)
    val nodes = remember { mutableStateListOf<SpeedRushNode>() }
    var currentTargetIndex by remember { mutableIntStateOf(0) }
    var currentTargetSymbol by remember { mutableStateOf(SpeedRushSymbol.BOLT) }
    var currentTargetColor by remember { mutableStateOf(Color(0xFF00E5FF)) }

    // Dynamic spawn velocity based on combo / hits
    var targetLifetimeMs by remember { mutableLongStateOf(1000L) }
    var nodeTimeRemainingMs by remember { mutableLongStateOf(1000L) }
    var penaltyTrigger by remember { mutableIntStateOf(0) }
    var flashBonusTrigger by remember { mutableIntStateOf(0) }

    var saveResult by remember { mutableStateOf<SpeedRushScoreManager.GameSaveResult?>(null) }

    val bestScore = remember(context) { SpeedRushScoreManager.getBestScore(context) }
    val bestAccuracy = remember(context) { SpeedRushScoreManager.getBestAccuracy(context) }
    val bestReactionMs = remember(context) { SpeedRushScoreManager.getBestReactionMs(context) }

    // Function to spawn a new target configuration
    fun spawnNewTarget() {
        val totalNodes = 9
        val targetIdx = Random.nextInt(0, totalNodes)
        currentTargetIndex = targetIdx

        val targetSym = SpeedRushSymbol.values().random()
        val targetCol = SPEED_PALETTE.random()
        currentTargetSymbol = targetSym
        currentTargetColor = targetCol

        nodes.clear()
        for (i in 0 until totalNodes) {
            if (i == targetIdx) {
                nodes.add(
                    SpeedRushNode(
                        id = i,
                        symbol = targetSym,
                        color = targetCol,
                        isTarget = true
                    )
                )
            } else {
                // Decoy with different symbol or color
                val decoySym = SpeedRushSymbol.values().filter { it != targetSym }.random()
                val decoyCol = SPEED_PALETTE.filter { it != targetCol }.random()
                nodes.add(
                    SpeedRushNode(
                        id = i,
                        symbol = decoySym,
                        color = decoyCol,
                        isTarget = false
                    )
                )
            }
        }

        // Calculate dynamic lifetime: Starts at 1050ms, ramps down to 420ms
        val speedTier = (hitsCount / 5).coerceAtMost(10)
        targetLifetimeMs = (1050L - (speedTier * 65L)).coerceAtLeast(420L)
        nodeTimeRemainingMs = targetLifetimeMs
        targetSpawnTimeMs = System.currentTimeMillis()
    }

    fun finishGame() {
        if (gameState == SpeedRushGameState.GAME_OVER) return
        gameState = SpeedRushGameState.GAME_OVER
        val accuracy = if (totalAttempts > 0) {
            (hitsCount.toFloat() / totalAttempts * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val finalAvgReaction = if (reactionTimesList.isNotEmpty()) {
            reactionTimesList.average().roundToInt()
        } else {
            0
        }

        saveResult = SpeedRushScoreManager.recordGameResult(
            context = context,
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            avgReactionMs = finalAvgReaction,
            targetsHit = hitsCount
        )

        com.example.audio.AudioManager.playGameEndSound(context)
        com.example.audio.HapticManager.vibrateGameEnd(context)

        if (tournamentId != null && sessionToken != null) {
            coroutineScope.launch {
                try {
                    val repo = com.example.data.tournament.TournamentRepository(context)
                    val submitRes = repo.submitTournamentScore(
                        tournamentId = tournamentId,
                        tournamentTitle = "Speed Rush Reflex Grand Prix",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("SpeedRush", "Tournament submit error: ${e.message}")
                }
            }
        }
    }

    fun startCountdown() {
        gameState = SpeedRushGameState.COUNTDOWN
        countdownValue = 3
        timeRemainingMs = 30_000L
        score = 0
        combo = 0
        maxCombo = 0
        totalAttempts = 0
        hitsCount = 0
        missesCount = 0
        latestReactionMs = 0
        reactionTimesList.clear()
        saveResult = null
        spawnNewTarget()
    }

    // Main 30-Second Game Clock Loop
    LaunchedEffect(gameState) {
        if (gameState == SpeedRushGameState.PLAYING) {
            while (isActive && gameState == SpeedRushGameState.PLAYING) {
                delay(100L)
                if (timeRemainingMs <= 100L) {
                    timeRemainingMs = 0L
                    finishGame()
                    break
                } else {
                    timeRemainingMs -= 100L
                }
            }
        } else if (gameState == SpeedRushGameState.COUNTDOWN) {
            com.example.audio.AudioManager.playTapSound(context)
            while (countdownValue > 0) {
                delay(800L)
                countdownValue -= 1
                com.example.audio.AudioManager.playTapSound(context)
            }
            delay(200L)
            gameState = SpeedRushGameState.PLAYING
            com.example.audio.AudioManager.playGameStartSound(context)
            targetSpawnTimeMs = System.currentTimeMillis()
        }
    }

    // Sub-loop for target timeout ticker
    LaunchedEffect(gameState, targetSpawnTimeMs) {
        if (gameState == SpeedRushGameState.PLAYING) {
            while (isActive && gameState == SpeedRushGameState.PLAYING && nodeTimeRemainingMs > 0L) {
                delay(50L)
                nodeTimeRemainingMs = (nodeTimeRemainingMs - 50L).coerceAtLeast(0L)
                if (nodeTimeRemainingMs == 0L) {
                    // Target expired before tap: drop combo by 1, spawn new
                    combo = (combo - 1).coerceAtLeast(0)
                    spawnNewTarget()
                }
            }
        }
    }

    fun onNodeClicked(nodeIndex: Int) {
        if (gameState != SpeedRushGameState.PLAYING) return
        totalAttempts++
        val clickedNode = nodes.getOrNull(nodeIndex) ?: return

        val now = System.currentTimeMillis()
        val reactionTime = (now - targetSpawnTimeMs).coerceAtLeast(50L).toInt()

        if (clickedNode.isTarget) {
            // Correct Tap!
            hitsCount++
            latestReactionMs = reactionTime
            reactionTimesList.add(reactionTime)

            val newCombo = combo + 1
            combo = newCombo
            if (newCombo > maxCombo) maxCombo = newCombo

            // Speed tier bonus: Tapping within 300ms gives massive points
            val speedBonus = when {
                reactionTime < 250 -> 250
                reactionTime < 380 -> 180
                reactionTime < 550 -> 120
                else -> 80
            }

            val comboMultiplier = (1.0f + (newCombo * 0.15f)).coerceAtMost(3.5f)
            val points = ((100 + speedBonus) * comboMultiplier).toInt()
            score += points
            flashBonusTrigger++

            com.example.audio.AudioManager.playCorrectSound(context)
            if (newCombo % 5 == 0) {
                com.example.audio.AudioManager.playScoreComboSound(context, newCombo)
            }

            // Immediate rapid next target spawn
            spawnNewTarget()
        } else {
            // Wrong Tap!
            missesCount++
            combo = 0
            penaltyTrigger++
            // Deduct 1 second penalty & 50 points
            timeRemainingMs = (timeRemainingMs - 1000L).coerceAtLeast(0L)
            score = (score - 50).coerceAtLeast(0)
            com.example.audio.AudioManager.playWrongSound(context)
            com.example.audio.HapticManager.vibrateWrong(context)
            if (timeRemainingMs == 0L) {
                finishGame()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B14),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            )
            .testTag("speed_rush_arena")
    ) {
        when (gameState) {
            SpeedRushGameState.READY -> {
                SpeedRushReadyScreen(
                    bestScore = bestScore,
                    bestAccuracy = bestAccuracy,
                    bestReactionMs = bestReactionMs,
                    onBack = onBack,
                    onStart = { startCountdown() }
                )
            }
            SpeedRushGameState.COUNTDOWN -> {
                SpeedRushCountdownOverlay(
                    countdownValue = countdownValue,
                    onBack = onBack
                )
            }
            SpeedRushGameState.PLAYING, SpeedRushGameState.PAUSED -> {
                SpeedRushPlayLayout(
                    timeRemainingMs = timeRemainingMs,
                    totalTimeMs = totalTimeMs,
                    score = score,
                    combo = combo,
                    hits = hitsCount,
                    targetLifetimeMs = targetLifetimeMs,
                    nodeTimeRemainingMs = nodeTimeRemainingMs,
                    currentTargetSymbol = currentTargetSymbol,
                    currentTargetColor = currentTargetColor,
                    latestReactionMs = latestReactionMs,
                    averageReactionMs = averageReactionMs,
                    nodes = nodes,
                    penaltyTrigger = penaltyTrigger,
                    flashBonusTrigger = flashBonusTrigger,
                    onNodeClicked = { onNodeClicked(it) },
                    onPause = { gameState = SpeedRushGameState.PAUSED }
                )

                if (gameState == SpeedRushGameState.PAUSED) {
                    SpeedRushPauseDialog(
                        onResume = { gameState = SpeedRushGameState.PLAYING },
                        onRestart = { startCountdown() },
                        onExit = onBack
                    )
                }
            }
            SpeedRushGameState.GAME_OVER -> {
                SpeedRushGameOverScreen(
                    score = score,
                    hits = hitsCount,
                    totalAttempts = totalAttempts,
                    maxCombo = maxCombo,
                    avgReactionMs = averageReactionMs,
                    saveResult = saveResult,
                    onPlayAgain = { startCountdown() },
                    onExit = onBack
                )
            }
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
}

@Composable
fun SpeedRushReadyScreen(
    bestScore: Int,
    bestAccuracy: Float,
    bestReactionMs: Int,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val formattedScore = remember(bestScore) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(bestScore)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("speed_rush_back_button")
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Surface(
                color = DifficultyHard.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DifficultyHard.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = DifficultyHard,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VELOCITY & REFLEX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DifficultyHard,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Visual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                DifficultyHard.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B),
                    border = BorderStroke(2.dp, DifficultyHard),
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = DifficultyHard,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Speed Rush",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "30-second velocity sprint! Rapidly identify and tap active target nodes as speed accelerates progressively!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .widthIn(max = 380.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rules Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HOW TO PLAY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkillRushCoinGold,
                    letterSpacing = 1.sp
                )

                SpeedRushRuleRow(
                    icon = Icons.Default.AdsClick,
                    title = "Tap Active Target",
                    desc = "Watch the top directive and tap the highlighted active node."
                )
                SpeedRushRuleRow(
                    icon = Icons.Default.Speed,
                    title = "Speed Acceleration",
                    desc = "Target lifetime drops from 1050ms to 420ms as you score hits."
                )
                SpeedRushRuleRow(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Streak Multipliers",
                    desc = "Consecutive fast taps stack combo multiplier bonuses up to x3.5."
                )
                SpeedRushRuleRow(
                    icon = Icons.Default.Warning,
                    title = "Miss Penalty",
                    desc = "Wrong node clicks deduct -1s penalty and reset your streak."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeedRushStatCard(
                title = "BEST SCORE",
                value = "$formattedScore pts",
                icon = Icons.Default.EmojiEvents,
                tint = SkillRushCoinGold,
                modifier = Modifier.weight(1f)
            )
            SpeedRushStatCard(
                title = "FASTEST REACTION",
                value = if (bestReactionMs > 0) "${bestReactionMs}ms" else "--",
                icon = Icons.Default.Speed,
                tint = DifficultyHard,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .widthIn(max = 420.dp)
                .testTag("speed_rush_start_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DifficultyHard,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "START SPEED RUSH",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SpeedRushRuleRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SpeedRushStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SpeedRushCountdownOverlay(
    countdownValue: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "VELOCITY RUSH",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DifficultyHard,
                letterSpacing = 2.sp
            )

            Text(
                text = if (countdownValue > 0) "$countdownValue" else "RUSH!",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Tap active targets instantly as they switch!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun SpeedRushPlayLayout(
    timeRemainingMs: Long,
    totalTimeMs: Long,
    score: Int,
    combo: Int,
    hits: Int,
    targetLifetimeMs: Long,
    nodeTimeRemainingMs: Long,
    currentTargetSymbol: SpeedRushSymbol,
    currentTargetColor: Color,
    latestReactionMs: Int,
    averageReactionMs: Int,
    nodes: List<SpeedRushNode>,
    penaltyTrigger: Int,
    flashBonusTrigger: Int,
    onNodeClicked: (Int) -> Unit,
    onPause: () -> Unit
) {
    val timeSeconds = (timeRemainingMs / 1000f)
    val timeProgress = (timeRemainingMs.toFloat() / totalTimeMs.toFloat()).coerceIn(0f, 1f)
    val nodeProgress = (nodeTimeRemainingMs.toFloat() / targetLifetimeMs.toFloat()).coerceIn(0f, 1f)

    val timerColor by animateColorAsState(
        targetValue = when {
            timeRemainingMs < 6_000L -> DifficultyHard
            timeRemainingMs < 12_000L -> SkillRushStreakFire
            else -> DifficultyEasy
        },
        label = "timerColor"
    )

    val speedTierName = when {
        targetLifetimeMs <= 500L -> "LIGHTSPEED 🔥"
        targetLifetimeMs <= 700L -> "HYPER ⚡"
        targetLifetimeMs <= 900L -> "TURBO 🚀"
        else -> "WARMUP 🎯"
    }

    // Screen Shake on Penalty
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(penaltyTrigger) {
        if (penaltyTrigger > 0) {
            shakeOffset.animateTo(12f, tween(durationMillis = 50, easing = LinearEasing))
            shakeOffset.animateTo(-12f, tween(durationMillis = 50, easing = LinearEasing))
            shakeOffset.animateTo(0f, tween(durationMillis = 50, easing = LinearEasing))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed Tier Badge
                Surface(
                    color = DifficultyHard.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DifficultyHard.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = speedTierName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = DifficultyHard,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        letterSpacing = 0.5.sp
                    )
                }

                // Reaction speed live indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = SkillRushCoinGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (latestReactionMs > 0) "${latestReactionMs}ms (${hits} hits)" else "Ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Pause Button
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("speed_rush_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Score & Streak & Timer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Score
                Column {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.getDefault()).format(score),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Combo Badge
                if (combo > 1) {
                    Surface(
                        color = SkillRushStreakFire.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SkillRushStreakFire.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = SkillRushStreakFire,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "x$combo STREAK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = SkillRushStreakFire
                            )
                        }
                    }
                }

                // Timer Display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TIME LEFT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fs", timeSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = timerColor,
                        modifier = Modifier.testTag("speed_rush_timer")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Total 30s Game Timer Progress Bar
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Current Directive Card (Shows the target to hit)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, currentTargetColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TARGET DIRECTIVE:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Avg Speed: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = if (averageReactionMs > 0) "${averageReactionMs}ms" else "--",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DifficultyEasy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentTargetColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, currentTargetColor),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentTargetSymbol.icon,
                                    contentDescription = null,
                                    tint = currentTargetColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "TAP ${currentTargetSymbol.label.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = currentTargetColor,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Target Lifetime Expiry Bar (Depletes as target stays active)
                    LinearProgressIndicator(
                        progress = { nodeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = currentTargetColor,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3x3 Speed Rush Interactive Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(nodes) { index, node ->
                        SpeedRushNodeItem(
                            node = node,
                            onClick = { onNodeClicked(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap the matching active target as quickly as possible!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpeedRushNodeItem(
    node: SpeedRushNode,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Glow and pulsing for active target
    val infiniteTransition = rememberInfiniteTransition(label = "nodePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (node.isTarget) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val targetBorder = if (node.isTarget) {
        BorderStroke(2.dp, node.color)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    val targetBg = if (node.isTarget) {
        node.color.copy(alpha = 0.22f)
    } else {
        Color(0xFF1E293B).copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(if (node.isTarget) pulseScale else 1.0f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("speed_rush_node_${node.id}"),
        shape = RoundedCornerShape(16.dp),
        color = targetBg,
        border = targetBorder,
        shadowElevation = if (node.isTarget) 12.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = node.symbol.icon,
                contentDescription = null,
                tint = if (node.isTarget) node.color else node.color.copy(alpha = 0.45f),
                modifier = Modifier.size(38.dp)
            )

            // Overcharged corner indicator if target
            if (node.isTarget) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .background(node.color, CircleShape)
                )
            }
        }
    }
}

@Composable
fun SpeedRushPauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = SkillRushCoinGold,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "GAME PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Catch your breath! Velocity will resume immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DifficultyHard)
                ) {
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RESTART", color = Color.White)
                }

                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text("EXIT TO ARENA", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun SpeedRushGameOverScreen(
    score: Int,
    hits: Int,
    totalAttempts: Int,
    maxCombo: Int,
    avgReactionMs: Int,
    saveResult: SpeedRushScoreManager.GameSaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalAttempts > 0) (hits.toFloat() / totalAttempts * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 25).coerceAtMost(30) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        accuracy >= 92f && avgReactionMs in 1..320 -> "S"
        accuracy >= 80f && avgReactionMs in 1..420 -> "A"
        accuracy >= 65f -> "B"
        else -> "C"
    }

    val gradeColor = when (grade) {
        "S" -> SkillRushCoinGold
        "A" -> DifficultyEasy
        "B" -> DifficultyMedium
        else -> DifficultyHard
    }

    val speedRank = when {
        avgReactionMs in 1..260 -> "Lightning Reflexes ⚡"
        avgReactionMs in 261..360 -> "Supersonic 🚀"
        avgReactionMs in 361..480 -> "Quickdraw 🎯"
        else -> "Steady Pace ⏱️"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Grade Badge
            Surface(
                shape = CircleShape,
                color = gradeColor.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, gradeColor),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = grade,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = gradeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SPEED RUSH COMPLETED",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            if (isNewBest) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = SkillRushCoinGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SkillRushCoinGold)
                ) {
                    Text(
                        text = "★ NEW BEST SCORE! ★",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = SkillRushCoinGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Display
            Text(
                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(score),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Rank: $speedRank",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = DifficultyEasy
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpeedStatRow(
                        label = "Reaction Speed",
                        value = if (avgReactionMs > 0) "${avgReactionMs}ms avg" else "--",
                        icon = Icons.Default.Speed,
                        color = DifficultyHard
                    )
                    SpeedStatRow(
                        label = "Accuracy",
                        value = String.format(Locale.getDefault(), "%.1f%% (%d/%d)", accuracy, hits, totalAttempts),
                        icon = Icons.Default.AdsClick,
                        color = DifficultyEasy
                    )
                    SpeedStatRow(
                        label = "Max Streak",
                        value = "x$maxCombo combo",
                        icon = Icons.Default.LocalFireDepartment,
                        color = SkillRushStreakFire
                    )
                    SpeedStatRow(
                        label = "Coins Earned",
                        value = "+$coinsEarned Coins",
                        icon = Icons.Default.MonetizationOn,
                        color = SkillRushCoinGold
                    )
                    SpeedStatRow(
                        label = "XP Earned",
                        value = "+$xpEarned XP",
                        icon = Icons.Default.AutoAwesome,
                        color = SkillRushPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DifficultyHard)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text("RETURN TO ARENA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SpeedStatRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
