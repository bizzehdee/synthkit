package com.bizzeh.synthkit.settings

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

// DataStore allows one instance per file in a process, so it lives at file level.
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application.settingsDataStore)

    /** Null until the first read finishes, so the first frame never shows the wrong theme. */
    val settings: StateFlow<AppSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val actions = SettingsActions(
        setTheme = { theme -> update { it.copy(theme = theme) } },
        setHaptics = { on -> update { it.copy(haptics = on) } },
        setClickInNewProjects = { on -> update { it.copy(clickInNewProjects = on) } },
        setNewTrackQuantise = { quantise -> update { it.copy(newTrackQuantise = quantise) } },
    )

    // A failed write is logged, not fatal: the screen keeps the old value.
    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                store.update(transform)
            } catch (e: IOException) {
                Log.e(TAG, "event=settings_save_failed error=${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "Settings"
    }
}
