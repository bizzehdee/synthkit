package com.bizzeh.synthkit.looper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.MinTouchTarget

private val GlyphSize = 18.dp

/** Record arms or starts a take; while a take runs it ends it. */
@Composable
fun RecordButton(recording: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(if (recording) R.string.stop_recording else R.string.record)
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.size(MinTouchTarget).semantics { contentDescription = description },
    ) {
        val color = if (recording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
        Canvas(Modifier.size(GlyphSize)) { drawCircle(color) }
    }
}

@Composable
fun PlayStopButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(if (playing) R.string.stop else R.string.play)
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(MinTouchTarget).semantics { contentDescription = description },
    ) {
        val color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(GlyphSize)) {
            if (playing) {
                drawRect(color, Offset.Zero, Size(size.width, size.height))
            } else {
                drawPath(Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(0f, size.height)
                    close()
                }, color)
            }
        }
    }
}

/**
 * One short line saying what the looper is doing and where it is. [inTrack] is
 * true on a track's screen, where the hint before the first take differs.
 */
@Composable
fun PhaseStatus(state: LooperState, modifier: Modifier = Modifier, inTrack: Boolean = false) {
    val bar = state.bar + 1
    val bars = state.project.loopBars
    val text = when {
        state.finishing -> stringResource(R.string.phase_finishing)
        state.phase == Phase.ARMED -> stringResource(R.string.phase_armed)
        state.phase == Phase.RECORDING_FIRST -> stringResource(R.string.phase_recording_first, bar)
        state.phase == Phase.PLAYING -> stringResource(R.string.phase_playing, bar, bars)
        state.phase == Phase.OVERDUBBING -> stringResource(R.string.phase_overdubbing, bar, bars)
        bars == 0 -> stringResource(if (inTrack) R.string.phase_no_loop_track else R.string.phase_no_loop)
        else -> stringResource(R.string.phase_stopped)
    }
    val recording = state.phase == Phase.RECORDING_FIRST || state.phase == Phase.OVERDUBBING
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (recording) MaterialTheme.colorScheme.error else Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

val LooperState.isRecording: Boolean
    get() = phase == Phase.ARMED || phase == Phase.RECORDING_FIRST || phase == Phase.OVERDUBBING

val LooperState.isPlaying: Boolean
    get() = phase != Phase.STOPPED
