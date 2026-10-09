package com.bizzeh.synthkit.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.bizzeh.synthkit.audio.PcmSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.nio.ByteOrder

/** AAC-LC in an MP4 container through the platform encoder; no extra library. */
object AacWriter {
    const val BITRATE = 192_000
    private const val CHANNELS = 2
    private const val BYTES_PER_FRAME = CHANNELS * 2
    private const val TIMEOUT_MICROS = 10_000L

    /** Encodes [source] into [file]. A failed or cancelled encode leaves no file behind. */
    suspend fun write(source: PcmSource, file: File, onProgress: (Float) -> Unit = {}) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, source.sampleRate, CHANNELS).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        var finished = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            encode(codec, muxer, source, onProgress)
            muxer.stop()
            finished = true
        } finally {
            runCatching { codec.stop() }
            codec.release()
            muxer?.release()
            if (!finished) file.delete()
        }
    }

    private suspend fun encode(codec: MediaCodec, muxer: MediaMuxer, source: PcmSource, onProgress: (Float) -> Unit) {
        val info = MediaCodec.BufferInfo()
        var samples = ShortArray(0)
        var framesQueued = 0L
        var inputDone = false
        var track = -1
        while (true) {
            currentCoroutineContext().ensureActive()
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_MICROS)
                if (index >= 0) {
                    val input = requireNotNull(codec.getInputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                    val capacityFrames = input.capacity() / BYTES_PER_FRAME
                    if (samples.size != capacityFrames * CHANNELS) samples = ShortArray(capacityFrames * CHANNELS)
                    val frames = source.read(samples)
                    val time = framesQueued * 1_000_000L / source.sampleRate
                    if (frames == 0) {
                        codec.queueInputBuffer(index, 0, 0, time, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        input.asShortBuffer().put(samples, 0, frames * CHANNELS)
                        codec.queueInputBuffer(index, 0, frames * BYTES_PER_FRAME, time, 0)
                        framesQueued += frames
                        onProgress(framesQueued.toFloat() / source.totalFrames)
                    }
                }
            }
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_MICROS)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                }
                index >= 0 -> {
                    val output = requireNotNull(codec.getOutputBuffer(index))
                    // The codec config arrives in the output format, so its buffer is not muxed.
                    val config = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!config && info.size > 0) {
                        if (track < 0) throw IOException("AAC data arrived before its format")
                        muxer.writeSampleData(track, output, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
}
