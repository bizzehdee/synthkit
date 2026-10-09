package com.bizzeh.synthkit.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetTest {
    @Test
    fun triplesBecomePresets() {
        val presets = Preset.fromNative(arrayOf("0", "24", "Nylon Guitar", "128", "0", "Standard 1"))

        assertEquals(listOf(Preset(0, 24, "Nylon Guitar"), Preset(128, 0, "Standard 1")), presets)
    }

    @Test
    fun noFieldsGiveNoPresets() {
        assertEquals(emptyList<Preset>(), Preset.fromNative(emptyArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteTripleIsRejected() {
        Preset.fromNative(arrayOf("0", "24"))
    }
}
