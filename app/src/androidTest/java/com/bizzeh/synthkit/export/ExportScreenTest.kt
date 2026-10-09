package com.bizzeh.synthkit.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(state: ExportState, passes: Int = 2, message: String? = null) {
        composeRule.setContent {
            ExportScreen(
                projectName = "Song",
                state = state,
                format = ExportFormat.WAV,
                passes = passes,
                onFormat = { calls += "format ${it.name}" },
                onPasses = { calls += "passes $it" },
                onExport = { calls += "export" },
                onCancel = { calls += "cancel" },
                onSave = { calls += "save" },
                onShare = { calls += "share" },
                onBack = { calls += "back" },
                message = message,
            )
        }
    }

    @Test
    fun idleOffersFormatsPassesAndExport() {
        show(ExportState.Idle)

        composeRule.onNodeWithText(string(R.string.export_title, "Song")).assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.export_passes, 2, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_format_mp3)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.export_passes_up)).performClick()
        composeRule.onNodeWithText(string(R.string.export_start)).performClick()

        assertEquals(listOf("format MP3", "passes 3", "export"), calls)
    }

    @Test
    fun passesStopAtTheirLimits() {
        show(ExportState.Idle, passes = ExportPlan.MAX_PASSES)

        composeRule.onNodeWithContentDescription(string(R.string.export_passes_up)).assertIsNotEnabled()
    }

    @Test
    fun runningShowsProgressAndCancel() {
        show(ExportState.Running(42))

        composeRule.onNodeWithText(string(R.string.export_running, 42)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeRule.onNodeWithText(string(R.string.export_format_mp3)).assertIsNotEnabled()

        assertEquals(listOf("cancel"), calls)
    }

    @Test
    fun doneOffersSaveAndShare() {
        show(ExportState.Done(File("Song.wav"), ExportFormat.WAV), message = string(R.string.export_saved))

        composeRule.onNodeWithText(string(R.string.export_done, "Song.wav")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_save)).performClick()
        composeRule.onNodeWithText(string(R.string.export_share)).performClick()
        composeRule.onNodeWithText(string(R.string.export_saved)).assertIsDisplayed()

        assertEquals(listOf("save", "share"), calls)
    }

    @Test
    fun failureSaysSoAndOffersAnotherTry() {
        show(ExportState.Failed)

        composeRule.onNodeWithText(string(R.string.export_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_start)).assertIsDisplayed()
    }
}
