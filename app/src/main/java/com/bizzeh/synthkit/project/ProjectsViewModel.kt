package com.bizzeh.synthkit.project

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bizzeh.synthkit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * The project list and every change to a project. Each change is saved at
 * once; writes run one at a time off the main thread, in the order made.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ProjectStore(File(application.filesDir, PROJECTS_FOLDER))
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val resources = application.resources

    private val mutableProjects = MutableStateFlow<List<StoredProject>?>(null)
    /** Null until the first load finishes. */
    val projects: StateFlow<List<StoredProject>?> = mutableProjects.asStateFlow()

    init {
        viewModelScope.launch(writer) { mutableProjects.value = store.list() }
    }

    fun project(id: String): Project? = mutableProjects.value?.firstOrNull { it.project.id == id }?.project

    /** Creates and saves a new empty project and returns it. */
    fun create(): Project {
        val names = mutableProjects.value.orEmpty().map { it.project.name }
        val project = Project(id = newId(), name = ProjectNames.next(names, resources.getString(R.string.project_default_name)))
        update(project)
        return project
    }

    fun rename(id: String, name: String) {
        val cleaned = ProjectNames.clean(name) ?: return
        project(id)?.let { update(it.copy(name = cleaned)) }
    }

    fun duplicate(id: String) {
        val original = project(id) ?: return
        val name = ProjectNames.clean(resources.getString(R.string.project_copy_name, original.name)) ?: return
        update(original.copy(id = newId(), name = name))
    }

    fun delete(id: String) {
        mutableProjects.value = mutableProjects.value.orEmpty().filter { it.project.id != id }
        write("delete") { store.delete(id) }
    }

    /** Replaces the project in the list and saves it. */
    fun update(project: Project) {
        val now = System.currentTimeMillis()
        mutableProjects.value = listOf(StoredProject(project, now)) +
            mutableProjects.value.orEmpty().filter { it.project.id != project.id }
        write("save") { store.save(project) }
    }

    // A failed write (for example a full disk) is logged, not fatal: the project
    // stays in memory and the next change tries again.
    private fun write(action: String, block: () -> Unit) {
        viewModelScope.launch(writer) {
            try {
                block()
            } catch (e: IOException) {
                Log.e(TAG, "event=project_${action}_failed error=${e.javaClass.simpleName}")
            }
        }
    }

    private fun newId() = UUID.randomUUID().toString()

    private companion object {
        const val TAG = "SynthKit"
        const val PROJECTS_FOLDER = "projects"
    }
}
