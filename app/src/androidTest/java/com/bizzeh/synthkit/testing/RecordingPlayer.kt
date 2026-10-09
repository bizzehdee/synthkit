package com.bizzeh.synthkit.testing

import com.bizzeh.synthkit.audio.InstrumentPlayer

/** Records every event so UI tests can assert what was played, without audio. */
class RecordingPlayer : InstrumentPlayer {
    sealed interface Event

    data class On(val channel: Int, val key: Int, val velocity: Float, val delayMillis: Float) : Event
    data class Off(val channel: Int, val key: Int, val delayMillis: Float) : Event
    data class Select(val channel: Int, val bank: Int, val program: Int) : Event
    data class AllOff(val channel: Int) : Event

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

    override fun selectInstrument(channel: Int, bank: Int, program: Int): Boolean {
        events += Select(channel, bank, program)
        return true
    }

    override fun allNotesOff(channel: Int): Boolean {
        events += AllOff(channel)
        return true
    }
}
