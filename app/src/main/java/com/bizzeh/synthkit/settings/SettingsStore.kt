package com.bizzeh.synthkit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bizzeh.synthkit.project.Quantise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * [AppSettings] in a preferences file. A missing, unreadable or unknown value
 * reads as its default, so a damaged file never stops the app.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences -> write(preferences, transform(read(preferences))) }
    }

    private fun read(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = enumOrNull<ThemeChoice>(preferences[THEME]) ?: defaults.theme,
            haptics = preferences[HAPTICS] ?: defaults.haptics,
            clickInNewProjects = preferences[CLICK_IN_NEW_PROJECTS] ?: defaults.clickInNewProjects,
            newTrackQuantise = enumOrNull<Quantise>(preferences[NEW_TRACK_QUANTISE]) ?: defaults.newTrackQuantise,
        )
    }

    private fun write(preferences: MutablePreferences, settings: AppSettings) {
        preferences[THEME] = settings.theme.name
        preferences[HAPTICS] = settings.haptics
        preferences[CLICK_IN_NEW_PROJECTS] = settings.clickInNewProjects
        preferences[NEW_TRACK_QUANTISE] = settings.newTrackQuantise.name
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? = enumValues<T>().firstOrNull { it.name == name }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CLICK_IN_NEW_PROJECTS = booleanPreferencesKey("click_in_new_projects")
        val NEW_TRACK_QUANTISE = stringPreferencesKey("new_track_quantise")
    }
}
