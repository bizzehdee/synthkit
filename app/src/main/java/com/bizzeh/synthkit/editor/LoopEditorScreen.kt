package com.bizzeh.synthkit.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.drums.GmPercussionNotes
import com.bizzeh.synthkit.drums.shortDrumName
import com.bizzeh.synthkit.instruments.InstrumentCatalogue
import com.bizzeh.synthkit.looper.TakeBuilder
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.Track
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

@Composable
fun LoopEditorScreen(
    color: Color,
    project: Project,
    track: Track,
    instrumentName: String,
    onBack: () -> Unit,
    onQuantise: (Quantise) -> Unit,
    onNotes: (List<Note>) -> Unit,
    onDoubleLoop: () -> Unit,
    onAudition: (Note) -> Unit,
    modifier: Modifier = Modifier,
    playhead: (() -> Int?)? = null,
    transport: @Composable () -> Unit = {},
) {
    val drums = track.bank == InstrumentCatalogue.DRUM_KIT_BANK
    val rows = StepGrid.rows(drums)
    val columns = StepGrid.columns(project.loopTicks)
    val notes = track.notes
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    if (selected != null && selected !in notes.indices) selected = null
    val gmNames = stringArrayResource(R.array.gm_percussion_names)
    val pitchNames = stringArrayResource(R.array.pitch_class_short_names)
    val shortNames = firstPageLabels()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Note controls replace the loop controls in the same bar so the grid never moves.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PanelIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            val index = selected
            if (index == null) {
                Text(
                    stringResource(R.string.editor_title, instrumentName),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val quantiseLabel = stringResource(R.string.quantise)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = quantiseLabel }) {
                    Quantise.entries.forEachIndexed { i, option ->
                        SegmentedButton(
                            selected = track.quantise == option,
                            onClick = { onQuantise(option) },
                            shape = SegmentedButtonDefaults.itemShape(i, Quantise.entries.size),
                        ) {
                            Text(stringResource(when (option) {
                                Quantise.OFF -> R.string.quantise_off
                                Quantise.EIGHTH -> R.string.quantise_eighth
                                Quantise.SIXTEENTH -> R.string.quantise_sixteenth
                            }))
                        }
                    }
                }
                OutlinedButton(
                    onClick = onDoubleLoop,
                    enabled = project.loopBars * 2 <= Project.MAX_LOOP_BARS,
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) { Text(stringResource(R.string.double_loop)) }
            } else {
                Text(stringResource(R.string.velocity), style = MaterialTheme.typography.labelLarge)
                val velocityLabel = stringResource(R.string.velocity)
                Slider(
                    value = notes[index].velocity.toFloat(),
                    onValueChange = { onNotes(StepGrid.setVelocity(notes, index, it.roundToInt())) },
                    onValueChangeFinished = { notes.getOrNull(index)?.let(onAudition) },
                    valueRange = 1f..127f,
                    modifier = Modifier.weight(1f).semantics { contentDescription = velocityLabel },
                )
                TextButton(onClick = {
                    onNotes(StepGrid.remove(notes, index))
                    selected = null
                }, modifier = Modifier.heightIn(min = MinTouchTarget)) { Text(stringResource(R.string.note_delete)) }
                PanelIconButton(Icons.Filled.Check, stringResource(R.string.note_done), { selected = null })
            }
            transport()
        }
        StepGridCanvas(
            noteColor = color,
            playhead = playhead,
            rows = rows,
            rowLabel = { key ->
                if (drums) {
                    shortNames[key] ?: gmNames[key - GmPercussionNotes.first]
                } else {
                    pitchNames[key % 12] + (key / 12 - 1)
                }
            },
            columns = columns,
            notes = notes,
            selected = selected,
            initialRow = StepGrid.initialRow(rows, notes, drums),
            description = stringResource(
                R.string.grid_description,
                pluralStringResource(R.plurals.grid_steps, columns, columns),
                pluralStringResource(R.plurals.grid_notes, notes.size, notes.size),
            ),
            onTap = { cell ->
                val hit = StepGrid.noteAt(notes, cell, columns)
                when {
                    hit == null -> {
                        val added = StepGrid.add(notes, cell)
                        onNotes(added)
                        onAudition(added.last())
                        selected = null
                    }
                    hit == selected -> {
                        onNotes(StepGrid.remove(notes, hit))
                        selected = null
                    }
                    else -> {
                        selected = hit
                        onAudition(notes[hit])
                    }
                }
            },
            onMove = { index, cell ->
                val moved = StepGrid.move(notes, index, cell)
                onNotes(moved)
                onAudition(moved[index])
                selected = index
            },
            onRowTap = { key -> onAudition(Note(0, key, StepGrid.NEW_NOTE_VELOCITY, TakeBuilder.ONE_SHOT_TICKS)) },
            rowsDescription = stringResource(R.string.grid_rows_description),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun firstPageLabels(): Map<Int, String> =
    GmPercussionNotes.mapNotNull { note -> shortDrumName(note)?.let { note to stringResource(it) } }.toMap()
