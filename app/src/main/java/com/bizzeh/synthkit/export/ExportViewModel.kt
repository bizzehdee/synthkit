package com.bizzeh.synthkit.export

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bizzeh.synthkit.audio.PcmSource
import com.bizzeh.synthkit.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val percent: Int) : ExportState
    data class Done(val file: File, val format: ExportFormat) : ExportState
    data object Failed : ExportState
}

/** Runs one export at a time off the main thread; it survives activity recreation. */
class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val folder = File(application.cacheDir, EXPORT_FOLDER)
    private val resolver = application.contentResolver
    private val mutableState = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = mutableState.asStateFlow()
    private var job: Job? = null

    fun export(
        project: Project,
        format: ExportFormat,
        passes: Int,
        openRender: (ExportSpec) -> PcmSource?,
        trackName: (bank: Int, program: Int) -> String,
    ) {
        if (job?.isActive == true) return
        mutableState.value = ExportState.Running(0)
        job = viewModelScope.launch {
            mutableState.value = try {
                val file = withContext(Dispatchers.Default) { write(project, format, passes, openRender, trackName) }
                Log.i(TAG, "event=export_done format=${format.name} passes=$passes bytes=${file.length()}")
                ExportState.Done(file, format)
            } catch (e: CancellationException) {
                ExportState.Idle
            } catch (e: IOException) {
                Log.e(TAG, "event=export_failed format=${format.name} error=${e.javaClass.simpleName}")
                ExportState.Failed
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "event=export_failed format=${format.name} error=${e.javaClass.simpleName}")
                ExportState.Failed
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun reset() {
        if (job?.isActive != true) mutableState.value = ExportState.Idle
    }

    /** Copies the finished export to a place the player chose with the system picker. */
    fun saveTo(uri: Uri, onResult: (Boolean) -> Unit) {
        val done = mutableState.value as? ExportState.Done ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    resolver.openOutputStream(uri)?.use { out -> done.file.inputStream().use { it.copyTo(out) } } != null
                } catch (e: IOException) {
                    Log.e(TAG, "event=export_save_failed error=${e.javaClass.simpleName}")
                    false
                }
            }
            onResult(saved)
        }
    }

    private suspend fun write(
        project: Project,
        format: ExportFormat,
        passes: Int,
        openRender: (ExportSpec) -> PcmSource?,
        trackName: (Int, Int) -> String,
    ): File {
        // Only the latest export is kept; earlier ones would only use space.
        folder.deleteRecursively()
        if (!folder.mkdirs()) throw IOException("Could not create the export folder")
        val file = File(folder, "${fileName(project.name)}.${format.extension}")
        var lastPercent = -1
        val progress = { fraction: Float ->
            val percent = (fraction * 100).toInt().coerceIn(0, 100)
            if (percent != lastPercent) {
                lastPercent = percent
                mutableState.value = ExportState.Running(percent)
            }
        }
        if (format == ExportFormat.MIDI) {
            file.outputStream().buffered().use { MidiWriter.write(project, passes, trackName, it) }
            return file
        }
        val source = openRender(ExportPlan.spec(project, passes)) ?: throw IOException("The engine refused the export")
        source.use {
            when (format) {
                ExportFormat.WAV -> try {
                    file.outputStream().buffered().use { out -> WavWriter.write(it, out, progress) }
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
                ExportFormat.MP3, ExportFormat.FLAC -> NativeEncoder.write(format, it, file, progress)
                ExportFormat.AAC -> AacWriter.write(it, file, progress)
                ExportFormat.MIDI -> Unit
            }
        }
        return file
    }

    companion object {
        private const val TAG = "SynthKit"
        const val EXPORT_FOLDER = "exports"
        private const val MAX_NAME = 60

        /** A file name from the project name: letters, digits, spaces, dashes and underscores only. */
        fun fileName(projectName: String): String =
            projectName.filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
                .trim()
                .take(MAX_NAME)
                .ifEmpty { "export" }
    }
}
