package com.bizzeh.synthkit.drums

import androidx.annotation.StringRes
import com.bizzeh.synthkit.R

data class DrumPad(@param:StringRes val label: Int, val note: Int)

/** First page order and GM percussion notes from docs/gm-layouts.md. */
val FirstPageDrumPads = listOf(
    DrumPad(R.string.drum_kick, 36),
    DrumPad(R.string.drum_snare, 38),
    DrumPad(R.string.drum_closed_hat, 42),
    DrumPad(R.string.drum_open_hat, 46),
    DrumPad(R.string.drum_low_tom, 45),
    DrumPad(R.string.drum_high_tom, 50),
    DrumPad(R.string.drum_crash, 49),
    DrumPad(R.string.drum_ride, 51),
)
