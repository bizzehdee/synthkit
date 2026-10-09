package com.bizzeh.synthkit.editor

import com.bizzeh.synthkit.project.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepGridTest {
    private val notes = listOf(Note(0, 36, 100, 120), Note(250, 38, 90, 120), Note(1910, 42, 80, 120))

    @Test
    fun aBarHasSixteenColumns() {
        assertEquals(16, StepGrid.columns(1920))
        assertEquals(128, StepGrid.columns(1920 * 8))
    }

    @Test
    fun drumRowsFollowThePadsAndMelodicRowsRunDownFromTheTop() {
        assertEquals(36, StepGrid.rows(drums = true).first())
        assertEquals(47, StepGrid.rows(drums = true).size)
        assertEquals(127, StepGrid.rows(drums = false).first())
        assertEquals(128, StepGrid.rows(drums = false).size)
    }

    @Test
    fun editorOpensAtTheFirstRowWithANote() {
        val melodic = StepGrid.rows(drums = false)

        assertEquals(melodic.indexOf(72), StepGrid.initialRow(melodic, listOf(Note(0, 60, 1, 1), Note(0, 72, 1, 1)), false))
        assertEquals(melodic.indexOf(60), StepGrid.initialRow(melodic, emptyList(), false))
        assertEquals(0, StepGrid.initialRow(StepGrid.rows(true), emptyList(), true))
    }

    @Test
    fun notesShowInTheNearestColumnWrappingAtTheEnd() {
        assertEquals(Cell(2, 38), StepGrid.cellOf(notes[1], 16))
        assertEquals(Cell(0, 42), StepGrid.cellOf(notes[2], 16))
    }

    @Test
    fun noteAtFindsOnlyTheMatchingCell() {
        assertEquals(1, StepGrid.noteAt(notes, Cell(2, 38), 16))
        assertNull(StepGrid.noteAt(notes, Cell(2, 36), 16))
    }

    @Test
    fun addPutsANewSixteenthOnTheCell() {
        val added = StepGrid.add(notes, Cell(4, 46))

        assertEquals(Note(480, 46, 100, 120), added.last())
        assertEquals(4, added.size)
    }

    @Test
    fun removeDropsOnlyThatNote() {
        assertEquals(listOf(notes[0], notes[2]), StepGrid.remove(notes, 1))
    }

    @Test
    fun moveChangesTimeAndPitchButKeepsLengthAndVelocity() {
        val moved = StepGrid.move(notes, 1, Cell(8, 40))

        assertEquals(Note(960, 40, 90, 120), moved[1])
    }

    @Test
    fun velocityIsKeptInTheMidiRange() {
        assertEquals(127, StepGrid.setVelocity(notes, 0, 300)[0].velocity)
        assertEquals(1, StepGrid.setVelocity(notes, 0, 0)[0].velocity)
    }
}
