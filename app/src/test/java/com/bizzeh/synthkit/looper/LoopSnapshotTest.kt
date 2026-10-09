package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.LoopNote
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class LoopSnapshotTest {
    private fun track(id: String, vararg notes: Note) = Track(id, 0, 0, takes = listOf(Take(notes.toList())))
    private val drums = track("a", Note(130, 36, 127, 120))
    private val keys = track("b", Note(1900, 60, 127, 100))
    private val project = Project(id = "p", name = "P", loopBars = 1, tracks = listOf(drums, keys))

    @Test
    fun eachTrackPlaysOnItsOwnChannelWithNoteOffsWrapping() {
        assertEquals(
            listOf(
                LoopNote(130, 1, 36, 1f), LoopNote(250, 1, 36, 0f),
                LoopNote(1900, 2, 60, 1f), LoopNote(80, 2, 60, 0f),
            ),
            LoopSnapshot.build(project),
        )
    }

    @Test
    fun mutedTracksAreLeftOut() {
        val muted = project.copy(tracks = listOf(drums.copy(muted = true), keys))

        assertEquals(setOf(2), LoopSnapshot.build(muted).map { it.channel }.toSet())
    }

    @Test
    fun soloOverridesMute() {
        val solo = project.copy(tracks = listOf(drums.copy(solo = true, muted = true), keys))

        assertEquals(setOf(1), LoopSnapshot.build(solo).map { it.channel }.toSet())
    }

    @Test
    fun quantiseMovesPlaybackButNotTheStoredNote() {
        val quantised = project.copy(tracks = listOf(drums.copy(quantise = Quantise.SIXTEENTH), keys))

        assertEquals(120, LoopSnapshot.build(quantised).first().tick)
        assertEquals(130, quantised.tracks[0].notes.single().tick)
    }

    @Test
    fun noLoopMeansNoNotes() {
        assertEquals(emptyList<LoopNote>(), LoopSnapshot.build(Project(id = "p", name = "P")))
    }
}
