package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.audio.PcmSource

/** Frame i holds left = i and right = -i, so tests can check every sample. */
class FakePcmSource(override val totalFrames: Long, private val deliver: Long = totalFrames) : PcmSource {
    override val sampleRate = 44_100
    private var position = 0L
    var closed = false

    override fun read(buffer: ShortArray): Int {
        val frames = minOf((buffer.size / 2).toLong(), deliver - position).toInt()
        repeat(frames) {
            buffer[it * 2] = (position + it).toShort()
            buffer[it * 2 + 1] = (-(position + it)).toShort()
        }
        position += frames
        return frames
    }

    override fun close() {
        closed = true
    }
}
