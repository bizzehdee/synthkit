package com.bizzeh.synthkit.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.theme.Eyebrow
import com.bizzeh.synthkit.ui.theme.LcdSmall
import com.bizzeh.synthkit.ui.theme.LcdText
import com.bizzeh.synthkit.ui.theme.PanelLabel
import com.bizzeh.synthkit.ui.theme.StudioTheme

private val Corner = 12.dp

/** A square panel button holding an icon. */
@Composable
fun PanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    framed: Boolean = true,
) {
    val p = StudioTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .then(if (framed) Modifier.raised(p, Corner, fill = p.panel) else Modifier)
            .clip(RoundedCornerShape(Corner))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) p.text else p.off)
    }
}

/** The instrument chip: family light, small family name over the instrument, and a caret. */
@Composable
fun FamilyChip(color: Color, family: String, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .raised(p, Corner, fill = p.panel)
            .clip(RoundedCornerShape(Corner))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Box(Modifier.size(10.dp).glow(color, 5.dp, spread = 5.dp).background(color, CircleShape))
        Column(modifier = Modifier.widthIn(max = 260.dp)) {
            Text(family.uppercase(), style = Eyebrow, color = p.muted, maxLines = 1)
            Text(title, style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = p.muted)
    }
}

/** The dark display panel; it looks the same in both themes, like hardware. */
@Composable
fun LcdDisplay(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .height(MinTouchTarget)
            .background(p.lcd, RoundedCornerShape(10.dp))
            .border(1.dp, p.lcdLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        content = content,
    )
}

/** Four beat lights; [lit] is the beat to light, from 0, or null for none. */
@Composable
fun BeatLights(lit: Int?, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        repeat(4) { beat ->
            val on = beat == lit
            Box(
                Modifier
                    .size(8.dp)
                    .glow(p.amber, 2.dp, enabled = on, spread = 4.dp)
                    .background(if (on) p.amber else Color(0xFF3A3550), RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** A value on the display, with an optional small unit after it. */
@Composable
fun LcdValue(text: String, unit: String? = null) {
    val p = StudioTheme.palette
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text, style = LcdText, color = p.amber, maxLines = 1)
        unit?.let { Text(it, style = LcdSmall, color = p.amberDim, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)) }
    }
}

@Composable
fun RecTag(label: String) {
    val p = StudioTheme.palette
    Text(
        label,
        style = LcdSmall,
        color = p.onLit,
        modifier = Modifier.background(p.record, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** The round Record button; it glows red while a take records or waits for its first note. */
@Composable
fun RoundRecordButton(live: Boolean, enabled: Boolean, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .glow(p.record, MinTouchTarget / 2, enabled = live, spread = 10.dp)
            .background(if (live) Color(0xFF3A1218) else p.panel, CircleShape)
            .border(if (live) 2.dp else 1.dp, if (live) p.record else p.line, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(Modifier.size(18.dp).background(if (enabled) p.record else p.off, CircleShape))
    }
}

/** The round Play/Stop button: a triangle to play, a square to stop. */
@Composable
fun RoundPlayStopButton(playing: Boolean, enabled: Boolean, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    val glyph = if (enabled) p.text else p.off
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .background(p.panel, CircleShape)
            .border(1.dp, p.line, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.size(16.dp)) {
            if (playing) {
                drawRoundRect(glyph, size = Size(size.width * 0.9f, size.height * 0.9f), topLeft = Offset(size.width * 0.05f, size.height * 0.05f), cornerRadius = CornerRadius(3.dp.toPx()))
            } else {
                drawPath(Path().apply {
                    moveTo(size.width * 0.15f, 0f)
                    lineTo(size.width, size.height / 2)
                    lineTo(size.width * 0.15f, size.height)
                    close()
                }, glyph)
            }
        }
    }
}

/** A labelled switch with an indicator light, such as HOLD or CLICK. */
@Composable
fun LitToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit, color: Color, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .height(MinTouchTarget)
            .raised(p, Corner, fill = if (on) color.copy(alpha = 0.16f).compositeOver(p.panel) else p.panel, edge = if (on) color else p.line)
            .clip(RoundedCornerShape(Corner))
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp),
    ) {
        Box(Modifier.size(8.dp).glow(color, 4.dp, enabled = on, spread = 5.dp).background(if (on) color else p.off, CircleShape))
        Text(label.uppercase(), style = PanelLabel, color = p.text)
    }
}

/** A square on/off button such as Mute or Solo, filled with [onColor] when on. */
@Composable
fun SquareToggle(
    label: String,
    on: Boolean,
    onColor: Color,
    contentDescription: String,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = StudioTheme.palette
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .background(if (on) onColor else p.panel, RoundedCornerShape(10.dp))
            .border(1.dp, if (on) onColor else p.line, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(label, style = PanelLabel, color = if (on) p.onLit else p.muted)
    }
}

/**
 * A small horizontal fader from 0 to 1. Drag or tap to set it; TalkBack users
 * set it like a slider.
 */
@Composable
fun MiniFader(value: Float, onChange: (Float) -> Unit, color: Color, contentDescription: String, modifier: Modifier = Modifier, width: Dp = 104.dp) {
    val p = StudioTheme.palette
    val currentOnChange by rememberUpdatedState(onChange)
    BoxWithConstraints(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(width)
            .height(MinTouchTarget)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                stateDescription = "${(value * 100).toInt()}%"
                setProgress { target ->
                    currentOnChange(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { currentOnChange((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> currentOnChange((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        val knob = 14.dp
        Box(Modifier.width(maxWidth).height(4.dp).background(p.off, RoundedCornerShape(2.dp)))
        Box(Modifier.width(maxWidth * value).height(4.dp).background(color, RoundedCornerShape(2.dp)))
        Box(
            Modifier
                .offset(x = (maxWidth - knob) * value)
                .size(width = knob, height = 22.dp)
                .background(p.text, RoundedCornerShape(4.dp)),
        )
    }
}

/** Page position as dots, the current one drawn long. */
@Composable
fun PageDots(count: Int, current: Int, contentDescription: String, modifier: Modifier = Modifier) {
    val p = StudioTheme.palette
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        repeat(count) { page ->
            Box(
                Modifier
                    .size(width = if (page == current) 18.dp else 6.dp, height = 6.dp)
                    .background(if (page == current) p.text else p.off, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** A value between down and up buttons, such as the octave or the tempo. */
@Composable
fun Stepper(
    value: String,
    downDescription: String,
    upDescription: String,
    canGoDown: Boolean,
    canGoUp: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    onValueClick: (() -> Unit)? = null,
) {
    val p = StudioTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(MinTouchTarget).raised(p, Corner, fill = p.panel),
    ) {
        PanelIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, downDescription, onDown, enabled = canGoDown, framed = false)
        Text(
            value,
            style = LcdText.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
            color = p.text,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .then(if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier)
                .padding(horizontal = 4.dp),
        )
        PanelIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, upDescription, onUp, enabled = canGoUp, framed = false)
    }
}
