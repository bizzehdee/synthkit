package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportPlanTest {
    private val drums = Track("a", 128, 0, takes = listOf(Take(listOf(Note(130, 36, 127, 120)))), volume = 0.5f)
    private val keys = Track("b", 0, 24, takes = listOf(Take(listOf(Note(1900, 60, 127, 100)))))
    private val project = Project(id = "p", name = "P", tempoBpm = 96, loopBars = 1, tracks = listOf(drums, keys))

    @Test
    fun specCarriesTempoLoopPassesAndEachTracksSound() {
        val spec = ExportPlan.spec(project, passes = 3)

        assertEquals(96, spec.bpm)
        assertEquals(1920, spec.loopTicks)
        assertEquals(3, spec.passes)
        assertEquals(ExportPlan.SAMPLE_RATE, spec.sampleRate)
        assertEquals(listOf(ExportTrack(1, 128, 0, 0.5f), ExportTrack(2, 0, 24, 1f)), spec.tracks)
    }

    @Test
    fun noteOffsRunPastTheLoopEndInsteadOfWrapping() {
        val notes = ExportPlan.spec(project, 1).notes

        assertEquals(ExportNote(1900, 2, 60, 1f), notes[2])
        assertEquals(ExportNote(2000, 2, 60, 0f), notes[3])
    }

    @Test
    fun quantiseAndMuteApplyAsOnPlayback() {
        val edited = project.copy(tracks = listOf(drums.copy(quantise = Quantise.SIXTEENTH), keys.copy(muted = true)))

        val spec = ExportPlan.spec(edited, 1)

        assertEquals(listOf(1), spec.tracks.map { it.channel })
        assertEquals(120, spec.notes.first().tick)
    }

    @Test(expected = IllegalArgumentException::class)
    fun projectWithoutALoopCannotBeExported() {
        ExportPlan.spec(project.copy(loopBars = 0, tracks = emptyList()), 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun passesAreLimited() {
        ExportPlan.spec(project, 17)
    }
}
