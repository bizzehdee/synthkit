package com.bizzeh.synthkit.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectNamesTest {
    @Test
    fun nextNameTakesTheFirstFreeNumber() {
        assertEquals("Project 1", ProjectNames.next(emptyList(), "Project %1\$d"))
        assertEquals("Project 2", ProjectNames.next(listOf("Project 1", "Project 3"), "Project %1\$d"))
    }

    @Test
    fun cleanTrimsAndRejectsBlankNames() {
        assertEquals("Song", ProjectNames.clean("  Song "))
        assertNull(ProjectNames.clean("   "))
        assertEquals(Project.MAX_NAME_LENGTH, ProjectNames.clean("x".repeat(500))?.length)
    }
}
