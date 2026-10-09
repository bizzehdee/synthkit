package com.bizzeh.synthkit.audio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Loads the engine once per screen lifetime and plays only while the screen is visible. */
class AudioEngineViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Loading)
    val state: StateFlow<EngineState> = mutableState.asStateFlow()

    private var engine: AudioEngine? = null
    private var visible = false

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
        engine?.stop()
    }

    private fun startEngine(engine: AudioEngine) {
        if (!engine.start()) {
            Log.e(TAG, "event=engine_start_failed")
            mutableState.value = EngineState.Failed
        }
    }

    override fun onCleared() {
        engine?.close()
        engine = null
    }

    private companion object {
        const val TAG = "SynthKit"
    }
}
