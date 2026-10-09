package com.bizzeh.synthkit.play

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.InstrumentPlayer
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.chords.ChordsLayout
import com.bizzeh.synthkit.drums.DrumKitLayout
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.keys.KeysLayout
import com.bizzeh.synthkit.pads.ChromaticPadsLayout
import com.bizzeh.synthkit.instruments.PlayLayout

/** The live instrument plays on one channel; recorded tracks will use the others. */
const val LIVE_CHANNEL = 0

private const val TAG = "SynthKit"

/** [latency] is shown under the top bar when it is not null. */
@Composable
fun PlayScreen(
    instrument: Instrument,
    player: InstrumentPlayer,
    latency: LatencyReport?,
    onBack: () -> Unit,
    onChangeInstrument: () -> Unit,
    onOpened: (Instrument) -> Unit,
    kits: List<Instrument>,
    onOpenInstrument: (Instrument) -> Unit,
    warning: LatencyWarning?,
    onDismissWarning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnOpened by rememberUpdatedState(onOpened)
    LaunchedEffect(instrument.id) {
        if (!player.selectInstrument(LIVE_CHANNEL, instrument.bank, instrument.program)) {
            Log.e(TAG, "event=instrument_select_failed instrument=${instrument.id}")
        }
        currentOnOpened(instrument)
    }
    DisposableEffect(player) {
        onDispose { player.allNotesOff(LIVE_CHANNEL) }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = instrument.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onChangeInstrument) {
                Icon(Icons.Filled.Menu, contentDescription = null)
                Text(stringResource(R.string.change_instrument))
            }
        }
        warning?.let { LatencyWarningBanner(it, onDismissWarning) }
        latency?.let { LatencyReadout(it) }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Keyed by instrument so hold, octave and other layout state start fresh.
            key(instrument.id) {
                when (val layout = instrument.layout) {
                    PlayLayout.DrumKit -> DrumKitLayout(player, LIVE_CHANNEL, instrument, kits, onOpenInstrument)
                    is PlayLayout.Keys -> KeysLayout(player, LIVE_CHANNEL, layout.holdByDefault)
                    is PlayLayout.Chords -> ChordsLayout(player, LIVE_CHANNEL, layout.bassRoots)
                    is PlayLayout.ChromaticPads -> ChromaticPadsLayout(player, LIVE_CHANNEL, layout.root)
                }
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
