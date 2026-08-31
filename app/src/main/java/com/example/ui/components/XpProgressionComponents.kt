package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LevelProgress
import com.example.data.XpBreakdown
import com.example.ui.theme.DifficultyEasy
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushOnPrimaryContainer
import com.example.ui.theme.SkillRushPrimary
import com.example.ui.theme.SkillRushPrimaryContainer
import com.example.ui.theme.SkillRushStreakFire
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated, modern Material 3 XP Progress Bar showing current/required XP.
 */
@Composable
fun XpProgressBar(
    currentXp: Int,
    requiredXp: Int,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    showLabel: Boolean = true,
    animated: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = if (animated) tween(durationMillis = 800, easing = FastOutSlowInEasing) else spring(),
        label = "xp_progress_anim"
    )

    Column(
        modifier = modifier.testTag("xp_progress_bar_container"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SkillRushCoinGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "XP PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "$currentXp / $requiredXp XP (${(progressFraction * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SkillRushPrimary,
                    fontSize = 11.sp
                )
            }
        }

        // Custom Gradient Track & Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    RoundedCornerShape(height / 2)
                )
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                SkillRushPrimary,
                                Color(0xFF6366F1),
                                SkillRushCoinGold
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Modern Level Badge with visual prestige styling.
 */
@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    showLabel: Boolean = true
) {
    val tierColor = when {
        level >= 25 -> Color(0xFFFFD700) // Gold Titan
        level >= 20 -> Color(0xFFD500F9) // Purple Grandmaster
        level >= 15 -> Color(0xFF00E5FF) // Cyan Focus Master
        level >= 10 -> Color(0xFF00E676) // Emerald Expert
        level >= 6 -> Color(0xFFFF9100)  // Orange Challenger
        level >= 3 -> Color(0xFF3D5AFE)  // Blue Apprentice
        else -> SkillRushPrimary         // Base
    }

    Surface(
        modifier = modifier
            .size(size)
            .testTag("level_badge_$level"),
        shape = CircleShape,
        color = tierColor.copy(alpha = 0.15f),
        border = BorderStroke(2.dp, tierColor),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showLabel && size >= 36.dp) {
                    Text(
                        text = "LVL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = tierColor,
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                    )
                }
                Text(
                    text = "$level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = tierColor,
                    fontSize = if (size >= 44.dp) 16.sp else 13.sp,
                    lineHeight = if (size >= 44.dp) 16.sp else 13.sp
                )
            }
        }
    }
}

/**
 * Animated Celebration Dialog triggered upon Leveling Up.
 */
@Composable
fun LevelUpCelebrationDialog(
    newLevel: Int,
    previousLevel: Int,
    levelProgress: LevelProgress,
    onDismiss: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.2f) }
    val rotationAnim = remember { Animatable(-25f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rotationAnim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp)
                .testTag("level_up_dialog"),
            contentAlignment = Alignment.Center
        ) {
            // Confetti canvas
            ConfettiBackground()

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .scale(scaleAnim.value)
                    .rotate(rotationAnim.value),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(SkillRushCoinGold, SkillRushPrimary))),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SkillRushCoinGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, SkillRushCoinGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SkillRushCoinGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LEVEL UP REACHED!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = SkillRushCoinGold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Level Big Icon Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SkillRushPrimary.copy(alpha = 0.25f),
                            border = BorderStroke(3.dp, SkillRushCoinGold),
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(16.dp, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "LVL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = SkillRushCoinGold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "$newLevel",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Unlocked Title Announcement
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = levelProgress.levelTitle.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "You advanced from Level $previousLevel to Level $newLevel!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Next level milestone preview
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NEXT RANK TIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = levelProgress.nextLevelTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SkillRushCoinGold
                                )
                            }
                            XpProgressBar(
                                currentXp = levelProgress.currentLevelXp,
                                requiredXp = levelProgress.xpRequiredForNextLevel,
                                progressFraction = levelProgress.progressFraction,
                                showLabel = false,
                                height = 8.dp
                            )
                            Text(
                                text = "${levelProgress.xpRemainingForNextLevel} XP needed for Level ${newLevel + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Dismiss button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SkillRushPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("level_up_claim_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SkillRushCoinGold)
                            Text(
                                text = "CLAIM & CONTINUE",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Particle Confetti Canvas
 */
@Composable
fun ConfettiBackground() {
    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.003f + 0.001f,
                color = when (Random.nextInt(5)) {
                    0 -> SkillRushCoinGold
                    1 -> SkillRushPrimary
                    2 -> Color(0xFF00E5FF)
                    3 -> Color(0xFFFF4081)
                    else -> Color(0xFF00E676)
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = ((p.y + time * 0.8f) % 1f) * height
            val currentX = (p.x * width) + sin(time * 6.28f + p.y * 10f) * 15f
            drawCircle(
                color = p.color.copy(alpha = 0.85f),
                radius = p.radius.dp.toPx(),
                center = Offset(currentX, currentY)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val color: Color
)

/**
 * Game Over XP Card showing detailed XP gains and current level progress bar.
 */
@Composable
fun GameOverXpCard(
    xpBreakdown: XpBreakdown,
    levelProgress: LevelProgress,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_over_xp_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Level + Title + Total Earned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LevelBadge(level = levelProgress.level, size = 36.dp)
                    Column {
                        Text(
                            text = "LEVEL ${levelProgress.level}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = SkillRushPrimary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = levelProgress.levelTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SkillRushPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SkillRushPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SkillRushCoinGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+${xpBreakdown.totalEarned} XP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = SkillRushPrimary
                        )
                    }
                }
            }

            // XP Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                XpChip(label = "Base", value = "+${xpBreakdown.baseXp}", modifier = Modifier.weight(1f))
                XpChip(label = "Score", value = "+${xpBreakdown.scoreXp}", modifier = Modifier.weight(1f))
                if (xpBreakdown.accuracyBonusXp > 0) {
                    XpChip(label = "Accuracy", value = "+${xpBreakdown.accuracyBonusXp}", modifier = Modifier.weight(1f))
                }
                if (xpBreakdown.comboBonusXp > 0) {
                    XpChip(label = "Combo", value = "+${xpBreakdown.comboBonusXp}", modifier = Modifier.weight(1f))
                }
            }

            // Progress Bar
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
private fun XpChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SkillRushCoinGold,
                fontSize = 11.sp
            )
        }
    }
}
