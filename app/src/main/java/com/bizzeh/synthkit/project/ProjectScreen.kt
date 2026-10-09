package com.bizzeh.synthkit.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.instruments.Family
import com.bizzeh.synthkit.looper.LoopMath
import com.bizzeh.synthkit.looper.LooperState
import com.bizzeh.synthkit.looper.PlayStopButton
import com.bizzeh.synthkit.looper.isPlaying
import com.bizzeh.synthkit.looper.isRecording
import com.bizzeh.synthkit.looper.phaseText
import com.bizzeh.synthkit.project.ProjectValidation.MAX_TRACKS
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.BeatLights
import com.bizzeh.synthkit.ui.studio.LcdDisplay
import com.bizzeh.synthkit.ui.studio.LcdValue
import com.bizzeh.synthkit.ui.studio.LitToggle
import com.bizzeh.synthkit.ui.studio.MiniFader
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.studio.SquareToggle
import com.bizzeh.synthkit.ui.studio.glow
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.Eyebrow
import com.bizzeh.synthkit.ui.theme.PanelLabel
import com.bizzeh.synthkit.ui.theme.StudioTheme
import com.bizzeh.synthkit.ui.theme.familyColor
import kotlin.math.roundToInt

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
    familyOf: (Track) -> Family?,
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
    val p = StudioTheme.palette
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTempo by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PanelIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.project_eyebrow).uppercase(), style = Eyebrow, color = p.muted)
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = p.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ProjectLcd(state, onTempo = onTempo, onEditTempo = { editingTempo = true })
            LitToggle(
                label = stringResource(R.string.click_on_playback),
                on = project.metronomeOnPlayback,
                onChange = onClickOnPlayback,
                color = p.amber,
            )
            PlayStopButton(playing = state.isPlaying, enabled = state.isPlaying || project.loopBars > 0, onClick = onPlayStop)
            ExportButton(enabled = project.loopBars > 0, onClick = onExport)
        }
        if (!canAddTrack) {
            Text(pluralStringResource(R.plurals.track_limit, MAX_TRACKS, MAX_TRACKS), style = MaterialTheme.typography.bodySmall, color = p.muted)
        }
        if (project.tracks.isEmpty()) {
            Text(stringResource(R.string.no_tracks), style = MaterialTheme.typography.bodyLarge, color = p.muted, modifier = Modifier.padding(8.dp))
        } else if (project.loopBars == 0) {
            Text(stringResource(R.string.phase_no_loop), style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(horizontal = 8.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    name = instrumentName(track),
                    color = familyOf(track)?.let(::familyColor) ?: p.muted,
                    state = state,
                    actions = tracks,
                    onDelete = { confirmDelete = track.id },
                    onClear = { confirmClear = track.id },
                )
            }
            item {
                AddTrackRow(enabled = canAddTrack, onClick = onAddTrack)
            }
        }
    }

    if (editingTempo) TempoDialog(project.tempoBpm, onChange = onTempo, onDismiss = { editingTempo = false })
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

/** Tempo stepper, beat lights and loop position on the transport display. */
@Composable
private fun ProjectLcd(state: LooperState, onTempo: (Int) -> Unit, onEditTempo: () -> Unit) {
    val p = StudioTheme.palette
    val bpm = state.project.tempoBpm
    val bars = state.project.loopBars
    val status = phaseText(state)
    val tempoLabel = stringResource(R.string.tempo_edit, bpm)
    LcdDisplay {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp, MinTouchTarget)
                    .clickable(enabled = bpm > Project.MIN_TEMPO, role = Role.Button) { onTempo(bpm - 1) },
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.tempo_down), tint = p.amber)
            }
            Box(
                Modifier
                    .clickable(role = Role.Button, onClickLabel = tempoLabel, onClick = onEditTempo)
                    .semantics(mergeDescendants = true) { contentDescription = tempoLabel },
            ) {
                LcdValue(bpm.toString(), stringResource(R.string.lcd_bpm))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp, MinTouchTarget)
                    .clickable(enabled = bpm < Project.MAX_TEMPO, role = Role.Button) { onTempo(bpm + 1) },
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.tempo_up), tint = p.amber)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = status },
        ) {
            BeatLights(state.beat)
            LcdValue(if (bars == 0) "–/–" else "${state.bar + 1}/$bars")
        }
    }
}

@Composable
private fun ExportButton(enabled: Boolean, onClick: () -> Unit) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(MinTouchTarget)
            .raised(p, 24.dp, fill = p.panel)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, tint = if (enabled) p.text else p.off)
        Text(stringResource(R.string.export), style = MaterialTheme.typography.labelLarge, color = if (enabled) p.text else p.off)
    }
}

@Composable
private fun TrackRow(
    track: Track,
    name: String,
    color: Color,
    state: LooperState,
    actions: TrackActions,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    val p = StudioTheme.palette
    var menu by remember { mutableStateOf(false) }
    // recordingTrackId stays set after a take ends, so the phase decides.
    val recording = state.isRecording && state.recordingTrackId == track.id
    val recordingLabel = stringResource(R.string.track_recording)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .glow(p.record, 14.dp, enabled = recording, spread = 6.dp)
            .raised(p, 14.dp, edge = if (recording) p.record else p.line)
            .clip(RoundedCornerShape(14.dp))
            .semantics { if (recording) stateDescription = recordingLabel },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button) { actions.open(track.id) },
        ) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(color))
            NoteTile(color)
            Column(modifier = Modifier.width(150.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.take_count, track.takes.size, track.takes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = p.muted,
                )
            }
            LanePreview(track, state, color, Modifier.weight(1f).height(36.dp))
        }
        SquareToggle(
            label = stringResource(R.string.mute_short),
            on = track.muted,
            onColor = p.amber,
            contentDescription = stringResource(R.string.track_mute_named, name),
            onChange = { actions.setMuted(track.id, it) },
        )
        SquareToggle(
            label = stringResource(R.string.solo_short),
            on = track.solo,
            onColor = p.solo,
            contentDescription = stringResource(R.string.track_solo_named, name),
            onChange = { actions.setSolo(track.id, it) },
        )
        MiniFader(
            value = track.volume,
            onChange = { actions.setVolume(track.id, it) },
            color = color,
            contentDescription = stringResource(R.string.track_volume, name),
        )
        Box {
            PanelIconButton(Icons.Filled.MoreVert, stringResource(R.string.track_actions, name), { menu = true }, framed = false)
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_edit)) },
                    enabled = state.project.loopBars > 0,
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

/** A tile with a small drawn note; the core icon set has no music note. */
@Composable
private fun NoteTile(color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(color, Offset(size.width * 0.38f, size.height * 0.78f), Offset(size.width * 0.38f, size.height * 0.08f), stroke)
            drawLine(color, Offset(size.width * 0.38f, size.height * 0.08f), Offset(size.width * 0.9f, size.height * 0.2f), stroke)
            drawLine(color, Offset(size.width * 0.9f, size.height * 0.2f), Offset(size.width * 0.9f, size.height * 0.68f), stroke)
            drawCircle(color, radius = size.width * 0.16f, center = Offset(size.width * 0.24f, size.height * 0.8f))
            drawCircle(color, radius = size.width * 0.16f, center = Offset(size.width * 0.76f, size.height * 0.7f))
        }
    }
}

/** The track's notes with the playhead at beat resolution, as the bar and beat are all the state carries. */
@Composable
private fun LanePreview(track: Track, state: LooperState, color: Color, modifier: Modifier) {
    val beats = state.project.loopBars * BEATS_PER_BAR
    val playhead = state.beat?.takeIf { state.isPlaying && beats > 0 }?.let { (state.bar * BEATS_PER_BAR + it).toFloat() / beats }
    NotePreview(track, state.project.loopBars, color, modifier, playhead)
}

@Composable
private fun AddTrackRow(enabled: Boolean, onClick: () -> Unit) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .drawBehind {
                drawRoundRect(
                    p.line,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = if (enabled) p.muted else p.off)
        Text(
            stringResource(R.string.add_track),
            style = PanelLabel,
            color = if (enabled) p.muted else p.off,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The full tempo range on a slider. */
@Composable
private fun TempoDialog(bpm: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableFloatStateOf(bpm.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tempo_value, value.roundToInt())) },
        text = {
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = Project.MIN_TEMPO.toFloat()..Project.MAX_TEMPO.toFloat(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onChange(value.roundToInt())
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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
