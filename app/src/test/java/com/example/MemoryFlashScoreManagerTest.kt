package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MemoryFlashScoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryFlashScoreManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun recordGameResult_savesHighScoreLevelAndXpCorrectly() {
        val result1 = MemoryFlashScoreManager.recordGameResult(
            context = context,
            score = 350,
            level = 4,
            accuracy = 85.0f,
            maxCombo = 6
        )

        assertTrue(result1.isNewBestScore)
        assertTrue(result1.isNewHighestLevel)
        assertEquals(350, result1.newBestScore)
        assertEquals(4, result1.highestLevel)
        assertEquals(350, MemoryFlashScoreManager.getBestScore(context))
        assertEquals(4, MemoryFlashScoreManager.getHighestLevel(context))
        assertEquals(6, MemoryFlashScoreManager.getBestCombo(context))
        assertTrue(result1.xpEarned > 0)

        val result2 = MemoryFlashScoreManager.recordGameResult(
            context = context,
            score = 200,
            level = 2,
            accuracy = 70.0f,
            maxCombo = 3
        )

        assertEquals(false, result2.isNewBestScore)
        assertEquals(false, result2.isNewHighestLevel)
        assertEquals(350, result2.newBestScore)
        assertEquals(4, result2.highestLevel)
        assertEquals(350, MemoryFlashScoreManager.getBestScore(context))
        assertEquals(2, MemoryFlashScoreManager.getGamesPlayed(context))
    }
}
