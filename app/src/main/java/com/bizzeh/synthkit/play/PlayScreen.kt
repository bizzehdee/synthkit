package com.bizzeh.synthkit.play

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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

private const val TAG = "SynthKit"

/**
 * A track's instrument, playable on [channel], with [transport] controls in the
 * top bar. [latency] is shown under the bar when it is not null.
 */
@Composable
fun PlayScreen(
    instrument: Instrument,
    channel: Int,
    player: InstrumentPlayer,
    latency: LatencyReport?,
    warning: LatencyWarning?,
    onDismissWarning: () -> Unit,
    onBack: () -> Unit,
    onChangeInstrument: () -> Unit,
    kits: List<Instrument>,
    onKitChange: (Instrument) -> Unit,
    modifier: Modifier = Modifier,
    status: @Composable () -> Unit = {},
    transport: @Composable RowScope.() -> Unit = {},
) {
    DisposableEffect(player, channel) {
        onDispose {
            if (!player.allNotesOff(channel)) Log.w(TAG, "event=all_notes_off_failed channel=$channel")
        }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            // Name and status share the space left over, so the buttons never shrink.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instrument.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                status()
            }
            transport()
            IconButton(onClick = onChangeInstrument) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.change_instrument))
            }
        }
        warning?.let { LatencyWarningBanner(it, onDismissWarning) }
        latency?.let { LatencyReadout(it) }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Keyed by instrument so hold, octave and other layout state start fresh.
            key(instrument.id) {
                when (val layout = instrument.layout) {
                    PlayLayout.DrumKit -> DrumKitLayout(player, channel, instrument, kits, onKitChange)
                    is PlayLayout.Keys -> KeysLayout(player, channel, layout.holdByDefault)
                    is PlayLayout.Chords -> ChordsLayout(player, channel, layout.bassRoots)
                    is PlayLayout.ChromaticPads -> ChromaticPadsLayout(player, channel, layout.root)
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
