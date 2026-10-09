package com.bizzeh.synthkit.audio

import com.bizzeh.synthkit.export.ExportSpec
import com.bizzeh.synthkit.instruments.InstrumentCatalogue

sealed interface EngineState {
    data object Loading : EngineState
    data class Ready(
        val player: InstrumentPlayer,
        val transport: Transport,
        val catalogue: InstrumentCatalogue,
        /** Opens an offline render for export; null when refused. Safe off the main thread. */
        val openExport: (ExportSpec) -> PcmSource? = { null },
    ) : EngineState
    data object Failed : EngineState
}
