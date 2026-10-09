package com.bizzeh.synthkit.play

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.ui.MinTouchTarget

@Composable
fun HoldToggle(hold: Boolean, onHoldChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = hold,
        onClick = { onHoldChange(!hold) },
        label = { Text(stringResource(R.string.hold)) },
        modifier = modifier.heightIn(min = MinTouchTarget),
    )
}
