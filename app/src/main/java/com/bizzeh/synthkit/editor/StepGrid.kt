package com.bizzeh.synthkit.editor

import com.bizzeh.synthkit.drums.DrumPadOrder
import com.bizzeh.synthkit.looper.TakeBuilder
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.TICKS_PER_BEAT
import kotlin.math.roundToInt

/** A step-grid cell: a 1/16 column and a key. */
data class Cell(val column: Int, val key: Int)

/** Note edits for the loop editor. Every function returns a new list. */
object StepGrid {
    const val STEP_TICKS = TICKS_PER_BEAT / 4
    const val NEW_NOTE_VELOCITY = 100
    private const val MIDDLE_C = 60

    fun columns(loopTicks: Int): Int = loopTicks / STEP_TICKS

    /** Drum rows follow the pad order; melodic rows run from the highest note down. */
    fun rows(drums: Boolean): List<Int> = if (drums) DrumPadOrder else (127 downTo 0).toList()

    /** The row the editor opens at: the first row holding a note, or a sensible start. */
    fun initialRow(rows: List<Int>, notes: List<Note>, drums: Boolean): Int {
        val keys = notes.map { it.key }.toSet()
        val first = rows.indexOfFirst { it in keys }
        return when {
            first >= 0 -> first
            drums -> 0
            else -> rows.indexOf(MIDDLE_C)
        }
    }

    /** The cell a note is drawn in: its raw tick rounded to the nearest step. */
    fun cellOf(note: Note, columns: Int): Cell =
        Cell(Math.floorMod((note.tick.toDouble() / STEP_TICKS).roundToInt(), columns), note.key)

    fun noteAt(notes: List<Note>, cell: Cell, columns: Int): Int? =
        notes.indexOfFirst { cellOf(it, columns) == cell }.takeIf { it >= 0 }

    fun add(notes: List<Note>, cell: Cell): List<Note> =
        notes + Note(cell.column * STEP_TICKS, cell.key, NEW_NOTE_VELOCITY, TakeBuilder.ONE_SHOT_TICKS)

    fun remove(notes: List<Note>, index: Int): List<Note> = notes.filterIndexed { i, _ -> i != index }

    /** Moves a note to a cell; its length and velocity stay. */
    fun move(notes: List<Note>, index: Int, cell: Cell): List<Note> =
        notes.mapIndexed { i, note -> if (i == index) note.copy(tick = cell.column * STEP_TICKS, key = cell.key) else note }

    fun setVelocity(notes: List<Note>, index: Int, velocity: Int): List<Note> =
        notes.mapIndexed { i, note -> if (i == index) note.copy(velocity = velocity.coerceIn(1, 127)) else note }
}
