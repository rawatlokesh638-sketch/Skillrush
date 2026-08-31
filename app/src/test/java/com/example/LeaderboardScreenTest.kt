package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.leaderboard.LeaderboardEntry
import com.example.data.leaderboard.LeaderboardGameCategory
import com.example.data.leaderboard.LeaderboardRepository
import com.example.data.leaderboard.LeaderboardTimeframe
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LeaderboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepo = object : LeaderboardRepository {
        override suspend fun getLeaderboard(
            timeframe: LeaderboardTimeframe,
            category: LeaderboardGameCategory
        ): List<LeaderboardEntry> {
            return listOf(
                LeaderboardEntry(1, "1", "ApexTitan", 5000, false, 25),
                LeaderboardEntry(2, "2", "GrandmasterX", 4200, false, 20),
                LeaderboardEntry(3, "3", "Player 1", 3800, true, 18),
                LeaderboardEntry(4, "4", "SpeedDemon", 2900, false, 15)
            )
        }

        override suspend fun getCurrentUserEntry(
            timeframe: LeaderboardTimeframe,
            category: LeaderboardGameCategory
        ): LeaderboardEntry {
            return LeaderboardEntry(3, "3", "Player 1", 3800, true, 18)
        }
    }

    @Test
    fun leaderboardScreen_displaysTabsAndContent() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LeaderboardScreen(repository = fakeRepo)
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("leaderboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_tab_daily").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_tab_weekly").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_tab_all_time").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_refresh_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_top3_podium").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leaderboard_current_user_banner").assertIsDisplayed()

        // Switch to weekly
        composeTestRule.onNodeWithTag("leaderboard_tab_weekly").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("leaderboard_top3_podium").assertIsDisplayed()
    }
}
