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
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.StoredProject
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
    private val ready = EngineState.Ready(player, testCatalogue())
    private var library by mutableStateOf(Library())
    private val opened = mutableListOf<String>()
    private var projects by mutableStateOf(listOf(StoredProject(Project(id = "p1", name = "Song"), 0)))
    private val actions = ProjectActions(
        create = { Project(id = "p2", name = "Project 2").also { projects = projects + StoredProject(it, 1) } },
        rename = { _, _ -> },
        duplicate = {},
        delete = {},
    )

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
            )
        }
    }

    private fun showProject() {
        show()
        composeRule.onNodeWithText("Song").performClick()
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
    fun newProjectOpensItAtOnce() {
        show()

        composeRule.onNodeWithText(string(R.string.project_new)).performClick()

        composeRule.onNodeWithText(string(R.string.home_browse)).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Project 2").assertIsDisplayed()
    }

    @Test
    fun projectShowsTheFourQuickEntriesAndBrowse() {
        showProject()

        listOf(R.string.family_keys, R.string.family_guitar_bass, R.string.family_drums_percussion, R.string.family_synth)
            .forEach { label ->
                composeRule.onNodeWithContentDescription(string(R.string.home_open_family, string(label)))
                    .assertIsDisplayed()
            }
        composeRule.onNodeWithText(string(R.string.home_browse)).assertIsDisplayed()
    }

    @Test
    fun drumsQuickEntryOpensTheStandardKitAndBackReturnsToTheProject() {
        showProject()

        composeRule.onNodeWithContentDescription(
            string(R.string.home_open_family, string(R.string.family_drums_percussion)),
        ).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
        assertEquals(listOf("128:0"), opened)

        pressBack()
        composeRule.onNodeWithText(string(R.string.home_browse)).assertIsDisplayed()
    }

    @Test
    fun guitarQuickEntryOpensNylonGuitar() {
        showProject()

        composeRule.onNodeWithContentDescription(
            string(R.string.home_open_family, string(R.string.family_guitar_bass)),
        ).performClick()

        composeRule.onNodeWithText("Acoustic Guitar (nylon)").assertIsDisplayed()
        assertEquals(RecordingPlayer.Select(0, 0, 24), player.events.first())
    }

    @Test
    fun browserSearchOpensTheChosenInstrument() {
        showProject()

        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()
        composeRule.onNodeWithText(string(R.string.browser_search)).performTextInput("808")
        composeRule.onNodeWithText("808/909").performClick()

        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
        assertEquals(listOf("128:25"), opened)
    }

    @Test
    fun browserFamilySectionListsItsInstruments() {
        showProject()

        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()
        composeRule.onNodeWithText(string(R.string.family_brass_winds)).performClick()

        composeRule.onNodeWithText("Trumpet").assertIsDisplayed()
    }

    @Test
    fun favouriteToggleAddsTheInstrumentToFavourites() {
        showProject()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.favourite_add, "Acoustic Grand Piano")).performClick()
        composeRule.onNodeWithText(string(R.string.browser_favourites)).performClick()

        composeRule.onNodeWithText("Acoustic Grand Piano").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.favourite_remove, "Acoustic Grand Piano"))
            .assertIsDisplayed()
    }

    @Test
    fun emptyFavouritesAndRecentsExplainThemselves() {
        showProject()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithText(string(R.string.browser_favourites)).performClick()
        composeRule.onNodeWithText(string(R.string.browser_no_favourites)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.browser_recents)).performClick()
        composeRule.onNodeWithText(string(R.string.browser_no_recents)).assertIsDisplayed()
    }

    @Test
    fun recentsListOpenedInstrumentsNewestFirst() {
        library = Library(recents = listOf("0:40", "128:25"))
        showProject()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithText(string(R.string.browser_recents)).performClick()

        composeRule.onNodeWithText("Violin").assertIsDisplayed()
        composeRule.onNodeWithText("808/909").assertIsDisplayed()
    }

    @Test
    fun searchWithNoMatchSaysSo() {
        showProject()
        composeRule.onNodeWithText(string(R.string.home_browse)).performClick()

        composeRule.onNodeWithText(string(R.string.browser_search)).performTextInput("zzzz")

        composeRule.onNodeWithText(string(R.string.browser_no_results)).assertIsDisplayed()
    }

    @Test
    fun changeInstrumentFromPlayOpensTheBrowserAndBackReturnsToPlay() {
        showProject()
        composeRule.onNodeWithContentDescription(
            string(R.string.home_open_family, string(R.string.family_drums_percussion)),
        ).performClick()

        composeRule.onNodeWithText(string(R.string.change_instrument)).performClick()
        composeRule.onNodeWithText(string(R.string.browser_search)).assertIsDisplayed()

        pressBack()
        composeRule.onNodeWithContentDescription(string(R.string.drum_kick)).assertIsDisplayed()
    }
}
