package com.bizzeh.synthkit.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.instruments.Family
import com.bizzeh.synthkit.ui.MinPlayableHeight
import com.bizzeh.synthkit.ui.MinTouchTarget

/** The four quick entries from docs/gm-layouts.md, in order. */
val QuickEntryFamilies = listOf(Family.KEYS, Family.GUITAR_BASS, Family.DRUMS_PERCUSSION, Family.SYNTH)

@Composable
fun HomeScreen(onOpenFamily: (Family) -> Unit, onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickEntryFamilies.forEach { family ->
                val label = stringResource(family.label)
                val description = stringResource(R.string.home_open_family, label)
                HomeCard(
                    label = label,
                    onClick = { onOpenFamily(family) },
                    highlighted = true,
                    modifier = Modifier.weight(1f).semantics { contentDescription = description },
                )
            }
            HomeCard(
                label = stringResource(R.string.home_browse),
                onClick = onBrowse,
                highlighted = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeCard(label: String, onClick: () -> Unit, highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = if (highlighted) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    Card(
        onClick = onClick,
        colors = colors,
        modifier = modifier.fillMaxHeight().sizeIn(minWidth = MinTouchTarget, minHeight = MinPlayableHeight),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
