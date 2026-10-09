package com.bizzeh.synthkit.play

import com.bizzeh.synthkit.audio.NotePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class HeldNotesTest {
    private val events = mutableListOf<String>()
    private val player = object : NotePlayer {
        override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean {
            events += "on $channel $key"
            return true
        }

        override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean {
            events += "off $channel $key"
            return true
        }
    }

    @Test
    fun noteSoundsWhileHeldAndStopsOnRelease() {
        val notes = HeldNotes(player, channel = 2)

        notes.press(60)
        notes.release(60)

        assertEquals(listOf("on 2 60", "off 2 60"), events)
    }

    @Test
    fun secondFingerOnTheSameNoteKeepsItSoundingUntilBothLift() {
        val notes = HeldNotes(player, channel = 0)

        notes.press(60)
        notes.press(60)
        notes.release(60)
        val afterFirstLift = events.toList()
        notes.release(60)

        assertEquals(listOf("on 0 60", "on 0 60"), afterFirstLift)
        assertEquals("off 0 60", events.last())
    }

    @Test
    fun holdKeepsReleasedNotesRingingUntilHoldIsTurnedOff() {
        val notes = HeldNotes(player, channel = 0, hold = true)

        notes.press(60)
        notes.press(64)
        notes.release(60)
        notes.release(64)
        val whileHeld = events.toList()
        notes.hold = false

        assertEquals(listOf("on 0 60", "on 0 64"), whileHeld)
        assertEquals(setOf("off 0 60", "off 0 64"), events.drop(2).toSet())
    }

    @Test
    fun replayingASustainedNoteRestartsIt() {
        val notes = HeldNotes(player, channel = 0, hold = true)
        notes.press(60)
        notes.release(60)

        notes.press(60)

        assertEquals(listOf("on 0 60", "off 0 60", "on 0 60"), events)
    }

    @Test
    fun releaseWithoutPressIsIgnored() {
        HeldNotes(player, channel = 0).release(60)

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun releaseAllStopsHeldAndSustainedNotes() {
        val notes = HeldNotes(player, channel = 0, hold = true)
        notes.press(60)
        notes.release(60)
        notes.press(67)

        notes.releaseAll()
        notes.release(67)

        assertEquals(setOf("off 0 60", "off 0 67"), events.drop(2).toSet())
        assertEquals(4, events.size)
    }
}
