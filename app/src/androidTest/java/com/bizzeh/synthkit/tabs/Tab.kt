package com.bizzeh.synthkit.tabs

/** One note of a parsed tab, at a step counted from the start. */
data class TabNote(val step: Int, val note: Int, val velocity: Float = 1f, val lengthSteps: Int = 1)

/**
 * Parses the plain-text tabs used as test fixtures. Each character column is
 * one step. A section repeats when its header carries "2X" or "x 2".
 */
object Tab {
    private val REPEAT = Regex("""(\d+)\s*[xX](?!\w)|[xX]\s*(\d+)\s*$""")

    /** Drum lanes like "S |o---|" ; [map] turns a lane name and symbol into a GM note. */
    fun drums(text: String, map: (lane: String, symbol: Char) -> Int?): List<TabNote> {
        val lane = Regex("""^\s*([A-Za-z]{1,2})\s*\|(.*)$""")
        val notes = mutableListOf<TabNote>()
        var start = 0
        sections(text).forEach { (repeat, lines) ->
            val lanes = lines.mapNotNull { lane.find(it) }.map { it.groupValues[1] to it.groupValues[2].replace("|", "") }
            if (lanes.isEmpty()) return@forEach
            val length = lanes.maxOf { it.second.trimEnd().length }
            repeat(repeat) { pass ->
                lanes.forEach { (name, steps) ->
                    steps.forEachIndexed { step, symbol ->
                        if (step < length) map(name, symbol)?.let { notes += TabNote(start + pass * length + step, it) }
                    }
                }
            }
            start += repeat * length
        }
        return notes.sortedBy { it.step }
    }

    /**
     * String lanes like "e||-5-|" or "G:-11-", top string first. Six lanes use
     * guitar tuning, four use bass tuning. Each note rings until the next note
     * on its string. "x" is a muted string: short and quiet.
     */
    fun strings(text: String): List<TabNote> {
        val lane = Regex("""^\s*([A-Ga-g])\s*[:|]\|?(.*)$""")
        val notes = mutableListOf<TabNote>()
        var start = 0
        sections(text).forEach { (repeat, lines) ->
            val lanes = lines.mapNotNull { lane.find(it)?.groupValues?.get(2)?.replace("|", "") }
            if (lanes.size != 4 && lanes.size != 6) return@forEach
            val tuning = if (lanes.size == 6) GUITAR else BASS
            val length = lanes.maxOf { it.trimEnd().length }
            val section = mutableListOf<TabNote>()
            lanes.forEachIndexed { string, steps ->
                var column = 0
                var lastFret = 0
                val onString = mutableListOf<TabNote>()
                while (column < steps.length) {
                    val symbol = steps[column]
                    when {
                        symbol.isDigit() -> {
                            val digits = steps.substring(column).takeWhile { it.isDigit() }.take(2)
                            lastFret = digits.toInt()
                            onString += TabNote(column, tuning[string] + lastFret)
                            column += digits.length
                        }
                        symbol == 'x' -> {
                            onString += TabNote(column, tuning[string] + lastFret, velocity = 0.35f, lengthSteps = 0)
                            column++
                        }
                        else -> column++
                    }
                }
                onString.forEachIndexed { i, note ->
                    val next = onString.getOrNull(i + 1)?.step ?: length
                    section += if (note.lengthSteps == 0) note.copy(lengthSteps = 1) else note.copy(lengthSteps = next - note.step)
                }
            }
            repeat(repeat) { pass -> section.forEach { notes += it.copy(step = start + pass * length + it.step) } }
            start += repeat * length
        }
        return notes.sortedBy { it.step }
    }

    /** Blocks separated by blank lines or headers, each with its repeat count. */
    private fun sections(text: String): List<Pair<Int, List<String>>> {
        val result = mutableListOf<Pair<Int, List<String>>>()
        var repeat = 1
        var lines = mutableListOf<String>()
        fun flush() {
            if (lines.isNotEmpty()) result += repeat to lines
            lines = mutableListOf()
            repeat = 1
        }
        text.lines().forEach { line ->
            val isLane = Regex("""^\s*[A-Za-z]{1,2}\s*[:|]""").containsMatchIn(line)
            when {
                isLane -> lines += line
                line.isBlank() || line.trim().all { it == '-' } -> if (lines.isNotEmpty()) flush()
                else -> {
                    flush()
                    REPEAT.find(line)?.let { match ->
                        repeat = (match.groupValues[1].ifEmpty { match.groupValues[2] }).toInt()
                    }
                }
            }
        }
        flush()
        return result
    }

    /** Open strings, top lane first. */
    private val GUITAR = listOf(64, 59, 55, 50, 45, 40)
    private val BASS = listOf(43, 38, 33, 28)

    /** GM notes for the lane names and symbols in the drum fixtures. */
    fun gmDrum(lane: String, symbol: Char): Int? = when (lane to symbol) {
        "C" to 'X', "C" to 'x', "C" to 'o' -> 49
        "HH" to 'x', "HH" to 'o', "HH" to 'X', "H" to 'x' -> 42
        "H" to 'X' -> 46
        "H" to '#', "Hf" to 'x', "Hf" to 'o' -> 44
        "Rd" to 'x', "Rd" to 'X', "Rd" to 'o' -> 51
        "S" to 'o', "S" to 'g', "S" to 'O' -> 38
        "B" to 'o' -> 36
        "T" to 'o' -> 50
        "F" to 'o' -> 43
        else -> null
    }
}
