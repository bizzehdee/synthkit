package com.bizzeh.synthkit.audio

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEngineTest {
    private lateinit var engine: AudioEngine

    @Before
    fun loadBundledSoundFont() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        engine = requireNotNull(AudioEngine.load(assets)) { "bundled SoundFont failed to load" }
    }

    @After
    fun closeEngine() = engine.close()

    @Test
    fun startedEngineAcceptsDrumNotes() {
        assertTrue(engine.start())

        assertTrue(engine.noteOn(DRUM_CHANNEL, KICK, VELOCITY))
        assertTrue(engine.noteOn(DRUM_CHANNEL, SNARE, VELOCITY))
    }

    @Test
    fun engineRestartsAfterStop() {
        assertTrue(engine.start())
        engine.stop()

        assertTrue(engine.start())
    }

    @Test
    fun latencyIsReportedOnlyWhileTheStreamRuns() {
        assertNull(engine.latencyReport())

        assertTrue(engine.start())
        val running = requireNotNull(engine.latencyReport())
        assertTrue(running.sampleRate > 0)
        assertTrue(running.framesPerBurst > 0)
        assertTrue(running.bufferFrames >= running.framesPerBurst)

        engine.stop()
        assertNull(engine.latencyReport())
    }

    @Test
    fun runningStreamMeasuresOutputLatency() {
        assertTrue(engine.start())

        var latencyMs: Double? = null
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (latencyMs == null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
            latencyMs = engine.latencyReport()?.outputLatencyMs
        }

        assertTrue("latency not measured within 3 s", latencyMs != null && latencyMs > 0.0)
    }

    @Test
    fun outOfRangeNoteIsRejected() {
        assertFalse(engine.noteOn(DRUM_CHANNEL, 128, VELOCITY))
    }

    @Test
    fun closedEngineRefusesCalls() {
        engine.close()

        assertThrows(IllegalStateException::class.java) { engine.noteOn(DRUM_CHANNEL, KICK, VELOCITY) }
    }

    private companion object {
        const val DRUM_CHANNEL = 9
        const val KICK = 36
        const val SNARE = 38
        const val VELOCITY = 0.8f
    }
}
