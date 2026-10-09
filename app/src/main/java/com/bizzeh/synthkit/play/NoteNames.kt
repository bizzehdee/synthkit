package com.bizzeh.synthkit.play

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.bizzeh.synthkit.R

/** Scientific pitch name, so MIDI 60 is "C 4" and 61 is "C sharp 4". */
@Composable
fun noteName(note: Int): String {
    val pitchClasses = stringArrayResource(R.array.pitch_class_names)
    return stringResource(R.string.note_name, pitchClasses[note % 12], note / 12 - 1)
}
