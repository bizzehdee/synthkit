package com.bizzeh.synthkit.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.looper.LoopMath
import com.bizzeh.synthkit.ui.theme.StudioTheme

/**
 * A track's notes across the loop as small blocks, higher notes higher, with
 * bar lines and an optional playhead ([playhead] is 0 to 1 across the loop).
 */
@Composable
fun NotePreview(
    track: Track,
    loopBars: Int,
    color: Color,
    modifier: Modifier = Modifier,
    playhead: Float? = null,
    barLines: Boolean = true,
) {
    val p = StudioTheme.palette
    val loopTicks = loopBars * TICKS_PER_BAR
    Canvas(modifier.background(p.text.copy(alpha = 0.05f), RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))) {
        if (loopTicks == 0) return@Canvas
        if (barLines) {
            for (bar in 1 until loopBars) {
                val x = size.width * bar / loopBars
                drawLine(p.line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
        }
        val keys = track.notes.map { it.key }
        val low = keys.minOrNull() ?: 0
        val span = ((keys.maxOrNull() ?: 0) - low).coerceAtLeast(1)
        val noteHeight = (size.height * 0.3f).coerceAtLeast(3.dp.toPx())
        track.notes.forEach { note ->
            val start = LoopMath.quantise(note.tick, track.quantise.gridTicks, loopTicks).toFloat() / loopTicks
            val width = (note.lengthTicks.toFloat() / loopTicks).coerceAtLeast(0.006f)
            val y = (size.height - noteHeight) * (1f - (note.key - low).toFloat() / span)
            drawRoundRect(
                color.copy(alpha = if (track.muted) 0.35f else 0.9f),
                Offset(size.width * start, y),
                Size((size.width * width).coerceAtMost(size.width * (1f - start)), noteHeight),
                CornerRadius(2.dp.toPx()),
            )
        }
        playhead?.let { drawLine(p.amber, Offset(size.width * it, 0f), Offset(size.width * it, size.height), 2.dp.toPx()) }
    }
}
