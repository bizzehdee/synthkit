package com.bizzeh.synthkit.chords

enum class Mode(val intervals: List<Int>) {
    MAJOR(listOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR(listOf(0, 2, 3, 5, 7, 8, 10)),
}

enum class Quality { MAJOR, MINOR, DIMINISHED }

/** A diatonic triad. [root] is a pitch class 0 to 11; [degree] is 0 for I to 6 for vii. */
data class Triad(val degree: Int, val root: Int, val quality: Quality) {
    val pitchClasses: Set<Int>
        get() {
            val third = if (quality == Quality.MAJOR) 4 else 3
            val fifth = if (quality == Quality.DIMINISHED) 6 else 7
            return setOf(root, (root + third) % 12, (root + fifth) % 12)
        }

    /** Roman numeral, upper case for major, lower case for minor, ° for diminished. */
    val numeral: String
        get() {
            val roman = ROMAN[degree]
            return when (quality) {
                Quality.MAJOR -> roman
                Quality.MINOR -> roman.lowercase()
                Quality.DIMINISHED -> roman.lowercase() + "°"
            }
        }

    private companion object {
        val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII")
    }
}

object Harmony {
    /** Open strings of a guitar in standard tuning, low to high: E2 A2 D3 G3 B3 E4. */
    val GUITAR_STRINGS = listOf(40, 45, 50, 55, 59, 64)

    /** E1, the lowest note of a four-string bass. */
    const val BASS_LOWEST = 28

    fun triads(key: Int, mode: Mode): List<Triad> = (0 until 7).map { degree ->
        val scale = mode.intervals
        val root = scale[degree]
        val third = (scale[(degree + 2) % 7] - root + 12) % 12
        val fifth = (scale[(degree + 4) % 7] - root + 12) % 12
        val quality = when {
            third == 4 -> Quality.MAJOR
            fifth == 6 -> Quality.DIMINISHED
            else -> Quality.MINOR
        }
        Triad(degree, (key + root) % 12, quality)
    }

    /** Each string plays the nearest chord tone at or above its open note. */
    fun guitarVoicing(triad: Triad): List<Int> = GUITAR_STRINGS.map { open ->
        (open until open + 12).first { it % 12 in triad.pitchClasses }
    }

    /** The chord root at or above E1, moved by whole octaves. */
    fun bassRoot(triad: Triad, octaveShift: Int): Int =
        (BASS_LOWEST until BASS_LOWEST + 12).first { it % 12 == triad.root } + octaveShift * 12
}
