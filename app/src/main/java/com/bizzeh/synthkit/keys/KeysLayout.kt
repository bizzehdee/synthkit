package com.bizzeh.synthkit.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.HeldNotes
import com.bizzeh.synthkit.play.HoldToggle
import com.bizzeh.synthkit.play.OctaveButtons
import com.bizzeh.synthkit.play.noteName
import com.bizzeh.synthkit.ui.MinPlayableWidth
import com.bizzeh.synthkit.ui.MinTouchTarget

private const val START_NOTE = 48 // C3
private const val MAX_WHITE_KEYS = 22

/** Keys layout from docs/gm-layouts.md: keyboard, octave buttons, scroll strip and hold. */
@Composable
fun KeysLayout(player: NotePlayer, channel: Int, holdByDefault: Boolean, modifier: Modifier = Modifier) {
    var hold by rememberSaveable { mutableStateOf(holdByDefault) }
    val notes = remember(player, channel) { HeldNotes(player, channel, hold) }
    DisposableEffect(notes) { onDispose { notes.releaseAll() } }
    var firstWhite by rememberSaveable { mutableIntStateOf(KeyboardGeometry.whiteIndex(START_NOTE)) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Larger screens show more keys; keys never shrink below the minimum width.
        val whiteCount = (maxWidth / MinPlayableWidth).toInt().coerceIn(KeyboardGeometry.WHITES_PER_OCTAVE, MAX_WHITE_KEYS)
        val shown = KeyboardGeometry.clampFirstWhite(firstWhite, whiteCount)
        val whiteWidthPx = with(LocalDensity.current) { (maxWidth / whiteCount).toPx() }
        val shiftTo = { target: Int -> firstWhite = KeyboardGeometry.clampFirstWhite(target, whiteCount) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(MinTouchTarget),
            ) {
                OctaveButtons(
                    label = noteName(KeyboardGeometry.whiteNote(shown)).replace(" ", ""),
                    canGoDown = shown > KeyboardGeometry.LOWEST_WHITE,
                    canGoUp = shown < KeyboardGeometry.clampFirstWhite(Int.MAX_VALUE, whiteCount),
                    onDown = { shiftTo(shown - KeyboardGeometry.WHITES_PER_OCTAVE) },
                    onUp = { shiftTo(shown + KeyboardGeometry.WHITES_PER_OCTAVE) },
                )
                HoldToggle(hold = hold, onHoldChange = {
                    hold = it
                    notes.hold = it
                })
                ScrollStrip(
                    firstWhite = shown,
                    whiteCount = whiteCount,
                    whiteWidthPx = whiteWidthPx,
                    onShift = { steps -> shiftTo(shown + steps) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Keyboard(
                firstWhite = shown,
                whiteCount = whiteCount,
                notes = notes,
                onAccessibleTap = { playAccessibleNote(player, channel, it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Dragging the strip moves the keyboard one white key per key width dragged.
 * The thumb shows where the visible keys sit in the piano range.
 */
@Composable
private fun ScrollStrip(
    firstWhite: Int,
    whiteCount: Int,
    whiteWidthPx: Float,
    onShift: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragged by remember { mutableFloatStateOf(0f) }
    val description = stringResource(R.string.scroll_keyboard)
    val state = rememberDraggableState { delta ->
        dragged += delta
        val steps = (dragged / whiteWidthPx).toInt()
        if (steps != 0) {
            dragged -= steps * whiteWidthPx
            // Dragging right pulls lower keys into view.
            onShift(-steps)
        }
    }
    val range = KeyboardGeometry.HIGHEST_WHITE - KeyboardGeometry.LOWEST_WHITE + 1
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .draggable(state, Orientation.Horizontal)
            .semantics { contentDescription = description },
    ) {
        val thumbWidth = maxWidth * (whiteCount.toFloat() / range)
        val thumbOffset = maxWidth * ((firstWhite - KeyboardGeometry.LOWEST_WHITE).toFloat() / range)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(thumbWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
