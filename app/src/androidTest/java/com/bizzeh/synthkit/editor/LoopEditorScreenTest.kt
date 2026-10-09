package com.bizzeh.synthkit.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoopEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var notes by mutableStateOf(listOf<Note>())
    private var quantise by mutableStateOf(Quantise.OFF)
    private var bars by mutableStateOf(1)
    private var doubled = 0

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show() {
        composeRule.setContent {
            val track = Track("t", 128, 0, takes = if (notes.isEmpty()) emptyList() else listOf(Take(notes)), quantise = quantise)
            Box(Modifier.size(640.dp, 340.dp)) {
                LoopEditorScreen(
                    project = Project(id = "p", name = "P", loopBars = bars, tracks = listOf(track)),
                    track = track,
                    instrumentName = "Standard 1",
                    onBack = {},
                    onQuantise = { quantise = it },
                    onNotes = { notes = it },
                    onDoubleLoop = { doubled++ },
                )
            }
        }
    }

    // Centre of a grid cell; the grid opens at the first drum row (kick) with no scroll.
    private fun cell(column: Int, row: Int): Offset {
        val grid = composeRule.onNode(hasContentDescription("Step grid", substring = true)).fetchSemanticsNode().boundsInRoot
        val size = with(composeRule.density) { 48.dp.toPx() }
        return Offset(grid.left + (column + 0.5f) * size, grid.top + (row + 0.5f) * size)
    }

    private fun tap(column: Int, row: Int) {
        val at = cell(column, row)
        composeRule.onRoot().performTouchInput { click(at) }
        composeRule.waitForIdle()
    }

    @Test
    fun tappingAnEmptyStepAddsANoteOnThatRow() {
        show()

        tap(4, 1)

        assertEquals(listOf(Note(480, 38, 100, 120)), notes)
    }

    @Test
    fun tappingANoteSelectsItAndTappingAgainRemovesIt() {
        notes = listOf(Note(0, 36, 100, 120))
        show()

        tap(0, 0)
        composeRule.onNodeWithText(string(R.string.velocity)).assertIsDisplayed()
        tap(0, 0)

        assertTrue(notes.isEmpty())
    }

    @Test
    fun draggingANoteMovesIt() {
        notes = listOf(Note(0, 36, 100, 120))
        show()
        val from = cell(0, 0)
        val to = cell(3, 2)

        composeRule.onRoot().performTouchInput {
            down(from)
            moveTo(from + Offset(20f, 20f))
            moveTo(to)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf(Note(360, 42, 100, 120)), notes)
    }

    @Test
    fun velocitySliderAndDeleteEditTheSelectedNote() {
        notes = listOf(Note(0, 36, 100, 120))
        show()
        tap(0, 0)

        composeRule.onNodeWithContentDescription(string(R.string.velocity))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        assertEquals(40, notes.single().velocity)
        composeRule.onNodeWithText(string(R.string.note_delete)).performClick()

        assertTrue(notes.isEmpty())
    }

    @Test
    fun quantiseAndDoubleLoopReport() {
        show()

        composeRule.onNodeWithText(string(R.string.quantise_sixteenth)).performClick()
        composeRule.onNodeWithText(string(R.string.double_loop)).performClick()

        assertEquals(Quantise.SIXTEENTH, quantise)
        assertEquals(1, doubled)
    }

    @Test
    fun doubleLoopIsDisabledAtEightBars() {
        bars = 8
        show()

        composeRule.onNodeWithText(string(R.string.double_loop)).assertIsNotEnabled()
    }

    @Test
    fun gridDescribesItsSizeForTalkBack() {
        notes = listOf(Note(0, 36, 100, 120))
        show()

        composeRule.onNodeWithContentDescription(string(R.string.grid_description, "16 steps", "1 note")).assertIsDisplayed()
    }
}
