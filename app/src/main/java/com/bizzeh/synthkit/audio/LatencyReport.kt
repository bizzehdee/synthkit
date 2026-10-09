package com.bizzeh.synthkit.audio

/** What Oboe reports about the running output stream. */
data class LatencyReport(
    /** Null while the platform cannot calculate it yet. */
    val outputLatencyMs: Double?,
    val audioApi: String,
    val performanceMode: String,
    val sharingMode: String,
    val sampleRate: Int,
    val framesPerBurst: Int,
    val bufferFrames: Int,
    val underruns: Int,
    /** 0 when the platform does not report the output device. */
    val deviceId: Int = 0,
) {
    companion object {
        private const val FIELD_COUNT = 10

        /** Parses the fields from the native engine. Returns null when no stream is running. */
        fun fromNative(fields: Array<String>): LatencyReport? {
            require(fields.size == FIELD_COUNT) { "Expected $FIELD_COUNT fields, got ${fields.size}" }
            if (fields[0] != "1") return null
            return LatencyReport(
                outputLatencyMs = fields[1].toDouble().takeIf { it >= 0.0 },
                audioApi = fields[2],
                performanceMode = fields[3],
                sharingMode = fields[4],
                sampleRate = fields[5].toInt(),
                framesPerBurst = fields[6].toInt(),
                bufferFrames = fields[7].toInt(),
                underruns = fields[8].toInt(),
                deviceId = fields[9].toInt(),
            )
        }
    }
}
