package com.bizzeh.synthkit.browser

/** The player's favourite and recently opened instruments, by instrument id. */
data class Library(val favourites: Set<String> = emptySet(), val recents: List<String> = emptyList()) {

    fun toggleFavourite(id: String): Library =
        copy(favourites = if (id in favourites) favourites - id else favourites + id)

    /** Moves [id] to the front of the recents and keeps at most [MAX_RECENTS]. */
    fun opened(id: String): Library =
        copy(recents = (listOf(id) + recents.filter { it != id }).take(MAX_RECENTS))

    companion object {
        const val MAX_RECENTS = 8
    }
}
