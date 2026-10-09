package com.bizzeh.synthkit.play

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.StudioTheme

@Composable
fun LatencyWarningBanner(warning: LatencyWarning, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().raised(p, 12.dp, fill = p.panel, edge = p.amber).padding(start = 14.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = p.amber)
        Text(
            text = stringResource(
                when (warning) {
                    LatencyWarning.BLUETOOTH -> R.string.warning_bluetooth
                    LatencyWarning.NOT_LOW_LATENCY -> R.string.warning_not_low_latency
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = p.text,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
        )
        TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.dismiss), color = p.amber)
        }
    }
}
