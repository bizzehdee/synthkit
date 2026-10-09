package com.bizzeh.synthkit.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.instruments.Family
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.instruments.InstrumentCatalogue
import com.bizzeh.synthkit.ui.MinTouchTarget

private const val RECENTS_SECTION = "recents"
private const val FAVOURITES_SECTION = "favourites"

@Composable
fun BrowserScreen(
    catalogue: InstrumentCatalogue,
    library: Library,
    onOpen: (Instrument) -> Unit,
    onToggleFavourite: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialFamily: Family = Family.KEYS,
) {
    var section by rememberSaveable { mutableStateOf(initialFamily.name) }
    var query by rememberSaveable { mutableStateOf("") }

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(modifier = Modifier.width(200.dp).fillMaxHeight()) {
            item {
                SectionItem(stringResource(R.string.browser_recents), section == RECENTS_SECTION) {
                    section = RECENTS_SECTION
                    query = ""
                }
            }
            item {
                SectionItem(stringResource(R.string.browser_favourites), section == FAVOURITES_SECTION) {
                    section = FAVOURITES_SECTION
                    query = ""
                }
            }
            items(Family.entries) { family ->
                SectionItem(stringResource(family.label), section == family.name) {
                    section = family.name
                    query = ""
                }
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.browser_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
            val (shown, emptyMessage) = when {
                query.isNotBlank() -> catalogue.search(query) to R.string.browser_no_results
                section == RECENTS_SECTION ->
                    library.recents.mapNotNull(catalogue::byId) to R.string.browser_no_recents
                section == FAVOURITES_SECTION ->
                    catalogue.instruments.filter { it.id in library.favourites } to R.string.browser_no_favourites
                else -> catalogue.inFamily(Family.valueOf(section)) to R.string.browser_no_results
            }
            if (shown.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
                    Text(stringResource(emptyMessage), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(shown, key = { it.id }) { instrument ->
                        InstrumentRow(
                            instrument = instrument,
                            favourite = instrument.id in library.favourites,
                            onOpen = { onOpen(instrument) },
                            onToggleFavourite = { onToggleFavourite(instrument.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.heightIn(min = MinTouchTarget),
    )
}

@Composable
private fun InstrumentRow(
    instrument: Instrument,
    favourite: Boolean,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(
            text = instrument.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
        val description = stringResource(
            if (favourite) R.string.favourite_remove else R.string.favourite_add,
            instrument.name,
        )
        IconButton(onClick = onToggleFavourite) {
            Icon(
                imageVector = if (favourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = description,
                tint = if (favourite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
