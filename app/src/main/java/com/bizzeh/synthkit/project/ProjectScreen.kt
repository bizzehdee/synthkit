package com.bizzeh.synthkit.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.looper.LooperState
import com.bizzeh.synthkit.looper.phaseText
import com.bizzeh.synthkit.looper.PlayStopButton
import com.bizzeh.synthkit.looper.isPlaying
import com.bizzeh.synthkit.project.ProjectValidation.MAX_TRACKS
import com.bizzeh.synthkit.ui.MinTouchTarget

/** What a track row can ask for. */
class TrackActions(
    val open: (String) -> Unit,
    val setMuted: (String, Boolean) -> Unit,
    val setSolo: (String, Boolean) -> Unit,
    val setVolume: (String, Float) -> Unit,
    val undo: (String) -> Unit,
    val clear: (String) -> Unit,
    val changeInstrument: (String) -> Unit,
    val delete: (String) -> Unit,
    val edit: (String) -> Unit,
)

@Composable
fun ProjectScreen(
    state: LooperState,
    instrumentName: (Track) -> String,
    canAddTrack: Boolean,
    onBack: () -> Unit,
    onAddTrack: () -> Unit,
    onTempo: (Int) -> Unit,
    onClickOnPlayback: (Boolean) -> Unit,
    onPlayStop: () -> Unit,
    tracks: TrackActions,
    modifier: Modifier = Modifier,
    onExport: () -> Unit = {},
) {
    val project = state.project
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                project.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TempoControl(bpm = project.tempoBpm, onChange = onTempo)
            FilterChip(
                selected = project.metronomeOnPlayback,
                onClick = { onClickOnPlayback(!project.metronomeOnPlayback) },
                label = { Text(stringResource(R.string.click_on_playback)) },
                modifier = Modifier.heightIn(min = MinTouchTarget).padding(horizontal = 8.dp),
            )
            PlayStopButton(playing = state.isPlaying, enabled = state.isPlaying || project.loopBars > 0, onClick = onPlayStop)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
            Text(phaseText(state), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onExport,
                enabled = project.loopBars > 0,
                modifier = Modifier.heightIn(min = MinTouchTarget).padding(end = 8.dp),
            ) { Text(stringResource(R.string.export)) }
            Button(onClick = onAddTrack, enabled = canAddTrack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.add_track))
            }
        }
        if (!canAddTrack) {
            Text(pluralStringResource(R.plurals.track_limit, MAX_TRACKS, MAX_TRACKS), style = MaterialTheme.typography.bodySmall)
        }
        if (project.tracks.isEmpty()) {
            Text(stringResource(R.string.no_tracks), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(project.tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    name = instrumentName(track),
                    canEdit = project.loopBars > 0,
                    recording = state.recordingTrackId == track.id,
                    actions = tracks,
                    onDelete = { confirmDelete = track.id },
                    onClear = { confirmClear = track.id },
                )
                HorizontalDivider()
            }
        }
    }

    project.tracks.firstOrNull { it.id == confirmDelete }?.let { track ->
        ConfirmDialog(
            text = stringResource(R.string.track_delete_confirm, instrumentName(track)),
            confirm = stringResource(R.string.track_delete),
            onConfirm = { tracks.delete(track.id) },
            onDismiss = { confirmDelete = null },
        )
    }
    project.tracks.firstOrNull { it.id == confirmClear }?.let { track ->
        ConfirmDialog(
            text = stringResource(R.string.track_clear_confirm, instrumentName(track)),
            confirm = stringResource(R.string.track_clear),
            onConfirm = { tracks.clear(track.id) },
            onDismiss = { confirmClear = null },
        )
    }
}

@Composable
private fun TrackRow(
    track: Track,
    name: String,
    canEdit: Boolean,
    recording: Boolean,
    actions: TrackActions,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val volumeDescription = stringResource(R.string.track_volume, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (recording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { actions.open(track.id) }
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.take_count, track.takes.size, track.takes.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilterChip(
            selected = track.muted,
            onClick = { actions.setMuted(track.id, !track.muted) },
            label = { Text(stringResource(R.string.track_mute)) },
            modifier = Modifier.heightIn(min = MinTouchTarget).padding(horizontal = 4.dp),
        )
        FilterChip(
            selected = track.solo,
            onClick = { actions.setSolo(track.id, !track.solo) },
            label = { Text(stringResource(R.string.track_solo)) },
            modifier = Modifier.heightIn(min = MinTouchTarget).padding(horizontal = 4.dp),
        )
        Slider(
            value = track.volume,
            onValueChange = { actions.setVolume(track.id, it) },
            modifier = Modifier.width(140.dp).semantics { contentDescription = volumeDescription },
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.track_actions, name))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_edit)) },
                    enabled = canEdit,
                    onClick = {
                        menu = false
                        actions.edit(track.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_undo)) },
                    enabled = track.takes.isNotEmpty(),
                    onClick = {
                        menu = false
                        actions.undo(track.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_clear)) },
                    enabled = track.takes.isNotEmpty(),
                    onClick = {
                        menu = false
                        onClear()
                    },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.track_change_instrument)) }, onClick = {
                    menu = false
                    actions.changeInstrument(track.id)
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.track_delete)) }, onClick = {
                    menu = false
                    onDelete()
                })
            }
        }
    }
}

@Composable
private fun ConfirmDialog(text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
