package com.bizzeh.synthkit.chords

import org.junit.Assert.assertEquals
import org.junit.Test

class HarmonyTest {
    @Test
    fun cMajorHasTheStandardDiatonicTriads() {
        val triads = Harmony.triads(key = 0, mode = Mode.MAJOR)

        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11), triads.map { it.root })
        assertEquals(
            listOf("I", "ii", "iii", "IV", "V", "vi", "vii°"),
            triads.map { it.numeral },
        )
    }

    @Test
    fun aMinorHasTheNaturalMinorTriads() {
        val triads = Harmony.triads(key = 9, mode = Mode.MINOR)

        assertEquals(listOf(9, 11, 0, 2, 4, 5, 7), triads.map { it.root })
        assertEquals(listOf("i", "ii°", "III", "iv", "v", "VI", "VII"), triads.map { it.numeral })
    }

    @Test
    fun triadPitchClassesFollowTheQuality() {
        val (major, minor) = Harmony.triads(0, Mode.MAJOR).let { it[0] to it[1] }
        val diminished = Harmony.triads(0, Mode.MAJOR)[6]

        assertEquals(setOf(0, 4, 7), major.pitchClasses)
        assertEquals(setOf(2, 5, 9), minor.pitchClasses)
        assertEquals(setOf(11, 2, 5), diminished.pitchClasses)
    }

    @Test
    fun keysWrapAroundTheOctave() {
        assertEquals(listOf(11, 1, 3, 4, 6, 8, 10), Harmony.triads(11, Mode.MAJOR).map { it.root })
    }

    @Test
    fun guitarVoicingTakesTheNearestChordToneAboveEachOpenString() {
        val cMajor = Harmony.triads(0, Mode.MAJOR)[0]

        // E2 E, A2 -> C3, D3 -> E3, G3 G, B3 -> C4, E4 E
        assertEquals(listOf(40, 48, 52, 55, 60, 64), Harmony.guitarVoicing(cMajor))
    }

    @Test
    fun everyVoicedNoteIsAChordToneWithinAnOctaveOfItsString() {
        listOf(Mode.MAJOR, Mode.MINOR).forEach { mode ->
            (0 until 12).forEach { key ->
                Harmony.triads(key, mode).forEach { triad ->
                    Harmony.guitarVoicing(triad).zip(Harmony.GUITAR_STRINGS).forEach { (note, open) ->
                        assert(note % 12 in triad.pitchClasses && note - open in 0..11)
                    }
                }
            }
        }
    }

    @Test
    fun bassRootSitsInTheLowestOctaveAndShifts() {
        val triads = Harmony.triads(0, Mode.MAJOR)

        assertEquals(36, Harmony.bassRoot(triads[0], 0))
        assertEquals(28, Harmony.bassRoot(triads[2], 0))
        assertEquals(39, Harmony.bassRoot(Harmony.triads(3, Mode.MAJOR)[0], 0))
        assertEquals(48, Harmony.bassRoot(triads[0], 1))
    }
}
