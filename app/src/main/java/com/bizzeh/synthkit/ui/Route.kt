package com.bizzeh.synthkit.ui

/** A screen in the back stack, encoded as a string so the stack survives recreation. */
sealed interface Route {
    data object Home : Route
    data object Browser : Route
    data class Play(val instrumentId: String) : Route

    fun encode(): String = when (this) {
        Home -> HOME
        Browser -> BROWSER
        is Play -> PLAY_PREFIX + instrumentId
    }

    companion object {
        private const val HOME = "home"
        private const val BROWSER = "browser"
        private const val PLAY_PREFIX = "play/"

        fun decode(value: String): Route = when {
            value == HOME -> Home
            value == BROWSER -> Browser
            value.startsWith(PLAY_PREFIX) -> Play(value.removePrefix(PLAY_PREFIX))
            else -> throw IllegalArgumentException("Unknown route: $value")
        }

        /** Opening an instrument always leaves Home then Play, so Back returns home. */
        fun openInstrument(instrumentId: String): List<Route> = listOf(Home, Play(instrumentId))
    }
}
