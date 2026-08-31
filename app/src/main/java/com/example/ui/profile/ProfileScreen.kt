package com.example.ui.profile

import android.app.Activity
import com.example.data.AdManager
import com.example.data.RewardManager
import com.example.ui.components.AdMobBannerView

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.SkillRushBottomNavigation
import com.example.data.LevelProgress
import com.example.data.StreakManager
import com.example.data.UserProfileManager
import com.example.ui.components.AchievementsDialog
import com.example.ui.components.AchievementsSummaryCard
import com.example.ui.components.LevelBadge
import com.example.ui.components.StreakCalendarCard
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class TierMilestone(
    val levelRequired: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tierColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToNavTab: (Int) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedNavIndex by remember { mutableIntStateOf(3) }

    var totalXp by remember { mutableIntStateOf(UserProfileManager.getTotalXp(context)) }
    var levelProgress by remember { mutableStateOf(UserProfileManager.getLevelProgress(context)) }
    var coins by remember { mutableIntStateOf(UserProfileManager.getCoins(context)) }
    var streakDays by remember { mutableIntStateOf(UserProfileManager.getStreakDays(context)) }
    var playerName by remember { mutableStateOf(UserProfileManager.getPlayerName(context)) }
    var totalGames by remember { mutableIntStateOf(UserProfileManager.getTotalGamesPlayed(context)) }
    var bestScore by remember { mutableIntStateOf(UserProfileManager.getHighestOverallScore(context)) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(playerName) }

    val formattedCoins = remember(coins) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(coins)
    }
    val formattedTotalXp = remember(totalXp) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(totalXp)
    }

    val milestones = remember {
        listOf(
            TierMilestone(1, "Novice Reflexer", "Start your journey in the reaction arena", Icons.Default.SportsEsports, SkillRushPrimary),
            TierMilestone(3, "Speed Apprentice", "Master quick hand-eye coordination", Icons.Default.Bolt, Color(0xFF3D5AFE)),
            TierMilestone(6, "Skill Challenger", "Ascend through competitive micro-challenges", Icons.Default.MilitaryTech, Color(0xFFFF9100)),
            TierMilestone(10, "Precision Expert", "Achieve exceptional accuracy and focus", Icons.Default.AdsClick, Color(0xFF00E676)),
            TierMilestone(15, "Focus Master", "Dominate high-speed reflex trials", Icons.Default.Psychology, Color(0xFF00E5FF)),
            TierMilestone(20, "Grandmaster", "Elite status across all reaction arenas", Icons.Default.WorkspacePremium, Color(0xFFD500F9)),
            TierMilestone(25, "Apex Titan", "Legendary pinnacle of arcade mastery", Icons.Default.EmojiEvents, Color(0xFFFFD700))
        )
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
                            text = "PLAYER PROFILE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = SkillRushPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
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
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("profile_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Coins counter pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SkillRushCoinGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = SkillRushCoinGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formattedCoins,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = SkillRushCoinGold
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
            // Profile Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_header_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar + Level Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(80.dp)
                                    .shadow(8.dp, CircleShape),
                                shape = CircleShape,
                                color = SkillRushPrimaryContainer,
                                border = BorderStroke(2.5.dp, SkillRushPrimary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = SkillRushPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            LevelBadge(
                                level = levelProgress.level,
                                size = 32.dp,
                                modifier = Modifier.offset(x = 4.dp, y = 4.dp)
                            )
                        }

                        // Name + Edit Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                editedName = playerName
                                showEditNameDialog = true
                            }
                        ) {
                            Text(
                                text = playerName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Current Level Title Badge & UID Chip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SkillRushPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "RANK: ${levelProgress.levelTitle.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = SkillRushPrimary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SkillRushCoinGold.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = SkillRushCoinGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    val userUid = UserProfileManager.getUserUid(context)
                                    Text(
                                        text = if (userUid.length > 12) "UID: ${userUid.take(10)}..." else "UID: $userUid",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SkillRushCoinGold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Streak Calendar Card
            item {
                StreakCalendarCard(streakInfo = StreakManager.getStreakInfo(context))
            }

            // Achievements Summary Card
            item {
                AchievementsSummaryCard(onViewAll = { showAchievementsDialog = true })
            }

            // Level & XP Progression Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_xp_progression_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL ${levelProgress.level} PROGRESSION",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = SkillRushPrimary
                                )
                                Text(
                                    text = "Lifetime XP: $formattedTotalXp XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SkillRushCoinGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${levelProgress.xpRemainingForNextLevel} XP to Lvl ${levelProgress.level + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = SkillRushCoinGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Detailed XP Bar
                        XpProgressBar(
                            currentXp = levelProgress.currentLevelXp,
                            requiredXp = levelProgress.xpRequiredForNextLevel,
                            progressFraction = levelProgress.progressFraction,
                            height = 14.dp
                        )

                        // Next Milestone Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current: Level ${levelProgress.level} (${levelProgress.levelTitle})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Next: Level ${levelProgress.level + 1} (${levelProgress.nextLevelTitle})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SkillRushPrimary
                            )
                        }
                    }
                }
            }

            // Detailed Statistics Action Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStats() }
                        .testTag("profile_view_statistics_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, SkillRushPrimary.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SkillRushPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "Statistics",
                                    tint = SkillRushPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DETAILED STATISTICS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View accuracy, play time & game breakdowns",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SkillRushPrimary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "VIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stats Matrix (2x2 Grid)
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
                            text = "PLAYER CAREER STATS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap to view analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkillRushPrimary,
                            modifier = Modifier.clickable { onNavigateToStats() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            title = "Games Played",
                            value = "$totalGames",
                            icon = Icons.Default.SportsEsports,
                            iconColor = SkillRushPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            title = "Daily Streak",
                            value = "$streakDays Days",
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = SkillRushStreakFire,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            title = "Highest Score",
                            value = "$bestScore",
                            icon = Icons.Default.EmojiEvents,
                            iconColor = SkillRushCoinGold,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            title = "Coins Vault",
                            value = "$coins",
                            icon = Icons.Default.MonetizationOn,
                            iconColor = SkillRushCoinGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                val activity = context as? Activity
                val isRewardActive by RewardManager.isRewardOperationActive.collectAsState()
                
                Button(
                    onClick = {
                        activity?.let { act ->
                            RewardManager.processRewardedAd(
                                activity = act,
                                onSuccess = { amount ->
                                    coins = UserProfileManager.getCoins(context)
                                },
                                onFailure = { _ -> }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !isRewardActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillRushCoinGold,
                        contentColor = Color.White,
                        disabledContainerColor = SkillRushCoinGold.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRewardActive) "Processing..." else "Watch Ad for Free Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Level Progression Tier Roadmap
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tier_roadmap_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIER ROADMAP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Progressive Milestones",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    milestones.forEach { milestone ->
                        val isUnlocked = levelProgress.level >= milestone.levelRequired
                        val isCurrent = levelProgress.level in milestone.levelRequired until (milestone.levelRequired + 3)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    milestone.tierColor.copy(alpha = 0.12f)
                                } else if (isUnlocked) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                }
                            ),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) milestone.tierColor else if (isUnlocked) milestone.tierColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUnlocked) milestone.tierColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.5.dp, if (isUnlocked) milestone.tierColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = milestone.icon,
                                            contentDescription = null,
                                            tint = if (isUnlocked) milestone.tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isCurrent) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = milestone.tierColor
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = milestone.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (isUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Unlocked",
                                            tint = DifficultyEasy,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "LVL ${milestone.levelRequired}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Name Edit Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "EDIT PLAYER NAME",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = SkillRushPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { if (it.length <= 16) editedName = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editedName.trim().ifEmpty { "Player 1" }
                        UserProfileManager.setPlayerName(context, clean)
                        playerName = clean
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                    modifier = Modifier.testTag("save_name_button")
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showAchievementsDialog) {
        AchievementsDialog(onDismiss = { showAchievementsDialog = false })
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
