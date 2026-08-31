package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameChallenge
import com.example.model.GameData
import com.example.ui.game.memory_flash.MemoryFlashGameScreen
import com.example.ui.game.number_sprint.NumberSprintGameScreen
import com.example.ui.game.perfect_aim.PerfectAimGameScreen
import com.example.ui.game.speed_rush.SpeedRushGameScreen
import com.example.ui.game.spot_difference.SpotDifferenceGameScreen
import com.example.ui.game.tap_rush.TapRushGameScreen
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushOnPrimaryContainer
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushPrimaryContainer
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(
    gameId: String,
    tournamentId: String? = null,
    sessionToken: String? = null,
    onBack: () -> Unit
) {
    if (gameId == "tap_rush" || gameId == "reflex") {
        TapRushGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    if (gameId == "memory" || gameId == "memory_flash") {
        MemoryFlashGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    if (gameId == "accuracy" || gameId == "perfect_aim") {
        PerfectAimGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    if (gameId == "logic" || gameId == "number_sprint") {
        NumberSprintGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    if (gameId == "spot_difference" || gameId == "difference" || gameId == "observation" || gameId == "focus") {
        SpotDifferenceGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    if (gameId == "speed_rush" || gameId == "speed" || gameId == "frenzy" || gameId == "velocity") {
        SpeedRushGameScreen(onBack = onBack, tournamentId = tournamentId, sessionToken = sessionToken)
        return
    }

    val challenge = remember(gameId) {
        GameData.challenges.find { it.id == gameId } ?: GameData.challenges.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = challenge.title.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = SkillRushPrimary
                        )
                        Text(
                            text = "CATEGORY: ${challenge.category.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (challenge.id) {
                "memory" -> MemoryGameArena(challenge, onBack)
                "logic" -> LogicGameArena(challenge, onBack)
                "accuracy" -> AccuracyGameArena(challenge, onBack)
                else -> GenericGameArena(challenge, onBack)
            }
        }
    }
}

@Composable
fun ReflexGameArena(challenge: GameChallenge, onBack: () -> Unit) {
    val context = LocalContext.current
    var gameState by remember { mutableStateOf("READY") } // READY, WAITING, GO, FINISHED, TOO_EARLY
    var startTime by remember { mutableLongStateOf(0L) }
    var reactionTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(gameState) {
        if (gameState == "WAITING") {
            val waitTime = Random.nextLong(1500, 3500)
            delay(waitTime)
            if (gameState == "WAITING") {
                startTime = System.currentTimeMillis()
                gameState = "GO"
            }
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when (gameState) {
            "WAITING" -> Color(0xFFEF4444)
            "GO" -> Color(0xFF22C55E)
            "TOO_EARLY" -> Color(0xFFF97316)
            "FINISHED" -> SkillRushPrimary
            else -> SkillRushPrimaryContainer
        },
        animationSpec = tween(150),
        label = "reflex_bg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable {
                when (gameState) {
                    "READY" -> {
                        com.example.audio.AudioManager.playTapSound(context)
                        gameState = "WAITING"
                    }
                    "WAITING" -> {
                        com.example.audio.AudioManager.playWrongSound(context)
                        com.example.audio.HapticManager.vibrateWrong(context)
                        gameState = "TOO_EARLY"
                    }
                    "GO" -> {
                        reactionTimeMs = System.currentTimeMillis() - startTime
                        com.example.audio.AudioManager.playCorrectSound(context)
                        com.example.audio.HapticManager.vibrateCorrect(context)
                        gameState = "FINISHED"
                    }
                    "TOO_EARLY", "FINISHED" -> {
                        com.example.audio.AudioManager.playTapSound(context)
                        gameState = "READY"
                    }
                }
            }
            .padding(24.dp)
            .testTag("reflex_arena"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (gameState) {
            "READY" -> {
                Icon(
                    imageVector = challenge.icon,
                    contentDescription = null,
                    tint = SkillRushPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TAP TO START",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = SkillRushOnPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Wait for Green, then tap as fast as you can!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkillRushOnPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            "WAITING" -> {
                Text(
                    text = "WAIT FOR GREEN...",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            "GO" -> {
                Text(
                    text = "TAP NOW!",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            "TOO_EARLY" -> {
                Text(
                    text = "TOO EARLY!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Tap to try again",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            "FINISHED" -> {
                Text(
                    text = "$reactionTimeMs ms",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = if (reactionTimeMs < 250) "Godlike Reflexes! 🔥" else "Great reaction time! ⚡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { gameState = "READY" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SkillRushPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MemoryGameArena(challenge: GameChallenge, onBack: () -> Unit) {
    val context = LocalContext.current
    var round by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var activeTile by remember { mutableIntStateOf(-1) }
    var userTaps by remember { mutableStateOf(listOf<Int>()) }
    var statusText by remember { mutableStateOf("Watch the sequence...") }
    var acceptingInput by remember { mutableStateOf(false) }

    LaunchedEffect(round) {
        acceptingInput = false
        userTaps = emptyList()
        statusText = "Memorize the sequence..."
        val seqLength = (score + 3).coerceIn(3, 8)
        val newSeq = List(seqLength) { Random.nextInt(0, 9) }
        sequence = newSeq
        delay(600)
        for (index in newSeq) {
            activeTile = index
            delay(400)
            activeTile = -1
            delay(150)
        }
        statusText = "Your turn! Replay the pattern."
        acceptingInput = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LEVEL ${score + 1}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SkillRushPrimary
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3x3 Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 3) {
                        val tileIndex = row * 3 + col
                        val isHighlighted = activeTile == tileIndex
                        val tileColor = if (isHighlighted) SkillRushPrimary else SkillRushPrimaryContainer

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(tileColor)
                                .border(
                                    2.dp,
                                    if (isHighlighted) Color.White else SkillRushPrimary.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = acceptingInput) {
                                    if (!acceptingInput) return@clickable
                                    val tapPos = userTaps.size
                                    if (tapPos >= sequence.size) return@clickable

                                    val newTaps = userTaps + tileIndex
                                    userTaps = newTaps

                                    if (sequence.getOrNull(tapPos) == tileIndex) {
                                        com.example.audio.AudioManager.playCorrectSound(context)
                                        if (newTaps.size == sequence.size) {
                                            acceptingInput = false
                                            statusText = "Correct! Advancing..."
                                            score++
                                            round++
                                            com.example.audio.AudioManager.playScoreComboSound(context, score)
                                        }
                                    } else {
                                        acceptingInput = false
                                        statusText = "Incorrect! Starting over."
                                        score = 0
                                        round++
                                        com.example.audio.AudioManager.playWrongSound(context)
                                        com.example.audio.HapticManager.vibrateWrong(context)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tileIndex + 1}",
                                fontWeight = FontWeight.Bold,
                                color = if (isHighlighted) Color.White else SkillRushOnPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Score: ${score * 350} pts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LogicGameArena(challenge: GameChallenge, onBack: () -> Unit) {
    var numA by remember { mutableIntStateOf(Random.nextInt(12, 35)) }
    var numB by remember { mutableIntStateOf(Random.nextInt(5, 20)) }
    var correctAnswer by remember { mutableIntStateOf(numA + numB) }
    var options by remember { mutableStateOf(listOf<Int>()) }
    var score by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    fun refreshQuestion() {
        numA = Random.nextInt(15, 50)
        numB = Random.nextInt(8, 30)
        val ans = numA + numB
        correctAnswer = ans
        val wrong1 = ans + Random.nextInt(1, 5)
        val wrong2 = ans - Random.nextInt(1, 5)
        val wrong3 = ans + 10
        options = listOf(ans, wrong1, wrong2, wrong3).shuffled()
    }

    LaunchedEffect(Unit) {
        refreshQuestion()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "SOLVE RAPID CIPHER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = SkillRushPrimary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SkillRushPrimaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$numA + $numB = ?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = SkillRushOnPrimaryContainer
                )
                if (feedback.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = feedback,
                        fontWeight = FontWeight.Bold,
                        color = SkillRushPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                val context = LocalContext.current
                Button(
                    onClick = {
                        if (option == correctAnswer) {
                            score += 150
                            feedback = "Correct! +150"
                            com.example.audio.AudioManager.playCorrectSound(context)
                            refreshQuestion()
                        } else {
                            feedback = "Incorrect answer"
                            com.example.audio.AudioManager.playWrongSound(context)
                            com.example.audio.HapticManager.vibrateWrong(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$option",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Total Score: $score pts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AccuracyGameArena(challenge: GameChallenge, onBack: () -> Unit) {
    val context = LocalContext.current
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.5f) }
    var hits by remember { mutableIntStateOf(0) }
    var accuracyScore by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hits: $hits", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Score: $accuracyScore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = SkillRushPrimary)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            val targetSize = 64.dp
            val maxX = maxWidth - targetSize
            val maxY = maxHeight - targetSize

            Box(
                modifier = Modifier
                    .offset(x = maxX * targetX, y = maxY * targetY)
                    .size(targetSize)
                    .clip(CircleShape)
                    .background(SkillRushPrimary)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable {
                        hits++
                        accuracyScore += 250
                        targetX = Random.nextFloat()
                        targetY = Random.nextFloat()
                        com.example.audio.AudioManager.playCorrectSound(context)
                        com.example.audio.HapticManager.vibrateCorrect(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Text(
            text = "Tap the bullseye before it shifts position!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GenericGameArena(challenge: GameChallenge, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = challenge.icon,
            contentDescription = null,
            tint = SkillRushPrimary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = challenge.title.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = SkillRushPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = challenge.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
        ) {
            Text("Return to Challenge Selection", fontWeight = FontWeight.Bold)
        }
    }
}
