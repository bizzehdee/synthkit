package com.bizzeh.synthkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchLoadsSoundsAndShowsDrumPads() {
        val kick = composeRule.activity.getString(R.string.drum_kick)

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasContentDescription(kick)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(kick).assertIsDisplayed()
    }
}
