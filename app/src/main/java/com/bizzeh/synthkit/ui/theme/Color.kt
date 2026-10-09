package com.bizzeh.synthkit.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bizzeh.synthkit.instruments.Family

/** The studio colours. Screens read these through [StudioTheme.palette]. */
@Immutable
data class StudioPalette(
    val ground: Color,
    val panel: Color,
    val raised: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    /** An unlit light or an empty track. */
    val off: Color,
    val keybed: Color,
    /** Top-edge highlight that makes a surface look raised. */
    val highlight: Color,
) {
    // The LCD, record and active colours are the same in both themes, like hardware.
    val lcd = Color(0xFF07060C)
    val lcdLine = Color(0xFF2E2A4D)
    val amber = Color(0xFFFFB347)
    val amberDim = Color(0xFFB08A50)
    val record = Color(0xFFFF5A4E)
    val solo = Color(0xFF4D96FF)
    /** Text on amber, red and blue fills. */
    val onLit = Color(0xFF14121F)
    val ivory = Color(0xFFF2EFE6)
    val ivoryEdge = Color(0xFFCFCAC0)
    val ebony = Color(0xFF15131F)
}

val DarkPalette = StudioPalette(
    ground = Color(0xFF0E0D16),
    panel = Color(0xFF17152A),
    raised = Color(0xFF1B1930),
    line = Color(0xFF2E2A4D),
    text = Color(0xFFECEAF7),
    muted = Color(0xFF9D98BF),
    off = Color(0xFF3A3550),
    keybed = Color(0xFF07060C),
    highlight = Color(0xFF2B2747),
)

val LightPalette = StudioPalette(
    ground = Color(0xFFF3F1F9),
    panel = Color(0xFFFFFFFF),
    raised = Color(0xFFFFFFFF),
    line = Color(0xFFD9D5EA),
    text = Color(0xFF1B1830),
    muted = Color(0xFF5E5979),
    off = Color(0xFFCFCBE0),
    keybed = Color(0xFF1B1830),
    highlight = Color(0x14000000),
)

/** Each family's colour, used for its lights, tabs, previews and faders. */
fun familyColor(family: Family): Color = when (family) {
    Family.KEYS -> Color(0xFF8B7CFF)
    Family.GUITAR_BASS -> Color(0xFFFF9F43)
    Family.DRUMS_PERCUSSION -> Color(0xFF2EC4B6)
    Family.STRINGS_ORCHESTRA -> Color(0xFF4D96FF)
    Family.BRASS_WINDS -> Color(0xFFF7B731)
    Family.SYNTH -> Color(0xFFE056FD)
    Family.WORLD_MISC -> Color(0xFF7BD389)
}

/** Material roles mapped onto the palette, so dialogs, menus and sliders match the studio look. */
internal fun materialScheme(palette: StudioPalette, dark: Boolean): ColorScheme {
    val accent = if (dark) Color(0xFF8B7CFF) else Color(0xFF5B4BD6)
    val common = if (dark) darkColorScheme() else lightColorScheme()
    return common.copy(
        primary = accent,
        onPrimary = if (dark) palette.onLit else Color.White,
        primaryContainer = if (dark) Color(0xFF241F48) else Color(0xFFE6E2FF),
        onPrimaryContainer = palette.text,
        secondary = palette.amber,
        onSecondary = palette.onLit,
        secondaryContainer = if (dark) Color(0xFF3A2C14) else Color(0xFFFFE9C9),
        onSecondaryContainer = palette.text,
        tertiary = palette.amber,
        onTertiary = palette.onLit,
        tertiaryContainer = if (dark) Color(0xFF3A2C14) else Color(0xFFFFE9C9),
        onTertiaryContainer = palette.text,
        background = palette.ground,
        onBackground = palette.text,
        surface = palette.ground,
        onSurface = palette.text,
        surfaceVariant = palette.panel,
        onSurfaceVariant = palette.muted,
        surfaceContainerLowest = palette.ground,
        surfaceContainerLow = palette.panel,
        surfaceContainer = palette.panel,
        surfaceContainerHigh = palette.raised,
        surfaceContainerHighest = palette.raised,
        outline = palette.line,
        outlineVariant = palette.line,
        error = palette.record,
        onError = palette.onLit,
        errorContainer = if (dark) Color(0xFF3A1218) else Color(0xFFFFE3E0),
        onErrorContainer = palette.text,
    )
}
