package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TapRushScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapRushScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun recordGameResult_savesHighScoreAndXpCorrectly() {
        val result1 = TapRushScoreManager.recordGameResult(
            context = context,
            score = 25,
            accuracy = 92.5f,
            maxCombo = 8
        )

        assertTrue(result1.isNewBestScore)
        assertEquals(25, result1.newBestScore)
        assertEquals(25, TapRushScoreManager.getBestScore(context))
        assertEquals(8, TapRushScoreManager.getBestCombo(context))
        assertTrue(result1.xpEarned > 0)

        val result2 = TapRushScoreManager.recordGameResult(
            context = context,
            score = 20,
            accuracy = 80.0f,
            maxCombo = 5
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(25, result2.newBestScore)
        assertEquals(25, TapRushScoreManager.getBestScore(context))
        assertEquals(2, TapRushScoreManager.getGamesPlayed(context))
    }
}
