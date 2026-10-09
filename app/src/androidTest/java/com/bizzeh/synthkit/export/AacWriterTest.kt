package com.bizzeh.synthkit.export

import android.media.MediaExtractor
import android.media.MediaFormat
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
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class AacWriterTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val folder = File(context.cacheDir, "aac-test")
    private lateinit var engine: AudioEngine
    private val project = Project(
        id = "p",
        name = "P",
        loopBars = 1,
        tracks = listOf(Track("t", 0, 0, takes = listOf(Take(listOf(Note(0, 60, 110, 960)))))),
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

    @Test
    fun mp4HoldsOneStereoAacTrackOfTheRightLength() {
        val file = File(folder, "out.m4a")
        val progress = mutableListOf<Float>()

        requireNotNull(engine.openExport(ExportPlan.spec(project, passes = 1))).use {
            runBlocking { AacWriter.write(it, file) { p -> progress += p } }
        }

        val extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
        try {
            assertEquals(1, extractor.trackCount)
            val format = extractor.getTrackFormat(0)
            assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, format.getString(MediaFormat.KEY_MIME))
            assertEquals(44_100, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
            assertEquals(2, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
            // One bar at 120 BPM (2 s) plus the 2 s tail.
            val seconds = format.getLong(MediaFormat.KEY_DURATION) / 1_000_000.0
            assertTrue("duration $seconds", abs(seconds - 4.0) < 0.1)
        } finally {
            extractor.release()
        }
        assertEquals(1f, progress.last())
    }

    @Test
    fun cancelledEncodeLeavesNoFile() {
        val file = File(folder, "cancelled.m4a")

        val result = runCatching {
            requireNotNull(engine.openExport(ExportPlan.spec(project, passes = 1))).use {
                runBlocking(Job().apply { cancel() }) { AacWriter.write(it, file) }
            }
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertFalse(file.exists())
    }
}
