package com.bizzeh.synthkit.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectValidationTest {
    private val note = Note(tick = 0, key = 36, velocity = 100, lengthTicks = 120)
    private val track = Track(id = "t1", bank = 128, program = 0, takes = listOf(Take(listOf(note))))
    private val valid = Project(id = "p-1", name = "Song", loopBars = 1, tracks = listOf(track))

    private fun problemsOf(project: Project) = ProjectValidation.problems(project)

    @Test
    fun validProjectHasNoProblems() {
        assertEquals(emptyList<String>(), problemsOf(valid))
    }

    @Test
    fun newEmptyProjectIsValid() {
        assertEquals(emptyList<String>(), problemsOf(Project(id = "abc", name = "Project 1")))
    }

    @Test
    fun idsThatCouldLeaveTheFolderAreRejected() {
        listOf("../x", "a/b", "", "a b", "x".repeat(65)).forEach { id ->
            assertTrue(id, problemsOf(valid.copy(id = id)).isNotEmpty())
        }
    }

    @Test
    fun projectLevelLimitsAreChecked() {
        listOf(
            valid.copy(formatVersion = 2),
            valid.copy(name = " "),
            valid.copy(name = "n".repeat(101)),
            valid.copy(tempoBpm = 39),
            valid.copy(tempoBpm = 241),
            valid.copy(loopBars = 9),
            valid.copy(loopBars = -1),
            valid.copy(tracks = List(9) { track.copy(id = "t$it") }),
            valid.copy(tracks = listOf(track, track)),
        ).forEach { assertTrue(it.toString(), problemsOf(it).isNotEmpty()) }
    }

    @Test
    fun trackAndNoteLimitsAreChecked() {
        listOf(
            track.copy(id = "../t"),
            track.copy(bank = 7),
            track.copy(program = 128),
            track.copy(volume = 1.5f),
            track.copy(volume = Float.NaN),
            track.copy(takes = listOf(Take(listOf(note.copy(key = 128))))),
            track.copy(takes = listOf(Take(listOf(note.copy(velocity = 0))))),
            track.copy(takes = listOf(Take(listOf(note.copy(lengthTicks = 0))))),
            track.copy(takes = listOf(Take(listOf(note.copy(tick = -1))))),
            track.copy(takes = listOf(Take(listOf(note.copy(tick = TICKS_PER_BAR))))),
            track.copy(takes = listOf(Take(List(ProjectValidation.MAX_NOTES_PER_TRACK + 1) { note }))),
        ).forEach { assertTrue(it.toString().take(80), problemsOf(valid.copy(tracks = listOf(it))).isNotEmpty()) }
    }

    @Test
    fun notesNeedALoop() {
        assertTrue(problemsOf(valid.copy(loopBars = 0)).isNotEmpty())
    }
}
