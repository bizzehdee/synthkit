package com.bizzeh.synthkit.ui

/** A screen in the back stack, encoded as a string so the stack survives recreation. */
sealed interface Route {
    data object Projects : Route
    data class Project(val projectId: String) : Route
    data object Browser : Route
    data class Play(val instrumentId: String) : Route

    fun encode(): String = when (this) {
        Projects -> PROJECTS
        Browser -> BROWSER
        is Project -> PROJECT_PREFIX + projectId
        is Play -> PLAY_PREFIX + instrumentId
    }

    companion object {
        private const val PROJECTS = "projects"
        private const val BROWSER = "browser"
        private const val PROJECT_PREFIX = "project/"
        private const val PLAY_PREFIX = "play/"

        fun decode(value: String): Route = when {
            value == PROJECTS -> Projects
            value == BROWSER -> Browser
            value.startsWith(PROJECT_PREFIX) -> Project(value.removePrefix(PROJECT_PREFIX))
            value.startsWith(PLAY_PREFIX) -> Play(value.removePrefix(PLAY_PREFIX))
            else -> throw IllegalArgumentException("Unknown route: $value")
        }

        /** Opening an instrument keeps the stack up to the open project, so Back returns to it. */
        fun openInstrument(stack: List<Route>, instrumentId: String): List<Route> {
            val project = stack.indexOfLast { it is Project }
            return stack.take(project + 1).ifEmpty { listOf(Projects) } + Play(instrumentId)
        }
    }
}
