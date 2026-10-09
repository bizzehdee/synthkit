package com.bizzeh.synthkit.audio

/** A note of the loop snapshot. [tick] is inside the loop; velocity 0 is a note off. */
data class LoopNote(val tick: Int, val channel: Int, val key: Int, val velocity: Float)

/** A live note captured while recording, at its latency-compensated clock tick. */
data class RecordedEvent(val tick: Long, val channel: Int, val key: Int, val velocity: Float) {
    val isStopMarker: Boolean get() = channel == STOP_MARKER_CHANNEL

    companion object {
        /** Must match Sequencer::kStopMarkerChannel. */
        const val STOP_MARKER_CHANNEL = 255
        private const val FIELDS = 4

        fun fromNative(values: DoubleArray): List<RecordedEvent> {
            require(values.size % FIELDS == 0) { "Recorded values are not in fours: ${values.size}" }
            return values.toList().chunked(FIELDS).map { (tick, channel, key, velocity) ->
                RecordedEvent(tick.toLong(), channel.toInt(), key.toInt(), velocity.toFloat())
            }
        }
    }
}

/** The engine's tick clock, loop playback, metronome and recording. 480 ticks per beat. */
interface Transport {
    fun startTransport(): Boolean
    fun stopTransport(): Boolean
    fun setTempo(bpm: Int): Boolean
    fun setClick(on: Boolean): Boolean
    fun setRecording(on: Boolean): Boolean
    fun setLoop(origin: Long, length: Int, playing: Boolean): Boolean
    fun setVolume(channel: Int, volume: Float): Boolean

    /** Returns false and keeps the old loop when any note is out of range. */
    fun publishLoopNotes(notes: List<LoopNote>): Boolean

    /** Everything recorded since the last call, oldest first. */
    fun drainRecorded(): List<RecordedEvent>

    fun clockTicks(): Double

    companion object {
        /** Must match Sequencer::kClickChannel. */
        const val CLICK_CHANNEL = 15
    }
}
