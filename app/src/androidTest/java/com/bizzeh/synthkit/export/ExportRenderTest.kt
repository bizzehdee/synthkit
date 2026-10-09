package com.bizzeh.synthkit.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.AudioEngine
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ExportRenderTest {
    private lateinit var engine: AudioEngine
    private val project = Project(
        id = "p",
        name = "P",
        loopBars = 1,
        tracks = listOf(Track("t", 128, 0, takes = listOf(Take(listOf(Note(0, 36, 110, 120), Note(960, 38, 110, 120)))))),
    )

    @Before
    fun load() {
        engine = requireNotNull(AudioEngine.load(InstrumentationRegistry.getInstrumentation().targetContext.assets))
        engine.start()
    }

    @After
    fun close() = engine.close()

    @Test
    fun rendersTheWholeExportWhileLivePlayingContinues() {
        val render = requireNotNull(engine.openExport(ExportPlan.spec(project, passes = 2)))
        val buffer = ShortArray(4096 * 2)
        var frames = 0L
        var loudest = 0

        render.use {
            while (true) {
                engine.noteOn(1, 60, 0.5f)
                val read = it.read(buffer)
                if (read == 0) break
                frames += read
                for (i in 0 until read * 2) loudest = maxOf(loudest, abs(buffer[i].toInt()))
            }
        }

        // 2 bars at 120 BPM = 4 s, plus the 2 s tail.
        assertEquals(6L * 44_100, frames)
        assertTrue("export is silent", loudest > 1_000)
    }

    @Test
    fun badRequestsAreRefused() {
        val spec = ExportPlan.spec(project, 1)

        assertNull(engine.openExport(spec.copy(passes = 0)))
        assertNull(engine.openExport(spec.copy(sampleRate = 1)))
        assertNull(engine.openExport(spec.copy(notes = listOf(ExportNote(0, 16, 36, 1f)))))
        assertNull(engine.openExport(spec.copy(notes = listOf(ExportNote(5_000, 1, 36, 1f)))))
    }
}
