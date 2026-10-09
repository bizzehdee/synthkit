package com.bizzeh.synthkit.instruments

import androidx.annotation.StringRes
import com.bizzeh.synthkit.R

enum class Family(@param:StringRes val label: Int) {
    KEYS(R.string.family_keys),
    GUITAR_BASS(R.string.family_guitar_bass),
    DRUMS_PERCUSSION(R.string.family_drums_percussion),
    STRINGS_ORCHESTRA(R.string.family_strings_orchestra),
    BRASS_WINDS(R.string.family_brass_winds),
    SYNTH(R.string.family_synth),
    WORLD_MISC(R.string.family_world_misc),
}

/** How the instrument is played on screen. See docs/gm-layouts.md. */
sealed interface PlayLayout {
    data class Keys(val holdByDefault: Boolean) : PlayLayout

    /** [bassRoots]: pads play the chord root in a low octave instead of the chord. */
    data class Chords(val bassRoots: Boolean) : PlayLayout

    /** One-shot pads on the GM percussion map. */
    data object DrumKit : PlayLayout

    /** Chromatic pads from a root note, sounding while held. */
    data object ChromaticPads : PlayLayout
}

data class Instrument(
    val bank: Int,
    val program: Int,
    val name: String,
    val family: Family,
    val layout: PlayLayout,
) {
    /** Stable key for favourites and recents. */
    val id: String get() = "$bank:$program"
}
