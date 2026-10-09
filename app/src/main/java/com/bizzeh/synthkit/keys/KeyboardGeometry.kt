package com.bizzeh.synthkit.keys

/**
 * Piano key positions for a window of white keys. White keys are counted from
 * MIDI note 0 (C-1), so white key 7 is C0. Positions are in pixels.
 */
class KeyboardGeometry(
    val firstWhite: Int,
    val whiteCount: Int,
    val whiteWidth: Float,
    val blackWidth: Float,
    val blackHeight: Float,
) {
    data class Key(val note: Int, val left: Float, val width: Float, val black: Boolean)

    val whiteKeys: List<Key> = (0 until whiteCount).map { i ->
        Key(whiteNote(firstWhite + i), i * whiteWidth, whiteWidth, black = false)
    }

    val blackKeys: List<Key> = (0 until whiteCount).mapNotNull { i ->
        val white = whiteNote(firstWhite + i)
        val black = white + 1
        if (white % OCTAVE !in WHITES_WITH_SHARP || i == whiteCount - 1 || black > HIGHEST_NOTE) {
            null
        } else {
            Key(black, (i + 1) * whiteWidth - blackWidth / 2, blackWidth, black = true)
        }
    }

    /** The note under a point, black keys taking priority in their upper area. */
    fun noteAt(x: Float, y: Float): Int? {
        if (x < 0 || x >= whiteCount * whiteWidth) return null
        if (y < blackHeight) {
            blackKeys.firstOrNull { x >= it.left && x < it.left + it.width }?.let { return it.note }
        }
        return whiteKeys[(x / whiteWidth).toInt().coerceAtMost(whiteCount - 1)].note
    }

    companion object {
        private const val OCTAVE = 12
        private val WHITE_OFFSETS = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        private val WHITES_WITH_SHARP = setOf(0, 2, 5, 7, 9)

        /** A0 and C8, the range of an 88-key piano. */
        const val LOWEST_NOTE = 21
        const val HIGHEST_NOTE = 108
        val LOWEST_WHITE = whiteIndex(LOWEST_NOTE)
        val HIGHEST_WHITE = whiteIndex(HIGHEST_NOTE)
        const val WHITES_PER_OCTAVE = 7

        fun whiteNote(whiteIndex: Int): Int =
            whiteIndex / WHITES_PER_OCTAVE * OCTAVE + WHITE_OFFSETS[whiteIndex % WHITES_PER_OCTAVE]

        /** Index of a white note; the note must be white. */
        fun whiteIndex(note: Int): Int {
            val offset = WHITE_OFFSETS.indexOf(note % OCTAVE)
            require(offset >= 0) { "Not a white key: $note" }
            return note / OCTAVE * WHITES_PER_OCTAVE + offset
        }

        /** Keeps a window of [whiteCount] white keys inside the piano range. */
        fun clampFirstWhite(firstWhite: Int, whiteCount: Int): Int =
            firstWhite.coerceIn(LOWEST_WHITE, maxOf(LOWEST_WHITE, HIGHEST_WHITE - whiteCount + 1))
    }
}
