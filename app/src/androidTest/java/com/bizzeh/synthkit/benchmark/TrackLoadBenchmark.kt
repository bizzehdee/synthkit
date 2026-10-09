package com.bizzeh.synthkit.benchmark

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.audio.AudioEngine
import com.bizzeh.synthkit.audio.LoopNote
import com.bizzeh.synthkit.looper.trackChannel
import com.bizzeh.synthkit.testing.ManualOnly
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures audio callback load with 4, 6 and 8 busy tracks, to set the track
 * limit (TASK-016). Run by name on the phone to measure; results go to logcat
 * with the tag SynthKitBench.
 */
@ManualOnly
@RunWith(AndroidJUnit4::class)
class TrackLoadBenchmark {
    private lateinit var engine: AudioEngine

    @Before
    fun startEngine() {
        engine = requireNotNull(AudioEngine.load(InstrumentationRegistry.getInstrumentation().targetContext.assets))
        check(engine.start())
        engine.setTempo(120)
    }

    @After
    fun closeEngine() = engine.close()

    @Test
    fun loadWithFourSixAndEightBusyTracks() {
        listOf(4, 6, 8).forEach { tracks ->
            (0 until tracks).forEach { index ->
                val (bank, program) = INSTRUMENTS[index]
                engine.selectInstrument(trackChannel(index), bank, program)
            }
            engine.publishLoopNotes((0 until tracks).flatMap { busyBar(it) })
            engine.setLoop(0, BAR, playing = true)
            engine.startTransport()
            SystemClock.sleep(WARM_UP_MILLIS)
            val before = requireNotNull(engine.latencyReport())
            var peak = 0f
            var maxVoices = 0
            repeat(MEASURE_SECONDS) {
                SystemClock.sleep(1_000)
                val report = requireNotNull(engine.latencyReport())
                peak = maxOf(peak, report.loadPeak)
                maxVoices = maxOf(maxVoices, report.voices)
            }
            val after = requireNotNull(engine.latencyReport())
            Log.i(
                TAG,
                "event=track_load tracks=$tracks load_avg=%.2f load_peak=%.2f voices_max=%d underruns=%d burst=%d buffer=%d"
                    .format(java.util.Locale.ROOT, after.loadAverage, peak, maxVoices,
                        after.underruns - before.underruns, after.framesPerBurst, after.bufferFrames),
            )
            engine.stopTransport()
            (0 until tracks).forEach { engine.allNotesOff(trackChannel(it)) }
            SystemClock.sleep(1_000)
        }
    }

    // A dense bar: drums play 16th hats with kick and snare; melodic tracks a
    // three-note chord on every 8th note, held for an 8th.
    private fun busyBar(index: Int): List<LoopNote> {
        val channel = trackChannel(index)
        val drums = INSTRUMENTS[index].first == 128
        return (0 until 16).flatMap { step ->
            val tick = step * SIXTEENTH
            if (drums) {
                listOfNotNull(42, if (step % 4 == 0) 36 else null, if (step % 8 == 4) 38 else null).flatMap { key ->
                    listOf(LoopNote(tick, channel, key, 0.8f), LoopNote(tick + 60, channel, key, 0f))
                }
            } else if (step % 2 == 0) {
                listOf(48, 52, 55).map { it + 12 * (index % 3) }.flatMap { key ->
                    listOf(LoopNote(tick, channel, key, 0.8f), LoopNote((tick + 2 * SIXTEENTH) % BAR, channel, key, 0f))
                }
            } else {
                emptyList()
            }
        }
    }

    private companion object {
        const val TAG = "SynthKitBench"
        const val BAR = 1920
        const val SIXTEENTH = 120
        const val WARM_UP_MILLIS = 2_000L
        const val MEASURE_SECONDS = 10
        val INSTRUMENTS = listOf(128 to 0, 0 to 0, 0 to 48, 0 to 24, 128 to 25, 0 to 33, 0 to 89, 0 to 4)
    }
}
