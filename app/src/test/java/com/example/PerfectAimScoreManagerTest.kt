package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PerfectAimScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerfectAimScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun recordGameResult_savesHighScoreAccuracyAndXpCorrectly() {
        val result1 = PerfectAimScoreManager.recordGameResult(
            context = context,
            score = 1250,
            accuracy = 95.0f,
            maxCombo = 12,
            bullseyes = 8
        )

        assertTrue(result1.isNewBestScore)
        assertEquals(1250, result1.newBestScore)
        assertEquals(1250, PerfectAimScoreManager.getBestScore(context))
        assertEquals(95.0f, PerfectAimScoreManager.getBestAccuracy(context), 0.01f)
        assertEquals(12, PerfectAimScoreManager.getBestCombo(context))
        assertEquals(8, PerfectAimScoreManager.getTotalBullseyes(context))
        assertTrue(result1.xpEarned > 0)

        val result2 = PerfectAimScoreManager.recordGameResult(
            context = context,
            score = 800,
            accuracy = 80.0f,
            maxCombo = 5,
            bullseyes = 3
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(1250, result2.newBestScore)
        assertEquals(1250, PerfectAimScoreManager.getBestScore(context))
        assertEquals(2, PerfectAimScoreManager.getGamesPlayed(context))
        assertEquals(11, PerfectAimScoreManager.getTotalBullseyes(context))
    }
}
