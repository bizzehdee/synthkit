package com.bizzeh.synthkit.chords

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChordsLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val player = RecordingPlayer()
    private val cMajorVoicing = listOf(40, 48, 52, 55, 60, 64)

    private fun show(bassRoots: Boolean = false) {
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = 300.dp)) {
                ChordsLayout(player = player, channel = 0, bassRoots = bassRoots, color = Color.Cyan)
            }
        }
    }

    private fun centre(description: String): Offset =
        composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.center

    private fun touch(block: TouchInjectionScope.() -> Unit) {
        composeRule.onRoot().performTouchInput(block)
        composeRule.waitForIdle()
    }

    @Test
    fun cMajorShowsItsSevenTriadsAtTouchSize() {
        show()

        listOf("C major", "D minor", "E minor", "F major", "G major", "A minor", "B diminished").forEach {
            composeRule.onNodeWithContentDescription(it)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(72.dp)
        }
        composeRule.onNodeWithText("vii°").assertIsDisplayed()
    }

    @Test
    fun padPressStrumsTheChordAndReleaseStopsIt() {
        show()
        val c = centre("C major")

        touch { down(0, c) }
        val strum = player.ons.toList()
        touch { up(0) }

        assertEquals(cMajorVoicing, strum.map { it.key })
        assertEquals(listOf(0f, 12f, 24f, 36f, 48f, 60f), strum.map { it.delayMillis })
        assertEquals(cMajorVoicing.toSet(), player.offs.map { it.key }.toSet())
    }

    @Test
    fun choosingAMinorKeyChangesThePads() {
        show()

        composeRule.onNodeWithText(context.getString(R.string.chord_key, "C")).performClick()
        composeRule.onNodeWithText("A").performClick()
        composeRule.onNodeWithText(context.getString(R.string.mode_minor)).performClick()

        composeRule.onNodeWithContentDescription("A sharp minor").assertDoesNotExist()
        listOf("A minor", "B diminished", "C major", "D minor", "E minor", "F major", "G major").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        composeRule.onNodeWithText("ii°").assertIsDisplayed()
    }

    @Test
    fun swipingTheStripWhileHoldingAPadPlaysEachString() {
        show()
        val g = centre("G major")
        val strip = composeRule.onNodeWithContentDescription(context.getString(R.string.strum_strip))
            .fetchSemanticsNode().boundsInRoot

        touch { down(0, g) }
        player.events.clear()
        touch {
            down(1, Offset(strip.left + 5f, strip.center.y))
            moveTo(1, Offset(strip.right - 5f, strip.center.y))
            up(1)
        }

        assertEquals(Harmony.guitarVoicing(Harmony.triads(0, Mode.MAJOR)[4]), player.ons.map { it.key })
    }

    @Test
    fun stripWithoutAHeldPadIsSilent() {
        show()

        composeRule.onNodeWithContentDescription(context.getString(R.string.strum_strip)).performTouchInput {
            down(centerLeft)
            moveTo(centerRight)
            up()
        }

        assertEquals(emptyList<RecordingPlayer.Event>(), player.events)
    }

    @Test
    fun twoPadsHeldTogetherBothSound() {
        show()
        val c = centre("C major")
        val f = centre("F major")

        touch {
            down(0, c)
            down(1, f)
        }

        assertEquals(12, player.ons.size)
    }

    @Test
    fun bassPadsPlayRootsWithOctaveShiftAndNoStrip() {
        show(bassRoots = true)
        composeRule.onNodeWithContentDescription(context.getString(R.string.strum_strip)).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(context.getString(R.string.octave_up)).performClick()
        touch {
            down(0, centre("C major"))
            up(0)
        }

        assertEquals(listOf(48), player.ons.map { it.key })
    }

    @Test
    fun accessibilityTapStrumsTheChord() {
        show()

        composeRule.onNodeWithContentDescription("A minor").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(Harmony.guitarVoicing(Harmony.triads(0, Mode.MAJOR)[5]), player.ons.map { it.key })
    }
}
