package com.bizzeh.synthkit.project

import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class StoredProject(val project: Project, val modifiedMillis: Long)

/** One JSON file per project in [directory]. Not thread safe: callers serialise access. */
class ProjectStore(private val directory: File, private val log: (String) -> Unit = { Log.w(TAG, it) }) {

    fun list(): List<StoredProject> {
        val files = directory.listFiles { file -> file.isFile && file.name.endsWith(EXTENSION) }.orEmpty()
        return files.mapNotNull { file ->
            read(file)?.let { StoredProject(it, file.lastModified()) }
        }.sortedByDescending { it.modifiedMillis }
    }

    fun load(id: String): Project? = fileFor(id)?.takeIf { it.isFile }?.let(::read)

    /** Writes atomically: a crash mid-write leaves the previous version intact. */
    fun save(project: Project) {
        val problems = ProjectValidation.problems(project)
        require(problems.isEmpty()) { "Refusing to save an invalid project: $problems" }
        directory.mkdirs()
        val target = requireNotNull(fileFor(project.id))
        val temporary = File(directory, project.id + TEMPORARY_EXTENSION)
        temporary.writeText(json.encodeToString(Project.serializer(), project))
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun delete(id: String) {
        fileFor(id)?.delete()
    }

    private fun fileFor(id: String): File? = if (ProjectValidation.isSafeId(id)) File(directory, id + EXTENSION) else null

    // Event templates are constant; the file name is an app-generated id, not personal data.
    private fun read(file: File): Project? {
        if (file.length() > MAX_FILE_BYTES) {
            log("event=project_skipped file=${file.name} reason=too_large")
            return null
        }
        val project = try {
            json.decodeFromString(Project.serializer(), file.readText())
        } catch (e: SerializationException) {
            log("event=project_skipped file=${file.name} reason=unreadable")
            return null
        } catch (e: IllegalArgumentException) {
            log("event=project_skipped file=${file.name} reason=unreadable")
            return null
        } catch (e: IOException) {
            log("event=project_skipped file=${file.name} reason=io")
            return null
        }
        val problems = ProjectValidation.problems(project)
        if (problems.isNotEmpty() || file.name != project.id + EXTENSION) {
            log("event=project_skipped file=${file.name} reason=invalid")
            return null
        }
        return project
    }

    companion object {
        private const val TAG = "SynthKit"
        private const val EXTENSION = ".json"
        private const val TEMPORARY_EXTENSION = ".json.tmp"
        private const val MAX_FILE_BYTES = 5L * 1024 * 1024

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
