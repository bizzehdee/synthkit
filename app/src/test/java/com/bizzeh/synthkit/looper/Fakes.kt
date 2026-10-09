package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.InstrumentPlayer
import com.bizzeh.synthkit.audio.LoopNote
import com.bizzeh.synthkit.audio.RecordedEvent
import com.bizzeh.synthkit.audio.Transport

class FakeTransport : Transport {
    val calls = mutableListOf<String>()
    val pending = mutableListOf<RecordedEvent>()
    var clock = 0.0
    var loopNotes: List<LoopNote> = emptyList()
    var click = false
    var recording = false
    var running = false
    var loop: Triple<Long, Int, Boolean>? = null

    override fun startTransport(): Boolean { calls += "start"; running = true; return true }
    override fun stopTransport(): Boolean { calls += "stop"; running = false; return true }
    override fun setTempo(bpm: Int): Boolean { calls += "tempo $bpm"; return true }
    override fun setClick(on: Boolean): Boolean { click = on; return true }
    override fun setRecording(on: Boolean): Boolean { recording = on; return true }
    override fun setLoop(origin: Long, length: Int, playing: Boolean): Boolean { loop = Triple(origin, length, playing); return true }
    override fun setVolume(channel: Int, volume: Float): Boolean { calls += "volume $channel $volume"; return true }
    override fun publishLoopNotes(notes: List<LoopNote>): Boolean { loopNotes = notes; return true }
    override fun drainRecorded(): List<RecordedEvent> = pending.toList().also { pending.clear() }
    override fun clockTicks(): Double = clock

    fun on(tick: Long, channel: Int, key: Int) { pending += RecordedEvent(tick, channel, key, 100 / 127f) }
    fun off(tick: Long, channel: Int, key: Int) { pending += RecordedEvent(tick, channel, key, 0f) }
    fun stopMarker(tick: Long) { pending += RecordedEvent(tick, RecordedEvent.STOP_MARKER_CHANNEL, 0, 0f) }
}

class FakePlayer : InstrumentPlayer {
    val selected = mutableMapOf<Int, Pair<Int, Int>>()
    val silenced = mutableListOf<Int>()
    val played = mutableListOf<String>()

    override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean { played += "on $channel $key $velocity"; return true }
    override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean { played += "off $channel $key ${delayMillis}ms"; return true }
    override fun selectInstrument(channel: Int, bank: Int, program: Int): Boolean { selected[channel] = bank to program; return true }
    override fun allNotesOff(channel: Int): Boolean { silenced += channel; return true }
}
