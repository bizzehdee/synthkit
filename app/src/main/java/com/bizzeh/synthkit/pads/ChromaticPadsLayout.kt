package com.bizzeh.synthkit.pads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.bizzeh.synthkit.play.PadGrid
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.play.noteName
import com.bizzeh.synthkit.play.padRows
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.Stepper
import com.bizzeh.synthkit.ui.theme.LcdText
import com.bizzeh.synthkit.ui.theme.StudioTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Six columns: two rows make one octave, so octave shift leaves no gaps.
private const val COLUMNS = 6
private const val OCTAVE = 12
private const val HIGHEST_NOTE = 127
private const val ACCESSIBLE_NOTE_MILLIS = 400L

/** Consecutive semitones from a root, sounding while held, with octave shift. */
@Composable
fun ChromaticPadsLayout(player: NotePlayer, channel: Int, defaultRoot: Int, color: Color, modifier: Modifier = Modifier) {
    var root by rememberSaveable { mutableIntStateOf(defaultRoot) }
    val notes = remember(player, channel) { HeldNotes(player, channel) }
    DisposableEffect(notes) { onDispose { notes.releaseAll() } }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val count = COLUMNS * padRows(maxHeight - MinTouchTarget - 8.dp)
            val highestRoot = HIGHEST_NOTE - count + 1
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(
                    value = noteName(root).replace(" ", ""),
                    downDescription = stringResource(R.string.octave_down),
                    upDescription = stringResource(R.string.octave_up),
                    canGoDown = root - OCTAVE >= 0,
                    canGoUp = root + OCTAVE <= highestRoot,
                    onDown = { root -= OCTAVE },
                    onUp = { root += OCTAVE },
                )
                PadGrid(items = (root until root + count).toList(), columns = COLUMNS) { note, padModifier ->
                    val name = noteName(note)
                    PlayPad(
                        description = name,
                        color = color,
                        onPress = { notes.press(note) },
                        onRelease = { notes.release(note) },
                        onAccessibleTap = {
                            notes.press(note)
                            scope.launch {
                                delay(ACCESSIBLE_NOTE_MILLIS)
                                notes.release(note)
                            }
                        },
                        modifier = padModifier,
                    ) { lit ->
                        Text(
                            text = name.replace(" ", "").replace("sharp", "♯"),
                            style = LcdText,
                            color = if (lit) StudioTheme.palette.text else StudioTheme.palette.muted,
                        )
                    }
                }
            }
        }
    }
}
