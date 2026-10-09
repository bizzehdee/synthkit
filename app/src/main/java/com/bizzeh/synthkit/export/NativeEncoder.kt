package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.audio.PcmSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException

/** MP3 (LAME) and FLAC (libFLAC) encoding in native code, fed block by block. */
object NativeEncoder {
    private const val MP3 = 0
    private const val FLAC = 1
    private const val BLOCK_FRAMES = 4096

    init {
        System.loadLibrary("synthkit")
    }

    /** Encodes [source] into [file]. A failed or cancelled encode leaves no file behind. */
    suspend fun write(format: ExportFormat, source: PcmSource, file: File, onProgress: (Float) -> Unit = {}) {
        val code = when (format) {
            ExportFormat.MP3 -> MP3
            ExportFormat.FLAC -> FLAC
            else -> throw IllegalArgumentException("Not a native format: $format")
        }
        val handle = nativeOpen(code, file.absolutePath, source.sampleRate, source.totalFrames)
        if (handle == 0L) throw IOException("Could not start the ${format.name} encoder")
        var finished = false
        try {
            val buffer = ShortArray(BLOCK_FRAMES * 2)
            var written = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val frames = source.read(buffer)
                if (frames == 0) break
                if (!nativeEncode(handle, buffer, frames)) throw IOException("${format.name} encoding failed")
                written += frames
                onProgress(written.toFloat() / source.totalFrames)
            }
            if (!nativeFinish(handle)) throw IOException("${format.name} encoding failed at the end")
            finished = true
        } finally {
            nativeClose(handle)
            if (!finished) file.delete()
        }
    }

    @JvmStatic private external fun nativeOpen(format: Int, path: String, sampleRate: Int, totalFrames: Long): Long
    @JvmStatic private external fun nativeEncode(handle: Long, stereo: ShortArray, frames: Int): Boolean
    @JvmStatic private external fun nativeFinish(handle: Long): Boolean
    @JvmStatic private external fun nativeClose(handle: Long)
}
