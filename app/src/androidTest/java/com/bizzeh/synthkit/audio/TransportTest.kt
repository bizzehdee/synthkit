package com.bizzeh.synthkit.audio

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransportTest {
    private lateinit var engine: AudioEngine

    @Before
    fun startEngine() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        engine = requireNotNull(AudioEngine.load(assets))
        assertTrue(engine.start())
    }

    @After
    fun closeEngine() = engine.close()

    private fun drainFor(millis: Long): List<RecordedEvent> {
        SystemClock.sleep(millis)
        return engine.drainRecorded()
    }

    @Test
    fun clockRunsAtTheTempoOnceStarted() {
        assertTrue(engine.setTempo(120))
        assertTrue(engine.startTransport())

        SystemClock.sleep(500)

        // 500 ms at 120 BPM is one beat, 480 ticks; allow for scheduling jitter.
        assertTrue(engine.clockTicks() in 300.0..700.0)
    }

    @Test
    fun liveNotesAreRecordedInOrderAndStopLeavesAMarker() {
        engine.startTransport()
        engine.setRecording(true)
        SystemClock.sleep(100)

        engine.noteOn(1, 60, 0.8f)
        SystemClock.sleep(200)
        engine.noteOff(1, 60)
        SystemClock.sleep(100)
        engine.setRecording(false)
        val recorded = drainFor(100)

        assertEquals(3, recorded.size)
        assertEquals(60, recorded[0].key)
        assertTrue(recorded[0].velocity > 0f)
        assertEquals(0f, recorded[1].velocity)
        assertTrue(recorded[1].tick > recorded[0].tick)
        assertTrue(recorded[2].isStopMarker)
    }

    @Test
    fun nothingIsRecordedWhenRecordingIsOff() {
        engine.startTransport()

        engine.noteOn(1, 60, 0.8f)

        assertEquals(emptyList<RecordedEvent>(), drainFor(200))
    }

    @Test
    fun loopNotesAreAcceptedAndPlaybackIsNotRecorded() {
        engine.startTransport()
        assertTrue(engine.publishLoopNotes(listOf(LoopNote(0, 2, 36, 0.8f), LoopNote(240, 2, 36, 0f))))
        assertTrue(engine.setLoop(0, 1920, playing = true))
        engine.setRecording(true)

        assertEquals(emptyList<RecordedEvent>(), drainFor(600))
    }

    @Test
    fun outOfRangeLoopNotesAreRejected() {
        assertFalse(engine.publishLoopNotes(listOf(LoopNote(0, 16, 36, 0.8f))))
        assertFalse(engine.publishLoopNotes(listOf(LoopNote(-1, 1, 36, 0.8f))))
        assertFalse(engine.publishLoopNotes(listOf(LoopNote(0, 1, 128, 0.8f))))
        assertFalse(engine.publishLoopNotes(listOf(LoopNote(0, 1, 36, 2f))))
    }

    @Test
    fun transportInputsAreValidated() {
        assertFalse(engine.setTempo(300))
        assertFalse(engine.setLoop(-1, 1920, true))
        assertFalse(engine.setVolume(0, -0.5f))
        assertTrue(engine.setVolume(0, 0.5f))
        assertTrue(engine.setClick(true))
        assertTrue(engine.stopTransport())
    }
}
