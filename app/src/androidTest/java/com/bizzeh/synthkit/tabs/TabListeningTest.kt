package com.bizzeh.synthkit.tabs

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.AudioEngine
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plays the tab fixtures aloud through the real engine for a person to listen
 * to. Skipped unless the runner gets "-e listen true", so the normal suite
 * stays silent.
 */
@RunWith(AndroidJUnit4::class)
class TabListeningTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var engine: AudioEngine

    @Before
    fun startEngine() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("listen") == "true")
        engine = requireNotNull(AudioEngine.load(instrumentation.targetContext.assets))
        check(engine.start())
    }

    @After
    fun stopEngine() {
        if (::engine.isInitialized) engine.close()
    }

    private fun fixture(name: String) = instrumentation.context.assets.open(name).bufferedReader().readText()

    @Test
    fun drumTab1() = playDrums("drum-tab-1.txt")

    @Test
    fun drumTab2() = playDrums("drum-tab-2.txt")

    @Test
    fun guitarTab1() = playStrings("guitar-tab-1.txt", program = OVERDRIVEN_GUITAR, stepMillis = 150)

    @Test
    fun bassTab1() = playStrings("bass-tab-1.txt", program = FINGER_BASS, stepMillis = 110)

    private fun playDrums(name: String) {
        engine.selectInstrument(CHANNEL, 128, 0)
        play(Tab.drums(fixture(name), Tab::gmDrum), SIXTEENTH_AT_100_BPM, oneShot = true)
    }

    private fun playStrings(name: String, program: Int, stepMillis: Long) {
        engine.selectInstrument(CHANNEL, 0, program)
        play(Tab.strings(fixture(name)), stepMillis, oneShot = false)
    }

    // Sends each note shortly before it is due, with the remaining time as its
    // delay, so the engine places it on the exact frame.
    private fun play(notes: List<TabNote>, stepMillis: Long, oneShot: Boolean) {
        val start = SystemClock.uptimeMillis() + LEAD_MILLIS
        notes.forEach { note ->
            val due = start + note.step * stepMillis
            val wait = due - LOOKAHEAD_MILLIS - SystemClock.uptimeMillis()
            if (wait > 0) SystemClock.sleep(wait)
            val delay = (due - SystemClock.uptimeMillis()).coerceAtLeast(0).toFloat()
            engine.noteOn(CHANNEL, note.note, note.velocity * 0.8f, delay)
            if (!oneShot) engine.noteOff(CHANNEL, note.note, delay + note.lengthSteps * stepMillis - 5)
        }
        val end = start + ((notes.maxOfOrNull { it.step + it.lengthSteps } ?: 0) * stepMillis)
        SystemClock.sleep((end - SystemClock.uptimeMillis()).coerceAtLeast(0) + TAIL_MILLIS)
    }

    private companion object {
        const val CHANNEL = 0
        const val OVERDRIVEN_GUITAR = 29
        const val FINGER_BASS = 33
        const val SIXTEENTH_AT_100_BPM = 150L
        const val LEAD_MILLIS = 300L
        const val LOOKAHEAD_MILLIS = 40L
        const val TAIL_MILLIS = 1500L
    }
}
