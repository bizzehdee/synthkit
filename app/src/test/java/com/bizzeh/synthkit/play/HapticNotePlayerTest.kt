package com.bizzeh.synthkit.play

import com.bizzeh.synthkit.looper.FakePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticNotePlayerTest {
    private val player = FakePlayer()
    private var pulses = 0
    private val haptic = HapticNotePlayer(player) { pulses++ }

    @Test
    fun eachStartedNotePulsesOnceAndStillPlays() {
        haptic.noteOn(1, 36, 1f)
        haptic.noteOn(1, 38, 1f)

        assertEquals(2, pulses)
        assertEquals(listOf("on 1 36 1.0", "on 1 38 1.0"), player.played)
    }

    @Test
    fun delayedNotesAndReleasesDoNotPulse() {
        haptic.noteOn(1, 40, 1f, delayMillis = 15f)
        haptic.noteOff(1, 40)
        haptic.allNotesOff(1)

        assertEquals(0, pulses)
        assertEquals(listOf(1), player.silenced)
    }
}
