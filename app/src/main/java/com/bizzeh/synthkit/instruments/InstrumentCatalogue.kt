package com.bizzeh.synthkit.instruments

import com.bizzeh.synthkit.audio.Preset

/** Every playable instrument: the 128 GM programs and the SoundFont's GM drum kits. */
class InstrumentCatalogue private constructor(val instruments: List<Instrument>) {

    fun byId(id: String): Instrument? = instruments.firstOrNull { it.id == id }

    fun inFamily(family: Family): List<Instrument> = instruments.filter { it.family == family }

    /** Case-insensitive match on any part of the name. A blank query matches nothing. */
    fun search(query: String): List<Instrument> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return instruments.filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    /** The instrument a home-screen quick entry opens. */
    fun quickEntry(family: Family): Instrument? = inFamily(family).firstOrNull()

    companion object {
        const val MELODIC_BANK = 0
        const val DRUM_KIT_BANK = 128

        // GS kits whose sounds do not follow the GM percussion map, so GM pad
        // labels would be wrong: SFX (56) and CM-64/32L (127).
        private val NON_GM_KITS = setOf(56, 127)

        /**
         * [gmProgramNames] holds the 128 standard GM names in program order. Kits
         * come from [presets] in bank 128, in program order, before the pitched
         * percussion programs of the same tab.
         */
        fun build(presets: List<Preset>, gmProgramNames: List<String>): InstrumentCatalogue {
            require(gmProgramNames.size == GM_PROGRAMS) { "Expected $GM_PROGRAMS GM names" }
            val kits = presets
                .filter { it.bank == DRUM_KIT_BANK && it.program !in NON_GM_KITS }
                .sortedBy { it.program }
                .map { Instrument(it.bank, it.program, it.name, Family.DRUMS_PERCUSSION, PlayLayout.DrumKit) }
            val available = presets.filter { it.bank == MELODIC_BANK }.map { it.program }.toSet()
            val programs = (0 until GM_PROGRAMS).filter { it in available }.map { program ->
                val (family, layout) = gmPlacement(program)
                Instrument(MELODIC_BANK, program, gmProgramNames[program], family, layout)
            }
            return InstrumentCatalogue(kits + programs)
        }

        private const val GM_PROGRAMS = 128
        private const val MIDDLE_C = 60

        // C3: timpani sound an octave below most pitched percussion.
        private const val TIMPANI_ROOT = 48

        /** Family tab and layout for a 0-based GM program, from docs/gm-layouts.md. */
        fun gmPlacement(program: Int): Pair<Family, PlayLayout> = when (program) {
            in 0..23 -> Family.KEYS to PlayLayout.Keys(holdByDefault = false)
            in 24..31 -> Family.GUITAR_BASS to PlayLayout.Chords(bassRoots = false)
            in 32..39 -> Family.GUITAR_BASS to PlayLayout.Chords(bassRoots = true)
            in 40..46 -> Family.STRINGS_ORCHESTRA to PlayLayout.Keys(holdByDefault = false)
            47 -> Family.DRUMS_PERCUSSION to PlayLayout.ChromaticPads(root = TIMPANI_ROOT)
            in 48..55 -> Family.STRINGS_ORCHESTRA to PlayLayout.Keys(holdByDefault = true)
            in 56..79 -> Family.BRASS_WINDS to PlayLayout.Keys(holdByDefault = false)
            in 80..87 -> Family.SYNTH to PlayLayout.Keys(holdByDefault = false)
            in 88..95 -> Family.SYNTH to PlayLayout.Keys(holdByDefault = true)
            in 96..103 -> Family.SYNTH to PlayLayout.ChromaticPads(root = MIDDLE_C)
            in 104..107 -> Family.WORLD_MISC to PlayLayout.Chords(bassRoots = false)
            in 108..111 -> Family.WORLD_MISC to PlayLayout.Keys(holdByDefault = false)
            in 112..119 -> Family.DRUMS_PERCUSSION to PlayLayout.ChromaticPads(root = MIDDLE_C)
            in 120..127 -> Family.WORLD_MISC to PlayLayout.ChromaticPads(root = MIDDLE_C)
            else -> throw IllegalArgumentException("Not a GM program: $program")
        }
    }
}
