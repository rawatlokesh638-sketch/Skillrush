package com.example.ui.tournament

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tournament.TournamentRepository
import com.example.model.Tournament
import com.example.model.TournamentEntry
import com.example.model.TournamentStatus
import com.example.model.TournamentType
import com.example.ui.components.LevelBadge
import com.example.ui.theme.*
import com.example.util.rememberIsOnline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    onNavigateBack: () -> Unit,
    onStartTournamentGame: (gameId: String, tournamentId: String, sessionToken: String) -> Unit,
    onNavigateToNavTab: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { TournamentRepository(context) }
    val isOnlineState = rememberIsOnline()
    val isOnline = isOnlineState.value

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Daily, 2: Weekly, 3: Completed
    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Selected tournament for leaderboard modal sheet
    var selectedTournamentForLeaderboard by remember { mutableStateOf<Tournament?>(null) }

    // Ticking timer clock for live countdowns
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // Load tournaments
    fun refreshTournaments() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                tournaments = repository.getAvailableTournaments()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load tournaments"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(isOnline) {
        refreshTournaments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TOURNAMENTS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = SkillRushPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            // Live Online Status Pill
                            Surface(
                                shape = CircleShape,
                                color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                                    )
                                    Text(
                                        text = if (isOnline) "LIVE ONLINE" else "OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) Color(0xFF047857) else Color(0xFFB91C1C),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Compete globally for virtual coins & glory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tournaments_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SkillRushPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTournaments() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SkillRushPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            com.example.SkillRushBottomNavigation(
                selectedIndex = 0,
                onItemSelected = { index -> onNavigateToNavTab(index) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = SkillRushPrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                val tabs = listOf("ALL TOURNAMENTS", "DAILY", "WEEKLY", "COMPLETED")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // Filtered Tournaments List
            val filteredTournaments = remember(tournaments, selectedTabIndex, currentTimeMs) {
                tournaments.filter { tournament ->
                    val isExpired = currentTimeMs > tournament.endTimeMs
                    when (selectedTabIndex) {
                        1 -> tournament.type == TournamentType.DAILY && !isExpired
                        2 -> tournament.type == TournamentType.WEEKLY && !isExpired
                        3 -> isExpired || tournament.status == TournamentStatus.EXPIRED
                        else -> true
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = SkillRushPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading live tournaments...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Error connecting",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { refreshTournaments() },
                            colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
                        ) {
                            Text("Retry Connection")
                        }
                    }
                } else if (filteredTournaments.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tournaments in this section",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(filteredTournaments, key = { it.id }) { tournament ->
                            TournamentCard(
                                tournament = tournament,
                                currentTimeMs = currentTimeMs,
                                isOnline = isOnline,
                                onEnterMatch = {
                                    val sessionToken = UUID.randomUUID().toString()
                                    onStartTournamentGame(tournament.gameId, tournament.id, sessionToken)
                                },
                                onViewLeaderboard = {
                                    selectedTournamentForLeaderboard = tournament
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Leaderboard Bottom Sheet
    selectedTournamentForLeaderboard?.let { tournament ->
        TournamentLeaderboardBottomSheet(
            tournament = tournament,
            repository = repository,
            isOnline = isOnline,
            currentTimeMs = currentTimeMs,
            onDismiss = { selectedTournamentForLeaderboard = null },
            onPlayMatch = {
                selectedTournamentForLeaderboard = null
                val sessionToken = UUID.randomUUID().toString()
                onStartTournamentGame(tournament.gameId, tournament.id, sessionToken)
            }
        )
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    currentTimeMs: Long,
    isOnline: Boolean,
    onEnterMatch: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    val isExpired = currentTimeMs > tournament.endTimeMs
    val timeDiffMs = (tournament.endTimeMs - currentTimeMs).coerceAtLeast(0L)

    val hours = timeDiffMs / (1000 * 60 * 60)
    val minutes = (timeDiffMs / (1000 * 60)) % 60
    val seconds = (timeDiffMs / 1000) % 60
    val timeFormatted = if (hours > 24) {
        val days = hours / 24
        "${days}d ${hours % 24}h remaining"
    } else {
        String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    val cardBg = if (isExpired) {
        MaterialTheme.colorScheme.surfaceVariant
    } else if (tournament.type == TournamentType.WEEKLY) {
        SkillRushPrimary
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            if (tournament.type == TournamentType.WEEKLY && !isExpired) SkillRushCoinGold else SkillRushPrimary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header row with Badge & Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (tournament.type == TournamentType.WEEKLY) SkillRushCoinGold else SkillRushPrimaryContainer
                ) {
                    Text(
                        text = if (tournament.type == TournamentType.WEEKLY) "🏆 WEEKLY GRAND PRIX" else "⚡ DAILY CLASH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (tournament.type == TournamentType.WEEKLY) Color.Black else SkillRushOnPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp
                    )
                }

                // Countdown Timer Pill
                Surface(
                    shape = CircleShape,
                    color = if (isExpired) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpired) Icons.Default.EventAvailable else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isExpired) MaterialTheme.colorScheme.error else if (tournament.type == TournamentType.WEEKLY) Color.White else SkillRushPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isExpired) "TOURNAMENT CLOSED" else timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) MaterialTheme.colorScheme.error else if (tournament.type == TournamentType.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Game info
            Text(
                text = tournament.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (tournament.type == TournamentType.WEEKLY && !isExpired) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = tournament.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (tournament.type == TournamentType.WEEKLY && !isExpired) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Prize Pool & Featured Game Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Virtual Prize Pool
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkillRushCoinGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Virtual Coins",
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${tournament.virtualPrizePoolCoins} Coins Pool",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SkillRushCoinGold
                        )
                    }
                }

                // Featured Game
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tournament.gameTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewLeaderboard,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (tournament.type == TournamentType.WEEKLY && !isExpired) Color.White.copy(alpha = 0.8f) else SkillRushPrimary
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (tournament.type == TournamentType.WEEKLY && !isExpired) Color.White else SkillRushPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Standings", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (!isOnline) return@Button
                        onEnterMatch()
                    },
                    enabled = !isExpired && isOnline,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .testTag("enter_tournament_button_${tournament.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tournament.type == TournamentType.WEEKLY) SkillRushCoinGold else SkillRushPrimary,
                        contentColor = if (tournament.type == TournamentType.WEEKLY) Color.Black else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isExpired) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpired) "Closed" else if (!isOnline) "Offline" else "Enter Match",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentLeaderboardBottomSheet(
    tournament: Tournament,
    repository: TournamentRepository,
    isOnline: Boolean,
    currentTimeMs: Long,
    onDismiss: () -> Unit,
    onPlayMatch: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entriesState = repository.observeTournamentLeaderboard(tournament.id).collectAsState(initial = emptyList())
    val entries = entriesState.value
    val isExpired = currentTimeMs > tournament.endTimeMs

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SkillRushPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Live Leaderboard • ${entries.size} Players",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = SkillRushPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No scores submitted yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Be the first player on the tournament leaderboard!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Top 3 Podium
                if (entries.size >= 3) {
                    TournamentPodiumSection(top3 = entries.take(3))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Leaderboard List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.userId }) { entry ->
                        TournamentLeaderboardRow(entry = entry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            if (!isExpired && isOnline) {
                Button(
                    onClick = onPlayMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("tournament_modal_play_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillRushPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY TOURNAMENT MATCH NOW", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun TournamentPodiumSection(top3: List<TournamentEntry>) {
    val first = top3.getOrNull(0)
    val second = top3.getOrNull(1)
    val third = top3.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        second?.let {
            PodiumCard(entry = it, rank = 2, height = 90.dp, color = Color(0xFFC0C0C0))
        }
        // 1st Place (Gold)
        first?.let {
            PodiumCard(entry = it, rank = 1, height = 115.dp, color = SkillRushCoinGold)
        }
        // 3rd Place (Bronze)
        third?.let {
            PodiumCard(entry = it, rank = 3, height = 75.dp, color = Color(0xFFCD7F32))
        }
    }
}

@Composable
fun PodiumCard(entry: TournamentEntry, rank: Int, height: androidx.compose.ui.unit.Dp, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(95.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
            border = BorderStroke(2.dp, color),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.Black,
                    color = color,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${entry.score} pts",
            style = MaterialTheme.typography.labelSmall,
            color = SkillRushPrimary,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun TournamentLeaderboardRow(entry: TournamentEntry) {
    val isUser = entry.isCurrentPlayer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tournament_row_${entry.userId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) SkillRushPrimaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isUser) SkillRushPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (entry.rank <= 3) SkillRushCoinGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )

                LevelBadge(level = entry.level, size = 32.dp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) SkillRushPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isUser) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SkillRushPrimary
                            ) {
                                Text(
                                    text = "YOU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = entry.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${entry.score} pts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = SkillRushPrimary
            )
        }
    }
}
