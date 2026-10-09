package com.bizzeh.synthkit.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.browser.Library
import com.bizzeh.synthkit.export.ExportActions
import com.bizzeh.synthkit.export.ExportState
import com.bizzeh.synthkit.looper.SessionViewModel
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.StoredProject
import com.bizzeh.synthkit.testing.FakeTransport
import com.bizzeh.synthkit.testing.RecordingPlayer
import com.bizzeh.synthkit.testing.testCatalogue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SynthKitAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val player = RecordingPlayer()
    private val ready = EngineState.Ready(player, FakeTransport(), testCatalogue())
    private var library by mutableStateOf(Library())
    private val opened = mutableListOf<String>()
    private var projects by mutableStateOf(listOf(StoredProject(Project(id = "p1", name = "Song"), 0)))
    private val actions = ProjectActions(
        create = { Project(id = "p2", name = "Project 2").also { projects = projects + StoredProject(it, 1) } },
        rename = { _, _ -> },
        duplicate = {},
        delete = {},
        save = { saved -> projects = projects.map { if (it.project.id == saved.id) StoredProject(saved, 2) else it } },
    )
    private val sessions = SessionViewModel()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private fun pressBack() = composeRule.runOnUiThread {
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    private fun show(state: EngineState = ready) {
        composeRule.setContent {
            SynthKitApp(
                engineState = state,
                latency = null,
                library = library,
                onToggleFavourite = { library = library.toggleFavourite(it) },
                onInstrumentOpened = { opened += it },
                warning = null,
                onDismissWarning = {},
                projects = projects,
                projectActions = actions,
                sessions = sessions,
                exportState = ExportState.Idle,
                exportActions = ExportActions({ _, _, _, _, _ -> }, {}, {}, { _, _ -> }),
            )
        }
    }

    private fun openProject() {
        show()
        composeRule.onNodeWithText("Song").performClick()
    }

    private fun addTrack(family: Int) {
        composeRule.onNodeWithText(string(R.string.add_track)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.home_open_family, string(family))).performClick()
    }

    @Test
    fun loadingShowsProgress() {
        show(EngineState.Loading)

        composeRule.onNodeWithContentDescription(string(R.string.loading_sounds)).assertIsDisplayed()
    }

    @Test
    fun failureShowsTheErrorMessage() {
        show(EngineState.Failed)

        composeRule.onNodeWithText(string(R.string.sounds_failed)).assertIsDisplayed()
    }

    @Test
    fun launchShowsTheProjectList() {
        show()

        composeRule.onNodeWithText(string(R.string.projects_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Song").assertIsDisplayed()
    }

    @Test
    fun newProjectOpensItAtOnceAndBackReturnsToTheList() {
        show()

        composeRule.onNodeWithText(string(R.string.project_new)).performClick()
        composeRule.onNodeWithText(string(R.string.no_tracks)).assertIsDisplayed()

        pressBack()
        composeRule.onNodeWithText("Project 2").assertIsDisplayed()
    }

    @Test
    fun addTrackOffersTheFourQuickEntriesAndBrowse() {
        openProject()

        composeRule.onNodeWithText(string(R.string.add_track)).performClick()

        listOf(R.string.family_keys, R.string.family_guitar_bass, R.string.family_drums_percussion, R.string.family_synth)
            .forEach { composeRule.onNodeWithContentDescription(string(R.string.home_open_family, string(it))).assertIsDisplayed() }
        composeRule.onNodeWithText(string(R.string.home_browse)).assertIsDisplayed()
    }

    @Test
    fun drumsQuickEntryAddsADrumTrackAndOpensIt() {
        openProject()

        addTrack(R.string.family_drums_percussion)

        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.record)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.phase_no_loop_track)).assertIsDisplayed()
        assertEquals(listOf("128:0"), opened)
        assertEquals(RecordingPlayer.Select(1, 128, 0), player.events.filterIsInstance<RecordingPlayer.Select>().last())
        assertEquals(1, projects.single { it.project.id == "p1" }.project.tracks.size)
    }

    @Test
    fun backFromTheTrackReturnsToTheProjectWithTheNewTrack() {
        openProject()
        addTrack(R.string.family_guitar_bass)

        pressBack()

        composeRule.onNodeWithText("Acoustic Guitar (nylon)").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.add_track)).assertIsDisplayed()
    }

    @Test
    fun browseAddsTheChosenInstrument() {
        openProject()
        composeRule.onNodeWithText(string(R.string.add_track)).performClick()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithText(string(R.string.browser_search)).performTextInput("808")
        composeRule.onNodeWithText("808/909").performClick()

        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("808/909").assertIsDisplayed()
    }

    @Test
    fun changingADrumTrackInstrumentOffersOnlyKits() {
        openProject()
        addTrack(R.string.family_drums_percussion)

        composeRule.onNodeWithContentDescription(string(R.string.change_instrument)).performClick()
        composeRule.onNodeWithText(string(R.string.family_keys)).performClick()
        composeRule.onNodeWithText("Acoustic Grand Piano").assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.family_drums_percussion)).performClick()
        composeRule.onNodeWithText("808/909").performClick()

        composeRule.onNodeWithText(string(R.string.drum_kit, "808/909")).assertIsDisplayed()
    }

    @Test
    fun favouriteToggleAddsTheInstrumentToFavourites() {
        openProject()
        composeRule.onNodeWithText(string(R.string.add_track)).performClick()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.favourite_add, "Acoustic Grand Piano")).performClick()
        composeRule.onNodeWithText(string(R.string.browser_favourites)).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.favourite_remove, "Acoustic Grand Piano")).assertIsDisplayed()
    }

    @Test
    fun searchWithNoMatchSaysSo() {
        openProject()
        composeRule.onNodeWithText(string(R.string.add_track)).performClick()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithText(string(R.string.browser_search)).performTextInput("zzzz")

        composeRule.onNodeWithText(string(R.string.browser_no_results)).assertIsDisplayed()
    }
}
