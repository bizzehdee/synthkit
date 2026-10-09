package com.bizzeh.synthkit.play

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.InstrumentPlayer
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.chords.ChordsLayout
import com.bizzeh.synthkit.drums.DrumKitPads
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.instruments.PlayLayout
import com.bizzeh.synthkit.keys.KeysLayout
import com.bizzeh.synthkit.pads.ChromaticPadsLayout
import com.bizzeh.synthkit.ui.studio.FamilyChip
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.theme.familyColor
import kotlin.math.roundToInt

private const val TAG = "SynthKit"

/**
 * A track's instrument, playable on [channel]. The family chip opens the
 * instrument picker; [transport] fills the top bar between them; More holds
 * the audio diagnostics ([latency]).
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
    modifier: Modifier = Modifier,
    transport: @Composable RowScope.() -> Unit = {},
) {
    DisposableEffect(player, channel) {
        onDispose {
            if (!player.allNotesOff(channel)) Log.w(TAG, "event=all_notes_off_failed channel=$channel")
        }
    }
    val color = familyColor(instrument.family)
    var menu by remember { mutableStateOf(false) }
    var diagnostics by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Fits a 640 dp phone: the chip shrinks first, the buttons never do.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PanelIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            FamilyChip(
                color = color,
                family = stringResource(instrument.family.label),
                title = instrument.name,
                onClick = onChangeInstrument,
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 120.dp),
            )
            Spacer(Modifier.weight(0.001f))
            transport()
            Box {
                PanelIconButton(Icons.Filled.MoreVert, stringResource(R.string.more), { menu = true }, framed = false)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.change_instrument)) }, onClick = {
                        menu = false
                        onChangeInstrument()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.diagnostics)) }, onClick = {
                        menu = false
                        diagnostics = true
                    })
                }
            }
        }
        warning?.let { LatencyWarningBanner(it, onDismissWarning) }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Keyed by instrument so hold, octave and other layout state start fresh.
            key(instrument.id) {
                when (val layout = instrument.layout) {
                    PlayLayout.DrumKit -> DrumKitPads(player, channel, color)
                    is PlayLayout.Keys -> KeysLayout(player, channel, layout.holdByDefault, color)
                    is PlayLayout.Chords -> ChordsLayout(player, channel, layout.bassRoots, color)
                    is PlayLayout.ChromaticPads -> ChromaticPadsLayout(player, channel, layout.root, color)
                }
            }
        }
    }

    if (diagnostics) DiagnosticsDialog(latency, onDismiss = { diagnostics = false })
}

/** Audio stream facts for troubleshooting, kept off the play surface. */
@Composable
private fun DiagnosticsDialog(report: LatencyReport?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diagnostics_title)) },
        text = {
            if (report == null) {
                Text(stringResource(R.string.diagnostics_unavailable))
            } else {
                val latency = report.outputLatencyMs
                    ?.let { stringResource(R.string.latency_value_ms, it) }
                    ?: stringResource(R.string.latency_measuring)
                Text(
                    stringResource(
                        R.string.latency_readout,
                        latency,
                        report.audioApi,
                        report.performanceMode,
                        report.sharingMode,
                        report.sampleRate,
                        report.framesPerBurst,
                        report.bufferFrames,
                        report.underruns,
                        (report.loadAverage * 100).roundToInt(),
                        (report.loadPeak * 100).roundToInt(),
                        report.voices,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
