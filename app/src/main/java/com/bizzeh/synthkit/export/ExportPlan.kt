package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.looper.LoopMath
import com.bizzeh.synthkit.looper.LoopSnapshot
import com.bizzeh.synthkit.looper.trackChannel
import com.bizzeh.synthkit.project.Project

data class ExportTrack(val channel: Int, val bank: Int, val program: Int, val volume: Float)

/** [tick] is inside one pass; a note off may pass the loop end. Velocity 0 is a note off. */
data class ExportNote(val tick: Int, val channel: Int, val key: Int, val velocity: Float)

data class ExportSpec(
    val sampleRate: Int,
    val bpm: Int,
    val loopTicks: Int,
    val passes: Int,
    val tailMillis: Int,
    val tracks: List<ExportTrack>,
    val notes: List<ExportNote>,
)

object ExportPlan {
    const val SAMPLE_RATE = 44_100
    const val TAIL_MILLIS = 2_000
    const val MIN_PASSES = 1
    const val MAX_PASSES = 16
    const val DEFAULT_PASSES = 2

    /** What plays: the audible tracks, as on playback, with quantised notes that do not wrap. */
    fun spec(project: Project, passes: Int): ExportSpec {
        require(project.loopBars > 0) { "Nothing to export before the first take" }
        require(passes in MIN_PASSES..MAX_PASSES) { "Passes out of range: $passes" }
        val loopTicks = project.loopTicks
        val audible = LoopSnapshot.audible(project).map { it.id }.toSet()
        val playing = project.tracks.withIndex().filter { it.value.id in audible }
        return ExportSpec(
            sampleRate = SAMPLE_RATE,
            bpm = project.tempoBpm,
            loopTicks = loopTicks,
            passes = passes,
            tailMillis = TAIL_MILLIS,
            tracks = playing.map { (index, track) -> ExportTrack(trackChannel(index), track.bank, track.program, track.volume) },
            notes = playing.flatMap { (index, track) ->
                track.notes.flatMap { note ->
                    val start = LoopMath.quantise(note.tick, track.quantise.gridTicks, loopTicks)
                    listOf(
                        ExportNote(start, trackChannel(index), note.key, note.velocity / 127f),
                        ExportNote(start + note.lengthTicks, trackChannel(index), note.key, 0f),
                    )
                }
            },
        )
    }
}
