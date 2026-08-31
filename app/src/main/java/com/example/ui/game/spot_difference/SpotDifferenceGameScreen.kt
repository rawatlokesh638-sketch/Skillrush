package com.example.ui.game.spot_difference

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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.SpotDifferenceScoreManager
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

enum class SpotDiffGameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class DifferenceType {
    COLOR_SHIFT,
    ROTATION,
    SHAPE_CHANGE,
    BADGE_TOGGLE,
    MISSING_ELEMENT,
    NUMBER_CHANGE
}

enum class SpotDiffSymbol(val icon: ImageVector) {
    ROCKET(Icons.Default.RocketLaunch),
    STAR(Icons.Default.Star),
    DIAMOND(Icons.Default.Diamond),
    SHIELD(Icons.Default.Shield),
    HEART(Icons.Default.Favorite),
    BOLT(Icons.Default.Bolt),
    HEXAGON(Icons.Default.Hexagon),
    KEY(Icons.Default.VpnKey),
    PSYCHOLOGY(Icons.Default.Psychology),
    GRADE(Icons.Default.Grade),
    SECURITY(Icons.Default.Security),
    TROPHY(Icons.Default.EmojiEvents)
}

data class SpotDiffCell(
    val index: Int,
    val symbol: SpotDiffSymbol,
    val color: Color,
    val rotation: Float,
    val hasBadge: Boolean,
    val badgeNumber: Int,
    val isDifference: Boolean,
    val differenceType: DifferenceType?,
    // Modified attributes for Panel B
    val modSymbol: SpotDiffSymbol,
    val modColor: Color,
    val modRotation: Float,
    val modHasBadge: Boolean,
    val modBadgeNumber: Int,
    val modIsVisible: Boolean = true,
    var isFound: Boolean = false
)

private val SYMBOL_COLORS = listOf(
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFF4081), // Pink / Magenta
    Color(0xFFFFD700), // Gold
    Color(0xFF00E676), // Lime Green
    Color(0xFF7C4DFF), // Purple
    Color(0xFFFF9100), // Amber / Orange
    Color(0xFF40C4FF), // Sky Blue
    Color(0xFFFF5252)  // Coral Red
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDifferenceGameScreen(
    onBack: () -> Unit,
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(SpotDiffGameState.READY) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var currentRound by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctSpotsCount by remember { mutableIntStateOf(0) }
    var totalAttempts by remember { mutableIntStateOf(0) }
    var wrongClicksCount by remember { mutableIntStateOf(0) }
    var timeRemainingMs by remember { mutableLongStateOf(45_000L) }
    val totalRoundTimeMs by remember { mutableLongStateOf(45_000L) }

    val cells = remember { mutableStateListOf<SpotDiffCell>() }
    var targetDifferencesCount by remember { mutableIntStateOf(3) }
    var roundDifferencesFound by remember { mutableIntStateOf(0) }
    var showRoundClearBanner by remember { mutableStateOf(false) }

    var penaltyAlertTrigger by remember { mutableIntStateOf(0) }
    var saveResult by remember { mutableStateOf<SpotDifferenceScoreManager.GameSaveResult?>(null) }

    val bestScore = remember(context) { SpotDifferenceScoreManager.getBestScore(context) }
    val bestAccuracy = remember(context) { SpotDifferenceScoreManager.getBestAccuracy(context) }
    val totalFoundEver = remember(context) { SpotDifferenceScoreManager.getTotalDifferencesFound(context) }

    // Helper to generate a new level/round
    fun generateRound(round: Int) {
        cells.clear()
        roundDifferencesFound = 0
        val diffCount = when (round) {
            1 -> 3
            2 -> 4
            3 -> 5
            4 -> 6
            else -> 7
        }.coerceAtMost(8)
        targetDifferencesCount = diffCount

        val totalCells = 12 // 3x4 grid for optimal mobile visibility
        val allIndices = (0 until totalCells).shuffled(Random(System.currentTimeMillis() + round * 100))
        val diffIndices = allIndices.take(diffCount).toSet()

        val symbolsList = SpotDiffSymbol.values().toList()

        for (i in 0 until totalCells) {
            val isDiff = diffIndices.contains(i)
            val sym = symbolsList.random()
            val color = SYMBOL_COLORS.random()
            val rot = listOf(0f, 90f, 180f, 270f).random()
            val hasBadge = Random.nextBoolean()
            val badgeNum = Random.nextInt(1, 9)

            if (!isDiff) {
                cells.add(
                    SpotDiffCell(
                        index = i,
                        symbol = sym,
                        color = color,
                        rotation = rot,
                        hasBadge = hasBadge,
                        badgeNumber = badgeNum,
                        isDifference = false,
                        differenceType = null,
                        modSymbol = sym,
                        modColor = color,
                        modRotation = rot,
                        modHasBadge = hasBadge,
                        modBadgeNumber = badgeNum,
                        modIsVisible = true,
                        isFound = false
                    )
                )
            } else {
                val diffType = DifferenceType.values().random()
                var modSym = sym
                var modColor = color
                var modRot = rot
                var modHasBadge = hasBadge
                var modBadgeNum = badgeNum
                var modVisible = true

                when (diffType) {
                    DifferenceType.COLOR_SHIFT -> {
                        val otherColors = SYMBOL_COLORS.filter { it != color }
                        modColor = otherColors.random()
                    }
                    DifferenceType.ROTATION -> {
                        modRot = (rot + 90f) % 360f
                    }
                    DifferenceType.SHAPE_CHANGE -> {
                        val otherSymbols = symbolsList.filter { it != sym }
                        modSym = otherSymbols.random()
                    }
                    DifferenceType.BADGE_TOGGLE -> {
                        modHasBadge = !hasBadge
                    }
                    DifferenceType.NUMBER_CHANGE -> {
                        modHasBadge = true
                        modBadgeNum = if (badgeNum == 9) 1 else badgeNum + 1
                    }
                    DifferenceType.MISSING_ELEMENT -> {
                        modVisible = false
                    }
                }

                cells.add(
                    SpotDiffCell(
                        index = i,
                        symbol = sym,
                        color = color,
                        rotation = rot,
                        hasBadge = hasBadge,
                        badgeNumber = badgeNum,
                        isDifference = true,
                        differenceType = diffType,
                        modSymbol = modSym,
                        modColor = modColor,
                        modRotation = modRot,
                        modHasBadge = modHasBadge,
                        modBadgeNumber = modBadgeNum,
                        modIsVisible = modVisible,
                        isFound = false
                    )
                )
            }
        }
    }

    fun finishGame() {
        if (gameState == SpotDiffGameState.GAME_OVER) return
        gameState = SpotDiffGameState.GAME_OVER
        val accuracy = if (totalAttempts > 0) {
            (correctSpotsCount.toFloat() / totalAttempts * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }
        saveResult = SpotDifferenceScoreManager.recordGameResult(
            context = context,
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            differencesFound = correctSpotsCount,
            roundReached = currentRound
        )

        com.example.audio.AudioManager.playGameEndSound(context)
        com.example.audio.HapticManager.vibrateGameEnd(context)

        if (tournamentId != null && sessionToken != null) {
            coroutineScope.launch {
                try {
                    val repo = com.example.data.tournament.TournamentRepository(context)
                    val submitRes = repo.submitTournamentScore(
                        tournamentId = tournamentId,
                        tournamentTitle = "Spot Difference Challenge",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("SpotDiff", "Tournament submit error: ${e.message}")
                }
            }
        }
    }

    fun startCountdown() {
        gameState = SpotDiffGameState.COUNTDOWN
        countdownValue = 3
        currentRound = 1
        score = 0
        combo = 0
        maxCombo = 0
        correctSpotsCount = 0
        totalAttempts = 0
        wrongClicksCount = 0
        timeRemainingMs = 45_000L
        saveResult = null
        showRoundClearBanner = false
        generateRound(1)
    }

    // Timer Loop
    LaunchedEffect(gameState) {
        if (gameState == SpotDiffGameState.PLAYING) {
            while (isActive && gameState == SpotDiffGameState.PLAYING) {
                delay(100L)
                if (timeRemainingMs <= 100L) {
                    timeRemainingMs = 0L
                    finishGame()
                    break
                } else {
                    timeRemainingMs -= 100L
                }
            }
        } else if (gameState == SpotDiffGameState.COUNTDOWN) {
            com.example.audio.AudioManager.playTapSound(context)
            while (countdownValue > 0) {
                delay(800L)
                countdownValue -= 1
                com.example.audio.AudioManager.playTapSound(context)
            }
            delay(200L)
            gameState = SpotDiffGameState.PLAYING
            com.example.audio.AudioManager.playGameStartSound(context)
        }
    }

    // Handle Cell Tap (Works whether tapped on Left/Top panel or Right/Bottom panel)
    fun onCellClicked(cellIndex: Int) {
        if (gameState != SpotDiffGameState.PLAYING) return
        totalAttempts++
        val cell = cells.getOrNull(cellIndex) ?: return

        if (cell.isDifference && !cell.isFound) {
            // Correct Spot!
            val updated = cell.copy(isFound = true)
            cells[cellIndex] = updated
            correctSpotsCount++
            roundDifferencesFound++

            val newCombo = combo + 1
            combo = newCombo
            if (newCombo > maxCombo) maxCombo = newCombo

            val comboMultiplier = (1.0f + (newCombo * 0.2f)).coerceAtMost(3.0f)
            val timeBonus = ((timeRemainingMs / 1000L).coerceAtMost(45L) * 2).toInt()
            val pointsEarned = (150 * comboMultiplier).toInt() + timeBonus
            score += pointsEarned

            com.example.audio.AudioManager.playCorrectSound(context)

            // Check if round is completed!
            if (roundDifferencesFound >= targetDifferencesCount) {
                com.example.audio.AudioManager.playScoreComboSound(context, currentRound)
                coroutineScope.launch {
                    showRoundClearBanner = true
                    score += 500 // Round clear bonus
                    timeRemainingMs = (timeRemainingMs + 20_000L).coerceAtMost(60_000L) // Add 20s
                    delay(1200L)
                    showRoundClearBanner = false
                    currentRound++
                    generateRound(currentRound)
                }
            }
        } else if (!cell.isDifference || cell.isFound) {
            // Wrong click or already found -> Penalty
            wrongClicksCount++
            combo = 0
            penaltyAlertTrigger++
            // Deduct 3 seconds
            timeRemainingMs = (timeRemainingMs - 3000L).coerceAtLeast(0L)
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
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .testTag("spot_difference_arena")
    ) {
        when (gameState) {
            SpotDiffGameState.READY -> {
                SpotDiffReadyScreen(
                    bestScore = bestScore,
                    bestAccuracy = bestAccuracy,
                    totalFound = totalFoundEver,
                    onBack = onBack,
                    onStart = { startCountdown() }
                )
            }
            SpotDiffGameState.COUNTDOWN -> {
                SpotDiffCountdownOverlay(
                    countdownValue = countdownValue,
                    onBack = onBack
                )
            }
            SpotDiffGameState.PLAYING, SpotDiffGameState.PAUSED -> {
                SpotDiffPlayLayout(
                    currentRound = currentRound,
                    score = score,
                    combo = combo,
                    timeRemainingMs = timeRemainingMs,
                    totalRoundTimeMs = totalRoundTimeMs,
                    targetDifferences = targetDifferencesCount,
                    differencesFound = roundDifferencesFound,
                    cells = cells,
                    showRoundClearBanner = showRoundClearBanner,
                    penaltyTrigger = penaltyAlertTrigger,
                    onCellClicked = { onCellClicked(it) },
                    onPause = { gameState = SpotDiffGameState.PAUSED }
                )

                if (gameState == SpotDiffGameState.PAUSED) {
                    SpotDiffPauseDialog(
                        onResume = { gameState = SpotDiffGameState.PLAYING },
                        onRestart = { startCountdown() },
                        onExit = onBack
                    )
                }
            }
            SpotDiffGameState.GAME_OVER -> {
                SpotDiffGameOverScreen(
                    score = score,
                    solvedDifferences = correctSpotsCount,
                    totalAttempts = totalAttempts,
                    wrongClicks = wrongClicksCount,
                    roundsCleared = currentRound - 1,
                    maxCombo = maxCombo,
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
fun SpotDiffReadyScreen(
    bestScore: Int,
    bestAccuracy: Float,
    totalFound: Int,
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .testTag("spot_difference_back_button")
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
                color = DifficultyMedium.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DifficultyMedium.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = DifficultyMedium,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FOCUS & OBSERVATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DifficultyMedium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Hero Graphic
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
                                DifficultyMedium.copy(alpha = 0.35f),
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
                    border = BorderStroke(2.dp, DifficultyMedium),
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = DifficultyMedium,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Spot Difference",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Find all subtle differences between twin holographic matrices before time expires!",
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

                RuleItem(
                    icon = Icons.Default.AdsClick,
                    title = "Tap Any Difference",
                    desc = "Tap the modified element on either top or bottom matrix."
                )
                RuleItem(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Combo Streaks",
                    desc = "Rapid correct spots stack combo point multipliers."
                )
                RuleItem(
                    icon = Icons.Default.Warning,
                    title = "Penalty on Misses",
                    desc = "Wrong clicks subtract -3s penalty from the countdown clock."
                )
                RuleItem(
                    icon = Icons.Default.Timer,
                    title = "Round Bonus Time",
                    desc = "Clearing all differences adds +20s and advances to harder patterns."
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
            StatCard(
                title = "BEST SCORE",
                value = "$formattedScore pts",
                icon = Icons.Default.EmojiEvents,
                tint = SkillRushCoinGold,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "ACCURACY",
                value = if (bestAccuracy > 0) String.format(Locale.getDefault(), "%.0f%%", bestAccuracy) else "--",
                icon = Icons.Default.CheckCircle,
                tint = DifficultyEasy,
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
                .testTag("spot_difference_start_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DifficultyMedium,
                contentColor = Color.Black
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
                    text = "START CHALLENGE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun RuleItem(
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
fun StatCard(
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
fun SpotDiffCountdownOverlay(
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
                text = "GET READY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SkillRushCoinGold,
                letterSpacing = 2.sp
            )

            Text(
                text = if (countdownValue > 0) "$countdownValue" else "SPOTTED!",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Scan both panels and tap all differences!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun SpotDiffPlayLayout(
    currentRound: Int,
    score: Int,
    combo: Int,
    timeRemainingMs: Long,
    totalRoundTimeMs: Long,
    targetDifferences: Int,
    differencesFound: Int,
    cells: List<SpotDiffCell>,
    showRoundClearBanner: Boolean,
    penaltyTrigger: Int,
    onCellClicked: (Int) -> Unit,
    onPause: () -> Unit
) {
    val timeSeconds = (timeRemainingMs / 1000f)
    val timeProgress = (timeRemainingMs.toFloat() / totalRoundTimeMs.toFloat()).coerceIn(0f, 1f)

    val timerColor by animateColorAsState(
        targetValue = when {
            timeRemainingMs < 10_000L -> DifficultyHard
            timeRemainingMs < 20_000L -> SkillRushStreakFire
            else -> DifficultyEasy
        },
        label = "timerColor"
    )

    // Shake animation on penalty
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(penaltyTrigger) {
        if (penaltyTrigger > 0) {
            shakeOffset.animateTo(
                targetValue = 12f,
                animationSpec = tween(durationMillis = 50, easing = LinearEasing)
            )
            shakeOffset.animateTo(
                targetValue = -12f,
                animationSpec = tween(durationMillis = 50, easing = LinearEasing)
            )
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 50, easing = LinearEasing)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                // Round badge
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "ROUND $currentRound",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        letterSpacing = 1.sp
                    )
                }

                // Differences remaining chip counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = SkillRushCoinGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$differencesFound / $targetDifferences Found",
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
                        .testTag("spot_difference_pause_button")
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

            // Score & Combo & Timer Row
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
                                text = "STREAK x$combo",
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
                        modifier = Modifier.testTag("spot_difference_timer")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timer Progress Bar
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Matrix Panels (Panel A & Panel B)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val availableHeight = maxHeight
                val isCompactVertical = availableHeight < 560.dp

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Matrix A Panel (Original Pattern)
                    MatrixPanel(
                        panelLabel = "MATRIX A (ORIGINAL)",
                        labelColor = Color(0xFF38BDF8),
                        cells = cells,
                        isModifiedPanel = false,
                        onCellClicked = onCellClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Matrix B Panel (Twin Matrix with Differences)
                    MatrixPanel(
                        panelLabel = "MATRIX B (MODIFIED)",
                        labelColor = SkillRushCoinGold,
                        cells = cells,
                        isModifiedPanel = true,
                        onCellClicked = onCellClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer info chip
            Text(
                text = "Tap the difference on either matrix! Wrong taps cost -3s",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }

        // Round Clear Banner Overlay
        AnimatedVisibility(
            visible = showRoundClearBanner,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(2.dp, DifficultyEasy),
                shadowElevation = 16.dp,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DifficultyEasy,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "ROUND $currentRound CLEARED!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "+500 PTS • +20 SECONDS TIME BONUS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkillRushCoinGold
                    )
                }
            }
        }
    }
}

@Composable
fun MatrixPanel(
    panelLabel: String,
    labelColor: Color,
    cells: List<SpotDiffCell>,
    isModifiedPanel: Boolean,
    onCellClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.75f),
        border = BorderStroke(1.dp, labelColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = panelLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(cells) { index, cell ->
                    SpotDiffCellItem(
                        cell = cell,
                        isModifiedPanel = isModifiedPanel,
                        onClick = { onCellClicked(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpotDiffCellItem(
    cell: SpotDiffCell,
    isModifiedPanel: Boolean,
    onClick: () -> Unit
) {
    val symbol = if (isModifiedPanel) cell.modSymbol else cell.symbol
    val color = if (isModifiedPanel) cell.modColor else cell.color
    val rotation = if (isModifiedPanel) cell.modRotation else cell.rotation
    val hasBadge = if (isModifiedPanel) cell.modHasBadge else cell.hasBadge
    val badgeNumber = if (isModifiedPanel) cell.modBadgeNumber else cell.badgeNumber
    val isVisible = if (isModifiedPanel) cell.modIsVisible else true

    val interactionSource = remember { MutableInteractionSource() }

    val bgBorderColor = when {
        cell.isFound -> DifficultyEasy
        else -> Color.White.copy(alpha = 0.08f)
    }

    val cellBgColor = when {
        cell.isFound -> DifficultyEasy.copy(alpha = 0.18f)
        else -> Color(0xFF0F172A).copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = cellBgColor,
        border = BorderStroke(if (cell.isFound) 2.dp else 1.dp, bgBorderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isVisible) {
                Icon(
                    imageVector = symbol.icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation)
                )

                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(12.dp)
                            .background(SkillRushStreakFire, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$badgeNumber",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Green Checkmark indicator when found
            if (cell.isFound) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DifficultyEasy.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DifficultyEasy,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Spotted",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotDiffPauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
                ) {
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RESTART", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DifficultyHard)
                ) {
                    Text("QUIT TO ARENA", fontWeight = FontWeight.Bold, color = DifficultyHard)
                }
            }
        }
    }
}

@Composable
fun SpotDiffGameOverScreen(
    score: Int,
    solvedDifferences: Int,
    totalAttempts: Int,
    wrongClicks: Int,
    roundsCleared: Int,
    maxCombo: Int,
    saveResult: SpotDifferenceScoreManager.GameSaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalAttempts > 0) (solvedDifferences.toFloat() / totalAttempts * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 20).coerceAtMost(30) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        score >= 3000 || (roundsCleared >= 3 && accuracy >= 85f) -> "S"
        score >= 2000 || accuracy >= 80f -> "A"
        score >= 1000 || accuracy >= 60f -> "B"
        else -> "C"
    }

    val gradeColor = when (grade) {
        "S" -> SkillRushCoinGold
        "A" -> DifficultyEasy
        "B" -> SkillRushPrimary
        else -> Color(0xFF94A3B8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Text(
            text = "CHALLENGE COMPLETE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SkillRushCoinGold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grade & Score Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grade Ring
                Surface(
                    shape = CircleShape,
                    color = gradeColor.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, gradeColor),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = grade,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = gradeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "FINAL SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = NumberFormat.getNumberInstance(Locale.getDefault()).format(score),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (isNewBest) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = SkillRushCoinGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SkillRushCoinGold)
                    ) {
                        Text(
                            text = "★ NEW HIGH SCORE ★",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SkillRushCoinGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats breakdown
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatRow("Differences Spotted", "$solvedDifferences found", Icons.Default.CheckCircle, DifficultyEasy)
                        StatRow("Rounds Cleared", "$roundsCleared rounds", Icons.Default.EmojiEvents, SkillRushCoinGold)
                        StatRow("Accuracy", String.format(Locale.getDefault(), "%.1f%%", accuracy), Icons.Default.AdsClick, SkillRushPrimary)
                        StatRow("Max Streak", "x$maxCombo", Icons.Default.LocalFireDepartment, SkillRushStreakFire)
                        StatRow("Coins Earned", "+$coinsEarned Coins", Icons.Default.MonetizationOn, SkillRushCoinGold)
                        StatRow("XP Earned", "+$xpEarned XP", Icons.Default.AutoAwesome, SkillRushPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text("ARENA MENU", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
