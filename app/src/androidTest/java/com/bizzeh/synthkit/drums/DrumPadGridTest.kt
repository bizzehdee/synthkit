package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.NotePlayer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrumPadGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val played = mutableListOf<Note>()
    private val player = NotePlayer { channel, key, velocity ->
        played += Note(channel, key, velocity)
        true
    }

    private data class Note(val channel: Int, val key: Int, val velocity: Float)

    // About the size of a 5-inch phone in landscape, the smallest screen the app targets.
    private fun showGridOnSmallPhone() {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = 360.dp)) {
                DrumPadGrid(pads = FirstPageDrumPads, player = player)
            }
        }
    }

    private fun pad(pad: DrumPad): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(context.getString(pad.label))

    private fun centreInRoot(pad: DrumPad): Offset =
        pad(pad).fetchSemanticsNode().boundsInRoot.center

    @Test
    fun everyFirstPagePadIsShownAtTouchSize() {
        showGridOnSmallPhone()

        FirstPageDrumPads.forEach { drumPad ->
            pad(drumPad)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(72.dp)
        }
    }

    @Test
    fun fingerDownPlaysThePadNoteBeforeRelease() {
        showGridOnSmallPhone()
        val snare = FirstPageDrumPads[1]

        pad(snare).performTouchInput { down(center) }
        composeRule.waitForIdle()

        assertEquals(listOf(Note(DRUM_CHANNEL, 38, DRUM_VELOCITY)), played)
    }

    @Test
    fun twoFingersOnTwoPadsPlayBothNotes() {
        showGridOnSmallPhone()
        val kick = centreInRoot(FirstPageDrumPads[0])
        val closedHat = centreInRoot(FirstPageDrumPads[2])

        composeRule.onRoot().performTouchInput {
            down(0, kick)
            down(1, closedHat)
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(36, 42), played.map { it.key })
    }

    @Test
    fun secondFingerOnAPadThatIsAlreadyHeldPlaysAgain() {
        showGridOnSmallPhone()
        val crashPad = FirstPageDrumPads[6]
        val crash = centreInRoot(crashPad)

        composeRule.onRoot().performTouchInput {
            down(0, crash)
            down(1, crash + Offset(10f, 10f))
            up(1)
            up(0)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(49, 49), played.map { it.key })
    }

    @Test
    fun releaseDoesNotPlayAgain() {
        showGridOnSmallPhone()

        pad(FirstPageDrumPads[0]).performClick()
        composeRule.waitForIdle()

        assertEquals(1, played.size)
    }

    @Test
    fun accessibilityClickPlaysThePad() {
        showGridOnSmallPhone()

        pad(FirstPageDrumPads[7]).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(51), played.map { it.key })
    }
}
