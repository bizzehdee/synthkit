package com.bizzeh.synthkit.drums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.testing.RecordingNotePlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrumScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val ready = EngineState.Ready(RecordingNotePlayer())
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

    @Test
    fun loadingShowsProgressAndNoPads() {
        composeRule.setContent { DrumScreen(state = EngineState.Loading, latency = null) }

        composeRule.onNodeWithContentDescription(context.getString(R.string.loading_sounds)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).assertDoesNotExist()
    }

    @Test
    fun failureShowsTheErrorMessage() {
        composeRule.setContent { DrumScreen(state = EngineState.Failed, latency = null) }

        composeRule.onNodeWithText(context.getString(R.string.sounds_failed)).assertIsDisplayed()
    }

    @Test
    fun latencyReportIsShownAboveThePads() {
        composeRule.setContent { DrumScreen(state = ready, latency = report) }

        composeRule.onNode(hasText("11.3 ms", substring = true)).assertIsDisplayed()
        composeRule.onNode(hasText("burst 96", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.drum_kick)).assertIsDisplayed()
    }

    @Test
    fun unmeasuredLatencySaysMeasuring() {
        composeRule.setContent { DrumScreen(state = ready, latency = report.copy(outputLatencyMs = null)) }

        composeRule.onNode(hasText(context.getString(R.string.latency_measuring), substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun noReportShowsNoReadout() {
        composeRule.setContent { DrumScreen(state = ready, latency = null) }

        composeRule.onNode(hasText("burst", substring = true)).assertDoesNotExist()
    }
}
