package com.example.ui.auth

import android.content.Context
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.data.validation.ScoreValidator
import com.example.ui.theme.SkillRushCoinGold
import com.example.ui.theme.SkillRushPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameSetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var gamerTagInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val presetGamerTags = remember {
        listOf(
            "ApexReflex", "VortexStriker", "PulseKnight",
            "HyperNova", "CyberDash", "QuantumPulse",
            "SwiftReflex", "NeonShadow", "BlazeStriker", "ZeroPoint"
        )
    }

    fun handleAuthenticationAndSetup(tag: String, pass: String) {
        val cleanTag = tag.trim()
        val cleanKey = cleanTag.lowercase(Locale.ROOT)
        val db = FirebaseDatabase.getInstance()
        val usernameRef = db.getReference("usernames").child(cleanKey)

        usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.value != null) {
                    // USERNAME ALREADY EXISTS IN DATABASE
                    val storedPassword = snapshot.child("password").getValue(String::class.java)
                    val existingUid = snapshot.child("uid").getValue(String::class.java)
                        ?: ("SR_" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase())

                    if (storedPassword == pass) {
                        // Password is correct! Fetch existing user's data and restore
                        val userRef = db.getReference("users").child(existingUid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                if (userSnapshot.exists()) {
                                    val coins = userSnapshot.child("coins").getValue(Int::class.java) ?: 0
                                    val xp = userSnapshot.child("xp").getValue(Int::class.java) ?: 0
                                    val games = userSnapshot.child("gamesPlayed").getValue(Int::class.java) ?: 0
                                    val streak = userSnapshot.child("streak").getValue(Int::class.java) ?: 0
                                    val lastUpdated = userSnapshot.child("lastUpdated").getValue(Long::class.java) ?: System.currentTimeMillis()

                                    // Restore best scores
                                    userSnapshot.child("bestScores").let { scores ->
                                        TapRushScoreManager.saveBestScore(context, (scores.child("tapRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                        MemoryFlashScoreManager.saveBestScore(context, (scores.child("memoryFlash").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                        PerfectAimScoreManager.saveBestScore(context, (scores.child("perfectAim").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                        NumberSprintScoreManager.saveBestScore(context, (scores.child("numberSprint").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                        SpotDifferenceScoreManager.saveBestScore(context, (scores.child("spotDifference").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                        SpeedRushScoreManager.saveBestScore(context, (scores.child("speedRush").getValue(Int::class.java) ?: 0).coerceIn(0, 10000))
                                    }

                                    (userSnapshot.child("stats").value as? Map<String, Any?>)?.let { cloudStatsMap ->
                                        com.example.data.stats.StatsManager.mergeCloudStats(context, cloudStatsMap)
                                    }

                                    UserProfileManager.overwriteLocalDataFromCloud(context, coins, xp, games, streak, lastUpdated)
                                }

                                UserProfileManager.saveUserAccount(context, existingUid, cleanTag, pass)
                                isLoading = false
                                onSetupComplete()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Fallback login with existing UID
                                UserProfileManager.saveUserAccount(context, existingUid, cleanTag, pass)
                                isLoading = false
                                onSetupComplete()
                            }
                        })
                    } else {
                        // Password is wrong
                        isLoading = false
                        errorMessage = "Incorrect password! This Gamer Tag is already registered. Please enter your correct password."
                    }
                } else {
                    // USERNAME IS COMPLETELY NEW -> Generate brand new UID
                    val newUid = "SR_" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                    val timestamp = System.currentTimeMillis()

                    val usernameRecord = mapOf(
                        "uid" to newUid,
                        "username" to cleanTag,
                        "password" to pass,
                        "createdAt" to timestamp
                    )

                    val userRecord = mapOf(
                        "uid" to newUid,
                        "username" to cleanTag,
                        "password" to pass,
                        "coins" to 0,
                        "xp" to 0,
                        "gamesPlayed" to 0,
                        "streak" to 0,
                        "createdAt" to timestamp,
                        "lastUpdated" to timestamp
                    )

                    // Write both records to Firebase
                    db.getReference("usernames").child(cleanKey).setValue(usernameRecord)
                    db.getReference("users").child(newUid).setValue(userRecord).addOnCompleteListener {
                        UserProfileManager.saveUserAccount(context, newUid, cleanTag, pass)
                        isLoading = false
                        onSetupComplete()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // If network/permission error, fallback gracefully
                val fallbackUid = "SR_" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                UserProfileManager.saveUserAccount(context, fallbackUid, cleanTag, pass)
                isLoading = false
                onSetupComplete()
            }
        })
    }

    fun submitAccount() {
        val trimmedTag = gamerTagInput.trim()
        val trimmedPassword = passwordInput.trim()

        if (trimmedTag.length < 3) {
            errorMessage = "Gamer tag must be at least 3 characters long."
            return
        }
        if (trimmedTag.length > 16) {
            errorMessage = "Gamer tag must be 16 characters or less."
            return
        }
        if (!trimmedTag.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            errorMessage = "Only letters, numbers, and underscores are allowed in Gamer Tag."
            return
        }
        if (trimmedPassword.length < 4) {
            errorMessage = "Password must be at least 4 characters long."
            return
        }

        errorMessage = null
        isLoading = true

        // Ensure Firebase Auth is active
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener {
                handleAuthenticationAndSetup(trimmedTag, trimmedPassword)
            }
        } else {
            handleAuthenticationAndSetup(trimmedTag, trimmedPassword)
        }
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
            .padding(20.dp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Icon Header
                Surface(
                    shape = CircleShape,
                    color = SkillRushPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, SkillRushPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Welcome to SkillRush",
                            tint = SkillRushPrimary,
                            modifier = Modifier.size(40.dp)
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
                        text = "Set your Gamer Tag & Password. Returning players will automatically restore their unique UID & stats!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E),
                        textAlign = TextAlign.Center
                    )
                }

                // Gamer Tag Input Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "GAMER TAG / USERNAME",
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

                    Text(
                        text = "${gamerTagInput.length}/16 • 3-16 characters (letters, numbers, _)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp
                    )
                }

                // Password Input Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ACCOUNT PASSWORD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B949E),
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            if (it.length <= 32) {
                                passwordInput = it
                                errorMessage = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Enter secure password (min 4 chars)",
                                color = Color(0xFF484F58)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SkillRushCoinGold
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible }
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                    tint = Color(0xFF8B949E)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            submitAccount()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D1117),
                            unfocusedContainerColor = Color(0xFF0D1117),
                            focusedBorderColor = SkillRushCoinGold,
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field")
                    )
                }

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF3D1418),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF8080),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Submit Button / Loading Indicator
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        submitAccount()
                    },
                    enabled = !isLoading && gamerTagInput.trim().length >= 3 && passwordInput.trim().length >= 4,
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CONNECTING DATABASE...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    } else {
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
}
