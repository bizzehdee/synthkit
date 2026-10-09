package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import com.bizzeh.synthkit.testing.RecordingPlayer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrumKitPadsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val player = RecordingPlayer()

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(height: Dp) {
        composeRule.setContent {
            Box(modifier = Modifier.requiredSize(width = 640.dp, height = height)) {
                DrumKitPads(player = player, channel = CHANNEL, color = Color.Cyan)
            }
        }
    }

    private fun centre(name: String): Offset =
        composeRule.onNodeWithContentDescription(name).fetchSemanticsNode().boundsInRoot.center

    private fun nextPage() {
        composeRule.onNodeWithContentDescription(string(R.string.pad_page_next)).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun smallPhoneShowsTheFirstEightPadsAtTouchSizeOnPageOneOfSix() {
        show(PHONE_HEIGHT)

        listOf(
            R.string.drum_kick, R.string.drum_snare, R.string.drum_closed_hat, R.string.drum_open_hat,
            R.string.drum_low_tom, R.string.drum_high_tom, R.string.drum_crash, R.string.drum_ride,
        ).forEach {
            composeRule.onNodeWithContentDescription(string(it))
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(72.dp)
        }
        composeRule.onNodeWithContentDescription(string(R.string.drum_clap)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page, 1, 6)).assertIsDisplayed()
    }

    @Test
    fun nextAndPreviousButtonsChangeThePage() {
        show(PHONE_HEIGHT)
        composeRule.onNodeWithContentDescription(string(R.string.pad_page_previous)).assertIsNotEnabled()

        nextPage()

        composeRule.onNodeWithContentDescription(string(R.string.drum_clap)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page, 2, 6)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page_previous)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page, 1, 6)).assertIsDisplayed()
    }

    @Test
    fun swipingAcrossThePadsDoesNotChangeThePage() {
        show(PHONE_HEIGHT)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.pad_page, 1, 6)).assertIsDisplayed()
    }

    @Test
    fun lastPageHoldsTheHighestGmPercussionNote() {
        show(PHONE_HEIGHT)

        repeat(5) { nextPage() }

        composeRule.onNodeWithContentDescription("Open Triangle").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page_next)).assertIsNotEnabled()
    }

    @Test
    fun tallerAreaShowsSixteenPadsOnPageOneOfThree() {
        show(TALL_HEIGHT)

        composeRule.onNodeWithContentDescription(string(R.string.drum_floor_tom)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.drum_splash)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.pad_page, 1, 3)).assertExists()
    }

    @Test
    fun fingerDownPlaysOnceAndReleaseIsSilent() {
        show(PHONE_HEIGHT)
        val snare = centre(string(R.string.drum_snare))

        composeRule.onRoot().performTouchInput { down(0, snare) }
        composeRule.waitForIdle()
        val onDown = player.events.toList()
        composeRule.onRoot().performTouchInput { up(0) }
        composeRule.waitForIdle()

        assertEquals(listOf(RecordingPlayer.On(CHANNEL, 38, FIXED_VELOCITY, 0f)), onDown)
        assertEquals(onDown, player.events)
    }

    @Test
    fun aTapThatSlidesALittleDoesNotMoveThePads() {
        show(PHONE_HEIGHT)
        val snare = centre(string(R.string.drum_snare))
        val slide = with(composeRule.density) { 30.dp.toPx() }

        composeRule.onRoot().performTouchInput {
            down(0, snare)
            moveTo(0, snare - Offset(slide / 2, 0f))
            moveTo(0, snare - Offset(slide, 0f))
        }
        composeRule.waitForIdle()
        val during = centre(string(R.string.drum_snare))
        composeRule.onRoot().performTouchInput { up(0) }

        assertEquals(snare, during)
    }

    @Test
    fun twoFingersOnTwoPadsPlayBothNotes() {
        show(PHONE_HEIGHT)
        val kick = centre(string(R.string.drum_kick))
        val hat = centre(string(R.string.drum_closed_hat))

        composeRule.onRoot().performTouchInput {
            down(0, kick)
            down(1, hat)
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(36, 42), player.ons.map { it.key })
    }

    @Test
    fun secondFingerOnAPadThatIsAlreadyHeldPlaysAgain() {
        show(PHONE_HEIGHT)
        val crash = centre(string(R.string.drum_crash))

        composeRule.onRoot().performTouchInput {
            down(0, crash)
            down(1, crash + Offset(10f, 10f))
            up(1)
            up(0)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(49, 49), player.ons.map { it.key })
    }

    @Test
    fun accessibilityClickPlaysThePad() {
        show(PHONE_HEIGHT)

        composeRule.onNodeWithContentDescription(string(R.string.drum_ride)).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(listOf(51), player.ons.map { it.key })
    }

    companion object {
        private const val CHANNEL = 0
        private val PHONE_HEIGHT = 300.dp

        /**
         * Fits four 72 dp rows plus the page bar. It is taller than the Galaxy A03
         * window, so tests lay it out with requiredSize and the page bar falls off-screen.
         */
        val TALL_HEIGHT = 368.dp
    }
}
