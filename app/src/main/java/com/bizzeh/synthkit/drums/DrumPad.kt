package com.bizzeh.synthkit.drums

import androidx.annotation.StringRes
import com.bizzeh.synthkit.R

/** GM percussion notes 35 to 81. */
val GmPercussionNotes = 35..81

/**
 * The 4 x 4 first page from docs/gm-layouts.md with short names; a 4 x 2 page
 * shows the first eight. GM has no rimshot, so that slot is Electric Snare.
 */
val FirstPageDrums: List<Pair<Int, Int>> = listOf(
    36 to R.string.drum_kick,
    38 to R.string.drum_snare,
    42 to R.string.drum_closed_hat,
    46 to R.string.drum_open_hat,
    45 to R.string.drum_low_tom,
    50 to R.string.drum_high_tom,
    49 to R.string.drum_crash,
    51 to R.string.drum_ride,
    39 to R.string.drum_clap,
    56 to R.string.drum_cowbell,
    54 to R.string.drum_tambourine,
    40 to R.string.drum_electric_snare,
    37 to R.string.drum_side_stick,
    43 to R.string.drum_floor_tom,
    44 to R.string.drum_pedal_hat,
    55 to R.string.drum_splash,
)

/** Every GM percussion note: the first page order, then the rest in note order. */
val DrumPadOrder: List<Int> = FirstPageDrums.map { it.first }.let { first ->
    first + GmPercussionNotes.filter { it !in first }
}

/** The short name for a first-page note, or null when the GM name is used. */
@StringRes
fun shortDrumName(note: Int): Int? = FirstPageDrums.firstOrNull { it.first == note }?.second
