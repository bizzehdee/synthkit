package com.bizzeh.synthkit.play

import com.bizzeh.synthkit.audio.NotePlayer

/**
 * Notes that sound while a finger holds them, with sustain-pedal style hold:
 * while hold is on, released notes ring until hold is turned off or the note is
 * played again. Several fingers may hold the same note; it stops when the last
 * one lifts.
 */
class HeldNotes(private val player: NotePlayer, private val channel: Int, hold: Boolean = false) {
    private val fingers = mutableMapOf<Int, Int>()
    private val sustained = mutableSetOf<Int>()

    var hold: Boolean = hold
        set(value) {
            field = value
            if (!value) {
                sustained.forEach { player.noteOff(channel, it) }
                sustained.clear()
            }
        }

    fun press(key: Int) {
        if (sustained.remove(key)) player.noteOff(channel, key)
        player.noteOn(channel, key, FIXED_VELOCITY)
        fingers[key] = (fingers[key] ?: 0) + 1
    }

    fun release(key: Int) {
        val remaining = (fingers[key] ?: return) - 1
        if (remaining > 0) {
            fingers[key] = remaining
            return
        }
        fingers.remove(key)
        if (hold) sustained += key else player.noteOff(channel, key)
    }

    /** Stops every note, held or sustained. */
    fun releaseAll() {
        (fingers.keys + sustained).forEach { player.noteOff(channel, it) }
        fingers.clear()
        sustained.clear()
    }
}
