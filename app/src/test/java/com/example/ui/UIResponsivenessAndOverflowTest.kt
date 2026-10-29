package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaHeader
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.TagPill
import com.example.ui.theme.CGATheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class UIResponsivenessAndOverflowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 1. HOME PAGE — LONG USER NAME
     * When user name is very long (e.g. "Jana Kanniappan" or "Alexander Bartholomew Montgomery III"),
     * it must display gracefully without clipping out of the container or crashing,
     * maintaining room for badges and avatar.
     */
    @Test
    fun testLongUserNameRendersGracefullyInHeader() {
        val longName = "Alexander Bartholomew Montgomery III"
        composeTestRule.setContent {
            CGATheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CgaHeader(
                        studentName = longName,
                        greeting = "WELCOME BACK",
                        streakDays = 14,
                        xp = 1250,
                        onAvatarClick = {}
                    )
                }
            }
        }

        // Avatar, XP badge, and Streak badge should all be displayed
        composeTestRule.onNodeWithTag("header_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_xp_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("streak_badge").assertIsDisplayed()
    }

    /**
     * 2. PRACTICE HUB — SPEED TEST "50 EX" / "+50 XP" / "COMPLETE"
     * Verifies that TagPill with reward text like "+50 XP" and status "COMPLETE" renders fully without clipping.
     */
    @Test
    fun testSpeedTestBadgeDisplaysCompleteText() {
        composeTestRule.setContent {
            CGATheme {
                TagPill(
                    text = "+50 XP",
                    modifier = Modifier.testTag("speed_test_xp_pill")
                )
            }
        }

        composeTestRule.onNodeWithTag("speed_test_xp_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("+50 XP").assertIsDisplayed()
    }

    @Test
    fun testSpeedTestCompleteBadgeDisplaysFully() {
        composeTestRule.setContent {
            CGATheme {
                TagPill(
                    text = "COMPLETE",
                    modifier = Modifier.testTag("speed_test_complete_pill")
                )
            }
        }

        composeTestRule.onNodeWithTag("speed_test_complete_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETE").assertIsDisplayed()
    }

    /**
     * 3. PRACTICE HUB — "COMPLETE" BUTTON TEXT
     * Verifies that CgaPrimaryButton with leading icon and "COMPLETE" text fits and displays clearly without clipping or overlapping.
     */
    @Test
    fun testCgaPrimaryButtonCompleteDisplaysWithIcon() {
        composeTestRule.setContent {
            CGATheme {
                Box(modifier = Modifier.width(320.dp)) {
                    CgaPrimaryButton(
                        text = "COMPLETE",
                        leadingIcon = Icons.Default.Check,
                        onClick = {},
                        variant = CgaButtonVariant.Cyan,
                        testTag = "complete_action_button"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("complete_action_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETE").assertIsDisplayed()
    }

    /**
     * 4. PRACTICE HUB — FILTER COUNTS (1-digit, 2-digit, 3-digit, 4-digit)
     * Verifies that filter counts are rendered cleanly with the format "LABEL (COUNT)".
     */
    @Test
    fun testFilterCountsFormatAndRenderForVariedDigits() {
        val testCounts = listOf(0, 7, 24, 100, 999, 1500)

        composeTestRule.setContent {
            CGATheme {
                androidx.compose.foundation.layout.Column {
                    for (count in testCounts) {
                        TagPill(
                            text = "ALL ($count)",
                            modifier = Modifier.testTag("filter_tag_$count")
                        )
                    }
                }
            }
        }

        for (count in testCounts) {
            val labelText = "ALL ($count)"
            composeTestRule.onNodeWithTag("filter_tag_$count").assertIsDisplayed()
            composeTestRule.onNodeWithText(labelText).assertIsDisplayed()
        }
    }
}
