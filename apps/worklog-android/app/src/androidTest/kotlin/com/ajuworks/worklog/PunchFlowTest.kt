package com.ajuworks.worklog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The punch loop, on a device. Deliberately thin: everything numeric is
 * covered by :core's JVM tests, so this only proves the buttons are wired to
 * the repository and that the screen reflects the stored state.
 *
 * Runs against whatever is already in the app's database, so it asserts on
 * transitions rather than absolute totals.
 */
@RunWith(AndroidJUnit4::class)
class PunchFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun clockInThenBreakThenClockOut() {
        val clockIn = composeRule.activity.getString(R.string.action_clock_in)
        val clockOut = composeRule.activity.getString(R.string.action_clock_out)
        val takeBreak = composeRule.activity.getString(R.string.action_start_break)
        val endBreak = composeRule.activity.getString(R.string.action_end_break)
        val working = composeRule.activity.getString(R.string.status_working)
        val onBreak = composeRule.activity.getString(R.string.status_on_break)

        // Start from a known state.
        if (composeRule.onAllNodesWithText(clockOut).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(clockOut).performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText(clockIn).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(working).assertIsDisplayed()

        composeRule.onNodeWithText(takeBreak).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(onBreak).assertIsDisplayed()

        composeRule.onNodeWithText(endBreak).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(working).assertIsDisplayed()

        composeRule.onNodeWithText(clockOut).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(clockIn).assertIsDisplayed()
    }
}

