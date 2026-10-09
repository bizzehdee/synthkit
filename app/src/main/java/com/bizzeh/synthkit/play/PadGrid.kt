package com.bizzeh.synthkit.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.ui.MinPlayableHeight

val PadSpacing = 8.dp

/** Four rows when four pads of the minimum height fit, else two: larger screens show more pads, never smaller ones. */
fun padRows(height: Dp): Int = if (height >= MinPlayableHeight * 4 + PadSpacing * 3) 4 else 2

/** [items] laid out in [columns] columns, top row first; short rows are padded. */
@Composable
fun <T> PadGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    pad: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PadSpacing)) {
        items.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PadSpacing),
            ) {
                row.forEach { item -> pad(item, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
