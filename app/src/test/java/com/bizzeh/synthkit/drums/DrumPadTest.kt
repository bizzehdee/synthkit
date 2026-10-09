package com.bizzeh.synthkit.drums

import org.junit.Assert.assertEquals
import org.junit.Test

class DrumPadTest {
    @Test
    fun firstPageFollowsTheAgreedOrderOfGmPercussionNotes() {
        // kick, snare, closed hat, open hat, low tom, high tom, crash, ride
        val expected = listOf(36, 38, 42, 46, 45, 50, 49, 51)

        assertEquals(expected, FirstPageDrumPads.map { it.note })
    }
}
