package com.bizzeh.synthkit.drums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.NotePlayer
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.ui.MinTouchTarget

// Kits in a 3-column grid: one kit per row would scroll on a landscape phone.
private const val KIT_COLUMNS = 3

@Composable
fun DrumKitLayout(
    player: NotePlayer,
    channel: Int,
    kit: Instrument,
    kits: List<Instrument>,
    onKitChange: (Instrument) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KitPicker(kit = kit, kits = kits, onKitChange = onKitChange)
        DrumKitPads(player = player, channel = channel, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KitPicker(kit: Instrument, kits: List<Instrument>, onKitChange: (Instrument) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.drum_kit, kit.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            kits.chunked(KIT_COLUMNS).forEach { row ->
                Row {
                    row.forEach { choice ->
                        TextButton(
                            onClick = {
                                open = false
                                if (choice.id != kit.id) onKitChange(choice)
                            },
                            modifier = Modifier.sizeIn(minWidth = 120.dp, minHeight = MinTouchTarget),
                        ) {
                            Text(choice.name)
                        }
                    }
                }
            }
        }
    }
}
