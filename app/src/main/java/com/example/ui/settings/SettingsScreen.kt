package com.example.ui.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SkillRushBottomNavigation
import com.example.data.SettingsManager
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushPrimaryContainer
import com.example.ui.theme.SkillRushOnPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNavTab: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedNavIndex by remember { mutableIntStateOf(3) } // Settings or Profile tab

    var soundEnabled by remember { mutableStateOf(SettingsManager.isSoundEnabled(context)) }
    var musicEnabled by remember { mutableStateOf(SettingsManager.isMusicEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(SettingsManager.isVibrationEnabled(context)) }
    var notificationsEnabled by remember { mutableStateOf(SettingsManager.isNotificationsEnabled(context)) }
    var reducedMotionEnabled by remember { mutableStateOf(SettingsManager.isReducedMotionEnabled(context)) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf<String?>(null) } // "privacy", "terms", "about"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
            // Preferences Section
            item {
                SettingsSectionHeader(title = "Preferences & Audio")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Sound Effects",
                            subtitle = "Play audio cues during mini-games",
                            checked = soundEnabled,
                            onCheckedChange = {
                                soundEnabled = it
                                SettingsManager.setSoundEnabled(context, it)
                                if (it) com.example.audio.AudioManager.playTapSound(context)
                            },
                            testTag = "switch_sound"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        SettingsSwitchRow(
                            icon = Icons.Default.MusicNote,
                            title = "Background Music",
                            subtitle = "Play arcade theme soundtracks",
                            checked = musicEnabled,
                            onCheckedChange = {
                                musicEnabled = it
                                SettingsManager.setMusicEnabled(context, it)
                                if (it) {
                                    com.example.audio.AudioManager.startBackgroundMusic(context)
                                } else {
                                    com.example.audio.AudioManager.stopBackgroundMusic()
                                }
                            },
                            testTag = "switch_music"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        SettingsSwitchRow(
                            icon = Icons.Default.Vibration,
                            title = "Vibration & Haptics",
                            subtitle = "Tactile feedback on taps and events",
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                SettingsManager.setVibrationEnabled(context, it)
                                if (it) com.example.audio.HapticManager.vibrateTap(context)
                            },
                            testTag = "switch_vibration"
                        )
                    }
                }
            }

            // Notifications & Accessibility
            item {
                SettingsSectionHeader(title = "Notifications & Accessibility")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        SettingsSwitchRow(
                            icon = Icons.Default.Notifications,
                            title = "Daily Reminders",
                            subtitle = "Remind me about Daily Challenges & streaks",
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                SettingsManager.setNotificationsEnabled(context, it)
                            },
                            testTag = "switch_notifications"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        SettingsSwitchRow(
                            icon = Icons.Default.Animation,
                            title = "Reduced Motion",
                            subtitle = "Minimize complex screen animations",
                            checked = reducedMotionEnabled,
                            onCheckedChange = {
                                reducedMotionEnabled = it
                                SettingsManager.setReducedMotionEnabled(context, it)
                            },
                            testTag = "switch_reduced_motion"
                        )
                    }
                }
            }

            // Data Management
            item {
                SettingsSectionHeader(title = "Data & Progress")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Reset Game Progress",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Clear all scores, levels, coins and streaks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_progress_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                text = "Reset Progress...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // About & Legal
            item {
                SettingsSectionHeader(title = "About & Legal")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            title = "About SkillRush",
                            subtitle = "Version 1.0.0 (Build 100)",
                            onClick = { showInfoDialog = "about" },
                            testTag = "about_row"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        SettingsClickableRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "Read our offline privacy terms",
                            onClick = { showInfoDialog = "privacy" },
                            testTag = "privacy_row"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        SettingsClickableRow(
                            icon = Icons.Default.Description,
                            title = "Terms of Service",
                            subtitle = "End-user license agreement",
                            onClick = { showInfoDialog = "terms" },
                            testTag = "terms_row"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset All Game Data?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all your high scores, game history, coins, XP, streaks, and achievements. This action cannot be undone. Your settings will be preserved.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.resetGameProgress(context)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset Everything", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    modifier = Modifier.testTag("cancel_reset_button")
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Info / Legal Dialogs
    if (showInfoDialog != null) {
        val (dialogTitle, dialogContent) = when (showInfoDialog) {
            "privacy" -> Pair(
                "Privacy Policy",
                "SkillRush is an offline-first arcade reflexes application. We do not collect, store, or transmit any personal data, analytics, or telemetry to external servers. All progress, scores, and preferences are stored securely and locally on your device."
            )
            "terms" -> Pair(
                "Terms of Service",
                "By using SkillRush, you agree to enjoy our fast-paced mini-games responsibly. SkillRush is provided free of charge, with optional future ad monetization via Google AdMob. All virtual coins and XP have no real-world cash value."
            )
            else -> Pair(
                "About SkillRush",
                "SkillRush v1.0.0 (Build 100)\n\nThe ultimate arcade test of reflexes, memory, precision, and speed. Crafted with Jetpack Compose and Material Design 3.\n\n© 2026 SkillRush Studios. All rights reserved."
            )
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = {
                Text(text = dialogTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = dialogContent, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = null },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = SkillRushPrimary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = SkillRushPrimaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SkillRushPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SkillRushPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = SkillRushPrimaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SkillRushPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
