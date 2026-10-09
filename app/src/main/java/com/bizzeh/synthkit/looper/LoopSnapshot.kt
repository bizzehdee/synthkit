package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.LoopNote
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Track

/** MIDI channel of the track at [index]; channel 0 stays free and 15 is the click. */
fun trackChannel(index: Int): Int = index + 1

object LoopSnapshot {
    /** Tracks that sound: the soloed ones if any track is soloed, else every unmuted one. */
    fun audible(project: Project): List<Track> {
        val soloed = project.tracks.filter { it.solo }
        return if (soloed.isNotEmpty()) soloed else project.tracks.filter { !it.muted }
    }

    /** Every audible note as a note on and a note off, quantised for playback. */
    fun build(project: Project): List<LoopNote> {
        val loopTicks = project.loopTicks
        if (loopTicks == 0) return emptyList()
        val audible = audible(project).map { it.id }.toSet()
        return project.tracks.withIndex().filter { it.value.id in audible }.flatMap { (index, track) ->
            val channel = trackChannel(index)
            track.notes.flatMap { note ->
                val start = LoopMath.quantise(note.tick, track.quantise.gridTicks, loopTicks)
                val end = Math.floorMod(start + note.lengthTicks, loopTicks)
                listOf(
                    LoopNote(start, channel, note.key, note.velocity / 127f),
                    LoopNote(end, channel, note.key, 0f),
                )
            }
        }
    }
}
