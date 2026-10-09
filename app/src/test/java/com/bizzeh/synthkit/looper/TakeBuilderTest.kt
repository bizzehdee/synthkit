package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.RecordedEvent
import com.bizzeh.synthkit.project.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class TakeBuilderTest {
    private fun on(tick: Long, key: Int, velocity: Float = 100 / 127f) = RecordedEvent(tick, 1, key, velocity)
    private fun off(tick: Long, key: Int) = RecordedEvent(tick, 1, key, 0f)

    @Test
    fun noteOnAndOffBecomeOneNote() {
        val notes = TakeBuilder.build(listOf(on(500, 60), off(740, 60)), origin = 480, loopTicks = 1920, stopTick = 2400, oneShot = false)

        assertEquals(listOf(Note(20, 60, 100, 240)), notes)
    }

    @Test
    fun noteStillHeldAtStopEndsThere() {
        val notes = TakeBuilder.build(listOf(on(500, 60)), 480, 1920, stopTick = 1000, oneShot = false)

        assertEquals(500, notes.single().lengthTicks)
    }

    @Test
    fun repeatedKeyPairsInOrder() {
        val notes = TakeBuilder.build(
            listOf(on(480, 60), on(600, 60), off(700, 60), off(900, 60)),
            480, 1920, 2400, oneShot = false,
        )

        assertEquals(listOf(Note(0, 60, 100, 220), Note(120, 60, 100, 300)), notes)
    }

    @Test
    fun drumHitsAreSixteenthNotes() {
        val notes = TakeBuilder.build(listOf(on(480, 36), on(960, 38)), 480, 1920, 2400, oneShot = true)

        assertEquals(listOf(Note(0, 36, 100, 120), Note(480, 38, 100, 120)), notes)
    }

    @Test
    fun noteJustBeforeTheLoopStartBecomesAPickupAtTheEnd() {
        val notes = TakeBuilder.build(listOf(on(470, 36)), 480, 1920, 2400, oneShot = true)

        assertEquals(1910, notes.single().tick)
    }

    @Test
    fun stopMarkersAndStrayNoteOffsAreIgnored() {
        val notes = TakeBuilder.build(
            listOf(off(500, 60), RecordedEvent(600, RecordedEvent.STOP_MARKER_CHANNEL, 0, 0f)),
            480, 1920, 2400, oneShot = false,
        )

        assertEquals(emptyList<Note>(), notes)
    }

    @Test
    fun lengthNeverReachesTheWholeLoop() {
        val notes = TakeBuilder.build(listOf(on(480, 60)), 480, 1920, stopTick = 480 + 5000, oneShot = false)

        assertEquals(1919, notes.single().lengthTicks)
    }

    @Test
    fun velocityMapsToMidi() {
        val notes = TakeBuilder.build(listOf(on(480, 60, 1f), on(480, 61, 0.001f)), 480, 1920, 2400, oneShot = true)

        assertEquals(listOf(127, 1), notes.map { it.velocity })
    }
}
