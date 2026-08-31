package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserProfileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserProfileManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear preferences
        context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("skillrush_achievements_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun initialState_isZeroForCoinsStreakAndPoints() {
        assertEquals(0, UserProfileManager.getCoins(context))
        assertEquals(0, UserProfileManager.getTotalXp(context))
        assertEquals(0, UserProfileManager.getStreakDays(context))
        assertEquals(1, UserProfileManager.getLevel(context))
        assertEquals(0, UserProfileManager.getTotalGamesPlayed(context))
    }

    @Test
    fun awardGameRewards_awardsBetween10And15Coins() {
        assertEquals(0, UserProfileManager.getCoins(context))

        // Game with average performance
        val session1 = UserProfileManager.awardGameRewards(
            context = context,
            score = 350,
            accuracyPercent = 80f
        )

        assertTrue("Coins earned should be >= 10, was ${session1.coinsEarned}", session1.coinsEarned >= 10)
        assertTrue("Coins earned should be <= 15, was ${session1.coinsEarned}", session1.coinsEarned <= 15)
        assertTrue("Total coins should include game rewards and achievement bonuses", UserProfileManager.getCoins(context) >= session1.coinsEarned)
        assertEquals(1, UserProfileManager.getTotalGamesPlayed(context))
        assertEquals(1, UserProfileManager.getStreakDays(context))

        // Second game session with high performance
        val session2 = UserProfileManager.awardGameRewards(
            context = context,
            score = 1200,
            accuracyPercent = 95f
        )

        assertTrue("Coins earned should be >= 10, was ${session2.coinsEarned}", session2.coinsEarned >= 10)
        assertTrue("Coins earned should be <= 15, was ${session2.coinsEarned}", session2.coinsEarned <= 15)
        assertTrue("Total coins accumulated cleanly", UserProfileManager.getCoins(context) >= session1.coinsEarned + session2.coinsEarned)
        assertEquals(2, UserProfileManager.getTotalGamesPlayed(context))
    }
}

