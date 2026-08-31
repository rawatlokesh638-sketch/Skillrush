package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpeedRushScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeedRushScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("speed_rush_game_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun recordGameResult_savesHighScoreAccuracyAndReaction() {
        val result1 = SpeedRushScoreManager.recordGameResult(
            context = context,
            score = 2400,
            accuracy = 95.0f,
            maxCombo = 12,
            avgReactionMs = 230,
            targetsHit = 28
        )

        assertTrue(result1.isNewBestScore)
        assertEquals(2400, result1.newBestScore)
        assertEquals(2400, SpeedRushScoreManager.getBestScore(context))
        assertEquals(95.0f, SpeedRushScoreManager.getBestAccuracy(context), 0.01f)
        assertEquals(230, SpeedRushScoreManager.getBestReactionMs(context))
        assertEquals(12, SpeedRushScoreManager.getBestCombo(context))
        assertEquals(28, SpeedRushScoreManager.getTotalTargetsHit(context))
        assertTrue(result1.xpEarned > 0)
        assertTrue(result1.coinsEarned in 10..15)

        val result2 = SpeedRushScoreManager.recordGameResult(
            context = context,
            score = 1500,
            accuracy = 80.0f,
            maxCombo = 5,
            avgReactionMs = 310,
            targetsHit = 15
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(2400, result2.newBestScore)
        assertEquals(2400, SpeedRushScoreManager.getBestScore(context))
        assertEquals(230, SpeedRushScoreManager.getBestReactionMs(context))
        assertEquals(43, SpeedRushScoreManager.getTotalTargetsHit(context))
        assertEquals(2, SpeedRushScoreManager.getGamesPlayed(context))
    }
}
