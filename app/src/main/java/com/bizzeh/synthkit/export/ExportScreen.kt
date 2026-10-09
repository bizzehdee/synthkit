package com.bizzeh.synthkit.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.MinTouchTarget

@Composable
fun ExportScreen(
    projectName: String,
    state: ExportState,
    format: ExportFormat,
    passes: Int,
    onFormat: (ExportFormat) -> Unit,
    onPasses: (Int) -> Unit,
    onExport: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val running = state is ExportState.Running
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.export_title, projectName),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val formatLabel = stringResource(R.string.export_format)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = formatLabel }) {
            ExportFormat.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = format == option,
                    onClick = { onFormat(option) },
                    enabled = !running,
                    shape = SegmentedButtonDefaults.itemShape(index, ExportFormat.entries.size),
                ) { Text(stringResource(label(option))) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onPasses(passes - 1) }, enabled = !running && passes > ExportPlan.MIN_PASSES) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.export_passes_down))
            }
            Text(pluralStringResource(R.plurals.export_passes, passes, passes), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onPasses(passes + 1) }, enabled = !running && passes < ExportPlan.MAX_PASSES) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.export_passes_up))
            }
        }
        when (state) {
            ExportState.Idle, ExportState.Failed -> {
                if (state == ExportState.Failed) {
                    Text(stringResource(R.string.export_failed), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onExport, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.export_start))
                }
            }
            is ExportState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_running, state.percent))
                    LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(start = 16.dp).heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.cancel))
                }
            }
            is ExportState.Done -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.export_done, state.file.name))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onSave, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                        Text(stringResource(R.string.export_save))
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(stringResource(R.string.export_share))
                    }
                    OutlinedButton(onClick = onExport, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                        Text(stringResource(R.string.export_start))
                    }
                }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

private fun label(format: ExportFormat): Int = when (format) {
    ExportFormat.MIDI -> R.string.export_format_midi
    ExportFormat.WAV -> R.string.export_format_wav
    ExportFormat.MP3 -> R.string.export_format_mp3
    ExportFormat.FLAC -> R.string.export_format_flac
    ExportFormat.AAC -> R.string.export_format_aac
}
