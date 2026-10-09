package com.bizzeh.synthkit.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bizzeh.synthkit.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()
    private val song = StoredProject(Project(id = "p1", name = "Song", tempoBpm = 96), 0)

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(projects: List<StoredProject>?) {
        composeRule.setContent {
            ProjectListScreen(
                projects = projects,
                familyOf = { null },
                onOpen = { calls += "open $it" },
                onCreate = { calls += "create" },
                onRename = { id, name -> calls += "rename $id $name" },
                onDuplicate = { calls += "duplicate $it" },
                onDelete = { calls += "delete $it" },
            )
        }
    }

    private fun openMenu() =
        composeRule.onNodeWithContentDescription(string(R.string.project_actions, "Song")).performClick()

    @Test
    fun emptyListExplainsHowToStart() {
        show(emptyList())

        composeRule.onNodeWithText(string(R.string.projects_empty)).assertIsDisplayed()
    }

    @Test
    fun projectRowShowsNameTempoAndTracksAndOpens() {
        show(listOf(song))

        composeRule.onNodeWithText(string(R.string.project_meta, 96, "0 tracks", string(R.string.project_no_loop))).assertIsDisplayed()
        composeRule.onNodeWithText("Song").performClick()

        assertEquals(listOf("open p1"), calls)
    }

    @Test
    fun newProjectButtonCreates() {
        show(emptyList())

        composeRule.onNodeWithText(string(R.string.project_new)).performClick()

        assertEquals(listOf("create"), calls)
    }

    @Test
    fun renameSavesTheNewName() {
        show(listOf(song))

        openMenu()
        composeRule.onNodeWithText(string(R.string.project_rename)).performClick()
        composeRule.onNodeWithText(string(R.string.project_name)).performTextClearance()
        composeRule.onNodeWithText(string(R.string.project_name)).performTextInput("Demo")
        composeRule.onNodeWithText(string(R.string.save)).performClick()

        assertEquals(listOf("rename p1 Demo"), calls)
    }

    @Test
    fun blankNameCannotBeSaved() {
        show(listOf(song))

        openMenu()
        composeRule.onNodeWithText(string(R.string.project_rename)).performClick()
        composeRule.onNodeWithText(string(R.string.project_name)).performTextClearance()

        composeRule.onNodeWithText(string(R.string.save)).assertIsNotEnabled()
    }

    @Test
    fun duplicateIsOfferedFromTheMenu() {
        show(listOf(song))

        openMenu()
        composeRule.onNodeWithText(string(R.string.project_duplicate)).performClick()

        assertEquals(listOf("duplicate p1"), calls)
    }

    @Test
    fun deleteAsksFirstAndCancelKeepsTheProject() {
        show(listOf(song))

        openMenu()
        composeRule.onNodeWithText(string(R.string.project_delete)).performClick()
        composeRule.onNodeWithText(string(R.string.project_delete_confirm, "Song")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun confirmedDeleteDeletes() {
        show(listOf(song))

        openMenu()
        composeRule.onNodeWithText(string(R.string.project_delete)).performClick()
        composeRule.onNodeWithText(string(R.string.project_delete)).performClick()

        assertEquals(listOf("delete p1"), calls)
    }
}
