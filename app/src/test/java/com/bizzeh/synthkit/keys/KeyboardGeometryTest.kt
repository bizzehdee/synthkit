package com.bizzeh.synthkit.keys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardGeometryTest {
    private val c4 = KeyboardGeometry.whiteIndex(60)
    private val geometry = KeyboardGeometry(
        firstWhite = c4,
        whiteCount = 8,
        whiteWidth = 100f,
        blackWidth = 60f,
        blackHeight = 200f,
    )

    @Test
    fun whiteKeysRunUpTheCMajorScale() {
        assertEquals(listOf(60, 62, 64, 65, 67, 69, 71, 72), geometry.whiteKeys.map { it.note })
    }

    @Test
    fun blackKeysSitBetweenTheRightWhiteKeys() {
        assertEquals(listOf(61, 63, 66, 68, 70), geometry.blackKeys.map { it.note })
        assertEquals(70f, geometry.blackKeys.first().left)
    }

    @Test
    fun upperAreaOfABlackKeyPlaysTheBlackKey() {
        assertEquals(61, geometry.noteAt(100f, 50f))
    }

    @Test
    fun lowerAreaUnderABlackKeyPlaysTheWhiteKey() {
        assertEquals(62, geometry.noteAt(100f, 250f))
    }

    @Test
    fun upperAreaBetweenBlackKeysPlaysTheWhiteKey() {
        assertEquals(64, geometry.noteAt(250f, 50f))
    }

    @Test
    fun pointsOutsideTheKeyboardPlayNothing() {
        assertNull(geometry.noteAt(-1f, 10f))
        assertNull(geometry.noteAt(800f, 10f))
    }

    @Test
    fun noBlackKeyHangsOffTheRightEdge() {
        val endsOnF = KeyboardGeometry(c4, 4, 100f, 60f, 200f)

        assertEquals(listOf(61, 63), endsOnF.blackKeys.map { it.note })
    }

    @Test
    fun whiteIndexAndWhiteNoteAreInverse() {
        listOf(21, 24, 60, 71, 108).forEach { note ->
            assertEquals(note, KeyboardGeometry.whiteNote(KeyboardGeometry.whiteIndex(note)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun blackNoteHasNoWhiteIndex() {
        KeyboardGeometry.whiteIndex(61)
    }

    @Test
    fun windowIsKeptInsideThePianoRange() {
        assertEquals(KeyboardGeometry.LOWEST_WHITE, KeyboardGeometry.clampFirstWhite(0, 13))
        assertEquals(KeyboardGeometry.HIGHEST_WHITE - 12, KeyboardGeometry.clampFirstWhite(1000, 13))
        assertEquals(c4, KeyboardGeometry.clampFirstWhite(c4, 13))
    }

    @Test
    fun highestKeyIsC8WithNoSharp() {
        val top = KeyboardGeometry(KeyboardGeometry.HIGHEST_WHITE - 1, 2, 100f, 60f, 200f)

        assertEquals(listOf(107, 108), top.whiteKeys.map { it.note })
        assertEquals(emptyList<Int>(), top.blackKeys.map { it.note })
    }
}
