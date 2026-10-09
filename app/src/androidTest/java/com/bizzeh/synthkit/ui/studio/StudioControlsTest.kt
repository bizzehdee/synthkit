package com.bizzeh.synthkit.ui.studio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bizzeh.synthkit.ui.theme.SynthKitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class StudioControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        composeRule.setContent { SynthKitTheme(darkTheme = true) { content() } }

    @Test
    fun litToggleSwitchesAndReportsItsState() {
        var on by mutableStateOf(false)
        show { LitToggle("Hold", on, { on = it }, Color.Magenta) }

        composeRule.onNodeWithText("HOLD").assertIsOff().performClick()

        assertTrue(on)
        composeRule.onNodeWithText("HOLD").assertIsOn()
    }

    @Test
    fun squareToggleSwitchesAndReportsItsState() {
        var on by mutableStateOf(false)
        show { SquareToggle("M", on, Color.Yellow, "Mute drums", { on = it }) }

        composeRule.onNodeWithContentDescription("Mute drums").performClick()

        composeRule.onNodeWithContentDescription("Mute drums").assertIsOn()
    }

    @Test
    fun faderFollowsTapsAndAccessibilityActions() {
        var level by mutableFloatStateOf(0.5f)
        show { MiniFader(level, { level = it }, Color.Cyan, "Volume") }

        composeRule.onNodeWithContentDescription("Volume").performSemanticsAction(SemanticsActions.SetProgress) { it(0.2f) }
        assertEquals(0.2f, level)
        composeRule.onNodeWithContentDescription("Volume").performTouchInput { click(centerRight.copy(x = width - 1f)) }

        assertTrue("level $level", abs(level - 1f) < 0.05f)
    }

    @Test
    fun disabledTransportButtonsIgnoreTaps() {
        var taps = 0
        show {
            RoundRecordButton(live = false, enabled = false, contentDescription = "Record", onClick = { taps++ })
            RoundPlayStopButton(playing = false, enabled = false, contentDescription = "Play", onClick = { taps++ })
        }

        composeRule.onNodeWithContentDescription("Record").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("Play").assertIsNotEnabled().performClick()

        assertEquals(0, taps)
    }

    @Test
    fun stepperButtonsRespectTheirLimits() {
        val calls = mutableListOf<String>()
        show { Stepper("C3", "Down", "Up", canGoDown = false, canGoUp = true, onDown = { calls += "down" }, onUp = { calls += "up" }) }

        composeRule.onNodeWithContentDescription("Down").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Up").performClick()

        assertEquals(listOf("up"), calls)
        composeRule.onNodeWithText("C3").assertIsDisplayed()
    }

    @Test
    fun chipButtonAndDotsShowAndRespond() {
        var opened = 0
        show {
            FamilyChip(Color.Cyan, "Drums", "Standard 1", onClick = { opened++ })
            PanelIconButton(Icons.Filled.Menu, "Instruments", onClick = { opened++ })
            PageDots(count = 6, current = 0, contentDescription = "Page 1 of 6")
        }

        composeRule.onNodeWithText("Standard 1").performClick()
        composeRule.onNodeWithContentDescription("Instruments").performClick()

        assertEquals(2, opened)
        composeRule.onNodeWithText("DRUMS").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Page 1 of 6").assertIsDisplayed()
    }
}
