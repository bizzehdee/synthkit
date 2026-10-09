package com.bizzeh.synthkit.audio

import android.content.res.AssetManager
import com.bizzeh.synthkit.export.ExportSpec

/**
 * Low-latency SoundFont playback. Note calls must come from one thread at a
 * time; the native side hands them to the audio thread through a single-producer
 * queue.
 */
class AudioEngine private constructor(private var handle: Long) : InstrumentPlayer, Transport, AutoCloseable {

    fun start(): Boolean = nativeStart(checkOpen())

    fun stop() = nativeStop(checkOpen())

    /** Returns false when the note is out of range or the event queue is full. */
    override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean =
        nativeNoteOn(checkOpen(), channel, key, velocity, delayMillis)

    override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean =
        nativeNoteOff(checkOpen(), channel, key, delayMillis)

    override fun allNotesOff(channel: Int): Boolean = nativeAllNotesOff(checkOpen(), channel)

    /** Returns false when the SoundFont has no such preset. */
    fun programChange(channel: Int, bank: Int, program: Int): Boolean =
        nativeProgramChange(checkOpen(), channel, bank, program)

    override fun selectInstrument(channel: Int, bank: Int, program: Int): Boolean =
        allNotesOff(channel) && programChange(channel, bank, program)

    override fun startTransport(): Boolean = nativeStartTransport(checkOpen())

    override fun stopTransport(): Boolean = nativeStopTransport(checkOpen())

    override fun setTempo(bpm: Int): Boolean = nativeSetTempo(checkOpen(), bpm)

    override fun setClick(on: Boolean): Boolean = nativeSetClick(checkOpen(), on)

    override fun setRecording(on: Boolean): Boolean = nativeSetRecording(checkOpen(), on)

    override fun setLoop(origin: Long, length: Int, playing: Boolean): Boolean =
        nativeSetLoop(checkOpen(), origin, length, playing)

    override fun setVolume(channel: Int, volume: Float): Boolean = nativeSetVolume(checkOpen(), channel, volume)

    override fun publishLoopNotes(notes: List<LoopNote>): Boolean = nativePublishLoopNotes(
        checkOpen(),
        IntArray(notes.size) { notes[it].tick },
        IntArray(notes.size) { notes[it].channel },
        IntArray(notes.size) { notes[it].key },
        FloatArray(notes.size) { notes[it].velocity },
    )

    override fun drainRecorded(): List<RecordedEvent> = RecordedEvent.fromNative(nativeDrainRecorded(checkOpen()))

    override fun clockTicks(): Double = nativeClockTicks(checkOpen())

    /** Recording subtracts this from every live note, so takes match what the player heard. */
    fun setLatency(millis: Float): Boolean = nativeSetLatency(checkOpen(), millis)

    /** Opens an offline render; safe off the main thread while playing. Null when refused. */
    fun openExport(spec: ExportSpec): ExportRender? = nativeOpenExport(
        checkOpen(),
        spec.sampleRate, spec.bpm, spec.loopTicks, spec.passes, spec.tailMillis,
        IntArray(spec.tracks.size) { spec.tracks[it].channel },
        IntArray(spec.tracks.size) { spec.tracks[it].bank },
        IntArray(spec.tracks.size) { spec.tracks[it].program },
        FloatArray(spec.tracks.size) { spec.tracks[it].volume },
        IntArray(spec.notes.size) { spec.notes[it].tick },
        IntArray(spec.notes.size) { spec.notes[it].channel },
        IntArray(spec.notes.size) { spec.notes[it].key },
        FloatArray(spec.notes.size) { spec.notes[it].velocity },
    ).takeIf { it != 0L }?.let { ExportRender(it, spec.sampleRate) }

    fun presets(): List<Preset> = Preset.fromNative(nativePresets(checkOpen()))

    /** Null when no stream is running. */
    fun latencyReport(): LatencyReport? = LatencyReport.fromNative(nativeLatencyReport(checkOpen()))

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private fun checkOpen(): Long {
        check(handle != 0L) { "AudioEngine is closed" }
        return handle
    }

    companion object {
        const val SOUND_FONT_ASSET = "GeneralUser-GS.sf2"

        init {
            System.loadLibrary("synthkit")
        }

        /** Loads the bundled SoundFont. Slow: call it off the main thread. Returns null on failure. */
        fun load(assets: AssetManager): AudioEngine? =
            nativeCreate(assets, SOUND_FONT_ASSET).takeIf { it != 0L }?.let(::AudioEngine)

        @JvmStatic private external fun nativeCreate(assets: AssetManager, assetPath: String): Long
        @JvmStatic private external fun nativeDestroy(handle: Long)
        @JvmStatic private external fun nativeStart(handle: Long): Boolean
        @JvmStatic private external fun nativeStop(handle: Long)
        @JvmStatic private external fun nativeNoteOn(
            handle: Long,
            channel: Int,
            key: Int,
            velocity: Float,
            delayMillis: Float,
        ): Boolean
        @JvmStatic private external fun nativeNoteOff(handle: Long, channel: Int, key: Int, delayMillis: Float): Boolean
        @JvmStatic private external fun nativeAllNotesOff(handle: Long, channel: Int): Boolean
        @JvmStatic private external fun nativeProgramChange(handle: Long, channel: Int, bank: Int, program: Int): Boolean
        @JvmStatic private external fun nativePresets(handle: Long): Array<String>
        @JvmStatic private external fun nativeSetVolume(handle: Long, channel: Int, volume: Float): Boolean
        @JvmStatic private external fun nativeStartTransport(handle: Long): Boolean
        @JvmStatic private external fun nativeStopTransport(handle: Long): Boolean
        @JvmStatic private external fun nativeSetTempo(handle: Long, bpm: Int): Boolean
        @JvmStatic private external fun nativeSetClick(handle: Long, on: Boolean): Boolean
        @JvmStatic private external fun nativeSetRecording(handle: Long, on: Boolean): Boolean
        @JvmStatic private external fun nativeSetLoop(handle: Long, origin: Long, length: Int, playing: Boolean): Boolean
        @JvmStatic private external fun nativeSetLatency(handle: Long, millis: Float): Boolean
        @JvmStatic private external fun nativeClockTicks(handle: Long): Double
        @JvmStatic private external fun nativePublishLoopNotes(
            handle: Long,
            ticks: IntArray,
            channels: IntArray,
            keys: IntArray,
            velocities: FloatArray,
        ): Boolean
        @JvmStatic private external fun nativeDrainRecorded(handle: Long): DoubleArray
        @JvmStatic private external fun nativeOpenExport(
            handle: Long,
            sampleRate: Int,
            bpm: Int,
            loopTicks: Int,
            passes: Int,
            tailMillis: Int,
            trackChannels: IntArray,
            banks: IntArray,
            programs: IntArray,
            volumes: FloatArray,
            ticks: IntArray,
            channels: IntArray,
            keys: IntArray,
            velocities: FloatArray,
        ): Long
        @JvmStatic private external fun nativeLatencyReport(handle: Long): Array<String>
    }
}
