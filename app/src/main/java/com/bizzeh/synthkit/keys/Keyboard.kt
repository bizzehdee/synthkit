package com.bizzeh.synthkit.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import com.bizzeh.synthkit.play.HeldNotes
import com.bizzeh.synthkit.play.noteName

/** Black keys are narrower than the 48 dp minimum by agreement, so two octaves fit on a phone. */
val BlackKeyWidth = 30.dp
private const val BLACK_KEY_HEIGHT_FRACTION = 0.6f

// TalkBack has no press and release, so a double-tap plays the note briefly.
private const val ACCESSIBLE_NOTE_MILLIS = 400f

/**
 * A piano keyboard showing [whiteCount] white keys from [firstWhite]. Each
 * finger plays the key it lands on and keeps that note until it lifts.
 */
@Composable
fun Keyboard(
    firstWhite: Int,
    whiteCount: Int,
    notes: HeldNotes,
    onAccessibleTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressed = remember { mutableStateMapOf<PointerId, Int>() }
    val currentNotes by rememberUpdatedState(notes)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val whiteWidth: Dp = maxWidth / whiteCount
        val keyHeight: Dp = maxHeight
        val blackHeight: Dp = keyHeight * BLACK_KEY_HEIGHT_FRACTION
        val geometry = with(density) {
            KeyboardGeometry(firstWhite, whiteCount, whiteWidth.toPx(), BlackKeyWidth.toPx(), blackHeight.toPx())
        }
        val currentGeometry by rememberUpdatedState(geometry)
        val sounding = pressed.values.toSet()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    try {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    when {
                                        change.changedToDownIgnoreConsumed() ->
                                            currentGeometry.noteAt(change.position.x, change.position.y)?.let { note ->
                                                pressed[change.id] = note
                                                currentNotes.press(note)
                                                change.consume()
                                            }
                                        change.changedToUpIgnoreConsumed() ->
                                            pressed.remove(change.id)?.let { note ->
                                                currentNotes.release(note)
                                                change.consume()
                                            }
                                    }
                                }
                            }
                        }
                    } finally {
                        pressed.values.forEach { currentNotes.release(it) }
                        pressed.clear()
                    }
                },
        ) {
            val colors = MaterialTheme.colorScheme
            geometry.whiteKeys.forEach { key ->
                val description = noteName(key.note)
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .offset(x = with(density) { key.left.toDp() })
                        .size(whiteWidth, keyHeight)
                        .background(if (key.note in sounding) colors.tertiaryContainer else colors.surface)
                        .border(1.dp, colors.outline)
                        .keySemantics(description) { onAccessibleTap(key.note) },
                ) {
                    if (key.note % 12 == 0) {
                        Text(
                            text = description.replace(" ", ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
            }
            geometry.blackKeys.forEach { key ->
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { key.left.toDp() })
                        .size(BlackKeyWidth, blackHeight)
                        .background(
                            if (key.note in sounding) colors.tertiary else colors.inverseSurface,
                            RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                        )
                        .keySemantics(noteName(key.note)) { onAccessibleTap(key.note) },
                )
            }
        }
    }
}

private fun Modifier.keySemantics(description: String, onTap: () -> Unit): Modifier = semantics {
    contentDescription = description
    role = Role.Button
    onClick(label = description) {
        onTap()
        true
    }
}

/** Plays [note] briefly for an accessibility double-tap. */
fun playAccessibleNote(player: NotePlayer, channel: Int, note: Int) {
    player.noteOn(channel, note, FIXED_VELOCITY)
    player.noteOff(channel, note, ACCESSIBLE_NOTE_MILLIS)
}
