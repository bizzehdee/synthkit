package com.bizzeh.synthkit.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatencyReportTest {
    private fun fields(running: String = "1", latency: String = "11.250000") = arrayOf(
        running, latency, "AAudio", "LowLatency", "Exclusive", "48000", "96", "192", "3",
    )

    @Test
    fun runningStreamIsParsed() {
        val report = LatencyReport.fromNative(fields())

        assertEquals(
            LatencyReport(
                outputLatencyMs = 11.25,
                audioApi = "AAudio",
                performanceMode = "LowLatency",
                sharingMode = "Exclusive",
                sampleRate = 48000,
                framesPerBurst = 96,
                bufferFrames = 192,
                underruns = 3,
            ),
            report,
        )
    }

    @Test
    fun noRunningStreamGivesNoReport() {
        assertNull(LatencyReport.fromNative(fields(running = "0")))
    }

    @Test
    fun negativeLatencyMeansNotMeasuredYet() {
        val report = LatencyReport.fromNative(fields(latency = "-1.000000"))

        assertNull(report?.outputLatencyMs)
        assertEquals(48000, report?.sampleRate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongFieldCountIsRejected() {
        LatencyReport.fromNative(arrayOf("1", "2.0"))
    }
}
