package com.example.model

import android.content.Context
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.MemoryFlashScoreManager
import com.example.data.NumberSprintScoreManager
import com.example.data.PerfectAimScoreManager
import com.example.data.SpeedRushScoreManager
import com.example.data.SpotDifferenceScoreManager
import com.example.data.TapRushScoreManager

enum class GameDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

data class GameChallenge(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val description: String,
    val difficulty: GameDifficulty,
    val bestScore: String,
    val isLocked: Boolean = false,
    val unlockRequirement: String? = null,
    val accentHue: Long = 0xFF6750A4
)

object GameData {
    fun getChallenges(context: Context? = null): List<GameChallenge> {
        val tapRushBest = if (context != null) {
            val score = TapRushScoreManager.getBestScore(context)
            if (score > 0) "$score pts" else "0 pts"
        } else {
            "0 pts"
        }

        val memoryFlashBest = if (context != null) {
            val score = MemoryFlashScoreManager.getBestScore(context)
            val level = MemoryFlashScoreManager.getHighestLevel(context)
            if (score > 0) "Lvl $level ($score pts)" else "0 pts"
        } else {
            "0 pts"
        }

        val perfectAimBest = if (context != null) {
            val score = PerfectAimScoreManager.getBestScore(context)
            val acc = PerfectAimScoreManager.getBestAccuracy(context)
            if (score > 0) String.format(Locale.getDefault(), "%.0f%% (%d pts)", acc, score) else "0 pts"
        } else {
            "0 pts"
        }

        val numberSprintBest = if (context != null) {
            val score = NumberSprintScoreManager.getBestScore(context)
            if (score > 0) "$score pts" else "0 pts"
        } else {
            "0 pts"
        }

        val spotDifferenceBest = if (context != null) {
            val score = SpotDifferenceScoreManager.getBestScore(context)
            if (score > 0) "$score pts" else "0 pts"
        } else {
            "0 pts"
        }

        val speedRushBest = if (context != null) {
            val score = SpeedRushScoreManager.getBestScore(context)
            if (score > 0) "$score pts" else "0 pts"
        } else {
            "0 pts"
        }

        return listOf(
            GameChallenge(
                id = "tap_rush",
                title = "Tap Rush",
                category = "Reflex",
                icon = Icons.Default.Bolt,
                description = "30-second reflex frenzy: tap dynamic targets rapidly while avoiding misses!",
                difficulty = GameDifficulty.EASY,
                bestScore = tapRushBest,
                isLocked = false
            ),
            GameChallenge(
                id = "memory",
                title = "Memory Flash",
                category = "Memory",
                icon = Icons.Default.Psychology,
                description = "Memorize the 3-second illuminated sequence and replay in exact order!",
                difficulty = GameDifficulty.MEDIUM,
                bestScore = memoryFlashBest,
                isLocked = false
            ),
            GameChallenge(
                id = "logic",
                title = "Number Sprint",
                category = "Logic",
                icon = Icons.Default.Extension,
                description = "30-second rapid arithmetic sprint: solve quick math puzzles and number patterns!",
                difficulty = GameDifficulty.MEDIUM,
                bestScore = numberSprintBest,
                isLocked = false
            ),
            GameChallenge(
                id = "spot_difference",
                title = "Spot Difference",
                category = "Focus",
                icon = Icons.Default.Visibility,
                description = "Scan dual holographic matrices and tap all subtle differences before time runs out!",
                difficulty = GameDifficulty.MEDIUM,
                bestScore = spotDifferenceBest,
                isLocked = false
            ),
            GameChallenge(
                id = "accuracy",
                title = "Perfect Aim",
                category = "Accuracy",
                icon = Icons.Default.AdsClick,
                description = "30-second precision target challenge: hit center bullseyes as targets shrink and speed up!",
                difficulty = GameDifficulty.MEDIUM,
                bestScore = perfectAimBest,
                isLocked = false
            ),
            GameChallenge(
                id = "speed_rush",
                title = "Speed Rush",
                category = "Speed",
                icon = Icons.Default.Speed,
                description = "30-second velocity sprint: rapidly identify and tap dynamic active target indicators before they switch!",
                difficulty = GameDifficulty.HARD,
                bestScore = speedRushBest,
                isLocked = false
            )
        )
    }

    val challenges: List<GameChallenge>
        get() = getChallenges(null)
}
