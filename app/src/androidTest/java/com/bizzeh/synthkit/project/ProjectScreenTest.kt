package com.bizzeh.synthkit.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.looper.LooperState
import com.bizzeh.synthkit.looper.Phase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()
    private val drums = Track(id = "t1", bank = 128, program = 0, takes = listOf(Take(listOf(Note(0, 36, 100, 120)))))
    private val project = Project(id = "p", name = "Song", tempoBpm = 100, loopBars = 2, tracks = listOf(drums))
    private val actions = TrackActions(
        open = { calls += "open $it" },
        setMuted = { id, on -> calls += "mute $id $on" },
        setSolo = { id, on -> calls += "solo $id $on" },
        setVolume = { _, _ -> },
        undo = { calls += "undo $it" },
        clear = { calls += "clear $it" },
        changeInstrument = { calls += "change $it" },
        delete = { calls += "delete $it" },
        edit = { calls += "edit $it" },
    )

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(state: LooperState = LooperState(project), canAdd: Boolean = true) {
        composeRule.setContent {
            ProjectScreen(
                state = state,
                instrumentName = { "Standard 1" },
                canAddTrack = canAdd,
                onBack = { calls += "back" },
                onAddTrack = { calls += "add" },
                onTempo = { calls += "tempo $it" },
                onClickOnPlayback = { calls += "click $it" },
                onPlayStop = { calls += "playstop" },
                tracks = actions,
            )
        }
    }

    private fun menu(item: Int) {
        composeRule.onNodeWithContentDescription(string(R.string.track_actions, "Standard 1")).performClick()
        composeRule.onNodeWithText(string(item)).performClick()
    }

    @Test
    fun showsTheProjectTempoAndTracks() {
        show()

        composeRule.onNodeWithText("Song").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.tempo_value, 100)).assertIsDisplayed()
        composeRule.onNodeWithText("Standard 1").assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.take_count, 1, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.phase_stopped)).assertIsDisplayed()
    }

    @Test
    fun trackRowControlsCallTheirActions() {
        show()

        composeRule.onNodeWithText("Standard 1").performClick()
        composeRule.onNodeWithText(string(R.string.track_mute)).performClick()
        composeRule.onNodeWithText(string(R.string.track_solo)).performClick()
        menu(R.string.track_undo)
        menu(R.string.track_change_instrument)
        menu(R.string.track_edit)

        assertEquals(listOf("open t1", "mute t1 true", "solo t1 true", "undo t1", "change t1", "edit t1"), calls)
    }

    @Test
    fun deleteAndClearAskFirst() {
        show()

        menu(R.string.track_delete)
        composeRule.onNodeWithText(string(R.string.track_delete_confirm, "Standard 1")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        menu(R.string.track_clear)
        composeRule.onNodeWithText(string(R.string.track_clear)).performClick()

        assertEquals(listOf("clear t1"), calls)
    }

    @Test
    fun tempoButtonsAndClickToggleReport() {
        show()

        composeRule.onNodeWithContentDescription(string(R.string.tempo_up)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.tempo_down)).performClick()
        composeRule.onNodeWithText(string(R.string.click_on_playback)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.play)).performClick()

        assertEquals(listOf("tempo 101", "tempo 99", "click true", "playstop"), calls)
    }

    @Test
    fun playIsDisabledUntilALoopExists() {
        show(LooperState(project.copy(loopBars = 0, tracks = emptyList())))

        composeRule.onNodeWithContentDescription(string(R.string.play)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.no_tracks)).assertIsDisplayed()
    }

    @Test
    fun playingShowsTheBarAndAStopButton() {
        show(LooperState(project, Phase.PLAYING, bar = 1))

        composeRule.onNodeWithText(string(R.string.phase_playing, 2, 2)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.stop)).assertIsDisplayed()
    }

    @Test
    fun addTrackIsDisabledAtTheLimit() {
        show(canAdd = false)

        composeRule.onNodeWithText(string(R.string.add_track)).assertIsNotEnabled()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.track_limit, ProjectValidation.MAX_TRACKS, ProjectValidation.MAX_TRACKS),
        ).assertIsDisplayed()
    }
}
