package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.game.GameSelectionScreen
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
class GameSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gameSelection_displaysHeadingAndCards() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GameSelectionScreen()
            }
        }

        composeTestRule.onNodeWithTag("heading_choose_challenge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_card_tap_rush").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_card_memory").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_card_logic").assertIsDisplayed()
    }
}

