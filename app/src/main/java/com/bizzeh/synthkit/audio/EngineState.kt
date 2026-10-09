package com.bizzeh.synthkit.audio

import com.bizzeh.synthkit.instruments.InstrumentCatalogue

sealed interface EngineState {
    data object Loading : EngineState
    data class Ready(
        val player: InstrumentPlayer,
        val transport: Transport,
        val catalogue: InstrumentCatalogue,
    ) : EngineState
    data object Failed : EngineState
}
