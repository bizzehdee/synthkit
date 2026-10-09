package com.bizzeh.synthkit.chords

import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.FIXED_VELOCITY

/**
 * Plays chord pads. A pad press strums its chord low to high; the strum strip
 * plays single strings of the most recently pressed pad that is still held.
 * Each pad's notes stop when its last finger lifts. Bass pads play the root only.
 */
class ChordPlayer(
    private val player: NotePlayer,
    private val channel: Int,
    private val bassRoots: Boolean,
) {
    var key: Int = 0
    var mode: Mode = Mode.MAJOR
    var bassOctaveShift: Int = 0

    private val fingers = mutableMapOf<Int, Int>()
    private val sounding = mutableMapOf<Int, MutableSet<Int>>()
    private val pressOrder = mutableListOf<Int>()
    private val voicings = mutableMapOf<Int, List<Int>>()

    val triads: List<Triad> get() = Harmony.triads(key, mode)

    fun pressPad(degree: Int) {
        fingers[degree] = (fingers[degree] ?: 0) + 1
        pressOrder.remove(degree)
        pressOrder += degree
        val triad = triads[degree]
        val notes = if (bassRoots) listOf(Harmony.bassRoot(triad, bassOctaveShift)) else Harmony.guitarVoicing(triad)
        voicings[degree] = notes
        notes.forEachIndexed { string, note -> strike(degree, note, string * STRUM_SPACING_MILLIS) }
    }

    fun releasePad(degree: Int) {
        val remaining = (fingers[degree] ?: return) - 1
        if (remaining > 0) {
            fingers[degree] = remaining
            return
        }
        fingers.remove(degree)
        pressOrder.remove(degree)
        voicings.remove(degree)
        sounding.remove(degree)?.forEach { player.noteOff(channel, it) }
    }

    /** Plays one string of the latest held chord. Does nothing when no pad is held. */
    fun strikeString(string: Int) {
        val degree = pressOrder.lastOrNull() ?: return
        val note = voicings[degree]?.getOrNull(string) ?: return
        strike(degree, note, 0f)
    }

    fun releaseAll() {
        sounding.values.flatten().toSet().forEach { player.noteOff(channel, it) }
        fingers.clear()
        sounding.clear()
        pressOrder.clear()
        voicings.clear()
    }

    private fun strike(degree: Int, note: Int, delayMillis: Float) {
        val notes = sounding.getOrPut(degree) { mutableSetOf() }
        if (!notes.add(note)) player.noteOff(channel, note, delayMillis)
        player.noteOn(channel, note, FIXED_VELOCITY, delayMillis)
    }

    companion object {
        const val STRUM_SPACING_MILLIS = 12f
        const val STRINGS = 6
    }
}
