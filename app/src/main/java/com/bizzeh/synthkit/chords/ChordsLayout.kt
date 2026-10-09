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
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.ui.studio.Stepper
import com.bizzeh.synthkit.ui.studio.glow
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.Eyebrow
import com.bizzeh.synthkit.ui.theme.StudioTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.bizzeh.synthkit.ui.MinPlayableHeight
import com.bizzeh.synthkit.ui.MinTouchTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TalkBack has no press and release, so a double-tap plays the chord for this long.
private const val ACCESSIBLE_CHORD_MILLIS = 800L
private const val MAX_BASS_OCTAVE_SHIFT = 2
private const val STRING_LIGHT_MILLIS = 150L

/** Chords layout from docs/gm-layouts.md: key, mode, seven triad pads and a strum strip. */
@Composable
fun ChordsLayout(player: NotePlayer, channel: Int, bassRoots: Boolean, color: Color, modifier: Modifier = Modifier) {
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
                Stepper(
                    value = "%+d".format(octaveShift),
                    downDescription = stringResource(R.string.octave_down),
                    upDescription = stringResource(R.string.octave_up),
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
                    color = color,
                    onPress = { chords.pressPad(triad.degree) },
                    onRelease = { chords.releasePad(triad.degree) },
                    onAccessibleTap = {
                        chords.pressPad(triad.degree)
                        scope.launch {
                            delay(ACCESSIBLE_CHORD_MILLIS)
                            chords.releasePad(triad.degree)
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) { lit ->
                    val p = StudioTheme.palette
                    Column {
                        Text(triad.numeral, style = Eyebrow, color = if (lit) color else p.muted)
                        Text(label, style = MaterialTheme.typography.headlineSmall, color = p.text)
                    }
                }
            }
        }
        if (!bassRoots) {
            StrumStrip(onStrike = chords::strikeString, color = color, modifier = Modifier.fillMaxWidth().height(MinPlayableHeight))
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
        val p = StudioTheme.palette
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .raised(p, 12.dp, fill = p.panel)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { open = true }
                .padding(horizontal = 14.dp),
        ) {
            Text(stringResource(R.string.chord_key, names[key]), style = MaterialTheme.typography.titleMedium, color = p.text)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = p.muted)
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
private fun StrumStrip(onStrike: (Int) -> Unit, color: Color, modifier: Modifier = Modifier) {
    val currentOnStrike by rememberUpdatedState(onStrike)
    val description = stringResource(R.string.strum_strip)
    val lanes = ChordPlayer.STRINGS
    val p = StudioTheme.palette
    // The string last struck lights up briefly.
    var litLane by remember { mutableIntStateOf(-1) }
    LaunchedEffect(litLane) {
        if (litLane >= 0) {
            delay(STRING_LIGHT_MILLIS)
            litLane = -1
        }
    }
    val strike = { lane: Int ->
        litLane = lane
        currentOnStrike(lane)
    }
    Row(
        modifier = modifier
            .raised(p, 12.dp, fill = p.panel)
            .clip(RoundedCornerShape(12.dp))
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
                                    strike(lane)
                                }
                                change.changedToUpIgnoreConsumed() -> lastLane.remove(change.id)
                                change.pressed -> {
                                    val previous = lastLane[change.id] ?: return@forEach
                                    if (lane != previous) {
                                        val step = if (lane > previous) 1 else -1
                                        var crossed = previous + step
                                        while (true) {
                                            strike(crossed)
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
            val lit = string == litLane
            Box(
                modifier = Modifier
                    .width((1.5f + (lanes - string) / 2f).dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
                    .glow(color, 1.dp, enabled = lit, spread = 4.dp)
                    .background(if (lit) color else p.muted.copy(alpha = 0.6f)),
            )
        }
    }
}
