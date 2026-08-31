package com.example.ui.game

import com.example.ui.components.AdMobBannerView

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.SkillRushBottomNavigation
import com.example.data.UserProfileManager
import com.example.model.GameChallenge
import com.example.model.GameData
import com.example.model.GameDifficulty
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(
    onNavigateBack: () -> Unit = {},
    onPlayGame: (String) -> Unit = {},
    onNavigateToNavTab: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf<GameDifficulty?>(null) }

    val coins = remember(context) { UserProfileManager.getCoins(context) }
    val formattedCoins = remember(coins) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(coins)
    }

    val allChallenges = remember(context) { GameData.getChallenges(context) }
    val challenges: List<com.example.model.GameChallenge> = remember(selectedFilter, allChallenges) {
        if (selectedFilter == null) {
            allChallenges
        } else {
            allChallenges.filter { it.difficulty == selectedFilter }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SKILLRUSH",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = SkillRushPrimaryContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "ARENA",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkillRushOnPrimaryContainer,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = SkillRushCoinGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formattedCoins,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AdMobBannerView()
                SkillRushBottomNavigation(
                    selectedIndex = 1,
                    onItemSelected = { index ->
                        if (index == 0) {
                            onNavigateBack()
                        } else {
                            onNavigateToNavTab(index)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
            ) {
            item {
                GameSelectionHeader()
            }

            item {
                DifficultyFilterBar(
                    selectedDifficulty = selectedFilter,
                    onSelectDifficulty = { selectedFilter = it }
                )
            }

            itemsIndexed(challenges, key = { _, item -> item.id }) { index, challenge ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 60)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(durationMillis = 300, delayMillis = index * 60)
                            )
                ) {
                    GameChallengeCard(
                        challenge = challenge,
                        onPlayClick = {
                            if (challenge.isLocked) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "🔒 ${challenge.title} is locked! ${challenge.unlockRequirement ?: "Requirement not met."}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                onPlayGame(challenge.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameSelectionHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "CHOOSE YOUR CHALLENGE",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = SkillRushPrimary,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.testTag("heading_choose_challenge")
        )
        Text(
            text = "Sharpen your mental acuity with fast-paced micro-games.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun DifficultyFilterBar(
    selectedDifficulty: GameDifficulty?,
    onSelectDifficulty: (GameDifficulty?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedDifficulty == null,
            onClick = { onSelectDifficulty(null) },
            label = {
                Text(
                    text = "ALL (5)",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SkillRushPrimary,
                selectedLabelColor = Color.White,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            border = null
        )

        GameDifficulty.values().forEach { difficulty ->
            val isSelected = selectedDifficulty == difficulty
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectDifficulty(if (isSelected) null else difficulty)
                },
                label = {
                    Text(
                        text = difficulty.label.uppercase(),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (difficulty) {
                        GameDifficulty.EASY -> DifficultyEasy
                        GameDifficulty.MEDIUM -> DifficultyMedium
                        GameDifficulty.HARD -> DifficultyHard
                    },
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                border = null
            )
        }
    }
}

@Composable
fun GameChallengeCard(
    challenge: GameChallenge,
    onPlayClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onPlayClick() }
            .testTag("game_card_${challenge.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isLocked) {
                LockedContainerColor.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(
            1.dp,
            if (challenge.isLocked) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            } else {
                SkillRushPrimary.copy(alpha = 0.18f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (challenge.isLocked) 0.dp else 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Category Icon + Title + Difficulty Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (challenge.isLocked) {
                                    LockedItemColor.copy(alpha = 0.15f)
                                } else {
                                    SkillRushPrimaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (challenge.isLocked) Icons.Default.Lock else challenge.icon,
                            contentDescription = challenge.title,
                            tint = if (challenge.isLocked) LockedItemColor else SkillRushPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (challenge.isLocked) LockedItemColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = challenge.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (challenge.isLocked) LockedItemColor else SkillRushPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Difficulty & Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (challenge.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LockedItemColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "LOCKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        DifficultyBadge(difficulty = challenge.difficulty)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short Description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (challenge.isLocked) {
                    LockedItemColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Section: Best Score on left, Play / Unlock button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Best score or Unlock requirement info
                if (challenge.isLocked && challenge.unlockRequirement != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "Requirement",
                            tint = LockedItemColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = challenge.unlockRequirement,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = LockedItemColor,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = "BEST SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = SkillRushCoinGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = challenge.bestScore,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Action Button
                if (challenge.isLocked) {
                    OutlinedButton(
                        onClick = onPlayClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LockedItemColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LockedItemColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("unlock_button_${challenge.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onPlayClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkillRushPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("play_button_${challenge.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: GameDifficulty) {
    val (bgColor, textColor) = when (difficulty) {
        GameDifficulty.EASY -> DifficultyEasyContainer to DifficultyEasy
        GameDifficulty.MEDIUM -> DifficultyMediumContainer to DifficultyMedium
        GameDifficulty.HARD -> DifficultyHardContainer to DifficultyHard
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = difficulty.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
