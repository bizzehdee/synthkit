package com.bizzeh.synthkit.drums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.audio.NotePlayer

private const val COLUMNS = 4
private val PadSpacing = 8.dp
private val MinPadWidth = 48.dp
private val MinPadHeight = 72.dp

@Composable
fun DrumPadGrid(pads: List<DrumPad>, player: NotePlayer, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(PadSpacing),
    ) {
        pads.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PadSpacing),
            ) {
                row.forEach { pad ->
                    DrumPadButton(
                        pad = pad,
                        onHit = { player.noteOn(DRUM_CHANNEL, pad.note, DRUM_VELOCITY) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrumPadButton(pad: DrumPad, onHit: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(pad.label)
    val currentOnHit by rememberUpdatedState(onHit)
    var pressed by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = MinPadWidth, minHeight = MinPadHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) colors.tertiary else colors.primaryContainer)
            // Sound starts on finger down, not on release. Each finger that lands
            // on the pad plays, so several fingers can play pads at the same time.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.changedToDownIgnoreConsumed()) {
                                currentOnHit()
                                change.consume()
                            }
                        }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick(label = label) {
                    currentOnHit()
                    true
                }
            },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (pressed) colors.onTertiary else colors.onPrimaryContainer,
        )
    }
}
