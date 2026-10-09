package com.bizzeh.synthkit

import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** End to end on the real engine: record a drum loop in a new project. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    @After
    fun removeProjects() {
        SystemClock.sleep(500)
        File(composeRule.activity.filesDir, "projects").deleteRecursively()
    }

    private fun waitForText(text: String, substring: Boolean = false) = composeRule.waitUntil(timeoutMillis = 10_000) {
        composeRule.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun recordingPadsInANewProjectMakesALoopWithOneTake() {
        waitForText(string(R.string.project_new))
        composeRule.onNodeWithText(string(R.string.project_new)).performClick()
        composeRule.onNodeWithText(string(R.string.add_track)).performClick()
        composeRule.onNodeWithContentDescription(
            string(R.string.home_open_family, string(R.string.family_drums_percussion)),
        ).performClick()
        val kick = composeRule.onNodeWithContentDescription(string(R.string.drum_kick))

        composeRule.onNodeWithContentDescription(string(R.string.record)).performClick()
        SystemClock.sleep(300)
        kick.performClick()
        waitForText(string(R.string.phase_recording_first, 1))
        SystemClock.sleep(1500)
        kick.performClick()
        composeRule.onNodeWithContentDescription(string(R.string.stop_recording)).performClick()

        // The take is about one bar long, so the loop rounds to one bar and plays at once.
        waitForText(" of 1", substring = true)
        composeRule.onNodeWithContentDescription(string(R.string.back)).performClick()
        val oneTake = composeRule.activity.resources.getQuantityString(R.plurals.take_count, 1, 1)
        composeRule.onNodeWithText(oneTake).assertIsDisplayed()
    }
}
