package com.bizzeh.synthkit.looper

import org.junit.Assert.assertEquals
import org.junit.Test

class LoopMathTest {
    @Test
    fun firstTapSnapsToTheNearestBeat() {
        assertEquals(480, LoopMath.snapToBeat(470))
        assertEquals(480, LoopMath.snapToBeat(700))
        assertEquals(960, LoopMath.snapToBeat(721))
        assertEquals(0, LoopMath.snapToBeat(-100))
    }

    @Test
    fun lengthRoundsToTheNearestWholeBarFromOneToEight() {
        assertEquals(1, LoopMath.roundedBars(100))
        assertEquals(1, LoopMath.roundedBars(1920 + 900))
        assertEquals(2, LoopMath.roundedBars(1920 + 1000))
        assertEquals(8, LoopMath.roundedBars(1920L * 20))
    }

    @Test
    fun ticksWrapIntoTheLoop() {
        assertEquals(10, LoopMath.wrap(490, 480, 1920))
        assertEquals(1910, LoopMath.wrap(470, 480, 1920))
        assertEquals(10, LoopMath.wrap(480 + 1920 * 3 + 10, 480, 1920))
    }

    @Test
    fun quantiseMovesToTheNearestGridLineAndWraps() {
        assertEquals(130, LoopMath.quantise(130, 0, 1920))
        assertEquals(120, LoopMath.quantise(130, 120, 1920))
        assertEquals(240, LoopMath.quantise(130, 240, 1920))
        assertEquals(0, LoopMath.quantise(1900, 120, 1920))
    }
}
