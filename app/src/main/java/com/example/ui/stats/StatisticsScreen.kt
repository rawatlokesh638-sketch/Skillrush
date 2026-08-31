package com.example.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.SkillRushBottomNavigation
import com.example.data.stats.DailyActivityItem
import com.example.data.stats.GameStatItem
import com.example.data.stats.OverallStats
import com.example.data.stats.StatsManager
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToNavTab: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedNavIndex by remember { mutableIntStateOf(3) }

    val overallStats by remember(context) { mutableStateOf(StatsManager.getOverallStats(context)) }
    val gameStatsList by remember(context) { mutableStateOf(StatsManager.getAllGameStats(context)) }
    val activityHistory by remember(context) { mutableStateOf(StatsManager.getDailyActivityHistory(context, 7)) }

    var selectedGameIdFilter by remember { mutableStateOf<String?>(null) } // null = All Games

    val formattedScoreSum = remember(overallStats.totalScoreSum) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(overallStats.totalScoreSum)
    }

    val formattedPlayTime = remember(overallStats.totalPlayTimeSeconds) {
        val totalSec = overallStats.totalPlayTimeSeconds
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        if (hours > 0) {
            "${hours}h ${mins}m"
        } else if (mins > 0) {
            "${mins}m ${secs}s"
        } else {
            "${secs}s"
        }
    }

    val filteredGames = remember(selectedGameIdFilter, gameStatsList) {
        if (selectedGameIdFilter == null) {
            gameStatsList
        } else {
            gameStatsList.filter { it.gameId == selectedGameIdFilter }
        }
    }

    Scaffold(
        modifier = Modifier.testTag("statistics_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CAREER STATS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Performance analytics & progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SkillRushPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = SkillRushPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = SkillRushPrimary
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
            SkillRushBottomNavigation(
                selectedIndex = selectedNavIndex,
                onItemSelected = { index ->
                    selectedNavIndex = index
                    onNavigateToNavTab(index)
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
            // Check for empty / new player state
            if (overallStats.totalGamesPlayed == 0) {
                item {
                    EmptyStatsCard(onPlayNow = onNavigateToGames)
                }
            } else {
                // Overall Highlights Hero Card
                item {
                    OverallStatsHeroCard(
                        overallStats = overallStats,
                        formattedScoreSum = formattedScoreSum,
                        formattedPlayTime = formattedPlayTime
                    )
                }

                // 2x2 Core Performance Metrics Grid
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CAREER PERFORMANCE METRICS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricTile(
                                title = "Highest Score",
                                value = "${overallStats.highestScore}",
                                subtitle = "Single Game Record",
                                icon = Icons.Default.EmojiEvents,
                                iconColor = SkillRushCoinGold,
                                modifier = Modifier.weight(1f)
                            )
                            StatMetricTile(
                                title = "Average Score",
                                value = "%.1f".format(overallStats.averageScore),
                                subtitle = "Per Completed Round",
                                icon = Icons.Default.ShowChart,
                                iconColor = SkillRushPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricTile(
                                title = "Overall Accuracy",
                                value = "%.1f%%".format(overallStats.overallAccuracy),
                                subtitle = "Accuracy Precision",
                                icon = Icons.Default.AdsClick,
                                iconColor = Color(0xFF00E676),
                                modifier = Modifier.weight(1f)
                            )
                            StatMetricTile(
                                title = "Best Streak / Combo",
                                value = "${overallStats.bestCombo}x",
                                subtitle = "Highest Hit Streak",
                                icon = Icons.Default.Bolt,
                                iconColor = SkillRushStreakFire,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Daily Activity Chart (Last 7 Days)
                item {
                    DailyActivityCard(activityHistory = activityHistory)
                }

                // Game-Specific Statistics Breakdown Header & Filter Tabs
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MINI-GAME BREAKDOWN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${gameStatsList.size} Micro-Games",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Filter Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedGameIdFilter == null,
                                    onClick = { selectedGameIdFilter = null },
                                    label = { Text("All Games") },
                                    modifier = Modifier.testTag("stats_game_tab_all")
                                )
                            }
                            items(gameStatsList) { game ->
                                FilterChip(
                                    selected = selectedGameIdFilter == game.gameId,
                                    onClick = { selectedGameIdFilter = game.gameId },
                                    label = { Text(game.title) },
                                    modifier = Modifier.testTag("stats_game_tab_${game.gameId}")
                                )
                            }
                        }
                    }
                }

                // Game-Specific Detail Cards
                items(filteredGames) { game ->
                    GameStatCard(game = game)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OverallStatsHeroCard(
    overallStats: OverallStats,
    formattedScoreSum: String,
    formattedPlayTime: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_overall_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SkillRushPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = SkillRushPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "TOTAL PLAY TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedPlayTime,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkillRushCoinGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LIFETIME XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SkillRushCoinGold,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${overallStats.totalXpEarned} XP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = SkillRushCoinGold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatSubItem(
                    label = "Games Played",
                    value = "${overallStats.totalGamesPlayed}",
                    icon = Icons.Default.SportsEsports
                )
                HeroStatSubItem(
                    label = "Completed",
                    value = "${overallStats.totalGamesCompleted}",
                    icon = Icons.Default.CheckCircle
                )
                HeroStatSubItem(
                    label = "Total Score",
                    value = formattedScoreSum,
                    icon = Icons.Default.Score
                )
            }
        }
    }
}

@Composable
private fun HeroStatSubItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SkillRushPrimary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun StatMetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DailyActivityCard(activityHistory: List<DailyActivityItem>) {
    val maxGamesInDay = remember(activityHistory) {
        activityHistory.maxOfOrNull { it.gamesPlayed }?.coerceAtLeast(1) ?: 1
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-DAY ACTIVITY HISTORY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SkillRushPrimary
                    )
                    Text(
                        text = "Daily games completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SkillRushPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "LAST 7 DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = SkillRushPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                activityHistory.forEach { item ->
                    val heightFraction = (item.gamesPlayed.toFloat() / maxGamesInDay).coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${item.gamesPlayed}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (item.isToday) SkillRushPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (item.isToday) {
                                        Brush.verticalGradient(listOf(SkillRushPrimary, SkillRushPrimary.copy(alpha = 0.6f)))
                                    } else if (item.gamesPlayed > 0) {
                                        Brush.verticalGradient(listOf(SkillRushCoinGold, SkillRushCoinGold.copy(alpha = 0.5f)))
                                    } else {
                                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.dayLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (item.isToday) FontWeight.Black else FontWeight.Normal,
                            color = if (item.isToday) SkillRushPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStatCard(game: GameStatItem) {
    val gameIcon = when (game.gameId) {
        "tap_rush" -> Icons.Default.TouchApp
        "memory_flash" -> Icons.Default.Psychology
        "perfect_aim" -> Icons.Default.AdsClick
        "number_sprint" -> Icons.Default.Calculate
        "spot_difference" -> Icons.Default.FindInPage
        "speed_rush" -> Icons.Default.Speed
        else -> Icons.Default.SportsEsports
    }

    val iconColor = when (game.gameId) {
        "tap_rush" -> SkillRushPrimary
        "memory_flash" -> Color(0xFF3D5AFE)
        "perfect_aim" -> Color(0xFFFF9100)
        "number_sprint" -> Color(0xFF00E676)
        "spot_difference" -> Color(0xFF00E5FF)
        "speed_rush" -> Color(0xFFD500F9)
        else -> SkillRushPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_stat_card_${game.gameId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, iconColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = gameIcon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${game.gamesPlayed} Games Played",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "BEST: ${game.bestScore}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = iconColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GameSubStatTile(
                    title = "Avg Score",
                    value = "%.1f".format(game.averageScore),
                    modifier = Modifier.weight(1f)
                )
                GameSubStatTile(
                    title = "Accuracy",
                    value = "%.1f%%".format(game.bestAccuracy),
                    modifier = Modifier.weight(1f)
                )
                GameSubStatTile(
                    title = "Best Combo",
                    value = "${game.bestCombo}x",
                    modifier = Modifier.weight(1f)
                )
                GameSubStatTile(
                    title = "Total Score",
                    value = "${game.totalScore}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GameSubStatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun EmptyStatsCard(onPlayNow: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_empty_state_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SkillRushPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "Empty Stats",
                        tint = SkillRushPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = "NO GAME STATISTICS YET",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Complete your first reaction micro-challenge to build your career statistics, track performance accuracy, and monitor daily progress!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onPlayNow,
                colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_play_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START PLAYING NOW",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
