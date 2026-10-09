package com.bizzeh.synthkit.audio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loads the engine once per screen lifetime and plays only while the screen is visible. */
class AudioEngineViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Loading)
    val state: StateFlow<EngineState> = mutableState.asStateFlow()

    private val mutableLatency = MutableStateFlow<LatencyReport?>(null)
    val latency: StateFlow<LatencyReport?> = mutableLatency.asStateFlow()

    private var engine: AudioEngine? = null
    private var visible = false
    private var latencyPolling: Job? = null
    private var lastLoggedStream: LatencyReport? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) { AudioEngine.load(application.assets) }
            if (loaded == null) {
                Log.e(TAG, "event=engine_load_failed")
                mutableState.value = EngineState.Failed
                return@launch
            }
            engine = loaded
            mutableState.value = EngineState.Ready(loaded)
            if (visible) startEngine(loaded)
        }
    }

    fun onVisible() {
        visible = true
        engine?.let(::startEngine)
    }

    fun onHidden() {
        visible = false
        latencyPolling?.cancel()
        latencyPolling = null
        engine?.stop()
        mutableLatency.value = null
    }

    private fun startEngine(engine: AudioEngine) {
        if (!engine.start()) {
            Log.e(TAG, "event=engine_start_failed")
            mutableState.value = EngineState.Failed
            return
        }
        lastLoggedStream = null
        latencyPolling?.cancel()
        latencyPolling = viewModelScope.launch {
            while (true) {
                val report = engine.latencyReport()
                mutableLatency.value = report
                logWhenStreamChanges(report)
                delay(LATENCY_POLL_MILLIS)
            }
        }
    }

    // Logs the first measured latency of each stream configuration, so a stream
    // reopened on a new output device is logged again without logging every poll.
    private fun logWhenStreamChanges(report: LatencyReport?) {
        val latencyMs = report?.outputLatencyMs ?: return
        val stream = report.copy(outputLatencyMs = null, underruns = 0)
        if (stream == lastLoggedStream) return
        lastLoggedStream = stream
        Log.i(
            TAG,
            "event=latency_measured latency_ms=%.1f api=%s performance_mode=%s sharing_mode=%s sample_rate=%d frames_per_burst=%d buffer_frames=%d".format(
                java.util.Locale.ROOT,
                latencyMs,
                report.audioApi,
                report.performanceMode,
                report.sharingMode,
                report.sampleRate,
                report.framesPerBurst,
                report.bufferFrames,
            ),
        )
    }

    override fun onCleared() {
        engine?.close()
        engine = null
    }

    private companion object {
        const val TAG = "SynthKit"
        const val LATENCY_POLL_MILLIS = 1_000L
    }
}
