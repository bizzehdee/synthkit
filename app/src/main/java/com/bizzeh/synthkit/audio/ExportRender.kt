package com.bizzeh.synthkit.audio

/** An offline render of a project from the native engine. Use from one worker thread. */
class ExportRender internal constructor(private var handle: Long, override val sampleRate: Int) : PcmSource {
    override val totalFrames: Long = nativeTotalFrames(handle)

    override fun read(buffer: ShortArray): Int {
        check(handle != 0L) { "ExportRender is closed" }
        return nativeRender(handle, buffer)
    }

    override fun close() {
        if (handle != 0L) {
            nativeClose(handle)
            handle = 0L
        }
    }

    private companion object {
        @JvmStatic private external fun nativeRender(handle: Long, buffer: ShortArray): Int
        @JvmStatic private external fun nativeTotalFrames(handle: Long): Long
        @JvmStatic private external fun nativeClose(handle: Long)
    }
}
