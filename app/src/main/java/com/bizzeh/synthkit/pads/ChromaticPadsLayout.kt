package com.bizzeh.synthkit.pads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.HeldNotes
import com.bizzeh.synthkit.play.OctaveButtons
import com.bizzeh.synthkit.play.PadGrid
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.play.noteName
import com.bizzeh.synthkit.play.padRows
import com.bizzeh.synthkit.ui.MinTouchTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Six columns: two rows make one octave, so octave shift leaves no gaps.
private const val COLUMNS = 6
private const val OCTAVE = 12
private const val HIGHEST_NOTE = 127
private const val ACCESSIBLE_NOTE_MILLIS = 400L

/** Consecutive semitones from a root, sounding while held, with octave shift. */
@Composable
fun ChromaticPadsLayout(player: NotePlayer, channel: Int, defaultRoot: Int, modifier: Modifier = Modifier) {
    var root by rememberSaveable { mutableIntStateOf(defaultRoot) }
    val notes = remember(player, channel) { HeldNotes(player, channel) }
    DisposableEffect(notes) { onDispose { notes.releaseAll() } }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val count = COLUMNS * padRows(maxHeight - MinTouchTarget - 8.dp)
            val highestRoot = HIGHEST_NOTE - count + 1
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OctaveButtons(
                    label = noteName(root).replace(" ", ""),
                    canGoDown = root - OCTAVE >= 0,
                    canGoUp = root + OCTAVE <= highestRoot,
                    onDown = { root -= OCTAVE },
                    onUp = { root += OCTAVE },
                    modifier = Modifier.height(MinTouchTarget),
                )
                PadGrid(items = (root until root + count).toList(), columns = COLUMNS) { note, padModifier ->
                    val name = noteName(note)
                    PlayPad(
                        description = name,
                        onPress = { notes.press(note) },
                        onRelease = { notes.release(note) },
                        onAccessibleTap = {
                            notes.press(note)
                            scope.launch {
                                delay(ACCESSIBLE_NOTE_MILLIS)
                                notes.release(note)
                            }
                        },
                        idleColor = MaterialTheme.colorScheme.primaryContainer,
                        pressedColor = MaterialTheme.colorScheme.tertiary,
                        modifier = padModifier,
                    ) { pressed ->
                        Text(
                            text = name.replace(" ", ""),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (pressed) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
