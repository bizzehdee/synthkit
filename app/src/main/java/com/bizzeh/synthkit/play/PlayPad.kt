package com.bizzeh.synthkit.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

/**
 * A playable pad. Every finger that lands on it calls [onPress] at once, and
 * [onRelease] when that finger lifts, so several fingers and pads work together.
 * TalkBack users double-tap, which calls [onAccessibleTap].
 */
@Composable
fun PlayPad(
    description: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onAccessibleTap: () -> Unit,
    idleColor: Color,
    pressedColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnAccessibleTap by rememberUpdatedState(onAccessibleTap)
    var fingers by remember { mutableIntStateOf(0) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = MinPlayableWidth, minHeight = MinPlayableHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(if (fingers > 0) pressedColor else idleColor)
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
        content(fingers > 0)
    }
}
