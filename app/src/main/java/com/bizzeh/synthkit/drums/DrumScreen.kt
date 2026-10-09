package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.audio.LatencyReport

/** [latency] is shown above the pads when it is not null. */
@Composable
fun DrumScreen(state: EngineState, latency: LatencyReport?, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
    ) {
        when (state) {
            EngineState.Loading -> {
                val loading = stringResource(R.string.loading_sounds)
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
            }
            EngineState.Failed -> Text(
                text = stringResource(R.string.sounds_failed),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            is EngineState.Ready -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                latency?.let { LatencyReadout(it) }
                DrumPadGrid(
                    pads = FirstPageDrumPads,
                    player = state.player,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LatencyReadout(report: LatencyReport) {
    val latency = report.outputLatencyMs
        ?.let { stringResource(R.string.latency_value_ms, it) }
        ?: stringResource(R.string.latency_measuring)
    Text(
        text = stringResource(
            R.string.latency_readout,
            latency,
            report.audioApi,
            report.performanceMode,
            report.sharingMode,
            report.sampleRate,
            report.framesPerBurst,
            report.bufferFrames,
            report.underruns,
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
