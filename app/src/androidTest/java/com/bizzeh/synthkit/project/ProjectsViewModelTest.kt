package com.bizzeh.synthkit.project

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProjectsViewModelTest {
    private val application =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val folder = File(application.filesDir, "projects")

    @Before
    fun clearBefore() {
        folder.deleteRecursively()
    }

    // Saves run in the background; let them finish before the folder goes.
    @After
    fun clearAfter() {
        Thread.sleep(500)
        folder.deleteRecursively()
    }

    private fun loaded(): ProjectsViewModel {
        val model = ProjectsViewModel(application)
        val deadline = System.currentTimeMillis() + 5_000
        while (model.projects.value == null && System.currentTimeMillis() < deadline) Thread.sleep(20)
        return model
    }

    private fun reloaded(): List<Project> {
        Thread.sleep(300)
        return loaded().projects.value.orEmpty().map { it.project }
    }

    @Test
    fun changesAreSavedAndSurviveANewViewModel() {
        val model = loaded()
        val first = model.create(metronomeOnPlayback = true)
        val second = model.create(metronomeOnPlayback = false)
        model.rename(first.id, "Demo")
        model.duplicate(second.id)
        model.delete(second.id)

        val reloaded = reloaded()

        assertEquals(listOf("Demo", "Project 2 (copy)"), reloaded.map { it.name }.sorted())
        assertEquals(listOf(true, false), reloaded.sortedBy { it.name }.map { it.metronomeOnPlayback })
    }

    @Test
    fun newProjectsTakeTheNextFreeNumber() {
        val model = loaded()

        assertEquals("Project 1", model.create(false).name)
        assertEquals("Project 2", model.create(false).name)
        assertTrue(model.project(model.create(false).id) != null)
    }
}
