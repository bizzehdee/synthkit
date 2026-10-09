package com.bizzeh.synthkit.export

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {
    private fun written(frames: Long): ByteBuffer {
        val out = ByteArrayOutputStream()
        runBlocking { WavWriter.write(FakePcmSource(frames), out) }
        return ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun ascii(buffer: ByteBuffer, at: Int) = String(ByteArray(4) { buffer.get(at + it) }, Charsets.US_ASCII)

    @Test
    fun headerDescribes16BitStereoAt44100() {
        val wav = written(10_000)

        assertEquals("RIFF", ascii(wav, 0))
        assertEquals(36 + 40_000, wav.getInt(4))
        assertEquals("WAVE", ascii(wav, 8))
        assertEquals("fmt ", ascii(wav, 12))
        assertEquals(1, wav.getShort(20).toInt())
        assertEquals(2, wav.getShort(22).toInt())
        assertEquals(44_100, wav.getInt(24))
        assertEquals(44_100 * 4, wav.getInt(28))
        assertEquals(4, wav.getShort(32).toInt())
        assertEquals(16, wav.getShort(34).toInt())
        assertEquals("data", ascii(wav, 36))
        assertEquals(40_000, wav.getInt(40))
        assertEquals(44 + 40_000, wav.capacity())
    }

    @Test
    fun samplesAreLittleEndianAndInOrderAcrossBlocks() {
        val wav = written(10_000)

        assertEquals(0, wav.getShort(44).toInt())
        assertEquals(5_000, wav.getShort(44 + 5_000 * 4).toInt())
        assertEquals(-5_000, wav.getShort(44 + 5_000 * 4 + 2).toInt())
    }

    @Test
    fun progressReachesOne() {
        val progress = mutableListOf<Float>()

        runBlocking { WavWriter.write(FakePcmSource(10_000), ByteArrayOutputStream()) { progress += it } }

        assertEquals(1f, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test(expected = IOException::class)
    fun aShortSourceFailsInsteadOfWritingABrokenFile() {
        runBlocking { WavWriter.write(FakePcmSource(totalFrames = 10_000, deliver = 9_000), ByteArrayOutputStream()) }
    }

    @Test(expected = CancellationException::class)
    fun cancellingStopsTheWrite() {
        runBlocking(Job().apply { cancel() }) { WavWriter.write(FakePcmSource(10_000), ByteArrayOutputStream()) }
    }
}
