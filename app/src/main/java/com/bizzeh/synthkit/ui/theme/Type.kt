package com.bizzeh.synthkit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bizzeh.synthkit.R

private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)

/** Archivo, one variable font file serving every weight (variation settings need API 26, the minimum). */
val Archivo = FontFamily(
    Weights.map { weight ->
        Font(R.font.archivo_variable, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

/** The LCD display face. */
val DisplayMono = FontFamily(Font(R.font.jetbrains_mono_bold, FontWeight.Bold))

/** Small capitals over a value, as on hardware panels. */
val Eyebrow = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp)

/** Pad and button labels. */
val PanelLabel = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.3.sp)

val LcdText = TextStyle(fontFamily = DisplayMono, fontWeight = FontWeight.Bold, fontSize = 17.sp)
val LcdSmall = TextStyle(fontFamily = DisplayMono, fontWeight = FontWeight.Bold, fontSize = 10.sp)

internal val StudioTypography: Typography = Typography().let { base ->
    fun TextStyle.archivo() = copy(fontFamily = Archivo)
    Typography(
        displayLarge = base.displayLarge.archivo(),
        displayMedium = base.displayMedium.archivo(),
        displaySmall = base.displaySmall.archivo(),
        headlineLarge = base.headlineLarge.archivo().copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.archivo().copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.archivo().copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.archivo().copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.archivo().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.archivo().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.archivo(),
        bodyMedium = base.bodyMedium.archivo(),
        bodySmall = base.bodySmall.archivo(),
        labelLarge = base.labelLarge.archivo().copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.archivo().copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.archivo().copy(fontWeight = FontWeight.SemiBold),
    )
}
