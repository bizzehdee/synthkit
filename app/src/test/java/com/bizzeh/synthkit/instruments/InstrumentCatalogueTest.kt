package com.bizzeh.synthkit.instruments

import com.bizzeh.synthkit.audio.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentCatalogueTest {
    private val gmNames = (0 until 128).map { "Program $it" }
    private val melodic = (0 until 128).map { Preset(0, it, "sf2 name $it") }
    private val kits = listOf(
        Preset(128, 0, "Standard 1"),
        Preset(128, 25, "808/909"),
        Preset(128, 56, "SFX"),
        Preset(128, 127, "CM-64/32L"),
        Preset(120, 0, "Standard 1 Kit"),
        Preset(8, 4, "Variation"),
    )
    private val catalogue = InstrumentCatalogue.build(melodic + kits, gmNames)

    // Layout table rows, 1-based in the doc, converted to 0-based programs here.
    @Test
    fun everyGmGroupGetsItsAgreedFamilyAndLayout() {
        val expected = mapOf(
            0 to (Family.KEYS to PlayLayout.Keys(false)),
            8 to (Family.KEYS to PlayLayout.Keys(false)),
            16 to (Family.KEYS to PlayLayout.Keys(false)),
            24 to (Family.GUITAR_BASS to PlayLayout.Chords(false)),
            32 to (Family.GUITAR_BASS to PlayLayout.Chords(true)),
            40 to (Family.STRINGS_ORCHESTRA to PlayLayout.Keys(false)),
            46 to (Family.STRINGS_ORCHESTRA to PlayLayout.Keys(false)),
            47 to (Family.DRUMS_PERCUSSION to PlayLayout.ChromaticPads),
            48 to (Family.STRINGS_ORCHESTRA to PlayLayout.Keys(true)),
            56 to (Family.BRASS_WINDS to PlayLayout.Keys(false)),
            64 to (Family.BRASS_WINDS to PlayLayout.Keys(false)),
            72 to (Family.BRASS_WINDS to PlayLayout.Keys(false)),
            80 to (Family.SYNTH to PlayLayout.Keys(false)),
            88 to (Family.SYNTH to PlayLayout.Keys(true)),
            96 to (Family.SYNTH to PlayLayout.ChromaticPads),
            104 to (Family.WORLD_MISC to PlayLayout.Chords(false)),
            108 to (Family.WORLD_MISC to PlayLayout.Keys(false)),
            112 to (Family.DRUMS_PERCUSSION to PlayLayout.ChromaticPads),
            120 to (Family.WORLD_MISC to PlayLayout.ChromaticPads),
            127 to (Family.WORLD_MISC to PlayLayout.ChromaticPads),
        )

        expected.forEach { (program, placement) ->
            assertEquals("program $program", placement, InstrumentCatalogue.gmPlacement(program))
        }
    }

    @Test
    fun groupBoundariesFallInTheRightGroup() {
        assertEquals(Family.KEYS, InstrumentCatalogue.gmPlacement(23).first)
        assertEquals(PlayLayout.Chords(false), InstrumentCatalogue.gmPlacement(31).second)
        assertEquals(PlayLayout.Chords(true), InstrumentCatalogue.gmPlacement(39).second)
        assertEquals(PlayLayout.Keys(true), InstrumentCatalogue.gmPlacement(55).second)
        assertEquals(Family.BRASS_WINDS, InstrumentCatalogue.gmPlacement(79).first)
        assertEquals(PlayLayout.Keys(false), InstrumentCatalogue.gmPlacement(87).second)
        assertEquals(PlayLayout.Keys(true), InstrumentCatalogue.gmPlacement(95).second)
        assertEquals(Family.SYNTH, InstrumentCatalogue.gmPlacement(103).first)
        assertEquals(PlayLayout.Chords(false), InstrumentCatalogue.gmPlacement(107).second)
        assertEquals(PlayLayout.Keys(false), InstrumentCatalogue.gmPlacement(111).second)
        assertEquals(Family.DRUMS_PERCUSSION, InstrumentCatalogue.gmPlacement(119).first)
    }

    @Test(expected = IllegalArgumentException::class)
    fun programOutsideGmIsRejected() {
        InstrumentCatalogue.gmPlacement(128)
    }

    @Test
    fun gmProgramsUseTheStandardNames() {
        assertEquals("Program 24", catalogue.byId("0:24")?.name)
    }

    @Test
    fun onlyGmCompatibleKitsFromBank128AreListedFirstInTheDrumTab() {
        val drums = catalogue.inFamily(Family.DRUMS_PERCUSSION)

        assertEquals(listOf("Standard 1", "808/909"), drums.take(2).map { it.name })
        assertTrue(drums.drop(2).all { it.bank == 0 })
        assertNull(catalogue.byId("128:56"))
        assertNull(catalogue.byId("128:127"))
        assertNull(catalogue.byId("120:0"))
        assertNull(catalogue.byId("8:4"))
    }

    @Test
    fun catalogueHolds128ProgramsAndTheKits() {
        assertEquals(130, catalogue.instruments.size)
    }

    @Test
    fun programsMissingFromTheSoundFontAreLeftOut() {
        val partial = InstrumentCatalogue.build(melodic.filter { it.program != 5 }, gmNames)

        assertNull(partial.byId("0:5"))
    }

    @Test
    fun quickEntriesOpenTheFirstInstrumentOfTheirTab() {
        assertEquals("0:0", catalogue.quickEntry(Family.KEYS)?.id)
        assertEquals("0:24", catalogue.quickEntry(Family.GUITAR_BASS)?.id)
        assertEquals("128:0", catalogue.quickEntry(Family.DRUMS_PERCUSSION)?.id)
        assertEquals("0:80", catalogue.quickEntry(Family.SYNTH)?.id)
    }

    @Test
    fun searchMatchesAnyPartOfTheNameIgnoringCase() {
        assertEquals(listOf("0:12", "0:120", "0:121", "0:122", "0:123", "0:124", "0:125", "0:126", "0:127"),
            catalogue.search(" program 12").map { it.id })
        assertEquals(listOf("128:25"), catalogue.search("909").map { it.id })
    }

    @Test
    fun blankSearchMatchesNothing() {
        assertEquals(emptyList<Instrument>(), catalogue.search("  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun wrongNumberOfGmNamesIsRejected() {
        InstrumentCatalogue.build(melodic, gmNames.take(10))
    }
}
