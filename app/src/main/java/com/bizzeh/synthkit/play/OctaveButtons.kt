package com.bizzeh.synthkit.play

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bizzeh.synthkit.R

/** Octave down and up buttons around a label for the current position. */
@Composable
fun OctaveButtons(
    label: String,
    canGoDown: Boolean,
    canGoUp: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = onDown, enabled = canGoDown) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.octave_down))
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onUp, enabled = canGoUp) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.octave_up))
        }
    }
}
