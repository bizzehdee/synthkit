package com.bizzeh.synthkit.audio

import android.content.res.AssetManager

/**
 * Low-latency SoundFont playback. Note calls must come from one thread at a
 * time; the native side hands them to the audio thread through a single-producer
 * queue.
 */
class AudioEngine private constructor(private var handle: Long) : NotePlayer, AutoCloseable {

    fun start(): Boolean = nativeStart(checkOpen())

    fun stop() = nativeStop(checkOpen())

    /** Returns false when the note is out of range or the event queue is full. */
    override fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float): Boolean =
        nativeNoteOn(checkOpen(), channel, key, velocity, delayMillis)

    override fun noteOff(channel: Int, key: Int, delayMillis: Float): Boolean =
        nativeNoteOff(checkOpen(), channel, key, delayMillis)

    fun allNotesOff(channel: Int): Boolean = nativeAllNotesOff(checkOpen(), channel)

    /** Returns false when the SoundFont has no such preset. */
    fun programChange(channel: Int, bank: Int, program: Int): Boolean =
        nativeProgramChange(checkOpen(), channel, bank, program)

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
        @JvmStatic private external fun nativeLatencyReport(handle: Long): Array<String>
    }
}
