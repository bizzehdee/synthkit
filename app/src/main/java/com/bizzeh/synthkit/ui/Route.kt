package com.bizzeh.synthkit.ui

/**
 * A screen in the back stack, encoded as a string so the stack survives
 * recreation. Project and track ids are generated UUIDs, so they never contain "/".
 */
sealed interface Route {
    data object Projects : Route
    data class Project(val projectId: String) : Route
    data class AddTrack(val projectId: String) : Route

    /** Picks an instrument for a new track, or for [swapTrackId] when it is set. */
    data class Browser(val projectId: String, val swapTrackId: String? = null) : Route
    data class Track(val projectId: String, val trackId: String) : Route
    data class Editor(val projectId: String, val trackId: String) : Route
    data class Export(val projectId: String) : Route

    fun encode(): String = when (this) {
        Projects -> PROJECTS
        is Project -> "project/$projectId"
        is AddTrack -> "add-track/$projectId"
        is Browser -> listOfNotNull("browser", projectId, swapTrackId).joinToString("/")
        is Track -> "track/$projectId/$trackId"
        is Editor -> "editor/$projectId/$trackId"
        is Export -> "export/$projectId"
    }

    companion object {
        private const val PROJECTS = "projects"

        fun decode(value: String): Route {
            val parts = value.split("/")
            return when {
                value == PROJECTS -> Projects
                parts[0] == "project" && parts.size == 2 -> Project(parts[1])
                parts[0] == "add-track" && parts.size == 2 -> AddTrack(parts[1])
                parts[0] == "browser" && parts.size in 2..3 -> Browser(parts[1], parts.getOrNull(2))
                parts[0] == "track" && parts.size == 3 -> Track(parts[1], parts[2])
                parts[0] == "editor" && parts.size == 3 -> Editor(parts[1], parts[2])
                parts[0] == "export" && parts.size == 2 -> Export(parts[1])
                else -> throw IllegalArgumentException("Unknown route: $value")
            }
        }
    }
}

/** The project the screen belongs to, if any. */
val Route.projectId: String?
    get() = when (this) {
        Route.Projects -> null
        is Route.Project -> projectId
        is Route.AddTrack -> projectId
        is Route.Browser -> projectId
        is Route.Track -> projectId
        is Route.Editor -> projectId
        is Route.Export -> projectId
    }
