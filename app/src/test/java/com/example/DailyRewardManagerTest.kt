package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRewardManager
import com.example.data.UserProfileManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyRewardManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs for clean test runs
        context.getSharedPreferences("skillrush_daily_reward_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testDailyRewardAvailableInitially() {
        val isAvailable = DailyRewardManager.isDailyRewardAvailable(context)
        assertTrue("Daily reward should be available initially", isAvailable)
    }

    @Test
    fun testClaimDailyRewardGrantsCoinsAndXp() {
        val initialCoins = UserProfileManager.getCoins(context)
        val initialXp = UserProfileManager.getTotalXp(context)

        val result = DailyRewardManager.claimDailyReward(context, doubleWithAd = false)
        assertNotNull(result)
        assertEquals(1, result!!.dayNumber)
        assertTrue(result.finalCoins > 0)
        assertTrue(result.finalXp > 0)

        val newCoins = UserProfileManager.getCoins(context)
        val newXp = UserProfileManager.getTotalXp(context)
        assertEquals(initialCoins + result.finalCoins, newCoins)
        assertEquals(initialXp + result.finalXp, newXp)

        // After claiming, reward should not be available today
        assertFalse("Reward should not be available after claiming today", DailyRewardManager.isDailyRewardAvailable(context))
    }

    @Test
    fun testDuplicateClaimIgnored() {
        val firstResult = DailyRewardManager.claimDailyReward(context, doubleWithAd = false)
        assertNotNull(firstResult)

        val secondResult = DailyRewardManager.claimDailyReward(context, doubleWithAd = false)
        assertNull("Duplicate claim on same day should return null", secondResult)
    }

    @Test
    fun testDoubleRewardWithAdMultiplier() {
        val result = DailyRewardManager.claimDailyReward(context, doubleWithAd = true)
        assertNotNull(result)
        assertTrue(result!!.isDoubledWithAd)
        assertEquals(result.baseCoins * 2, result.finalCoins)
        assertEquals(result.baseXp * 2, result.finalXp)
    }
}
