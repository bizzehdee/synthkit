package com.bizzeh.synthkit.ui.theme

import androidx.compose.ui.graphics.Color
import com.bizzeh.synthkit.instruments.Family
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** WCAG 2 contrast for every text and background pair the screens use. */
class PaletteContrastTest {
    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun luminance(color: Color) = 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private fun assertReadable(name: String, text: Color, background: Color, minimum: Double = 4.5) {
        val ratio = contrast(text, background)
        assertTrue("$name is %.2f:1, needs %.1f:1".format(ratio, minimum), ratio >= minimum)
    }

    @Test
    fun bodyAndLabelTextIsReadableInBothThemes() {
        listOf("dark" to DarkPalette, "light" to LightPalette).forEach { (theme, p) ->
            listOf("ground" to p.ground, "panel" to p.panel, "raised" to p.raised).forEach { (surface, color) ->
                assertReadable("$theme text on $surface", p.text, color)
                assertReadable("$theme muted on $surface", p.muted, color)
            }
        }
    }

    @Test
    fun displayAndLitControlsAreReadable() {
        val p = DarkPalette
        assertReadable("amber on LCD", p.amber, p.lcd)
        assertReadable("dim amber on LCD", p.amberDim, p.lcd)
        assertReadable("text on amber", p.onLit, p.amber)
        assertReadable("text on record red", p.onLit, p.record)
        assertReadable("text on solo blue", p.onLit, p.solo)
        assertReadable("note names on ivory keys", Color(0xFF6A6584), p.ivory)
    }

    @Test
    fun everyFamilyHasItsOwnColour() {
        val colors = Family.entries.map(::familyColor)

        assertEquals(Family.entries.size, colors.toSet().size)
    }

    @Test
    fun familyLightsStandOutFromTheDarkSurfaces() {
        Family.entries.forEach { family ->
            assertReadable("$family light on raised", familyColor(family), DarkPalette.raised, minimum = 3.0)
        }
    }
}
