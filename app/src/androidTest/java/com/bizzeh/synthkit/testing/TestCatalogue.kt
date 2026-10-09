package com.bizzeh.synthkit.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.Preset
import com.bizzeh.synthkit.instruments.InstrumentCatalogue

/** A catalogue with every GM program and two kits, using the app's real GM names. */
fun testCatalogue(): InstrumentCatalogue {
    val names = InstrumentationRegistry.getInstrumentation().targetContext
        .resources.getStringArray(R.array.gm_program_names).toList()
    val presets = (0 until 128).map { Preset(0, it, "sf2 $it") } +
        listOf(Preset(128, 0, "Standard 1"), Preset(128, 25, "808/909"))
    return InstrumentCatalogue.build(presets, names)
}
