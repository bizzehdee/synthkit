package com.bizzeh.synthkit.pads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import com.bizzeh.synthkit.testing.RecordingPlayer
import com.bizzeh.synthkit.testing.RecordingPlayer.Off
import com.bizzeh.synthkit.testing.RecordingPlayer.On
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChromaticPadsLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val player = RecordingPlayer()

    private fun show(root: Int = 60) {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = 300.dp)) {
                ChromaticPadsLayout(player = player, channel = 0, defaultRoot = root)
            }
        }
    }

    private fun centre(name: String) =
        composeRule.onNodeWithContentDescription(name).fetchSemanticsNode().boundsInRoot.center

    @Test
    fun phoneShowsOneOctaveFromTheRootAtTouchSize() {
        show()

        listOf("C 4", "C sharp 4", "F 4", "B 4").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(72.dp)
        }
        composeRule.onNodeWithContentDescription("C 5").assertDoesNotExist()
    }

    @Test
    fun padSoundsWhileHeld() {
        show()
        val e = centre("E 4")

        composeRule.onRoot().performTouchInput { down(0, e) }
        composeRule.waitForIdle()
        val held = player.events.toList()
        composeRule.onRoot().performTouchInput { up(0) }
        composeRule.waitForIdle()

        assertEquals(listOf(On(0, 64, FIXED_VELOCITY, 0f)), held)
        assertEquals(Off(0, 64, 0f), player.events.last())
    }

    @Test
    fun twoPadsTogetherBothSound() {
        show()
        val c = centre("C 4")
        val g = centre("G 4")

        composeRule.onRoot().performTouchInput {
            down(0, c)
            down(1, g)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(60, 67), player.ons.map { it.key })
    }

    @Test
    fun octaveUpMovesEveryPadUpAnOctave() {
        show(root = 48)

        composeRule.onNodeWithContentDescription(context.getString(R.string.octave_up)).performClick()

        composeRule.onNodeWithContentDescription("C 4").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("C 3").assertDoesNotExist()
    }

    @Test
    fun accessibilityTapPlaysTheNote() {
        show()

        composeRule.onNodeWithContentDescription("A 4").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(69), player.ons.map { it.key })
    }
}
