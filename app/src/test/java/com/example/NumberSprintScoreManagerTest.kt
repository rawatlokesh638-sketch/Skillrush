package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NumberSprintScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberSprintScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun recordGameResult_savesHighScoreAccuracyAndXpCorrectly() {
        val result1 = NumberSprintScoreManager.recordGameResult(
            context = context,
            score = 1450,
            accuracy = 93.5f,
            maxCombo = 10,
            solvedQuestions = 12
        )

        assertTrue(result1.isNewBestScore)
        assertEquals(1450, result1.newBestScore)
        assertEquals(1450, NumberSprintScoreManager.getBestScore(context))
        assertEquals(93.5f, NumberSprintScoreManager.getBestAccuracy(context), 0.01f)
        assertEquals(10, NumberSprintScoreManager.getBestCombo(context))
        assertEquals(12, NumberSprintScoreManager.getTotalQuestionsSolved(context))
        assertTrue(result1.xpEarned > 0)

        val result2 = NumberSprintScoreManager.recordGameResult(
            context = context,
            score = 900,
            accuracy = 75.0f,
            maxCombo = 4,
            solvedQuestions = 7
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(1450, result2.newBestScore)
        assertEquals(1450, NumberSprintScoreManager.getBestScore(context))
        assertEquals(2, NumberSprintScoreManager.getGamesPlayed(context))
        assertEquals(19, NumberSprintScoreManager.getTotalQuestionsSolved(context))
    }
}
