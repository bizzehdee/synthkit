package com.bizzeh.synthkit.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.AudioEngine
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativeEncoderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val folder = File(context.cacheDir, "encoder-test")
    private lateinit var engine: AudioEngine
    private val project = Project(
        id = "p",
        name = "P",
        loopBars = 1,
        tracks = listOf(Track("t", 128, 0, takes = listOf(Take(listOf(Note(0, 36, 110, 120), Note(960, 38, 110, 120)))))),
    )

    @Before
    fun setUp() {
        folder.mkdirs()
        engine = requireNotNull(AudioEngine.load(context.assets))
    }

    @After
    fun tearDown() {
        engine.close()
        folder.deleteRecursively()
    }

    private fun render() = requireNotNull(engine.openExport(ExportPlan.spec(project, passes = 1)))

    @Test
    fun flacFileStartsWithItsMarkerAndIsSmallerThanWav() {
        val file = File(folder, "out.flac")

        render().use { runBlocking { NativeEncoder.write(ExportFormat.FLAC, it, file) } }

        val bytes = file.readBytes()
        assertEquals("fLaC", String(bytes, 0, 4, Charsets.US_ASCII))
        // 4 s of 16-bit stereo at 44.1 kHz is 705,600 bytes as WAV.
        assertTrue(bytes.size in 1_000 until 705_600)
    }

    @Test
    fun mp3FileStartsWithAFrameAtAbout192Kbps() {
        val file = File(folder, "out.mp3")
        val progress = mutableListOf<Float>()

        render().use { runBlocking { NativeEncoder.write(ExportFormat.MP3, it, file) { p -> progress += p } } }

        val bytes = file.readBytes()
        assertEquals(0xFF, bytes[0].toInt() and 0xFF)
        assertEquals(0xFA, bytes[1].toInt() and 0xFE)
        // 4 s at 192 kbit/s is about 96,000 bytes.
        assertTrue("size ${bytes.size}", bytes.size in 90_000..100_000)
        assertEquals(1f, progress.last())
    }

    @Test
    fun cancelledEncodeLeavesNoFile() {
        val file = File(folder, "cancelled.mp3")

        val cancelled = runCatching {
            render().use { runBlocking(Job().apply { cancel() }) { NativeEncoder.write(ExportFormat.MP3, it, file) } }
        }

        assertTrue(cancelled.exceptionOrNull() is CancellationException)
        assertFalse(file.exists())
    }
}
