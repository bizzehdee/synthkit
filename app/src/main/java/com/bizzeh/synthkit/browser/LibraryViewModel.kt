package com.bizzeh.synthkit.browser

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps [Library] in app-private preferences. */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val mutableLibrary = MutableStateFlow(
        Library(
            favourites = preferences.getStringSet(FAVOURITES, emptySet()).orEmpty().toSet(),
            recents = preferences.getString(RECENTS, null)
                ?.split(SEPARATOR)
                ?.filter { it.isNotEmpty() }
                .orEmpty(),
        ),
    )
    val library: StateFlow<Library> = mutableLibrary.asStateFlow()

    fun toggleFavourite(id: String) = update(mutableLibrary.value.toggleFavourite(id))

    fun opened(id: String) = update(mutableLibrary.value.opened(id))

    private fun update(library: Library) {
        mutableLibrary.value = library
        preferences.edit {
            putStringSet(FAVOURITES, library.favourites)
            putString(RECENTS, library.recents.joinToString(SEPARATOR))
        }
    }

    private companion object {
        const val PREFERENCES = "library"
        const val FAVOURITES = "favourites"
        const val RECENTS = "recents"
        // Instrument ids are "bank:program", so a comma never occurs inside one.
        const val SEPARATOR = ","
    }
}
