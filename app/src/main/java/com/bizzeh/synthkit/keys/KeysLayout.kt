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
import com.bizzeh.synthkit.ui.studio.LitToggle
import com.bizzeh.synthkit.ui.studio.Stepper
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.StudioTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.bizzeh.synthkit.play.noteName
import com.bizzeh.synthkit.ui.MinPlayableWidth
import com.bizzeh.synthkit.ui.MinTouchTarget

private const val START_NOTE = 48 // C3
private const val MAX_WHITE_KEYS = 22

/** Keys layout from docs/gm-layouts.md: keyboard, octave buttons, scroll strip and hold. */
@Composable
fun KeysLayout(player: NotePlayer, channel: Int, holdByDefault: Boolean, color: Color, modifier: Modifier = Modifier) {
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
                Stepper(
                    value = noteName(KeyboardGeometry.whiteNote(shown)).replace(" ", ""),
                    downDescription = stringResource(R.string.octave_down),
                    upDescription = stringResource(R.string.octave_up),
                    canGoDown = shown > KeyboardGeometry.LOWEST_WHITE,
                    canGoUp = shown < KeyboardGeometry.clampFirstWhite(Int.MAX_VALUE, whiteCount),
                    onDown = { shiftTo(shown - KeyboardGeometry.WHITES_PER_OCTAVE) },
                    onUp = { shiftTo(shown + KeyboardGeometry.WHITES_PER_OCTAVE) },
                )
                LitToggle(label = stringResource(R.string.hold), on = hold, color = color, onChange = {
                    hold = it
                    notes.hold = it
                })
                ScrollStrip(
                    color = color,
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
                color = color,
                onAccessibleTap = { playAccessibleNote(player, channel, it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Dragging the strip moves the keyboard one white key per key width dragged.
 * It draws the whole piano in miniature with a frame round the keys in view.
 */
@Composable
private fun ScrollStrip(
    color: Color,
    firstWhite: Int,
    whiteCount: Int,
    whiteWidthPx: Float,
    onShift: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = StudioTheme.palette
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
    val start = firstWhite - KeyboardGeometry.LOWEST_WHITE
    Box(
        modifier = modifier
            .raised(p, 12.dp, fill = p.panel)
            .draggable(state, Orientation.Horizontal)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
            val keyWidth = size.width / range
            for (key in 0 until range) {
                drawRect(
                    color = if (key in start until start + whiteCount) p.text else p.off,
                    topLeft = Offset(key * keyWidth, 0f),
                    size = Size(keyWidth - 1f, size.height),
                )
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(start * keyWidth - 3.dp.toPx(), -6.dp.toPx()),
                size = Size(whiteCount * keyWidth + 6.dp.toPx(), size.height + 12.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
        }
    }
}
