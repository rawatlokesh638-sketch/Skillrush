package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioManager
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun DailyRewardDialog(
    onDismiss: () -> Unit,
    onRewardClaimed: (DailyRewardClaimResult) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val rewardTiers = remember { DailyRewardManager.getRewardTiers() }
    val currentDay = remember { DailyRewardManager.getCurrentRewardDay(context) }
    val isAvailable = remember { DailyRewardManager.isDailyRewardAvailable(context) }

    var claimResult by remember { mutableStateOf<DailyRewardClaimResult?>(null) }
    var isClaimingAd by remember { mutableStateOf(false) }

    // Pulsing animation for the active day card
    val infiniteTransition = rememberInfiniteTransition(label = "daily_reward_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val todayTier = rewardTiers.find { it.dayNumber == currentDay } ?: rewardTiers.first()

    fun performClaim(doubleWithAd: Boolean) {
        val result = DailyRewardManager.claimDailyReward(context, doubleWithAd = doubleWithAd)
        if (result != null) {
            claimResult = result
            onRewardClaimed(result)
        }
    }

    Dialog(
        onDismissRequest = {
            if (claimResult != null) {
                onDismiss()
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp))
                    .testTag("daily_reward_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.5.dp, if (isAvailable) SkillRushCoinGold.copy(alpha = glowAlpha) else SkillRushPrimary.copy(alpha = 0.4f))
            ) {
                AnimatedContent(
                    targetState = claimResult != null,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "claim_transition"
                ) { hasClaimed ->
                    if (hasClaimed && claimResult != null) {
                        // CELEBRATION VIEW
                        RewardCelebrationContent(
                            result = claimResult!!,
                            onContinue = onDismiss
                        )
                    } else {
                        // REWARD CALENDAR & CLAIM VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Header with Close Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SkillRushCoinGold.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CardGiftcard,
                                                contentDescription = null,
                                                tint = SkillRushCoinGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "DAILY REWARD",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.White,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = if (isAvailable) "Day $currentDay Reward Ready!" else "Already Claimed Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAvailable) SkillRushCoinGold else Color(0xFF8B949E)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF8B949E)
                                    )
                                }
                            }

                            // 7-Day Reward Track
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(rewardTiers) { tier ->
                                    val isPastClaimed = tier.dayNumber < currentDay || (!isAvailable && tier.dayNumber == currentDay)
                                    val isTodayActive = tier.dayNumber == currentDay && isAvailable
                                    val isFutureLocked = tier.dayNumber > currentDay || (!isAvailable && tier.dayNumber > currentDay)

                                    DayRewardCard(
                                        tier = tier,
                                        isPastClaimed = isPastClaimed,
                                        isTodayActive = isTodayActive,
                                        isFutureLocked = isFutureLocked,
                                        scaleModifier = if (isTodayActive) Modifier.scale(pulseScale) else Modifier
                                    )
                                }
                            }

                            // Spotlight on Today's Loot
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0D1117),
                                border = BorderStroke(1.dp, if (isAvailable) SkillRushPrimary.copy(alpha = 0.5f) else Color(0xFF30363D))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TODAY'S BUNDLE • DAY $currentDay",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SkillRushPrimary,
                                        letterSpacing = 0.5.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Coins Item
                                        RewardLootPill(
                                            icon = Icons.Default.MonetizationOn,
                                            iconColor = SkillRushCoinGold,
                                            value = "+${todayTier.coins}",
                                            label = "COINS"
                                        )

                                        // XP Item
                                        RewardLootPill(
                                            icon = Icons.Default.Bolt,
                                            iconColor = SkillRushPrimary,
                                            value = "+${todayTier.xp}",
                                            label = "XP"
                                        )

                                        // Power-up Item if available
                                        if (todayTier.powerUp != null) {
                                            RewardLootPill(
                                                icon = getPowerUpIcon(todayTier.powerUp.type),
                                                iconColor = getPowerUpColor(todayTier.powerUp.type),
                                                value = "x${todayTier.powerUp.count}",
                                                label = todayTier.powerUp.type.displayName.uppercase()
                                            )
                                        }
                                    }
                                }
                            }

                            // Claim Action Buttons
                            if (isAvailable) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 2X Ad Multiplier Button
                                    Button(
                                        onClick = {
                                            if (activity != null) {
                                                isClaimingAd = true
                                                RewardManager.processRewardedAd(
                                                    activity = activity,
                                                    rewardType = "daily_reward_2x",
                                                    onSuccess = {
                                                        isClaimingAd = false
                                                        performClaim(doubleWithAd = true)
                                                    },
                                                    onFailure = {
                                                        isClaimingAd = false
                                                        // Fallback to normal claim if ad failed
                                                        performClaim(doubleWithAd = false)
                                                    }
                                                )
                                            } else {
                                                performClaim(doubleWithAd = false)
                                            }
                                        },
                                        enabled = !isClaimingAd,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("claim_2x_daily_reward_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SkillRushCoinGold,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        if (isClaimingAd) {
                                            CircularProgressIndicator(
                                                color = Color.Black,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "LOADING AD...",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CLAIM 2X REWARD (WATCH AD)",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    // Normal Claim Button
                                    OutlinedButton(
                                        onClick = {
                                            AudioManager.playTapSound(context)
                                            performClaim(doubleWithAd = false)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("claim_normal_daily_reward_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, SkillRushPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = SkillRushPrimary
                                        )
                                    ) {
                                        Text(
                                            text = "CLAIM REWARD (1X)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                // Already Claimed Today banner
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF21262D),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Come back tomorrow for Day ${if (currentDay >= 7) 1 else currentDay + 1} rewards!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRewardCard(
    tier: DailyRewardTier,
    isPastClaimed: Boolean,
    isTodayActive: Boolean,
    isFutureLocked: Boolean,
    scaleModifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isTodayActive -> Color(0xFF21262D)
        isPastClaimed -> Color(0xFF0D1117)
        else -> Color(0xFF161B22)
    }

    val borderColor = when {
        isTodayActive -> SkillRushCoinGold
        isPastClaimed -> Color(0xFF2EA043)
        tier.isJackpotDay -> SkillRushPrimary
        else -> Color(0xFF30363D)
    }

    Card(
        modifier = scaleModifier
            .width(62.dp)
            .height(98.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isTodayActive) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day Label
            Text(
                text = "DAY ${tier.dayNumber}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isTodayActive) SkillRushCoinGold else Color(0xFF8B949E)
            )

            // Center Icon
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isPastClaimed -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Claimed",
                            tint = Color(0xFF2EA043),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    tier.isJackpotDay -> {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Jackpot",
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    tier.powerUp != null -> {
                        Icon(
                            imageVector = getPowerUpIcon(tier.powerUp.type),
                            contentDescription = tier.powerUp.type.displayName,
                            tint = getPowerUpColor(tier.powerUp.type),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Reward Amount
            Text(
                text = "+${tier.coins}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPastClaimed) Color(0xFF8B949E) else Color.White
            )
        }
    }
}

@Composable
private fun RewardLootPill(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 13.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8B949E),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RewardCelebrationContent(
    result: DailyRewardClaimResult,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SkillRushCoinGold.copy(alpha = 0.2f),
            border = BorderStroke(2.dp, SkillRushCoinGold),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Success",
                    tint = SkillRushCoinGold,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (result.isDoubledWithAd) "2X REWARD CLAIMED!" else "REWARD CLAIMED!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = SkillRushCoinGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Day ${result.dayNumber} loot added to your inventory!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center
            )
        }

        // Summary of rewards gained
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.3f))
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
                    Text(
                        text = "Coins Awarded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8B949E)
                    )
                    Text(
                        text = "+${result.finalCoins}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SkillRushCoinGold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "XP Earned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8B949E)
                    )
                    Text(
                        text = "+${result.finalXp} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SkillRushPrimary
                    )
                }

                result.powerUpsGranted.forEach { powerUp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = powerUp.type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8B949E)
                        )
                        Text(
                            text = "+${powerUp.count}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = getPowerUpColor(powerUp.type)
                        )
                    }
                }

                if (result.didLevelUp) {
                    HorizontalDivider(color = Color(0xFF30363D), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "🎉 LEVEL UP! You reached Level ${result.currentLevel}!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("continue_after_daily_reward_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary)
        ) {
            Text(
                text = "CONTINUE TO ARENA",
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private fun getPowerUpIcon(type: PowerUpType): ImageVector {
    return when (type) {
        PowerUpType.TIME_FREEZE -> Icons.Default.HourglassTop
        PowerUpType.SCORE_BOOST -> Icons.Default.ElectricBolt
        PowerUpType.SHIELD -> Icons.Default.Shield
        PowerUpType.FOCUS_SURGE -> Icons.Default.CenterFocusStrong
    }
}

private fun getPowerUpColor(type: PowerUpType): Color {
    return when (type) {
        PowerUpType.TIME_FREEZE -> Color(0xFF00E5FF)
        PowerUpType.SCORE_BOOST -> SkillRushCoinGold
        PowerUpType.SHIELD -> Color(0xFF00E676)
        PowerUpType.FOCUS_SURGE -> Color(0xFFFF4081)
    }
}
