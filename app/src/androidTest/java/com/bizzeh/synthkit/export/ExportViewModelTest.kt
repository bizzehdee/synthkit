package com.bizzeh.synthkit.export

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.AudioEngine
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.targetContext.applicationContext as Application
    private lateinit var engine: AudioEngine
    private lateinit var model: ExportViewModel
    private val project = Project(
        id = "p",
        name = "My/Song",
        loopBars = 1,
        tracks = listOf(Track("t", 128, 0, takes = listOf(Take(listOf(Note(0, 36, 110, 120)))))),
    )

    @Before
    fun setUp() {
        engine = requireNotNull(AudioEngine.load(application.assets))
        instrumentation.runOnMainSync { model = ExportViewModel(application) }
    }

    @After
    fun tearDown() {
        engine.close()
        File(application.cacheDir, ExportViewModel.EXPORT_FOLDER).deleteRecursively()
    }

    private fun exportAndWait(format: ExportFormat, passes: Int = 1): ExportState {
        instrumentation.runOnMainSync { model.export(project, format, passes, engine::openExport) { _, _ -> "Kit" } }
        val deadline = SystemClock.uptimeMillis() + 30_000
        while (model.state.value is ExportState.Running && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        return model.state.value
    }

    @Test
    fun eachFormatProducesItsFileNamedAfterTheProject() {
        listOf(ExportFormat.WAV, ExportFormat.MIDI, ExportFormat.MP3).forEach { format ->
            val done = exportAndWait(format) as ExportState.Done

            assertEquals("MySong.${format.extension}", done.file.name)
            assertTrue(done.file.length() > 0)
        }
    }

    @Test
    fun onlyTheLatestExportIsKept() {
        val first = (exportAndWait(ExportFormat.WAV) as ExportState.Done).file
        exportAndWait(ExportFormat.MIDI)

        assertFalse(first.exists())
    }

    @Test
    fun saveCopiesTheFileToTheChosenPlace() {
        val done = exportAndWait(ExportFormat.MIDI) as ExportState.Done
        val target = File(application.cacheDir, "saved-copy.mid")
        var result: Boolean? = null

        instrumentation.runOnMainSync { model.saveTo(Uri.fromFile(target)) { result = it } }
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (result == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20)

        assertEquals(true, result)
        assertArrayEquals(done.file.readBytes(), target.readBytes())
        target.delete()
    }

    @Test
    fun cancelReturnsToIdleAndLeavesNoFile() {
        instrumentation.runOnMainSync {
            model.export(project, ExportFormat.FLAC, 16, engine::openExport) { _, _ -> "Kit" }
            model.cancel()
        }
        SystemClock.sleep(500)

        assertEquals(ExportState.Idle, model.state.value)
        assertTrue(File(application.cacheDir, ExportViewModel.EXPORT_FOLDER).listFiles().orEmpty().isEmpty())
    }
}
