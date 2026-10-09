package com.bizzeh.synthkit.testing

import com.bizzeh.synthkit.audio.NotePlayer

/** Records every note event so UI tests can assert what was played, without audio. */
class RecordingNotePlayer : NotePlayer {
    sealed interface Event {
        val key: Int
    }

    data class On(val channel: Int, override val key: Int, val velocity: Float, val delayMillis: Float) : Event
    data class Off(val channel: Int, override val key: Int, val delayMillis: Float) : Event

    val events = mutableListOf<Event>()

    val ons: List<On> get() = events.filterIsInstance<On>()
    val offs: List<Off> get() = events.filterIsInstance<Off>()

    override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean {
        events += On(channel, key, velocity, delayMillis)
        return true
    }

    override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean {
        events += Off(channel, key, delayMillis)
        return true
    }
}
