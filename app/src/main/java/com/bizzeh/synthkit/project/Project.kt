package com.bizzeh.synthkit.project

import kotlinx.serialization.Serializable

/** Ticks per quarter note. All note timing is stored in ticks, so tempo changes keep the music. */
const val TICKS_PER_BEAT = 480
const val BEATS_PER_BAR = 4
const val TICKS_PER_BAR = TICKS_PER_BEAT * BEATS_PER_BAR

@Serializable
data class Project(
    val formatVersion: Int = FORMAT_VERSION,
    val id: String,
    val name: String,
    val tempoBpm: Int = DEFAULT_TEMPO,
    /** 0 until the first take sets the loop. */
    val loopBars: Int = 0,
    val metronomeOnPlayback: Boolean = false,
    val tracks: List<Track> = emptyList(),
) {
    val loopTicks: Int get() = loopBars * TICKS_PER_BAR

    companion object {
        const val FORMAT_VERSION = 1
        const val DEFAULT_TEMPO = 120
        const val MIN_TEMPO = 40
        const val MAX_TEMPO = 240
        const val MAX_LOOP_BARS = 8
        const val MAX_NAME_LENGTH = 100
    }
}

@Serializable
data class Track(
    val id: String,
    val bank: Int,
    val program: Int,
    /** Oldest first; undo removes the last one. */
    val takes: List<Take> = emptyList(),
    val muted: Boolean = false,
    val solo: Boolean = false,
    val volume: Float = 1f,
    val quantise: Quantise = Quantise.OFF,
) {
    val notes: List<Note> get() = takes.flatMap { it.notes }
}

@Serializable
enum class Quantise(val gridTicks: Int) {
    OFF(0),
    EIGHTH(TICKS_PER_BEAT / 2),
    SIXTEENTH(TICKS_PER_BEAT / 4),
}

@Serializable
data class Take(val notes: List<Note>)

/** [tick] is the raw recorded position in the loop; quantise is applied on playback only. */
@Serializable
data class Note(val tick: Int, val key: Int, val velocity: Int, val lengthTicks: Int)
