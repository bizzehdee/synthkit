package com.bizzeh.synthkit.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.ui.MinTouchTarget
import com.bizzeh.synthkit.ui.theme.StudioTheme
import androidx.compose.ui.graphics.Color

private val LabelWidth = 96.dp
private val PlayheadWidth = 2.dp
private val HandleWidth = 4.dp
private const val STEPS_PER_BEAT = 4
private const val STEPS_PER_BAR = 16

/**
 * The step grid as one canvas, so a 128 x 128 grid costs no more than what is
 * visible. A tap on an empty cell or a note reports the cell; dragging a note
 * moves it; dragging empty space scrolls. A tap on a row name reports its key.
 * When [resizable], dragging the selected note's right edge sets its length.
 */
@Composable
fun StepGridCanvas(
    noteColor: Color,
    rows: List<Int>,
    rowLabel: (Int) -> String,
    columns: Int,
    notes: List<Note>,
    selected: Int?,
    initialRow: Int,
    description: String,
    onTap: (Cell) -> Unit,
    onMove: (Int, Cell) -> Unit,
    onRowTap: (Int) -> Unit,
    rowsDescription: String,
    resizable: Boolean,
    onResize: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    playhead: (() -> Int?)? = null,
) {
    val cellPx = with(LocalDensity.current) { MinTouchTarget.toPx() }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    var scrollX by remember { mutableFloatStateOf(0f) }
    var scrollY by remember { mutableFloatStateOf(initialRow * cellPx) }
    var dragTarget by remember { mutableStateOf<Pair<Int, Cell>?>(null) }
    var resizeTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val currentNotes by rememberUpdatedState(notes)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnRowTap by rememberUpdatedState(onRowTap)
    val currentOnResize by rememberUpdatedState(onResize)
    val currentSelected by rememberUpdatedState(selected)
    val p = StudioTheme.palette
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = p.muted)
    val measurer = rememberTextMeasurer()
    // Ticks every frame only while a loop plays; reading it in the draw block redraws without recomposing.
    val frame by produceState(0L, playhead != null) {
        if (playhead != null) while (true) withFrameNanos { value = it }
    }
    val lineWidth = with(LocalDensity.current) { PlayheadWidth.toPx() }
    val handleWidth = with(LocalDensity.current) { HandleWidth.toPx() }

    Row(modifier = modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .width(LabelWidth)
                .fillMaxHeight()
                .semantics { contentDescription = rowsDescription }
                .pointerInput(rows) {
                    detectTapGestures { position ->
                        val row = ((position.y + scrollY) / cellPx).toInt()
                        rows.getOrNull(row)?.let(currentOnRowTap)
                    }
                },
        ) {
            val first = (scrollY / cellPx).toInt().coerceAtLeast(0)
            for (row in first until rows.size) {
                val top = row * cellPx - scrollY
                if (top > size.height) break
                drawText(measurer, rowLabel(rows[row]), Offset(8f, top + cellPx / 3), labelStyle)
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(rows, columns, resizable) {
                    fun clampScroll() {
                        scrollX = scrollX.coerceIn(0f, (columns * cellPx - size.width).coerceAtLeast(0f))
                        scrollY = scrollY.coerceIn(0f, (rows.size * cellPx - size.height).coerceAtLeast(0f))
                    }
                    fun cellAt(position: Offset): Cell? {
                        val column = ((position.x + scrollX) / cellPx).toInt()
                        val row = ((position.y + scrollY) / cellPx).toInt()
                        return if (column in 0 until columns && row in rows.indices) Cell(column, rows[row]) else null
                    }
                    // The selected note's right edge, give or take a third of a cell, is its length handle.
                    fun handleAt(position: Offset): Int? {
                        val index = currentSelected?.takeIf { resizable } ?: return null
                        val note = currentNotes.getOrNull(index) ?: return null
                        val cell = StepGrid.cellOf(note, columns)
                        val edge = (cell.column + StepGrid.stepsOf(note, columns)) * cellPx - scrollX
                        val row = rows.indexOf(cell.key)
                        val onRow = ((position.y + scrollY) / cellPx).toInt() == row
                        return index.takeIf { onRow && position.x in edge - cellPx / 3..edge + cellPx / 3 }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val handle = handleAt(down.position)
                        val start = cellAt(down.position) ?: return@awaitEachGesture
                        val note = if (handle == null) StepGrid.noteAt(currentNotes, start, columns) else null
                        val grabOffset = note?.let { start.column - StepGrid.cellOf(currentNotes[it], columns).column } ?: 0
                        var travelled = 0f
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            travelled += delta.getDistance()
                            if (travelled > touchSlop) {
                                if (handle != null) {
                                    val first = StepGrid.cellOf(currentNotes[handle], columns).column
                                    val column = ((change.position.x + scrollX) / cellPx).toInt()
                                    resizeTarget = handle to (column - first + 1).coerceIn(1, columns - first)
                                } else if (note != null) {
                                    cellAt(change.position)?.let {
                                        dragTarget = note to it.copy(column = (it.column - grabOffset).coerceIn(0, columns - 1))
                                    }
                                } else {
                                    scrollX -= delta.x
                                    scrollY -= delta.y
                                    clampScroll()
                                }
                                change.consume()
                            }
                        }
                        val target = dragTarget
                        val resized = resizeTarget
                        dragTarget = null
                        resizeTarget = null
                        when {
                            travelled <= touchSlop -> currentOnTap(start)
                            resized != null -> currentOnResize(resized.first, resized.second)
                            note != null && target != null -> currentOnMove(target.first, target.second)
                        }
                    }
                },
        ) {
            val firstRow = (scrollY / cellPx).toInt().coerceAtLeast(0)
            val firstColumn = (scrollX / cellPx).toInt().coerceAtLeast(0)
            for (row in firstRow until rows.size) {
                val top = row * cellPx - scrollY
                if (top > size.height) break
                if (row % 2 == 0) drawRect(p.panel, Offset(0f, top), Size(size.width, cellPx))
            }
            for (column in firstColumn..columns) {
                val x = column * cellPx - scrollX
                if (x > size.width) break
                val width = when {
                    column % STEPS_PER_BAR == 0 -> 3f
                    column % STEPS_PER_BEAT == 0 -> 2f
                    else -> 1f
                }
                drawLine(p.line.copy(alpha = if (width > 1f) 1f else 0.5f), Offset(x, 0f), Offset(x, size.height), width)
            }
            currentNotes.forEachIndexed { index, note ->
                val dragging = dragTarget?.takeIf { it.first == index }?.second
                val cell = dragging ?: StepGrid.cellOf(note, columns)
                val row = rows.indexOf(cell.key)
                if (row < 0) return@forEachIndexed
                val steps = resizeTarget?.takeIf { it.first == index }?.second
                    ?: StepGrid.stepsOf(note, columns).coerceAtMost(columns - cell.column)
                val origin = Offset(cell.column * cellPx - scrollX + 3f, row * cellPx - scrollY + 3f)
                val noteSize = Size(steps * cellPx - 6f, cellPx - 6f)
                val color = if (index == selected || dragging != null) p.amber else noteColor
                drawRoundRect(
                    color.copy(alpha = 0.35f + 0.65f * note.velocity / 127f),
                    origin,
                    noteSize,
                    CornerRadius(8f, 8f),
                )
                if (resizable && index == selected) {
                    val grip = Size(handleWidth, noteSize.height / 2)
                    drawRoundRect(
                        p.onLit,
                        Offset(origin.x + noteSize.width - handleWidth * 2, origin.y + noteSize.height / 4),
                        grip,
                        CornerRadius(handleWidth / 2, handleWidth / 2),
                    )
                }
            }
            frame.let { _ ->
                val tick = playhead?.invoke() ?: return@let
                val x = tick.toFloat() / StepGrid.STEP_TICKS * cellPx - scrollX
                if (x in 0f..size.width) drawLine(p.amber, Offset(x, 0f), Offset(x, size.height), lineWidth)
            }
        }
    }
}
