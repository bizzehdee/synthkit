package com.bizzeh.synthkit.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.ui.theme.StudioPalette

private const val GLOW_RINGS = 4

/**
 * A soft light around a shape, drawn as fading rings outside it. Blur filters
 * and coloured shadows need Android 9 for hardware drawing; rings look the
 * same from Android 8 up.
 */
fun Modifier.glow(color: Color, corner: Dp, enabled: Boolean = true, spread: Dp = 10.dp): Modifier =
    if (!enabled) this else drawBehind {
        val step = spread.toPx() / GLOW_RINGS
        for (ring in GLOW_RINGS downTo 1) {
            val grow = step * ring
            drawRoundRect(
                color = color.copy(alpha = 0.09f * (GLOW_RINGS + 1 - ring)),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2, size.height + grow * 2),
                cornerRadius = CornerRadius(corner.toPx() + grow),
            )
        }
    }

/** A raised panel: fill, hairline border and a highlight along its top edge. */
fun Modifier.raised(palette: StudioPalette, corner: Dp, fill: Color = palette.raised, edge: Color = palette.line): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
        .background(fill, shape)
        .border(1.dp, edge, shape)
        .drawBehind {
            drawRoundRect(
                color = palette.highlight,
                topLeft = Offset(corner.toPx() / 2, 1.dp.toPx()),
                size = Size(size.width - corner.toPx(), 1.dp.toPx()),
            )
        }
}
