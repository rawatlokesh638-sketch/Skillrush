package com.example

import com.example.data.validation.ScoreValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScoreValidatorTest {

    @Test
    fun testValidMiniGameScoresAccepted() {
        val res1 = ScoreValidator.validateGameScore("tap_rush", score = 250, accuracyPercent = 95f, maxCombo = 12)
        assertTrue("Valid Tap Rush score should pass", res1.isValid)

        val res2 = ScoreValidator.validateGameScore("memory_flash", score = 600, accuracyPercent = 100f, maxCombo = 20)
        assertTrue("Valid Memory Flash score should pass", res2.isValid)
    }

    @Test
    fun testNegativeScoresRejected() {
        val res = ScoreValidator.validateGameScore("tap_rush", score = -50, accuracyPercent = 90f, maxCombo = 5)
        assertFalse("Negative score must be rejected", res.isValid)
    }

    @Test
    fun testImpossibleHighScoresRejected() {
        val res = ScoreValidator.validateGameScore("tap_rush", score = 999999, accuracyPercent = 100f, maxCombo = 50)
        assertFalse("Impossible high score must be rejected", res.isValid)
    }

    @Test
    fun testInvalidAccuracyRejected() {
        val res1 = ScoreValidator.validateGameScore("speed_rush", score = 300, accuracyPercent = -5f, maxCombo = 10)
        assertFalse("Accuracy below 0% must be rejected", res1.isValid)

        val res2 = ScoreValidator.validateGameScore("speed_rush", score = 300, accuracyPercent = 120f, maxCombo = 10)
        assertFalse("Accuracy above 100% must be rejected", res2.isValid)
    }

    @Test
    fun testProfileDataValidation() {
        val validProfile = ScoreValidator.validateProfileData(coins = 500, xp = 1200, gamesPlayed = 45, streak = 7)
        assertTrue("Valid profile data should pass", validProfile.isValid)

        val negativeCoins = ScoreValidator.validateProfileData(coins = -10, xp = 1200, gamesPlayed = 45, streak = 7)
        assertFalse("Negative coins must be rejected", negativeCoins.isValid)

        val impossibleXp = ScoreValidator.validateProfileData(coins = 500, xp = 99999999, gamesPlayed = 45, streak = 7)
        assertFalse("Impossible XP must be rejected", impossibleXp.isValid)
    }

    @Test
    fun testLeaderboardValidation() {
        val validUpload = ScoreValidator.validateLeaderboardUpload(score = 850, name = "ChampionReflexer", level = 12, badge = "Precision Expert")
        assertTrue("Valid leaderboard entry should pass", validUpload.isValid)

        val zeroScore = ScoreValidator.validateLeaderboardUpload(score = 0, name = "Player", level = 1, badge = "Novice")
        assertFalse("Zero leaderboard score must be rejected", zeroScore.isValid)

        val longName = ScoreValidator.validateLeaderboardUpload(score = 500, name = "ThisPlayerNameIsSuperUnreasonablyLongAndExceedsLimits", level = 5, badge = "Reflexer")
        assertFalse("Player name exceeding 30 characters must be rejected", longName.isValid)
    }

    @Test
    fun testRewardedAdBonusValidation() {
        val validBonus = ScoreValidator.validateRewardedAdBonus(25)
        assertTrue("Valid ad bonus amount (25 coins) should pass", validBonus.isValid)

        val zeroBonus = ScoreValidator.validateRewardedAdBonus(0)
        assertFalse("Zero ad bonus must be rejected", zeroBonus.isValid)

        val massiveBonus = ScoreValidator.validateRewardedAdBonus(10000)
        assertFalse("Massive ad bonus injection must be rejected", massiveBonus.isValid)
    }
}
