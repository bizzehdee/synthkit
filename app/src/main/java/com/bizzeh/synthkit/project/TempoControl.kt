package com.bizzeh.synthkit.project

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bizzeh.synthkit.R
import kotlin.math.roundToInt

/** Slower and faster buttons; tapping the value opens a slider over the whole range. */
@Composable
fun TempoControl(bpm: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = { onChange(bpm - 1) }, enabled = bpm > Project.MIN_TEMPO) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.tempo_down))
        }
        TextButton(onClick = { editing = true }) { Text(stringResource(R.string.tempo_value, bpm)) }
        IconButton(onClick = { onChange(bpm + 1) }, enabled = bpm < Project.MAX_TEMPO) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.tempo_up))
        }
    }
    if (editing) {
        var value by remember { mutableFloatStateOf(bpm.toFloat()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.tempo_value, value.roundToInt())) },
            text = {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = Project.MIN_TEMPO.toFloat()..Project.MAX_TEMPO.toFloat(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onChange(value.roundToInt())
                    editing = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
