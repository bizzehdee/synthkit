package com.bizzeh.synthkit.keys

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.testing.RecordingPlayer
import com.bizzeh.synthkit.testing.RecordingPlayer.Off
import com.bizzeh.synthkit.testing.RecordingPlayer.On
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeysLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val player = RecordingPlayer()

    private fun show(holdByDefault: Boolean = false) {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = 300.dp)) {
                KeysLayout(player = player, channel = CHANNEL, holdByDefault = holdByDefault)
            }
        }
    }

    private fun key(name: String): SemanticsNodeInteraction = composeRule.onNodeWithContentDescription(name)

    // A point in the lower part of a white key, below the black keys.
    private fun lowerCentre(name: String): Offset {
        val bounds = key(name).fetchSemanticsNode().boundsInRoot
        return Offset(bounds.center.x, bounds.bottom - 10f)
    }

    private fun upperCentre(name: String): Offset {
        val bounds = key(name).fetchSemanticsNode().boundsInRoot
        return Offset(bounds.center.x, bounds.top + 10f)
    }

    private fun touch(block: androidx.compose.ui.test.TouchInjectionScope.() -> Unit) {
        composeRule.onRoot().performTouchInput(block)
        composeRule.waitForIdle()
    }

    @Test
    fun smallPhoneShowsNearlyTwoOctavesAtTouchSize() {
        show()

        key("C 3").assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(72.dp)
        key("C 4").assertIsDisplayed()
        key("C sharp 3").assertWidthIsEqualTo(BlackKeyWidth)
    }

    @Test
    fun fingerPlaysTheKeyUntilItLifts() {
        show()
        val e3 = lowerCentre("E 3")

        touch { down(0, e3) }
        val whileHeld = player.events.toList()
        touch { up(0) }

        assertEquals(listOf(On(CHANNEL, 52, FIXED_VELOCITY, 0f)), whileHeld)
        assertEquals(Off(CHANNEL, 52, 0f), player.events.last())
    }

    @Test
    fun threeFingersPlayAChord() {
        show()
        val c = lowerCentre("C 3")
        val e = lowerCentre("E 3")
        val g = lowerCentre("G 3")

        touch {
            down(0, c)
            down(1, e)
            down(2, g)
        }
        val chord = player.ons.map { it.key }
        touch {
            up(0)
            up(1)
            up(2)
        }

        assertEquals(listOf(48, 52, 55), chord)
        assertEquals(setOf(48, 52, 55), player.offs.map { it.key }.toSet())
    }

    @Test
    fun upperPartOfABlackKeyPlaysTheSharp() {
        show()

        touch {
            down(0, upperCentre("F sharp 3"))
            up(0)
        }

        assertEquals(listOf(54), player.ons.map { it.key })
    }

    @Test
    fun slidingOntoAnotherKeyKeepsTheFirstNote() {
        show()
        val c = lowerCentre("C 3")
        val g = lowerCentre("G 3")

        touch {
            down(0, c)
            moveTo(0, g)
            up(0)
        }

        assertEquals(listOf(On(CHANNEL, 48, FIXED_VELOCITY, 0f), Off(CHANNEL, 48, 0f)), player.events)
    }

    @Test
    fun holdKeepsNotesRingingUntilTurnedOff() {
        show()
        composeRule.onNodeWithText(context.getString(R.string.hold)).performClick()

        touch {
            down(0, lowerCentre("D 3"))
            up(0)
        }
        val whileHeld = player.offs.toList()
        composeRule.onNodeWithText(context.getString(R.string.hold)).performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<Off>(), whileHeld)
        assertEquals(listOf(Off(CHANNEL, 50, 0f)), player.offs)
    }

    @Test
    fun holdStartsOnForInstrumentsThatDefaultToIt() {
        show(holdByDefault = true)

        composeRule.onNodeWithText(context.getString(R.string.hold)).assertIsSelected()
    }

    @Test
    fun octaveUpShowsTheNextOctave() {
        show()

        composeRule.onNodeWithContentDescription(context.getString(R.string.octave_up)).performClick()
        touch {
            down(0, lowerCentre("C 4"))
            up(0)
        }

        key("C 3").assertDoesNotExist()
        assertEquals(listOf(60), player.ons.map { it.key })
    }

    @Test
    fun octaveDownStopsAtTheBottomOfThePiano() {
        show()
        val down = composeRule.onNodeWithContentDescription(context.getString(R.string.octave_down))

        repeat(6) { down.performClick() }

        key("A 0").assertIsDisplayed()
    }

    @Test
    fun draggingTheScrollStripLeftShowsHigherKeys() {
        show()
        val strip = composeRule.onNodeWithContentDescription(context.getString(R.string.scroll_keyboard))

        strip.performTouchInput {
            down(centerRight)
            moveTo(centerLeft)
            up()
        }
        composeRule.waitForIdle()

        key("C 3").assertDoesNotExist()
    }

    @Test
    fun accessibilityTapPlaysTheNoteBriefly() {
        show()

        key("A 3").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(On(CHANNEL, 57, FIXED_VELOCITY, 0f), Off(CHANNEL, 57, 400f)), player.events)
    }

    @Test
    fun leavingTheLayoutStopsSoundingNotes() {
        var shown by mutableStateOf(true)
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = 300.dp)) {
                if (shown) KeysLayout(player = player, channel = CHANNEL, holdByDefault = true)
            }
        }
        touch {
            down(0, lowerCentre("C 3"))
            up(0)
        }

        shown = false
        composeRule.waitForIdle()

        assertTrue(Off(CHANNEL, 48, 0f) in player.offs)
    }

    private companion object {
        const val CHANNEL = 0
    }
}
