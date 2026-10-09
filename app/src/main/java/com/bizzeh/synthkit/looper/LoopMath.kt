package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.TICKS_PER_BAR
import com.bizzeh.synthkit.project.TICKS_PER_BEAT
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object LoopMath {
    /** The click beat nearest [tick]; beats fall on multiples of 480 from when recording was armed. */
    fun snapToBeat(tick: Long): Long = (tick.toDouble() / TICKS_PER_BEAT).roundToLong() * TICKS_PER_BEAT

    /** Take length rounded to the nearest whole bar, at least 1 and at most 8. */
    fun roundedBars(lengthTicks: Long): Int =
        (lengthTicks.toDouble() / TICKS_PER_BAR).roundToInt().coerceIn(1, Project.MAX_LOOP_BARS)

    /** Position of an absolute clock tick inside a loop that started at [origin]. */
    fun wrap(tick: Long, origin: Long, loopTicks: Int): Int = Math.floorMod(tick - origin, loopTicks.toLong()).toInt()

    /** [tick] moved to the nearest grid line; 0 means no quantise. Raw ticks stay stored. */
    fun quantise(tick: Int, gridTicks: Int, loopTicks: Int): Int {
        if (gridTicks == 0) return tick
        return Math.floorMod((tick.toDouble() / gridTicks).roundToInt() * gridTicks, loopTicks)
    }
}
