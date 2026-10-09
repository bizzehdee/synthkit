package com.bizzeh.synthkit.play

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.testing.RecordingPlayer
import com.bizzeh.synthkit.testing.testCatalogue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalogue = testCatalogue()
    private val kit = catalogue.byId("128:0")!!
    private val player = RecordingPlayer()
    private val report = LatencyReport(11.25, "AAudio", "LowLatency", "Exclusive", 48000, 96, 192, 0)
    private var backs = 0
    private var changes = 0
    private var dismissals = 0

    private fun show(instrument: Instrument, latency: LatencyReport? = null, warning: LatencyWarning? = null) {
        composeRule.setContent {
            PlayScreen(
                instrument = instrument,
                channel = CHANNEL,
                player = player,
                latency = latency,
                warning = warning,
                onDismissWarning = { dismissals++ },
                onBack = { backs++ },
                onChangeInstrument = { changes++ },
            ) { Text("transport slot") }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun drumKitShowsThePadsNameAndTransport() {
        show(kit)

        composeRule.onNodeWithText("Standard 1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).assertIsDisplayed()
        composeRule.onNodeWithText("transport slot").assertIsDisplayed()
    }

    @Test
    fun padsPlayOnTheTracksChannel() {
        show(kit)

        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).performClick()

        assertEquals(CHANNEL, player.ons.single().channel)
    }

    @Test
    fun eachLayoutShowsForItsInstrument() {
        var shown by mutableStateOf(catalogue.byId("0:47")!!)
        composeRule.setContent { PlayScreen(shown, CHANNEL, player, null, null, {}, {}, {}) }

        composeRule.onNodeWithContentDescription("C 3").assertIsDisplayed()
        shown = catalogue.byId("0:24")!!
        composeRule.onNodeWithContentDescription("C major").assertIsDisplayed()
        shown = catalogue.byId("0:0")!!
        composeRule.onNodeWithContentDescription(context.getString(R.string.hold)).assertIsDisplayed()
    }

    @Test
    fun leavingTheScreenSilencesTheTracksChannel() {
        var shown by mutableStateOf(true)
        composeRule.setContent { if (shown) PlayScreen(kit, CHANNEL, player, null, null, {}, {}, {}) }
        composeRule.waitForIdle()

        shown = false
        composeRule.waitForIdle()

        assertEquals(RecordingPlayer.AllOff(CHANNEL), player.events.last())
    }

    @Test
    fun backAndTheInstrumentChipAndMenuCallBack() {
        show(kit)

        composeRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        composeRule.onNodeWithText("Standard 1").performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.change_instrument)).performClick()

        assertEquals(1, backs)
        assertEquals(2, changes)
    }

    @Test
    fun warningIsShownAndCanBeDismissed() {
        show(kit, warning = LatencyWarning.BLUETOOTH)

        composeRule.onNodeWithText(context.getString(R.string.warning_bluetooth)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dismiss)).performClick()

        assertEquals(1, dismissals)
    }

    private fun openDiagnostics() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.more)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.diagnostics)).performClick()
    }

    @Test
    fun latencyStaysOffThePlaySurfaceAndShowsInDiagnostics() {
        show(kit, latency = report)
        composeRule.onNode(hasText("11.3 ms", substring = true)).assertDoesNotExist()

        openDiagnostics()

        composeRule.onNode(hasText("11.3 ms", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.close)).performClick()
        composeRule.onNode(hasText("11.3 ms", substring = true)).assertDoesNotExist()
    }

    @Test
    fun unmeasuredLatencySaysMeasuring() {
        show(kit, latency = report.copy(outputLatencyMs = null))

        openDiagnostics()

        composeRule.onNode(hasText(context.getString(R.string.latency_measuring), substring = true)).assertIsDisplayed()
    }

    @Test
    fun diagnosticsWithoutAudioSaySo() {
        show(kit)

        openDiagnostics()

        composeRule.onNodeWithText(context.getString(R.string.diagnostics_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dismiss)).assertDoesNotExist()
    }

    private companion object {
        const val CHANNEL = 3
    }
}
