package com.bizzeh.synthkit.play

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
import org.junit.Assert.assertTrue
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
    private val report = LatencyReport(
        outputLatencyMs = 11.25,
        audioApi = "AAudio",
        performanceMode = "LowLatency",
        sharingMode = "Exclusive",
        sampleRate = 48000,
        framesPerBurst = 96,
        bufferFrames = 192,
        underruns = 0,
    )
    private var backs = 0
    private var changes = 0
    private val opened = mutableListOf<String>()
    private val openedFromLayout = mutableListOf<String>()
    private var warning: LatencyWarning? = null
    private var dismissals = 0
    private val kits = listOf(catalogue.byId("128:0")!!, catalogue.byId("128:25")!!)

    private fun show(instrument: Instrument, latency: LatencyReport? = null) {
        composeRule.setContent {
            PlayScreen(
                instrument = instrument,
                player = player,
                latency = latency,
                onBack = { backs++ },
                onChangeInstrument = { changes++ },
                onOpened = { opened += it.id },
                kits = kits,
                onOpenInstrument = { openedFromLayout += it.id },
                warning = warning,
                onDismissWarning = { dismissals++ },
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun openingSelectsTheInstrumentOnTheLiveChannelAndReportsIt() {
        show(kit)

        assertEquals(listOf(RecordingPlayer.Select(LIVE_CHANNEL, 128, 0)), player.events)
        assertEquals(listOf("128:0"), opened)
        composeRule.onNodeWithText("Standard 1").assertIsDisplayed()
    }

    @Test
    fun drumKitShowsTheDrumPads() {
        show(kit)

        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).assertIsDisplayed()
    }

    @Test
    fun kitPickerOpensTheChosenKit() {
        show(kit)

        composeRule.onNodeWithText(context.getString(R.string.drum_kit, "Standard 1")).performClick()
        composeRule.onNodeWithText("808/909").performClick()

        assertEquals(listOf("128:25"), openedFromLayout)
    }

    @Test
    fun pitchedPercussionShowsChromaticPads() {
        show(catalogue.byId("0:47")!!)

        composeRule.onNodeWithContentDescription("C 3").assertIsDisplayed()
    }

    @Test
    fun guitarShowsChordPads() {
        show(catalogue.byId("0:24")!!)

        composeRule.onNodeWithContentDescription("C major").assertIsDisplayed()
    }

    @Test
    fun pianoShowsTheKeyboard() {
        show(catalogue.byId("0:0")!!)

        composeRule.onNodeWithContentDescription("C 3").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.hold)).assertIsDisplayed()
    }

    @Test
    fun switchingInstrumentSelectsTheNewOne() {
        var current by mutableStateOf(kit)
        composeRule.setContent {
            PlayScreen(current, player, null, {}, {}, {}, kits, {}, null, {})
        }
        composeRule.waitForIdle()

        current = catalogue.byId("128:25")!!
        composeRule.waitForIdle()

        assertEquals(
            listOf(RecordingPlayer.Select(LIVE_CHANNEL, 128, 0), RecordingPlayer.Select(LIVE_CHANNEL, 128, 25)),
            player.events.filterIsInstance<RecordingPlayer.Select>(),
        )
    }

    @Test
    fun leavingTheScreenSilencesTheLiveChannel() {
        var shown by mutableStateOf(true)
        composeRule.setContent {
            if (shown) PlayScreen(kit, player, null, {}, {}, {}, kits, {}, null, {})
        }
        composeRule.waitForIdle()

        shown = false
        composeRule.waitForIdle()

        assertTrue(player.events.last() == RecordingPlayer.AllOff(LIVE_CHANNEL))
    }

    @Test
    fun backAndChangeInstrumentButtonsCallBack() {
        show(kit)

        composeRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.change_instrument)).performClick()

        assertEquals(1, backs)
        assertEquals(1, changes)
    }

    @Test
    fun latencyReportIsShownWhenGiven() {
        show(kit, report)

        composeRule.onNode(hasText("11.3 ms", substring = true)).assertIsDisplayed()
        composeRule.onNode(hasText("burst 96", substring = true)).assertIsDisplayed()
    }

    @Test
    fun unmeasuredLatencySaysMeasuring() {
        show(kit, report.copy(outputLatencyMs = null))

        composeRule.onNode(hasText(context.getString(R.string.latency_measuring), substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun bluetoothWarningIsShownAndCanBeDismissed() {
        warning = LatencyWarning.BLUETOOTH
        show(kit)

        composeRule.onNodeWithText(context.getString(R.string.warning_bluetooth)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.dismiss)).performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun lowLatencyWarningKeepsThePadsPlayable() {
        warning = LatencyWarning.NOT_LOW_LATENCY
        show(kit)

        composeRule.onNodeWithText(context.getString(R.string.warning_not_low_latency)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).assertIsDisplayed()
    }

    @Test
    fun noWarningShowsNoBanner() {
        show(kit)

        composeRule.onNodeWithText(context.getString(R.string.dismiss)).assertDoesNotExist()
    }

    @Test
    fun noReportShowsNoReadout() {
        show(kit)

        composeRule.onNode(hasText("burst", substring = true)).assertDoesNotExist()
    }
}
