package com.bizzeh.synthkit.audio

/** 16-bit interleaved stereo audio read in blocks, for the export encoders. */
interface PcmSource : AutoCloseable {
    val sampleRate: Int
    val totalFrames: Long

    /** Fills [buffer] from the start with up to buffer.size / 2 frames; returns how many, 0 at the end. */
    fun read(buffer: ShortArray): Int
}
