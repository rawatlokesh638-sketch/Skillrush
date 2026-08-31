package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileManager
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushPrimary
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var gamerTagInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userUid by remember(context) { mutableStateOf(UserProfileManager.getUserUid(context)) }

    val presetGamerTags = remember {
        listOf(
            "ApexReflex", "VortexStriker", "PulseKnight",
            "HyperNova", "CyberDash", "QuantumPulse",
            "SwiftReflex", "NeonShadow", "BlazeStriker", "ZeroPoint"
        )
    }

    fun validateAndSubmit() {
        val trimmed = gamerTagInput.trim()
        if (trimmed.length < 3) {
            errorMessage = "Gamer tag must be at least 3 characters long."
            return
        }
        if (trimmed.length > 16) {
            errorMessage = "Gamer tag must be 16 characters or less."
            return
        }
        if (!trimmed.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            errorMessage = "Only letters, numbers, and underscores are allowed."
            return
        }

        errorMessage = null
        UserProfileManager.markUsernameSetupComplete(context, trimmed)
        onSetupComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            )
            .systemBarsPadding()
            .padding(24.dp)
            .testTag("username_setup_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
            border = BorderStroke(1.5.dp, SkillRushPrimary.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Game Icon Header
                Surface(
                    shape = CircleShape,
                    color = SkillRushPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, SkillRushPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Welcome to SkillRush",
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "WELCOME TO SKILLRUSH",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Set up your unique Gamer Tag to compete on live online leaderboards!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E),
                        textAlign = TextAlign.Center
                    )
                }

                // Unique UID Badge Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, SkillRushCoinGold.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = SkillRushCoinGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "UNIQUE USER ID",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SkillRushCoinGold,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = if (userUid.length > 14) "UID: ${userUid.take(12)}..." else "UID: $userUid",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                // Input Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ENTER YOUR GAMER TAG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B949E),
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = gamerTagInput,
                        onValueChange = {
                            if (it.length <= 16) {
                                gamerTagInput = it.filter { char -> char.isLetterOrDigit() || char == '_' }
                                errorMessage = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "e.g. CyberReflex99",
                                color = Color(0xFF484F58)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SkillRushPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val randomName = presetGamerTags[Random.nextInt(presetGamerTags.size)] + "_" + Random.nextInt(10, 99)
                                    gamerTagInput = randomName
                                    errorMessage = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Random Gamer Tag",
                                    tint = SkillRushCoinGold
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            validateAndSubmit()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D1117),
                            unfocusedContainerColor = Color(0xFF0D1117),
                            focusedBorderColor = SkillRushPrimary,
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "3-16 characters (letters, numbers, _)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8B949E),
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "${gamerTagInput.length}/16",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B949E),
                            fontSize = 10.sp
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        validateAndSubmit()
                    },
                    enabled = gamerTagInput.trim().length >= 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkillRushPrimary,
                        disabledContainerColor = SkillRushPrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_username_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START PLAYING ONLINE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
