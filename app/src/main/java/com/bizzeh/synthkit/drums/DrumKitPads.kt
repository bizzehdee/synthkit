package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.play.FIXED_VELOCITY
import com.bizzeh.synthkit.play.PadGrid
import com.bizzeh.synthkit.play.PadSpacing
import com.bizzeh.synthkit.play.PlayPad
import com.bizzeh.synthkit.play.padRows
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.PageDots
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.theme.PanelLabel
import com.bizzeh.synthkit.ui.theme.StudioTheme
import kotlinx.coroutines.launch

private const val COLUMNS = 4
private val PagerBarHeight = MinTouchTarget

/**
 * Every GM percussion note on one-shot lit pads, the agreed first page first
 * and further pages reached with the arrows beside the page dots.
 */
@Composable
fun DrumKitPads(player: NotePlayer, channel: Int, color: Color, modifier: Modifier = Modifier) {
    val gmNames = stringArrayResource(R.array.gm_percussion_names)
    val p = StudioTheme.palette
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageSize = COLUMNS * padRows(maxHeight - PagerBarHeight - PadSpacing)
        val pages = DrumPadOrder.chunked(pageSize)
        val pagerState = rememberPagerState { pages.size }
        Column(verticalArrangement = Arrangement.spacedBy(PadSpacing)) {
            // Swiping is off: drumming fingers slide, and a slide must never move the pads.
            HorizontalPager(state = pagerState, userScrollEnabled = false, modifier = Modifier.weight(1f)) { page ->
                PadGrid(items = pages[page], columns = COLUMNS) { note, padModifier ->
                    val name = shortDrumName(note)?.let { stringResource(it) } ?: gmNames[note - GmPercussionNotes.first]
                    val hit = { player.noteOn(channel, note, FIXED_VELOCITY) }
                    PlayPad(
                        description = name,
                        color = color,
                        onPress = { hit() },
                        onRelease = {},
                        onAccessibleTap = { hit() },
                        modifier = padModifier,
                    ) { lit ->
                        Text(
                            text = name.uppercase(),
                            style = PanelLabel,
                            color = if (lit) p.text else p.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            val scope = rememberCoroutineScope()
            val current = pagerState.currentPage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PadSpacing, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth().height(PagerBarHeight),
            ) {
                PanelIconButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    stringResource(R.string.pad_page_previous),
                    { scope.launch { pagerState.animateScrollToPage(current - 1) } },
                    enabled = current > 0,
                    framed = false,
                )
                PageDots(
                    count = pages.size,
                    current = current,
                    contentDescription = stringResource(R.string.pad_page, current + 1, pages.size),
                )
                PanelIconButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    stringResource(R.string.pad_page_next),
                    { scope.launch { pagerState.animateScrollToPage(current + 1) } },
                    enabled = current < pages.size - 1,
                    framed = false,
                )
            }
        }
    }
}
