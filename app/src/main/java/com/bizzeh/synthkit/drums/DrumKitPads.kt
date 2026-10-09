package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import com.bizzeh.synthkit.play.PadGrid
import com.bizzeh.synthkit.play.PadSpacing
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.play.padRows

private const val COLUMNS = 4
private val PageLabelHeight = 24.dp

/**
 * Every GM percussion note on one-shot pads, the agreed first page first and
 * further pages swiped sideways.
 */
@Composable
fun DrumKitPads(player: NotePlayer, channel: Int, modifier: Modifier = Modifier) {
    val gmNames = stringArrayResource(R.array.gm_percussion_names)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageSize = COLUMNS * padRows(maxHeight - PageLabelHeight - PadSpacing)
        val pages = DrumPadOrder.chunked(pageSize)
        val pagerState = rememberPagerState { pages.size }
        Column(verticalArrangement = Arrangement.spacedBy(PadSpacing)) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                PadGrid(items = pages[page], columns = COLUMNS) { note, padModifier ->
                    val name = shortDrumName(note)?.let { stringResource(it) } ?: gmNames[note - GmPercussionNotes.first]
                    val hit = { player.noteOn(channel, note, FIXED_VELOCITY) }
                    PlayPad(
                        description = name,
                        onPress = { hit() },
                        onRelease = {},
                        onAccessibleTap = { hit() },
                        idleColor = MaterialTheme.colorScheme.primaryContainer,
                        pressedColor = MaterialTheme.colorScheme.tertiary,
                        modifier = padModifier,
                    ) { pressed ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = if (pressed) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.pad_page, pagerState.currentPage + 1, pages.size),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
