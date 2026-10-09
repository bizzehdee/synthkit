package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.audio.PcmSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16-bit stereo PCM WAV, streamed: the header uses the source's known length. */
object WavWriter {
    private const val CHANNELS = 2
    private const val BYTES_PER_SAMPLE = 2
    private const val HEADER_BYTES = 44
    private const val BLOCK_FRAMES = 4096

    suspend fun write(source: PcmSource, out: OutputStream, onProgress: (Float) -> Unit = {}) {
        val dataBytes = source.totalFrames * CHANNELS * BYTES_PER_SAMPLE
        require(dataBytes + HEADER_BYTES - 8 <= UInt.MAX_VALUE.toLong()) { "Too long for a WAV file" }
        out.write(header(source.sampleRate, dataBytes))
        val samples = ShortArray(BLOCK_FRAMES * CHANNELS)
        val bytes = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        var written = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val frames = source.read(samples)
            if (frames == 0) break
            bytes.clear()
            bytes.asShortBuffer().put(samples, 0, frames * CHANNELS)
            out.write(bytes.array(), 0, frames * CHANNELS * BYTES_PER_SAMPLE)
            written += frames
            onProgress(written.toFloat() / source.totalFrames)
        }
        // A short source would leave a header that promises more audio than the file holds.
        if (written != source.totalFrames) throw IOException("Expected ${source.totalFrames} frames, got $written")
    }

    private fun header(sampleRate: Int, dataBytes: Long): ByteArray =
        ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((dataBytes + HEADER_BYTES - 8).toInt())
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(CHANNELS.toShort())
            putInt(sampleRate)
            putInt(sampleRate * CHANNELS * BYTES_PER_SAMPLE)
            putShort((CHANNELS * BYTES_PER_SAMPLE).toShort())
            putShort((BYTES_PER_SAMPLE * 8).toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes.toInt())
        }.array()
}
