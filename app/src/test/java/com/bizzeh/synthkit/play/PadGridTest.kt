package com.bizzeh.synthkit.play

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PadGridTest {
    @Test
    fun fourRowsOnlyWhenFourMinimumHeightPadsFit() {
        assertEquals(2, padRows(311.dp))
        assertEquals(4, padRows(312.dp))
        assertEquals(2, padRows(200.dp))
    }
}
