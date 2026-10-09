package com.bizzeh.synthkit.testing

import com.bizzeh.synthkit.audio.LoopNote
import com.bizzeh.synthkit.audio.RecordedEvent
import com.bizzeh.synthkit.audio.Transport

/** A transport with no audio: it accepts every command and records nothing. */
class FakeTransport : Transport {
    var loopNotes: List<LoopNote> = emptyList()

    override fun startTransport() = true
    override fun stopTransport() = true
    override fun setTempo(bpm: Int) = true
    override fun setClick(on: Boolean) = true
    override fun setRecording(on: Boolean) = true
    override fun setLoop(origin: Long, length: Int, playing: Boolean) = true
    override fun setVolume(channel: Int, volume: Float) = true
    override fun publishLoopNotes(notes: List<LoopNote>): Boolean {
        loopNotes = notes
        return true
    }
    override fun drainRecorded(): List<RecordedEvent> = emptyList()
    override fun clockTicks() = 0.0
}
