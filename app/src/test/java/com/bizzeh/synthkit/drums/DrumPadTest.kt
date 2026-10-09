package com.bizzeh.synthkit.drums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DrumPadTest {
    @Test
    fun firstPageFollowsTheAgreedOrderOfGmPercussionNotes() {
        // kick, snare, closed hat, open hat, low tom, high tom, crash, ride
        assertEquals(listOf(36, 38, 42, 46, 45, 50, 49, 51), DrumPadOrder.take(8))
        // clap, cowbell, tambourine, electric snare, side stick, floor tom, pedal hat, splash
        assertEquals(listOf(39, 56, 54, 40, 37, 43, 44, 55), DrumPadOrder.subList(8, 16))
    }

    @Test
    fun everyGmPercussionNoteAppearsExactlyOnce() {
        assertEquals(GmPercussionNotes.toList(), DrumPadOrder.sorted())
    }

    @Test
    fun laterPagesRunInNoteOrder() {
        val rest = DrumPadOrder.drop(16)

        assertEquals(rest.sorted(), rest)
        assertEquals(35, rest.first())
    }

    @Test
    fun onlyFirstPageNotesHaveShortNames() {
        assertEquals(com.bizzeh.synthkit.R.string.drum_floor_tom, shortDrumName(43))
        assertNull(shortDrumName(41))
    }
}
