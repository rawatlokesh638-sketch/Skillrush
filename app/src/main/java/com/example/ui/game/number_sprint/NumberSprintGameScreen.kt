package com.example.ui.game.number_sprint

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
import com.example.data.NumberSprintScoreManager
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.Locale

enum class NumberSprintGameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

data class MathPuzzle(
    val id: Int,
    val expression: String,
    val subtext: String = "Select the correct value",
    val correctAnswer: Int,
    val options: List<Int>,
    val difficultyTier: String
)

object NumberSprintPuzzleGenerator {
    fun generatePuzzle(questionNumber: Int): MathPuzzle {
        val tier = when {
            questionNumber <= 4 -> "Level 1: Novice"
            questionNumber <= 9 -> "Level 2: Adept"
            questionNumber <= 15 -> "Level 3: Expert"
            else -> "Level 4: Master"
        }

        val expression: String
        val correctAnswer: Int
        var subtext = "Solve the equation"

        when {
            // Tier 1: Addition and Subtraction within 50
            questionNumber <= 4 -> {
                val op = if (Random.nextBoolean()) "+" else "-"
                if (op == "+") {
                    val a = Random.nextInt(4, 25)
                    val b = Random.nextInt(3, 25)
                    expression = "$a + $b = ?"
                    correctAnswer = a + b
                } else {
                    val a = Random.nextInt(12, 40)
                    val b = Random.nextInt(2, a - 2)
                    expression = "$a - $b = ?"
                    correctAnswer = a - b
                }
            }

            // Tier 2: Multiplication, Division, and Missing terms
            questionNumber <= 9 -> {
                val mode = Random.nextInt(0, 3)
                when (mode) {
                    0 -> {
                        // Multiplication
                        val a = Random.nextInt(4, 12)
                        val b = Random.nextInt(3, 11)
                        expression = "$a × $b = ?"
                        correctAnswer = a * b
                    }
                    1 -> {
                        // Clean Division
                        val b = Random.nextInt(2, 9)
                        val multiplier = Random.nextInt(3, 12)
                        val a = b * multiplier
                        expression = "$a ÷ $b = ?"
                        correctAnswer = multiplier
                    }
                    else -> {
                        // Missing number: a + ? = c
                        val a = Random.nextInt(10, 45)
                        val b = Random.nextInt(5, 30)
                        val c = a + b
                        expression = "$a + [ ? ] = $c"
                        subtext = "Find the missing number"
                        correctAnswer = b
                    }
                }
            }

            // Tier 3: Two-step expressions & pattern reasoning
            questionNumber <= 15 -> {
                val mode = Random.nextInt(0, 3)
                when (mode) {
                    0 -> {
                        // (a * b) + c
                        val a = Random.nextInt(3, 9)
                        val b = Random.nextInt(4, 9)
                        val c = Random.nextInt(5, 25)
                        expression = "($a × $b) + $c = ?"
                        correctAnswer = (a * b) + c
                    }
                    1 -> {
                        // (a * b) - c
                        val a = Random.nextInt(4, 10)
                        val b = Random.nextInt(5, 10)
                        val c = Random.nextInt(3, 15)
                        expression = "($a × $b) - $c = ?"
                        correctAnswer = (a * b) - c
                    }
                    else -> {
                        // Number Sequence: e.g. 4, 8, 12, 16, ?
                        val start = Random.nextInt(2, 10)
                        val step = Random.nextInt(3, 7)
                        val seq = listOf(start, start + step, start + 2 * step, start + 3 * step)
                        expression = "${seq.joinToString(", ")}, [ ? ]"
                        subtext = "Complete the sequence"
                        correctAnswer = start + 4 * step
                    }
                }
            }

            // Tier 4: Master Speed Puzzles
            else -> {
                val mode = Random.nextInt(0, 3)
                when (mode) {
                    0 -> {
                        // Geometric / Double Sequence: 3, 6, 12, 24, ?
                        val start = Random.nextInt(2, 6)
                        val seq = listOf(start, start * 2, start * 4, start * 8)
                        expression = "${seq.joinToString(", ")}, [ ? ]"
                        subtext = "Next in the pattern"
                        correctAnswer = start * 16
                    }
                    1 -> {
                        // (a ÷ b) × c + d
                        val b = Random.nextInt(3, 7)
                        val mult = Random.nextInt(4, 10)
                        val a = b * mult
                        val c = Random.nextInt(2, 6)
                        val d = Random.nextInt(5, 20)
                        expression = "($a ÷ $b) × $c + $d = ?"
                        correctAnswer = (mult * c) + d
                    }
                    else -> {
                        // Square minus number: n² - k
                        val n = Random.nextInt(5, 13)
                        val k = Random.nextInt(3, 20)
                        expression = "$n² - $k = ?"
                        correctAnswer = (n * n) - k
                    }
                }
            }
        }

        // Generate 3 plausible distractors
        val optionsSet = mutableSetOf(correctAnswer)
        val offsets = listOf(-1, 1, -2, 2, -10, 10, -5, 5, -3, 3, 4, -4).shuffled()
        var offsetIndex = 0

        while (optionsSet.size < 4 && offsetIndex < offsets.size) {
            val candidate = correctAnswer + offsets[offsetIndex]
            if (candidate >= 0 && candidate != correctAnswer) {
                optionsSet.add(candidate)
            }
            offsetIndex++
        }

        // Fallback if needed
        while (optionsSet.size < 4) {
            val candidate = (correctAnswer + Random.nextInt(-15, 16)).coerceAtLeast(0)
            if (candidate != correctAnswer) {
                optionsSet.add(candidate)
            }
        }

        return MathPuzzle(
            id = questionNumber,
            expression = expression,
            subtext = subtext,
            correctAnswer = correctAnswer,
            options = optionsSet.toList().shuffled(),
            difficultyTier = tier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberSprintGameScreen(
    onBack: () -> Unit,
    tournamentId: String? = null,
    sessionToken: String? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var tournamentResult by remember { mutableStateOf<com.example.model.TournamentSubmitResult?>(null) }

    var gameState by remember { mutableStateOf(NumberSprintGameState.READY) }
    var countdownNumber by remember { mutableIntStateOf(3) }
    var timeLeftMillis by remember { mutableLongStateOf(30000L) }
    val totalTimeMillis = 30000L

    var score by remember { mutableIntStateOf(0) }
    var currentCombo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var questionNumber by remember { mutableIntStateOf(1) }
    var solvedCount by remember { mutableIntStateOf(0) }
    var totalAnswers by remember { mutableIntStateOf(0) }

    // Current Active Puzzle
    var currentPuzzle by remember { mutableStateOf(NumberSprintPuzzleGenerator.generatePuzzle(1)) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isFeedbackLocked by remember { mutableStateOf(false) }
    var isCorrectFeedback by remember { mutableStateOf<Boolean?>(null) }

    // Best Records
    var bestScore by remember { mutableIntStateOf(NumberSprintScoreManager.getBestScore(context)) }
    var bestAccuracy by remember { mutableFloatStateOf(NumberSprintScoreManager.getBestAccuracy(context)) }
    var saveResult by remember { mutableStateOf<NumberSprintScoreManager.SaveResult?>(null) }

    // Coroutine Jobs
    var timerJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHaptic(type: HapticFeedbackType) {
        try {
            hapticFeedback.performHapticFeedback(type)
        } catch (_: Exception) {}
    }

    // End Game
    fun endGame() {
        timerJob?.cancel()
        gameState = NumberSprintGameState.GAME_OVER

        val accuracy = if (totalAnswers > 0) (solvedCount.toFloat() / totalAnswers * 100f) else 0f
        val result = NumberSprintScoreManager.recordGameResult(
            context = context,
            score = score,
            accuracy = accuracy,
            maxCombo = maxCombo,
            solvedQuestions = solvedCount
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
                        tournamentTitle = "Number Sprint Daily Challenge",
                        score = score,
                        sessionToken = sessionToken
                    )
                    tournamentResult = submitRes
                } catch (e: Exception) {
                    android.util.Log.e("NumberSprint", "Tournament submit error: ${e.message}")
                }
            }
        }
    }

    // Start Timer Loop
    fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            val tick = 50L
            while (isActive && timeLeftMillis > 0) {
                delay(tick)
                timeLeftMillis = (timeLeftMillis - tick).coerceAtLeast(0L)
            }
            if (isActive && timeLeftMillis <= 0) {
                endGame()
            }
        }
    }

    // Start 3-2-1 Countdown
    fun startCountdown() {
        timerJob?.cancel()
        gameState = NumberSprintGameState.COUNTDOWN
        countdownNumber = 3
        timeLeftMillis = totalTimeMillis
        score = 0
        currentCombo = 0
        maxCombo = 0
        questionNumber = 1
        solvedCount = 0
        totalAnswers = 0
        selectedOption = null
        isFeedbackLocked = false
        isCorrectFeedback = null
        currentPuzzle = NumberSprintPuzzleGenerator.generatePuzzle(1)
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
            gameState = NumberSprintGameState.PLAYING
            com.example.audio.AudioManager.playGameStartSound(context)
            startTimerLoop()
        }
    }

    // Pause Game
    fun pauseGame() {
        if (gameState == NumberSprintGameState.PLAYING) {
            timerJob?.cancel()
            gameState = NumberSprintGameState.PAUSED
        }
    }

    // Resume Game
    fun resumeGame() {
        if (gameState == NumberSprintGameState.PAUSED) {
            gameState = NumberSprintGameState.PLAYING
            startTimerLoop()
        }
    }

    // Restart Game
    fun restartGame() {
        timerJob?.cancel()
        startCountdown()
    }

    // Handle Option Tap
    fun handleOptionSelected(option: Int) {
        if (gameState != NumberSprintGameState.PLAYING || isFeedbackLocked) return

        isFeedbackLocked = true
        selectedOption = option
        totalAnswers++

        val isCorrect = (option == currentPuzzle.correctAnswer)
        isCorrectFeedback = isCorrect

        if (isCorrect) {
            solvedCount++
            currentCombo++
            if (currentCombo > maxCombo) maxCombo = currentCombo

            val basePoints = 100 + (questionNumber * 10)
            val comboBonus = (currentCombo - 1) * 25
            score += (basePoints + comboBonus)

            com.example.audio.AudioManager.playCorrectSound(context)
            if (currentCombo % 5 == 0) {
                com.example.audio.AudioManager.playScoreComboSound(context, currentCombo)
            }
        } else {
            currentCombo = 0
            score = (score - 20).coerceAtLeast(0)
            com.example.audio.AudioManager.playWrongSound(context)
            com.example.audio.HapticManager.vibrateWrong(context)
        }

        // Fast transition to next puzzle
        coroutineScope.launch {
            delay(280)
            questionNumber++
            currentPuzzle = NumberSprintPuzzleGenerator.generatePuzzle(questionNumber)
            selectedOption = null
            isCorrectFeedback = null
            isFeedbackLocked = false
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
                            text = "NUMBER SPRINT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = SkillRushPrimaryContainer
                        ) {
                            Text(
                                text = "LOGIC",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillRushOnPrimaryContainer,
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
                            timerJob?.cancel()
                            onBack()
                        },
                        modifier = Modifier.testTag("number_sprint_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    if (gameState == NumberSprintGameState.PLAYING) {
                        IconButton(
                            onClick = { pauseGame() },
                            modifier = Modifier.testTag("number_sprint_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = SkillRushPrimary
                            )
                        }
                    } else if (gameState == NumberSprintGameState.PAUSED) {
                        IconButton(
                            onClick = { resumeGame() },
                            modifier = Modifier.testTag("number_sprint_resume_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = SkillRushPrimary
                            )
                        }
                    }

                    if (gameState != NumberSprintGameState.READY && gameState != NumberSprintGameState.COUNTDOWN) {
                        IconButton(
                            onClick = { restartGame() },
                            modifier = Modifier.testTag("number_sprint_restart_button")
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
            // Live HUD: Score, Timer, Combo, Q#
            NumberSprintHud(
                score = score,
                timeLeftMillis = timeLeftMillis,
                totalTimeMillis = totalTimeMillis,
                combo = currentCombo,
                questionNumber = questionNumber,
                bestScore = bestScore
            )

            // Main Puzzle Arena
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
                    .padding(16.dp)
                    .testTag("number_sprint_arena"),
                contentAlignment = Alignment.Center
            ) {
                // Active Question View & 4 Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Question Header Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SkillRushPrimaryContainer
                    ) {
                        Text(
                            text = currentPuzzle.difficultyTier,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkillRushOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Expression Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentPuzzle.subtext,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentPuzzle.expression,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = SkillRushPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("number_sprint_expression")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4-Option Choice Grid (2x2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (rowIndex in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (colIndex in 0..1) {
                                    val optionIndex = rowIndex * 2 + colIndex
                                    if (optionIndex < currentPuzzle.options.size) {
                                        val optionValue = currentPuzzle.options[optionIndex]
                                        NumberSprintOptionButton(
                                            modifier = Modifier.weight(1f),
                                            optionValue = optionValue,
                                            index = optionIndex,
                                            isSelected = (selectedOption == optionValue),
                                            isCorrect = (optionValue == currentPuzzle.correctAnswer),
                                            showFeedback = isFeedbackLocked,
                                            onClick = { handleOptionSelected(optionValue) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Ready Overlay
                if (gameState == NumberSprintGameState.READY) {
                    NumberSprintReadyOverlay(
                        bestScore = bestScore,
                        bestAccuracy = bestAccuracy,
                        onStart = { startCountdown() }
                    )
                }

                // 3-2-1 Countdown Overlay
                if (gameState == NumberSprintGameState.COUNTDOWN) {
                    NumberSprintCountdownOverlay(number = countdownNumber)
                }

                // Paused Overlay
                if (gameState == NumberSprintGameState.PAUSED) {
                    NumberSprintPausedOverlay(
                        onResume = { resumeGame() },
                        onRestart = { restartGame() },
                        onQuit = { onBack() }
                    )
                }

                // Game Over Overlay
                if (gameState == NumberSprintGameState.GAME_OVER) {
                    NumberSprintGameOverOverlay(
                        score = score,
                        solvedCount = solvedCount,
                        totalAnswers = totalAnswers,
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

            // Bottom Action Bar
            NumberSprintBottomControls(
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
fun NumberSprintHud(
    score: Int,
    timeLeftMillis: Long,
    totalTimeMillis: Long,
    combo: Int,
    questionNumber: Int,
    bestScore: Int
) {
    val secondsRemaining = (timeLeftMillis / 1000f)
    val timeProgress = (timeLeftMillis.toFloat() / totalTimeMillis).coerceIn(0f, 1f)

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
                        modifier = Modifier.testTag("number_sprint_live_score")
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
                        modifier = Modifier.testTag("number_sprint_timer")
                    )
                }

                // Question Counter & Streak
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#$questionNumber",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("number_sprint_question_counter")
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

            // Progress bar
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
fun NumberSprintOptionButton(
    modifier: Modifier = Modifier,
    optionValue: Int,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showFeedback: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            showFeedback && isSelected && isCorrect -> DifficultyEasy
            showFeedback && isSelected && !isCorrect -> DifficultyHard
            showFeedback && !isSelected && isCorrect -> DifficultyEasy.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "option_bg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            showFeedback && isSelected -> Color.White
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "option_text"
    )

    val borderColor = when {
        showFeedback && isSelected && isCorrect -> DifficultyEasy
        showFeedback && isSelected && !isCorrect -> DifficultyHard
        else -> SkillRushPrimary.copy(alpha = 0.25f)
    }

    Card(
        modifier = modifier
            .height(58.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) {
                onClick()
            }
            .testTag("number_sprint_option_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$optionValue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = textColor,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun NumberSprintReadyOverlay(
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
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Text(
                    text = "NUMBER SPRINT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = SkillRushPrimary
                )

                Text(
                    text = "30-second rapid arithmetic sprint! Solve math equations, missing terms, and number patterns. Chain correct answers for big streak bonuses!",
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
                        .testTag("number_sprint_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START 30s SPRINT", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun NumberSprintCountdownOverlay(number: Int) {
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
                text = "CALCULATE AT SPRINT SPEED!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SkillRushCoinGold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun NumberSprintPausedOverlay(
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
                    text = "SPRINT PAUSED",
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
fun NumberSprintGameOverOverlay(
    score: Int,
    solvedCount: Int,
    totalAnswers: Int,
    maxCombo: Int,
    saveResult: NumberSprintScoreManager.SaveResult?,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val accuracy = if (totalAnswers > 0) (solvedCount.toFloat() / totalAnswers * 100f) else 0f
    val xpEarned = saveResult?.xpEarned ?: ((score / 10).coerceAtMost(30) + (maxCombo * 2).coerceAtMost(15) + (solvedCount * 2).coerceAtMost(20) + 10)
    val coinsEarned = saveResult?.coinsEarned ?: 10
    val isNewBest = saveResult?.isNewBestScore == true

    val grade = when {
        solvedCount >= 14 && accuracy >= 90f -> "S"
        solvedCount >= 10 && accuracy >= 80f -> "A"
        solvedCount >= 6 -> "B"
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
                            text = "🏆 NEW LOGIC RECORD!",
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
                            modifier = Modifier.testTag("number_sprint_final_score")
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
                        StatRow("Solved", "$solvedCount / $totalAnswers correct", Icons.Default.CheckCircle, DifficultyEasy)
                        StatRow("Accuracy", String.format(Locale.getDefault(), "%.1f%%", accuracy), Icons.Default.Percent, SkillRushPrimary)
                        StatRow("Max Streak", "x$maxCombo in a row", Icons.Default.LocalFireDepartment, SkillRushStreakFire)
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
                        .testTag("number_sprint_play_again_button")
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
fun NumberSprintBottomControls(
    gameState: NumberSprintGameState,
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
            NumberSprintGameState.READY -> {
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
                    Text("START 30s NUMBER SPRINT", fontWeight = FontWeight.Black)
                }
            }
            NumberSprintGameState.PLAYING -> {
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
            NumberSprintGameState.PAUSED -> {
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
