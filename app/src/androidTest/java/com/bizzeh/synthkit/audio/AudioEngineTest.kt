package com.bizzeh.synthkit.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
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
