package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.ui.game.GamePlayScreen
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
class GamePlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gamePlay_rendersTapRushArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "tap_rush", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("tap_rush_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tap_rush_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tap_rush_start_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tap_rush_timer").assertIsDisplayed()
    }

    @Test
    fun gamePlay_rendersMemoryArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "memory", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("memory_flash_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("memory_flash_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("memory_flash_start_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("memory_flash_lives_container").assertIsDisplayed()
    }

    @Test
    fun gamePlay_rendersPerfectAimArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "accuracy", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("perfect_aim_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perfect_aim_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perfect_aim_start_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("perfect_aim_timer").assertIsDisplayed()
    }

    @Test
    fun gamePlay_rendersNumberSprintArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "logic", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("number_sprint_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("number_sprint_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("number_sprint_start_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("number_sprint_timer").assertIsDisplayed()
    }

    @Test
    fun gamePlay_rendersSpotDifferenceArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "spot_difference", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("spot_difference_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("spot_difference_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("spot_difference_start_button").assertIsDisplayed()
    }

    @Test
    fun gamePlay_rendersSpeedRushArena() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GamePlayScreen(gameId = "speed_rush", onBack = {})
            }
        }
        composeTestRule.onNodeWithTag("speed_rush_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("speed_rush_arena").assertIsDisplayed()
        composeTestRule.onNodeWithTag("speed_rush_start_button").assertIsDisplayed()
    }
}
