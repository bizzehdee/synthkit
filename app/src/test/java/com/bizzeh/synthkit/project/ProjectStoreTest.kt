package com.bizzeh.synthkit.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val store by lazy { ProjectStore(folder.root, logged::add) }
    private val note = Note(tick = 240, key = 38, velocity = 100, lengthTicks = 120)
    private val project = Project(
        id = "p1",
        name = "Song",
        tempoBpm = 96,
        loopBars = 2,
        tracks = listOf(Track(id = "t1", bank = 128, program = 0, takes = listOf(Take(listOf(note))), volume = 0.5f)),
    )

    @Test
    fun savedProjectLoadsBackUnchanged() {
        store.save(project)

        assertEquals(project, store.load("p1"))
        assertEquals(listOf(project), store.list().map { it.project })
    }

    @Test
    fun savingAgainReplacesTheFileAndLeavesNoTemporaryFile() {
        store.save(project)
        store.save(project.copy(name = "Renamed"))

        assertEquals("Renamed", store.load("p1")?.name)
        assertEquals(listOf("p1.json"), folder.root.list()!!.toList())
    }

    @Test
    fun listShowsTheMostRecentlyChangedFirst() {
        store.save(project.copy(id = "old", name = "Old"))
        File(folder.root, "old.json").setLastModified(1_000)
        store.save(project.copy(id = "new", name = "New"))

        assertEquals(listOf("new", "old"), store.list().map { it.project.id })
    }

    @Test
    fun deleteRemovesTheProject() {
        store.save(project)

        store.delete("p1")

        assertNull(store.load("p1"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun corruptFileIsSkippedAndLogged() {
        store.save(project)
        File(folder.root, "broken.json").writeText("{ not json")

        assertEquals(listOf("p1"), store.list().map { it.project.id })
        assertTrue(logged.single().contains("reason=unreadable"))
    }

    @Test
    fun wellFormedButInvalidFileIsSkipped() {
        File(folder.root, "bad.json").writeText("""{"id":"bad","name":"Bad","tempoBpm":999}""")

        assertTrue(store.list().isEmpty())
        assertTrue(logged.single().contains("reason=invalid"))
    }

    @Test
    fun fileWhoseNameDoesNotMatchItsIdIsSkipped() {
        store.save(project)
        File(folder.root, "p1.json").copyTo(File(folder.root, "other.json"))

        assertEquals(listOf("p1"), store.list().map { it.project.id })
    }

    @Test
    fun oversizedFileIsSkippedWithoutReadingIt() {
        File(folder.root, "huge.json").writeBytes(ByteArray(5 * 1024 * 1024 + 1))

        assertTrue(store.list().isEmpty())
        assertTrue(logged.single().contains("reason=too_large"))
    }

    @Test
    fun unsafeIdsNeverTouchTheFileSystem() {
        File(folder.root.parentFile, "outside.json").writeText("{}")

        assertNull(store.load("../outside"))
        store.delete("../outside")

        assertTrue(File(folder.root.parentFile, "outside.json").exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidProjectIsNeverSaved() {
        store.save(project.copy(tempoBpm = 500))
    }

    @Test
    fun unknownFieldsFromALaterVersionAreIgnored() {
        File(folder.root, "p2.json").writeText("""{"id":"p2","name":"Song","futureField":true}""")

        assertEquals("Song", store.load("p2")?.name)
        assertFalse(logged.any())
    }
}
