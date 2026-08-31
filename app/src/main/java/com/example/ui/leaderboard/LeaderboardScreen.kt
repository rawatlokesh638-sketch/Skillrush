package com.example.ui.leaderboard

import com.example.ui.components.AdMobBannerView

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SkillRushBottomNavigation
import com.example.data.leaderboard.LeaderboardEntry
import com.example.data.leaderboard.LeaderboardRepository
import com.example.data.leaderboard.LeaderboardTimeframe
import com.example.data.leaderboard.LeaderboardUiState
import com.example.data.leaderboard.LocalLeaderboardRepository
import com.example.ui.theme.DifficultyEasy
import com.example.ui.theme.DifficultyHard
import com.example.ui.theme.DifficultyMedium
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushOnPrimaryContainer
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushPrimaryContainer
import com.example.ui.theme.SkillRushStreakFire
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToNavTab: (Int) -> Unit = {},
    repository: LeaderboardRepository? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val leaderboardRepo = remember(context, repository) {
        repository ?: com.example.data.leaderboard.OnlineLeaderboardRepository(context, com.example.data.leaderboard.LocalLeaderboardRepository(context))
    }

    var selectedTimeframe by remember { mutableStateOf(LeaderboardTimeframe.DAILY) }
    var selectedCategory by remember { mutableStateOf(com.example.data.leaderboard.LeaderboardGameCategory.GLOBAL) }
    var uiState by remember { mutableStateOf<LeaderboardUiState>(LeaderboardUiState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun loadData(timeframe: LeaderboardTimeframe, category: com.example.data.leaderboard.LeaderboardGameCategory, showFullLoading: Boolean = true) {
        coroutineScope.launch {
            if (showFullLoading) {
                uiState = LeaderboardUiState.Loading
            } else {
                isRefreshing = true
            }
            try {
                val entries = leaderboardRepo.getLeaderboard(timeframe, category)
                if (entries.isEmpty()) {
                    uiState = LeaderboardUiState.Empty
                } else {
                    val userEntry = entries.firstOrNull { it.isCurrentPlayer }
                    uiState = LeaderboardUiState.Success(
                        entries = entries,
                        currentUserEntry = userEntry,
                        timeframe = timeframe
                    )
                }
            } catch (e: Exception) {
                uiState = LeaderboardUiState.Error(e.message ?: "Failed to sync global rankings")
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(selectedTimeframe, selectedCategory) {
        loadData(selectedTimeframe, selectedCategory, showFullLoading = true)
    }

    // Refresh button rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LEADERBOARD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = SkillRushCoinGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                                    text = "GLOBAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkillRushCoinGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("leaderboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadData(selectedTimeframe, selectedCategory, showFullLoading = false) },
                        modifier = Modifier
                            .testTag("leaderboard_refresh_button")
                            .rotate(if (isRefreshing) rotationAnim else 0f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Leaderboard",
                            tint = SkillRushPrimary
                        )
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
                selectedIndex = 2,
                onItemSelected = { index ->
                    if (index == 0) {
                        onNavigateBack()
                    } else {
                        onNavigateToNavTab(index)
                    }
                }
            )
            }
        },
        modifier = Modifier.testTag("leaderboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Timeframe Tabs Bar
            LeaderboardTabsBar(
                selectedTimeframe = selectedTimeframe,
                onSelectTimeframe = { selectedTimeframe = it }
            )
            LeaderboardCategoryTabsBar(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it }
            )

            // Content based on UI State
            when (val state = uiState) {
                is LeaderboardUiState.Loading -> {
                    LeaderboardLoadingView()
                }
                is LeaderboardUiState.Error -> {
                    LeaderboardErrorView(
                        message = state.message,
                        onRetry = { loadData(selectedTimeframe, selectedCategory, showFullLoading = true) }
                    )
                }
                is LeaderboardUiState.Empty -> {
                    LeaderboardEmptyView(
                        onPlayGame = onNavigateToGames
                    )
                }
                is LeaderboardUiState.Success -> {
                    LeaderboardSuccessContent(
                        state = state,
                        onPlayMore = onNavigateToGames
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardTabsBar(
    selectedTimeframe: LeaderboardTimeframe,
    onSelectTimeframe: (LeaderboardTimeframe) -> Unit
) {
    val timeframes = LeaderboardTimeframe.values()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeframes.forEach { tf ->
                val isSelected = selectedTimeframe == tf
                val tag = when (tf) {
                    LeaderboardTimeframe.DAILY -> "leaderboard_tab_daily"
                    LeaderboardTimeframe.WEEKLY -> "leaderboard_tab_weekly"
                    LeaderboardTimeframe.ALL_TIME -> "leaderboard_tab_all_time"
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTimeframe(tf) }
                        .testTag(tag),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SkillRushPrimary else Color.Transparent,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardSuccessContent(
    state: LeaderboardUiState.Success,
    onPlayMore: () -> Unit
) {
    val top3 = state.entries.take(3)
    val remainingEntries = state.entries.drop(3)
    val userEntry = state.currentUserEntry

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("leaderboard_list"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Timeframe description pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = state.timeframe.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Top 3 Podium
            if (top3.isNotEmpty()) {
                item {
                    Top3PodiumView(
                        top3 = top3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("leaderboard_top3_podium")
                    )
                }
            }

            // Section Divider
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GLOBAL CONTENDERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TOTAL SCORES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Ladder List
            items(remainingEntries, key = { it.userId }) { entry ->
                LeaderboardRowItem(entry = entry)
            }
        }

        // Persistent Sticky Bottom User Banner
        if (userEntry != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("leaderboard_current_user_banner"),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.5.dp, SkillRushPrimary),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Rank & User Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SkillRushPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${userEntry.rank}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = userEntry.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SkillRushPrimaryContainer
                                ) {
                                    Text(
                                        text = "YOU",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkillRushOnPrimaryContainer,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Lvl ${userEntry.level} • ${userEntry.badge} • UID: ${if (userEntry.userId.length > 10) userEntry.userId.take(8) + ".." else userEntry.userId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Right: Score & Status
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${NumberFormat.getNumberInstance(Locale.getDefault()).format(userEntry.score)} pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = SkillRushCoinGold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = DifficultyEasy,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Live standing",
                                style = MaterialTheme.typography.labelSmall,
                                color = DifficultyEasy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Top3PodiumView(
    top3: List<LeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    val first = top3.getOrNull(0)
    val second = top3.getOrNull(1)
    val third = top3.getOrNull(2)

    Row(
        modifier = modifier.height(230.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Left)
        if (second != null) {
            PodiumColumn(
                entry = second,
                rank = 2,
                podiumHeight = 120.dp,
                podiumColor = Color(0xFF94A3B8),
                crownIcon = Icons.Default.MilitaryTech,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1st Place (Center - Tallest)
        if (first != null) {
            PodiumColumn(
                entry = first,
                rank = 1,
                podiumHeight = 150.dp,
                podiumColor = SkillRushCoinGold,
                crownIcon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1.15f)
            )
        }

        // 3rd Place (Right)
        if (third != null) {
            PodiumColumn(
                entry = third,
                rank = 3,
                podiumHeight = 95.dp,
                podiumColor = Color(0xFFCD7F32),
                crownIcon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PodiumColumn(
    entry: LeaderboardEntry,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    podiumColor: Color,
    crownIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Avatar + Crown
        Box(
            modifier = Modifier.padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(entry.avatarColorHex).copy(alpha = 0.25f),
                border = BorderStroke(2.dp, podiumColor),
                modifier = Modifier.size(if (rank == 1) 56.dp else 46.dp),
                shadowElevation = if (rank == 1) 8.dp else 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = podiumColor,
                        modifier = Modifier.size(if (rank == 1) 32.dp else 26.dp)
                    )
                }
            }

            // Trophy / Crown badge
            Surface(
                shape = CircleShape,
                color = podiumColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = crownIcon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Name & Score Label
        Text(
            text = if (entry.isCurrentPlayer) "YOU" else entry.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (entry.isCurrentPlayer) SkillRushPrimary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = NumberFormat.getNumberInstance(Locale.getDefault()).format(entry.score),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = podiumColor,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pedestal block
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.9f),
            border = BorderStroke(1.5.dp, podiumColor.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = podiumColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = podiumColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = entry.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LeaderboardRowItem(
    entry: LeaderboardEntry
) {
    val isUser = entry.isCurrentPlayer
    val borderStroke = if (isUser) {
        BorderStroke(1.5.dp, SkillRushPrimary)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    val cardBg = if (isUser) {
        SkillRushPrimaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank + Avatar + Name Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Rank number
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = if (entry.rank <= 10) SkillRushCoinGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(34.dp)
                )

                // Avatar Icon
                Surface(
                    shape = CircleShape,
                    color = Color(entry.avatarColorHex).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(entry.avatarColorHex).copy(alpha = 0.5f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(entry.avatarColorHex),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUser) "${entry.name} (You)" else entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) SkillRushPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lvl ${entry.level}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entry.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (entry.userId.length > 10) "UID:${entry.userId.take(8)}.." else "UID:${entry.userId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillRushCoinGold.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Score & Streak
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.getDefault()).format(entry.score)} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (entry.streakDays > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = SkillRushStreakFire,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${entry.streakDays}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillRushStreakFire,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leaderboard_loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                color = SkillRushPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Syncing Global Standings...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LeaderboardErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("leaderboard_error_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = DifficultyHard,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Connection Notice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Connection")
            }
        }
    }
}

@Composable
fun LeaderboardEmptyView(
    onPlayGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("leaderboard_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = null,
                tint = SkillRushCoinGold,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "No Rankings Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Play games in the Arena to establish your global skill score and climb the leaderboards!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onPlayGame,
                colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("ENTER ARENA", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LeaderboardCategoryTabsBar(
    selectedCategory: com.example.data.leaderboard.LeaderboardGameCategory,
    onSelectCategory: (com.example.data.leaderboard.LeaderboardGameCategory) -> Unit
) {
    val categories = com.example.data.leaderboard.LeaderboardGameCategory.values()
    androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = categories.indexOf(selectedCategory),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        edgePadding = 16.dp,
        divider = {}
    ) {
        categories.forEachIndexed { index, category ->
            androidx.compose.material3.Tab(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                text = { androidx.compose.material3.Text(category.label) }
            )
        }
    }
}
