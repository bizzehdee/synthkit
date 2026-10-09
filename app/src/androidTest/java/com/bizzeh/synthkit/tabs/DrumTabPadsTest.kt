package com.bizzeh.synthkit.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.drums.DrumKitPads
import com.bizzeh.synthkit.drums.DrumKitPadsTest
import com.bizzeh.synthkit.drums.shortDrumName
import com.bizzeh.synthkit.testing.RecordingPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plays the user's drum tabs through the pad grid, one 16th step at a time,
 * pressing every hit of a step together, and checks each step's notes.
 */
@RunWith(AndroidJUnit4::class)
class DrumTabPadsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val player = RecordingPlayer()

    private fun fixture(name: String) = instrumentation.context.assets.open(name).bufferedReader().readText()

    @Test
    fun drumTab1PlaysThroughThePads() = playThroughPads("drum-tab-1.txt")

    @Test
    fun drumTab2PlaysThroughThePads() = playThroughPads("drum-tab-2.txt")

    private fun playThroughPads(name: String) {
        // Tall enough for the 4 x 4 first page, which holds every note the tabs use.
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 640.dp, height = DrumKitPadsTest.TALL_HEIGHT)) {
                DrumKitPads(player = player, channel = 0, color = Color.Cyan)
            }
        }
        val steps = Tab.drums(fixture(name), Tab::gmDrum).groupBy({ it.step }, { it.note })
        val padCentres = steps.values.flatten().toSet().associateWith { note ->
            val label = instrumentation.targetContext.getString(requireNotNull(shortDrumName(note)) { "note $note is not on page 1" })
            composeRule.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot.center
        }
        assertTrue("tab has no hits", steps.isNotEmpty())

        val played = mutableMapOf<Int, List<Int>>()
        steps.toSortedMap().forEach { (step, notes) ->
            val distinct = notes.distinct()
            val before = player.ons.size
            composeRule.onRoot().performTouchInput {
                distinct.forEachIndexed { finger, note -> down(finger, padCentres.getValue(note) + Offset(0f, finger.toFloat())) }
                distinct.indices.forEach { up(it) }
            }
            composeRule.waitForIdle()
            played[step] = player.ons.drop(before).map { it.key }
        }

        steps.forEach { (step, notes) ->
            assertEquals("step $step", notes.distinct().sorted(), played.getValue(step).sorted())
        }
    }
}
