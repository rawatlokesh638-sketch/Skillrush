package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyRewardManager
import com.example.data.LevelProgress
import com.example.data.StreakManager
import com.example.data.UserProfileManager
import com.example.ui.components.DailyRewardDialog
import com.example.ui.components.LevelBadge
import com.example.ui.components.StreakCalendarCard
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushOnPrimaryContainer
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushPrimaryContainer
import com.example.ui.theme.SkillRushStreakFire
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToGames: () -> Unit = {},
    onPlayGame: (String) -> Unit = {},
    onDailyChallenge: () -> Unit = { onPlayGame("memory") },
    onNavigateToTournaments: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNavTab: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var isDailyRewardAvailable by remember(refreshTrigger) {
        mutableStateOf(DailyRewardManager.isDailyRewardAvailable(context))
    }
    val currentRewardDay by remember(refreshTrigger) {
        mutableIntStateOf(DailyRewardManager.getCurrentRewardDay(context))
    }

    val coins = remember(context, refreshTrigger) { UserProfileManager.getCoins(context) }
    val levelProgress = remember(context, refreshTrigger) { UserProfileManager.getLevelProgress(context) }
    val playerName = remember(context, refreshTrigger) { UserProfileManager.getPlayerName(context) }
    val streakDays = remember(context, refreshTrigger) { UserProfileManager.getStreakDays(context) }
    val streakInfo = remember(context, refreshTrigger) { StreakManager.getStreakInfo(context) }
    val bestScore = remember(context, refreshTrigger) { UserProfileManager.getHighestOverallScore(context) }
    val dailyInfo = remember(context, refreshTrigger) { com.example.data.DailyChallengeManager.getDailyChallengeInfo(context) }
    val activeAnnouncement by com.example.data.CloudSyncManager.activeAnnouncement.collectAsState()
    var dismissedAnnouncementId by remember { mutableStateOf<String?>(null) }

    // Auto prompt daily reward when player logs in/arrives if available
    LaunchedEffect(Unit) {
        if (DailyRewardManager.isDailyRewardAvailable(context)) {
            showDailyRewardDialog = true
        }
    }

    if (showDailyRewardDialog) {
        DailyRewardDialog(
            onDismiss = {
                showDailyRewardDialog = false
                refreshTrigger++
            },
            onRewardClaimed = {
                refreshTrigger++
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SkillRushBottomNavigation(
                selectedIndex = selectedNavIndex,
                onItemSelected = { index ->
                    selectedNavIndex = index
                    when (index) {
                        1 -> onNavigateToGames()
                        3 -> onNavigateToProfile()
                        else -> onNavigateToNavTab(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                HeaderSection(
                    levelProgress = levelProgress,
                    playerName = playerName,
                    coins = coins,
                    isDailyRewardAvailable = isDailyRewardAvailable,
                    onProfileClick = onNavigateToProfile,
                    onDailyRewardClick = { showDailyRewardDialog = true }
                )
            }
            activeAnnouncement?.let { ann ->
                if (dismissedAnnouncementId != ann.id) {
                    item {
                        com.example.ui.components.AnnouncementBanner(
                            announcement = ann,
                            onDismiss = { dismissedAnnouncementId = ann.id }
                        )
                    }
                }
            }
            item {
                SkillRushTitleSection(streakDays = streakDays, bestScore = bestScore)
            }
            item {
                // Visual Indicator for Daily Reward
                DailyRewardHomeBanner(
                    isRewardAvailable = isDailyRewardAvailable,
                    currentDay = currentRewardDay,
                    onClick = { showDailyRewardDialog = true }
                )
            }
            item {
                DailyChallengeCard(
                    dailyInfo = dailyInfo,
                    onStartChallenge = onDailyChallenge
                )
            }
            item {
                LiveTournamentsHomeCard(
                    onOpenTournaments = onNavigateToTournaments
                )
            }
            item {
                StreakCalendarCard(streakInfo = streakInfo)
            }
            item {
                PlayNowCenterpiece(onPlayNow = onNavigateToGames)
            }
            item {
                GameCategoriesSection(onSelectCategory = onPlayGame)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(
    levelProgress: LevelProgress,
    playerName: String = "Player 1",
    coins: Int = 0,
    isDailyRewardAvailable: Boolean = false,
    onProfileClick: () -> Unit = {},
    onDailyRewardClick: () -> Unit = {}
) {
    val formattedCoins = remember(coins) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(coins)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "header_gift_pulse")
    val giftScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gift_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .testTag("home_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player avatar + level badge + name info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LevelBadge(level = levelProgress.level, size = 44.dp)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = playerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Level ${levelProgress.level} • ${levelProgress.levelTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillRushPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Daily Reward Gift Button with Notification Dot
                    Surface(
                        shape = CircleShape,
                        color = if (isDailyRewardAvailable) SkillRushCoinGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, if (isDailyRewardAvailable) SkillRushCoinGold.copy(alpha = 0.6f) else SkillRushPrimary.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .clickable { onDailyRewardClick() }
                            .testTag("header_daily_reward_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Daily Reward",
                                tint = if (isDailyRewardAvailable) SkillRushCoinGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .then(if (isDailyRewardAvailable) Modifier.scale(giftScale) else Modifier)
                            )

                            if (isDailyRewardAvailable) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFFF3366), CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Coins balance pill badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.15f)),
                        modifier = Modifier.testTag("coin_balance_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // XP Progress Bar in header
            XpProgressBar(
                currentXp = levelProgress.currentLevelXp,
                requiredXp = levelProgress.xpRequiredForNextLevel,
                progressFraction = levelProgress.progressFraction,
                height = 8.dp
            )
        }
    }
}

@Composable
fun DailyRewardHomeBanner(
    isRewardAvailable: Boolean,
    currentDay: Int,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "daily_reward_banner_pulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_border_alpha"
    )
    val giftBounce by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_gift_bounce"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("daily_reward_home_banner"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRewardAvailable) Color(0xFF1E1F2A) else Color(0xFF14171E)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isRewardAvailable) SkillRushCoinGold.copy(alpha = pulseBorderAlpha) else SkillRushPrimary.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRewardAvailable) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRewardAvailable) SkillRushCoinGold.copy(alpha = 0.2f) else Color(0xFF2EA043).copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.5.dp,
                        if (isRewardAvailable) SkillRushCoinGold.copy(alpha = 0.8f) else Color(0xFF2EA043).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRewardAvailable) Icons.Default.CardGiftcard else Icons.Default.CheckCircle,
                            contentDescription = "Daily Reward",
                            tint = if (isRewardAvailable) SkillRushCoinGold else Color(0xFF2EA043),
                            modifier = Modifier
                                .size(24.dp)
                                .then(if (isRewardAvailable) Modifier.scale(giftBounce) else Modifier)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isRewardAvailable) "DAILY REWARD AVAILABLE!" else "DAY $currentDay REWARD CLAIMED",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isRewardAvailable) SkillRushCoinGold else Color.White,
                            letterSpacing = (-0.2).sp
                        )

                        if (isRewardAvailable) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SkillRushCoinGold
                            ) {
                                Text(
                                    text = "DAY $currentDay",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isRewardAvailable) "Tap to claim coins, XP & power-ups!" else "Streak safe! Day ${if (currentDay >= 7) 1 else currentDay + 1} unlocks tomorrow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRewardAvailable) Color(0xFFD0D7DE) else Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isRewardAvailable) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillRushCoinGold,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("claim_daily_reward_banner_button")
                ) {
                    Text(
                        text = "CLAIM",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E))
                ) {
                    Text(
                        text = "CALENDAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SkillRushTitleSection(
    streakDays: Int = 0,
    bestScore: Int = 0
) {
    val formattedScore = remember(bestScore) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(bestScore)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SKILLRUSH",
            style = MaterialTheme.typography.displayLarge,
            color = SkillRushPrimary,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1.5).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Streak & Best Score Row with divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Fire",
                        tint = SkillRushStreakFire,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (streakDays == 1) "1 Day" else "$streakDays Days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            // Best Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "BEST SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = formattedScore,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DailyChallengeCard(
    dailyInfo: com.example.data.DailyChallengeInfo,
    onStartChallenge: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_challenge_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.surfaceVariant else SkillRushPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = if (dailyInfo.isCompleted) BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.3f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Background decorative circle accent
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (dailyInfo.isCompleted) SkillRushPrimary.copy(alpha = 0.2f) else SkillRushPrimaryContainer
                    ) {
                        Text(
                            text = if (dailyInfo.isCompleted) "DAILY COMPLETED" else "DAILY CHALLENGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyInfo.isCompleted) SkillRushPrimary else SkillRushOnPrimaryContainer,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Countdown Timer Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Reset Timer",
                                tint = if (dailyInfo.isCompleted) SkillRushPrimary else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Resets in ${dailyInfo.timeRemainingFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = dailyInfo.gameTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.onSurface else Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (dailyInfo.isCompleted) 
                        "Completed! Score: ${dailyInfo.lastScore} • Earned +${dailyInfo.bonusXp} XP & +${dailyInfo.bonusCoins} Coins bonus."
                    else 
                        "${dailyInfo.description} Earn +${dailyInfo.bonusXp} XP & +${dailyInfo.bonusCoins} Coins daily bonus!",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else SkillRushPrimaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SkillRushCoinGold.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = SkillRushCoinGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "+${dailyInfo.bonusCoins} Coins",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = SkillRushCoinGold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SkillRushPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "+${dailyInfo.bonusXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (dailyInfo.isCompleted) SkillRushPrimary else Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onStartChallenge,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                            contentColor = if (dailyInfo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else SkillRushPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("start_challenge_button")
                    ) {
                        Icon(
                            imageVector = if (dailyInfo.isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (dailyInfo.isCompleted) "Completed" else "Start Task",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayNowCenterpiece(
    onPlayNow: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Large circular CTA button with thick border ring & strong shadow
        Surface(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = SkillRushPrimary.copy(alpha = 0.4f),
                    spotColor = SkillRushPrimary
                )
                .border(10.dp, SkillRushPrimaryContainer, CircleShape)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onPlayNow() }
                .testTag("play_now_button"),
            color = SkillRushPrimary,
            shape = CircleShape
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Icon",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PLAY NOW",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun GameCategoriesSection(
    onSelectCategory: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GameCategoryCard(
                name = "Reflex",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory("tap_rush") }
            )
            GameCategoryCard(
                name = "Memory",
                icon = Icons.Default.Psychology,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory("memory") }
            )
            GameCategoryCard(
                name = "Logic",
                icon = Icons.Default.Extension,
                modifier = Modifier.weight(1f),
                onClick = { onSelectCategory("logic") }
            )
        }
    }
}

@Composable
fun GameCategoryCard(
    name: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable { onClick() }
            .testTag("category_card_${name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = SkillRushPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
fun LiveTournamentsHomeCard(
    onOpenTournaments: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTournaments() }
            .testTag("live_tournaments_home_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SkillRushCoinGold.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SkillRushCoinGold.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "GLOBAL TOURNAMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillRushCoinGold,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "LIVE NOW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Daily & Weekly Arena Clashes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Compete against players globally in Tap Rush, Memory Flash & Perfect Aim for virtual coin pools!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SkillRushPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "25,000 Coins Prize Pool",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SkillRushOnPrimaryContainer,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onOpenTournaments,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("enter_tournaments_home_button")
                ) {
                    Text(
                        text = "Enter Arena",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkillRushBottomNavigation(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onItemSelected(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = {
                Text(
                    text = "Home",
                    fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkillRushPrimary,
                selectedTextColor = SkillRushPrimary,
                indicatorColor = SkillRushPrimaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onItemSelected(1) },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Games") },
            label = {
                Text(
                    text = "Games",
                    fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkillRushPrimary,
                selectedTextColor = SkillRushPrimary,
                indicatorColor = SkillRushPrimaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_games")
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { onItemSelected(2) },
            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Rank") },
            label = {
                Text(
                    text = "Rank",
                    fontWeight = if (selectedIndex == 2) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkillRushPrimary,
                selectedTextColor = SkillRushPrimary,
                indicatorColor = SkillRushPrimaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_rank")
        )
        NavigationBarItem(
            selected = selectedIndex == 3,
            onClick = { onItemSelected(3) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = {
                Text(
                    text = "Profile",
                    fontWeight = if (selectedIndex == 3) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkillRushPrimary,
                selectedTextColor = SkillRushPrimary,
                indicatorColor = SkillRushPrimaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}
