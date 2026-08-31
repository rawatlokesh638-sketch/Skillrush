package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpeedRushScoreManager
import com.example.data.TapRushScoreManager
import com.example.data.UserProfileManager
import com.example.data.leaderboard.LeaderboardTimeframe
import com.example.data.leaderboard.LocalLeaderboardRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeaderboardRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: LocalLeaderboardRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("skillrush_user_profile_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val speedRushPrefs = context.getSharedPreferences("skillrush_speed_rush_prefs", Context.MODE_PRIVATE)
        speedRushPrefs.edit().clear().commit()
        val tapRushPrefs = context.getSharedPreferences("skillrush_tap_rush_prefs", Context.MODE_PRIVATE)
        tapRushPrefs.edit().clear().commit()

        repository = LocalLeaderboardRepository(context)
    }

    @Test
    fun getLeaderboard_containsSortedEntriesAndCurrentPlayer() = runBlocking {
        // Save some scores for the current player
        SpeedRushScoreManager.recordGameResult(context, 1200, 95f, 15, 280, 20)
        TapRushScoreManager.recordGameResult(context, 1500, 98f, 25)

        val dailyLeaderboard = repository.getLeaderboard(LeaderboardTimeframe.DAILY)
        assertTrue(dailyLeaderboard.isNotEmpty())

        // Check that ranks are ascending from 1
        for (i in dailyLeaderboard.indices) {
            assertEquals(i + 1, dailyLeaderboard[i].rank)
        }

        // Current player should be in the list
        val userEntry = dailyLeaderboard.find { it.isCurrentPlayer }
        assertNotNull(userEntry)
        assertTrue(userEntry!!.score > 0)
    }

    @Test
    fun getLeaderboard_timeframesHaveDistinctScoring() = runBlocking {
        SpeedRushScoreManager.recordGameResult(context, 2000, 95f, 20, 280, 30)

        val daily = repository.getLeaderboard(LeaderboardTimeframe.DAILY)
        val allTime = repository.getLeaderboard(LeaderboardTimeframe.ALL_TIME)

        val dailyUser = daily.find { it.isCurrentPlayer }
        val allTimeUser = allTime.find { it.isCurrentPlayer }

        assertNotNull(dailyUser)
        assertNotNull(allTimeUser)
        assertTrue(allTimeUser!!.score >= dailyUser!!.score)
    }
}
