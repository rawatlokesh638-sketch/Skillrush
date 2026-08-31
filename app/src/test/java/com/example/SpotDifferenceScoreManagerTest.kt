package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpotDifferenceScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpotDifferenceScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs for isolation
        context.getSharedPreferences("spot_difference_game_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun recordGameResult_savesHighScoreAccuracyAndRounds() {
        val result1 = SpotDifferenceScoreManager.recordGameResult(
            context = context,
            score = 1800,
            accuracy = 90.0f,
            maxCombo = 5,
            differencesFound = 9,
            roundReached = 3
        )

        assertTrue(result1.isNewBestScore)
        assertEquals(1800, result1.newBestScore)
        assertEquals(1800, SpotDifferenceScoreManager.getBestScore(context))
        assertEquals(90.0f, SpotDifferenceScoreManager.getBestAccuracy(context), 0.01f)
        assertEquals(3, SpotDifferenceScoreManager.getHighestRound(context))
        assertEquals(9, SpotDifferenceScoreManager.getTotalDifferencesFound(context))
        assertEquals(5, SpotDifferenceScoreManager.getBestCombo(context))
        assertTrue(result1.xpEarned > 0)
        assertTrue(result1.coinsEarned in 10..15)

        val result2 = SpotDifferenceScoreManager.recordGameResult(
            context = context,
            score = 1200,
            accuracy = 70.0f,
            maxCombo = 2,
            differencesFound = 4,
            roundReached = 2
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(1800, result2.newBestScore)
        assertEquals(1800, SpotDifferenceScoreManager.getBestScore(context))
        assertEquals(2, SpotDifferenceScoreManager.getGamesPlayed(context))
        assertEquals(13, SpotDifferenceScoreManager.getTotalDifferencesFound(context))
    }
}
