package com.bizzeh.synthkit.play

import com.bizzeh.synthkit.audio.InstrumentPlayer

/**
 * Pulses once for each note a finger starts. Delayed notes, such as the later
 * strings of a strum, belong to a press that already pulsed.
 */
class HapticNotePlayer(private val player: InstrumentPlayer, private val pulse: () -> Unit) : InstrumentPlayer by player {
    override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean {
        val accepted = player.noteOn(channel, key, velocity, delayMillis)
        if (accepted && delayMillis == 0f) pulse()
        return accepted
    }
}
