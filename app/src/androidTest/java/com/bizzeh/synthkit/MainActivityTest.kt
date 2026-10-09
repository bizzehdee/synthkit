package com.bizzeh.synthkit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    @Test
    fun appLoadsSoundsAndPlaysDrumsInANewProject() {
        val newProject = string(R.string.project_new)
        val drums = string(R.string.home_open_family, string(R.string.family_drums_percussion))

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText(newProject)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(newProject).performClick()
        composeRule.onNodeWithContentDescription(drums).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
    }
}
