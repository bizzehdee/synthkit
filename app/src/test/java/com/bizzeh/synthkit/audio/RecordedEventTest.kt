package com.bizzeh.synthkit.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedEventTest {
    @Test
    fun foursBecomeEvents() {
        val events = RecordedEvent.fromNative(doubleArrayOf(472.0, 3.0, 64.0, 0.5, 960.0, 255.0, 0.0, 0.0))

        assertEquals(RecordedEvent(472, 3, 64, 0.5f), events[0])
        assertTrue(events[1].isStopMarker)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteEventIsRejected() {
        RecordedEvent.fromNative(doubleArrayOf(1.0, 2.0))
    }
}
