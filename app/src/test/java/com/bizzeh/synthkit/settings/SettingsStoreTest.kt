package com.bizzeh.synthkit.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bizzeh.synthkit.project.Quantise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") }
    }
    private val store by lazy { SettingsStore(dataStore) }

    @After
    fun closeStore() = scope.cancel()

    @Test
    fun aNewInstallReadsTheDefaults() = runBlocking {
        assertEquals(AppSettings(ThemeChoice.SYSTEM, haptics = true, clickInNewProjects = false, newTrackQuantise = Quantise.OFF), store.settings.first())
    }

    @Test
    fun everyChangeIsKept() = runBlocking {
        val changed = AppSettings(ThemeChoice.DARK, haptics = false, clickInNewProjects = true, newTrackQuantise = Quantise.SIXTEENTH)

        store.update { changed }

        assertEquals(changed, store.settings.first())
    }

    @Test
    fun anUnknownStoredValueReadsAsTheDefault() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("theme")] = "NEON"
            it[stringPreferencesKey("new_track_quantise")] = "THIRTY_SECOND"
        }

        assertEquals(AppSettings(), store.settings.first())
    }
}
