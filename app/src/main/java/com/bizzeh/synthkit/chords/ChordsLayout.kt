package com.bizzeh.synthkit.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.OctaveButtons
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.ui.MinPlayableHeight
import com.bizzeh.synthkit.ui.MinTouchTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TalkBack has no press and release, so a double-tap plays the chord for this long.
private const val ACCESSIBLE_CHORD_MILLIS = 800L
private const val MAX_BASS_OCTAVE_SHIFT = 2

/** Chords layout from docs/gm-layouts.md: key, mode, seven triad pads and a strum strip. */
@Composable
fun ChordsLayout(player: NotePlayer, channel: Int, bassRoots: Boolean, modifier: Modifier = Modifier) {
    var key by rememberSaveable { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(Mode.MAJOR) }
    var octaveShift by rememberSaveable { mutableIntStateOf(0) }
    val chords = remember(player, channel, bassRoots) { ChordPlayer(player, channel, bassRoots) }
    chords.key = key
    chords.mode = mode
    chords.bassOctaveShift = octaveShift
    DisposableEffect(chords) { onDispose { chords.releaseAll() } }

    val scope = rememberCoroutineScope()
    val shortNames = stringArrayResource(R.array.pitch_class_short_names)
    val spokenNames = stringArrayResource(R.array.pitch_class_names)

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(MinTouchTarget),
        ) {
            KeyPicker(key = key, names = shortNames, onKeyChange = { key = it })
            SingleChoiceSegmentedButtonRow {
                Mode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { mode = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, Mode.entries.size),
                    ) {
                        Text(stringResource(if (entry == Mode.MAJOR) R.string.mode_major else R.string.mode_minor))
                    }
                }
            }
            if (bassRoots) {
                OctaveButtons(
                    label = "%+d".format(octaveShift),
                    canGoDown = octaveShift > 0,
                    canGoUp = octaveShift < MAX_BASS_OCTAVE_SHIFT,
                    onDown = { octaveShift-- },
                    onUp = { octaveShift++ },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Harmony.triads(key, mode).forEach { triad ->
                val label = chordName(triad, shortNames[triad.root], spoken = false)
                val spoken = chordName(triad, spokenNames[triad.root], spoken = true)
                PlayPad(
                    description = spoken,
                    onPress = { chords.pressPad(triad.degree) },
                    onRelease = { chords.releasePad(triad.degree) },
                    onAccessibleTap = {
                        chords.pressPad(triad.degree)
                        scope.launch {
                            delay(ACCESSIBLE_CHORD_MILLIS)
                            chords.releasePad(triad.degree)
                        }
                    },
                    idleColor = MaterialTheme.colorScheme.primaryContainer,
                    pressedColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) { pressed ->
                    val textColor = if (pressed) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.titleLarge, color = textColor)
                        Text(triad.numeral, style = MaterialTheme.typography.labelLarge, color = textColor)
                    }
                }
            }
        }
        if (!bassRoots) {
            StrumStrip(onStrike = chords::strikeString, modifier = Modifier.fillMaxWidth().height(MinPlayableHeight))
        }
    }
}

@Composable
private fun chordName(triad: Triad, root: String, spoken: Boolean): String = stringResource(
    when (triad.quality) {
        Quality.MAJOR -> if (spoken) R.string.chord_spoken_major else R.string.chord_label_major
        Quality.MINOR -> if (spoken) R.string.chord_spoken_minor else R.string.chord_label_minor
        Quality.DIMINISHED -> if (spoken) R.string.chord_spoken_diminished else R.string.chord_label_diminished
    },
    root,
)

// Twelve keys in a 6 x 2 grid: a 12-row list is taller than a landscape phone.
private const val KEY_COLUMNS = 6

@Composable
private fun KeyPicker(key: Int, names: Array<String>, onKeyChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.chord_key, names[key]))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            names.indices.chunked(KEY_COLUMNS).forEach { row ->
                Row {
                    row.forEach { index ->
                        TextButton(
                            onClick = {
                                onKeyChange(index)
                                open = false
                            },
                            modifier = Modifier.sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget),
                        ) {
                            Text(names[index], style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Six string lanes, low string on the left. A finger strikes the lane it lands
 * on, then every lane it crosses, so a swipe plays at the speed of the swipe.
 */
@Composable
private fun StrumStrip(onStrike: (Int) -> Unit, modifier: Modifier = Modifier) {
    val currentOnStrike by rememberUpdatedState(onStrike)
    val description = stringResource(R.string.strum_strip)
    val lanes = ChordPlayer.STRINGS
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                val lastLane = mutableMapOf<PointerId, Int>()
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { change ->
                            val lane = (change.position.x / size.width * lanes).toInt().coerceIn(0, lanes - 1)
                            when {
                                change.changedToDownIgnoreConsumed() -> {
                                    lastLane[change.id] = lane
                                    currentOnStrike(lane)
                                }
                                change.changedToUpIgnoreConsumed() -> lastLane.remove(change.id)
                                change.pressed -> {
                                    val previous = lastLane[change.id] ?: return@forEach
                                    if (lane != previous) {
                                        val step = if (lane > previous) 1 else -1
                                        var crossed = previous + step
                                        while (true) {
                                            currentOnStrike(crossed)
                                            if (crossed == lane) break
                                            crossed += step
                                        }
                                        lastLane[change.id] = lane
                                    }
                                }
                            }
                            change.consume()
                        }
                    }
                }
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(lanes) { string ->
            Box(
                modifier = Modifier
                    .width((1 + (lanes - string) / 2).dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
    }
}
