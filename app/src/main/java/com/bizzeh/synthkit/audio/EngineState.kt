package com.bizzeh.synthkit.audio

sealed interface EngineState {
    data object Loading : EngineState
    data class Ready(val player: NotePlayer) : EngineState
    data object Failed : EngineState
}
