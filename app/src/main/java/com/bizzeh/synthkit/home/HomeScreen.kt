package com.bizzeh.synthkit.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.instruments.Family
import com.bizzeh.synthkit.ui.MinPlayableHeight
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.PanelIconButton
import com.bizzeh.synthkit.ui.studio.glow
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.Eyebrow
import com.bizzeh.synthkit.ui.theme.StudioTheme
import com.bizzeh.synthkit.ui.theme.familyColor

/** The four quick entries from docs/gm-layouts.md, in order. */
val QuickEntryFamilies = listOf(Family.KEYS, Family.GUITAR_BASS, Family.DRUMS_PERCUSSION, Family.SYNTH)

/** One lit tile per quick-entry family, then Browse. [quickName] names what each tile opens. */
@Composable
fun HomeScreen(
    title: String,
    onOpenFamily: (Family) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    quickName: (Family) -> String? = { null },
) {
    val p = StudioTheme.palette
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onBack?.let { PanelIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), it) }
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = p.text)
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickEntryFamilies.forEach { family ->
                val label = stringResource(family.label)
                val description = stringResource(R.string.home_open_family, label)
                FamilyTile(
                    label = label,
                    detail = quickName(family),
                    color = familyColor(family),
                    onClick = { onOpenFamily(family) },
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description },
                )
            }
            BrowseTile(onClick = onBrowse, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FamilyTile(label: String, detail: String?, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Box(
        modifier = modifier
            .fillMaxHeight()
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinPlayableHeight)
            .raised(p, 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.offset(x = 16.dp, y = 16.dp).size(width = 28.dp, height = 5.dp).glow(color, 3.dp, spread = 5.dp).background(color, RoundedCornerShape(3.dp)))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            detail?.let { Text(it.uppercase(), style = Eyebrow, color = p.muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Text(label, style = MaterialTheme.typography.titleLarge, color = p.text)
        }
    }
}

@Composable
private fun BrowseTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinPlayableHeight)
            .raised(p, 16.dp, fill = p.panel, edge = p.amber)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = p.amber, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.home_browse), style = MaterialTheme.typography.titleLarge, color = p.text, modifier = Modifier.padding(top = 8.dp))
    }
}
