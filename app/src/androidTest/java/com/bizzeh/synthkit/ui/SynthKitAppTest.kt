package com.bizzeh.synthkit.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.browser.Library
import com.bizzeh.synthkit.export.ExportActions
import com.bizzeh.synthkit.export.ExportState
import com.bizzeh.synthkit.looper.SessionViewModel
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.StoredProject
import com.bizzeh.synthkit.settings.AppSettings
import com.bizzeh.synthkit.settings.SettingsActions
import com.bizzeh.synthkit.settings.ThemeChoice
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
        create = { click -> Project(id = "p2", name = "Project 2", metronomeOnPlayback = click).also { projects = projects + StoredProject(it, 1) } },
        rename = { _, _ -> },
        duplicate = {},
        delete = {},
        save = { saved -> projects = projects.map { if (it.project.id == saved.id) StoredProject(saved, 2) else it } },
    )
    private val sessions = SessionViewModel()
    private var settings by mutableStateOf(AppSettings())
    private val settingsActions = SettingsActions(
        setTheme = { settings = settings.copy(theme = it) },
        setHaptics = { settings = settings.copy(haptics = it) },
        setClickInNewProjects = { settings = settings.copy(clickInNewProjects = it) },
        setNewTrackQuantise = { settings = settings.copy(newTrackQuantise = it) },
    )
    private var pulses = 0
    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            pulses++
        }
    }

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private fun pressBack() = composeRule.runOnUiThread {
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
    }

    private fun show(state: EngineState = ready) {
        composeRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
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
                settings = settings,
                settingsActions = settingsActions,
            )
            }
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

        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription(string(R.string.phase_no_loop_track)).assertIsDisplayed()
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

        composeRule.onNodeWithText("Standard 1").performClick()
        composeRule.onNodeWithText(string(R.string.family_keys)).performClick()
        composeRule.onNodeWithText("Acoustic Grand Piano").assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.family_drums_percussion)).performClick()
        composeRule.onNodeWithText("808/909").performClick()

        composeRule.onNodeWithText("808/909").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
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

    @Test
    fun gearOpensSettingsWhereChoicesChangeAndBackReturns() {
        show()

        composeRule.onNodeWithContentDescription(string(R.string.settings)).performClick()
        composeRule.onNodeWithText(string(R.string.theme_dark)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.settings_haptics)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.settings_click)).performClick()
        composeRule.onNodeWithText(string(R.string.quantise_sixteenth)).performClick()

        assertEquals(AppSettings(ThemeChoice.DARK, haptics = false, clickInNewProjects = true, newTrackQuantise = Quantise.SIXTEENTH), settings)
        pressBack()
        composeRule.onNodeWithText("Song").assertIsDisplayed()
    }

    @Test
    fun newProjectsAndTracksStartFromTheSettings() {
        settings = AppSettings(clickInNewProjects = true, newTrackQuantise = Quantise.EIGHTH)
        show()

        composeRule.onNodeWithText(string(R.string.project_new)).performClick()
        addTrack(R.string.family_keys)

        val created = projects.single { it.project.id == "p2" }.project
        assertEquals(true, created.metronomeOnPlayback)
        assertEquals(Quantise.EIGHTH, created.tracks.single().quantise)
    }

    @Test
    fun padsPulseOnlyWhenHapticsAreOn() {
        openProject()
        addTrack(R.string.family_drums_percussion)

        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).performTouchInput { down(center); up() }
        composeRule.waitForIdle()
        assertEquals(1, pulses)
        settings = settings.copy(haptics = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).performTouchInput { down(center); up() }
        composeRule.waitForIdle()

        assertEquals(1, pulses)
    }
}
