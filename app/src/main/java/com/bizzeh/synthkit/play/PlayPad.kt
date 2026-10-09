package com.bizzeh.synthkit.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.ui.MinPlayableHeight
import com.bizzeh.synthkit.ui.MinPlayableWidth
import com.bizzeh.synthkit.ui.studio.glow
import com.bizzeh.synthkit.ui.studio.raised
import com.bizzeh.synthkit.ui.theme.StudioTheme

private val PadCorner = 14.dp

/**
 * A lit playable pad. Every finger that lands on it calls [onPress] at once,
 * and [onRelease] when that finger lifts, so several fingers and pads work
 * together. While held it lights in [color]. TalkBack users double-tap, which
 * calls [onAccessibleTap]. [content] sits at the bottom-left, like a printed label.
 */
@Composable
fun PlayPad(
    description: String,
    color: Color,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onAccessibleTap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    val p = StudioTheme.palette
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnAccessibleTap by rememberUpdatedState(onAccessibleTap)
    var fingers by remember { mutableIntStateOf(0) }
    val lit = fingers > 0

    Box(
        modifier = modifier
            .sizeIn(minWidth = MinPlayableWidth, minHeight = MinPlayableHeight)
            .glow(color, PadCorner, enabled = lit, spread = 12.dp)
            .raised(p, PadCorner, fill = if (lit) color.copy(alpha = 0.24f).compositeOver(p.raised) else p.raised, edge = if (lit) color else p.line)
            .clip(RoundedCornerShape(PadCorner))
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { change ->
                                if (change.changedToDownIgnoreConsumed()) {
                                    fingers++
                                    currentOnPress()
                                    change.consume()
                                } else if (change.changedToUpIgnoreConsumed() && fingers > 0) {
                                    fingers--
                                    currentOnRelease()
                                    change.consume()
                                }
                            }
                        }
                    }
                } finally {
                    repeat(fingers) { currentOnRelease() }
                    fingers = 0
                }
            }
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick(label = description) {
                    currentOnAccessibleTap()
                    true
                }
            },
    ) {
        Box(
            Modifier
                .offset(x = 14.dp, y = 12.dp)
                .size(width = 22.dp, height = 4.dp)
                .glow(color, 2.dp, enabled = lit, spread = 5.dp)
                .background(if (lit) color else p.off, RoundedCornerShape(2.dp)),
        )
        Box(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 10.dp, bottom = 12.dp)) {
            content(lit)
        }
    }
}
