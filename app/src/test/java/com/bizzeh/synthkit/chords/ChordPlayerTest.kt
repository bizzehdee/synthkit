package com.bizzeh.synthkit.chords

import com.bizzeh.synthkit.audio.NotePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class ChordPlayerTest {
    private data class Event(val on: Boolean, val key: Int, val delay: Float)

    private val events = mutableListOf<Event>()
    private val player = object : NotePlayer {
        override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean {
            events += Event(true, key, delayMillis)
            return true
        }

        override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean {
            events += Event(false, key, delayMillis)
            return true
        }
    }

    private val ons get() = events.filter { it.on }

    @Test
    fun padPressStrumsTheChordLowToHigh() {
        val chords = ChordPlayer(player, 0, bassRoots = false)

        chords.pressPad(0)

        assertEquals(listOf(40, 48, 52, 55, 60, 64), ons.map { it.key })
        assertEquals(listOf(0f, 12f, 24f, 36f, 48f, 60f), ons.map { it.delay })
    }

    @Test
    fun padReleaseStopsItsNotes() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.pressPad(4)
        events.clear()

        chords.releasePad(4)

        assertEquals(Harmony.guitarVoicing(Harmony.triads(0, Mode.MAJOR)[4]).toSet(), events.map { it.key }.toSet())
        assertEquals(true, events.none { it.on })
    }

    @Test
    fun keyAndModeChangeTheChord() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.key = 9
        chords.mode = Mode.MINOR

        chords.pressPad(0)

        assertEquals(Harmony.guitarVoicing(Triad(0, 9, Quality.MINOR)), ons.map { it.key })
    }

    @Test
    fun strumStripPlaysSingleStringsOfTheHeldChord() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.pressPad(0)
        events.clear()

        chords.strikeString(1)
        chords.strikeString(2)

        // Each string restarts: its note was already sounding from the pad press.
        assertEquals(
            listOf(Event(false, 48, 0f), Event(true, 48, 0f), Event(false, 52, 0f), Event(true, 52, 0f)),
            events,
        )
    }

    @Test
    fun strumStripUsesTheLatestHeldPad() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.pressPad(0)
        chords.pressPad(3)
        chords.releasePad(3)
        events.clear()

        chords.strikeString(5)

        assertEquals(listOf(64), ons.map { it.key })
    }

    @Test
    fun strumStripDoesNothingWithoutAHeldPad() {
        val chords = ChordPlayer(player, 0, bassRoots = false)

        chords.strikeString(0)

        assertEquals(emptyList<Event>(), events)
    }

    @Test
    fun padStaysUntilItsLastFingerLifts() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.pressPad(0)
        chords.pressPad(0)
        chords.releasePad(0)
        val afterOneLift = events.count { !it.on }

        chords.releasePad(0)
        chords.releasePad(0)

        assertEquals(6, afterOneLift)
        assertEquals(6 + 6, events.count { !it.on })
    }

    @Test
    fun bassPadsPlayOnlyTheRootWithOctaveShift() {
        val bass = ChordPlayer(player, 0, bassRoots = true)
        bass.bassOctaveShift = 1

        bass.pressPad(4)
        bass.releasePad(4)

        // G1 is 31; one octave up is 43.
        assertEquals(listOf(Event(true, 43, 0f), Event(false, 43, 0f)), events)
    }

    @Test
    fun releaseAllStopsEverySoundingNote() {
        val chords = ChordPlayer(player, 0, bassRoots = false)
        chords.pressPad(0)
        chords.pressPad(5)
        events.clear()

        chords.releaseAll()
        chords.releasePad(0)

        val expected = (Harmony.guitarVoicing(Harmony.triads(0, Mode.MAJOR)[0]) +
            Harmony.guitarVoicing(Harmony.triads(0, Mode.MAJOR)[5])).toSet()
        assertEquals(expected, events.map { it.key }.toSet())
        assertEquals(expected.size, events.size)
    }
}
