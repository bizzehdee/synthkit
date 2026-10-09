package com.bizzeh.synthkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LandscapeOnlyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rotateMessage = InstrumentationRegistry.getInstrumentation()
        .targetContext.getString(R.string.rotate_to_landscape)

    private fun showIn(width: Dp, height: Dp) {
        composeRule.setContent {
            Box(modifier = Modifier.size(width, height)) {
                LandscapeOnly { Text(PLAY_CONTENT) }
            }
        }
    }

    @Test
    fun portraitWindowShowsRotateMessageInsteadOfContent() {
        showIn(width = 300.dp, height = 500.dp)

        composeRule.onNodeWithText(rotateMessage).assertIsDisplayed()
        composeRule.onNodeWithText(PLAY_CONTENT).assertDoesNotExist()
    }

    @Test
    fun landscapeWindowShowsContent() {
        showIn(width = 500.dp, height = 300.dp)

        composeRule.onNodeWithText(PLAY_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(rotateMessage).assertDoesNotExist()
    }

    private companion object {
        const val PLAY_CONTENT = "play content"
    }
}
