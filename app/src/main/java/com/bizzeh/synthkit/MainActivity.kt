package com.bizzeh.synthkit

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bizzeh.synthkit.audio.AudioEngineViewModel
import com.bizzeh.synthkit.browser.LibraryViewModel
import com.bizzeh.synthkit.export.ExportActions
import com.bizzeh.synthkit.export.ExportViewModel
import com.bizzeh.synthkit.looper.SessionViewModel
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.ProjectsViewModel
import com.bizzeh.synthkit.settings.SettingsViewModel
import com.bizzeh.synthkit.settings.ThemeChoice
import com.bizzeh.synthkit.ui.LandscapeOnly
import com.bizzeh.synthkit.ui.SynthKitApp
import com.bizzeh.synthkit.ui.theme.SynthKitTheme

class MainActivity : ComponentActivity() {
    private val audio: AudioEngineViewModel by viewModels()
    private val library: LibraryViewModel by viewModels()
    private val projects: ProjectsViewModel by viewModels()
    private val sessions: SessionViewModel by viewModels()
    private val exports: ExportViewModel by viewModels()
    private val settingsModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsModel.settings.collectAsState()
            val dark = when (settings?.theme) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM, null -> isSystemInDarkTheme()
            }
            // The bars follow the app's theme, which can differ from the phone's mode.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
            }
            SynthKitTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Settings load in a few milliseconds; waiting avoids a flash of the wrong theme.
                    val loaded = settings ?: return@Surface
                    LandscapeOnly {
                        val state by audio.state.collectAsState()
                        val latency by audio.latency.collectAsState()
                        val libraryState by library.library.collectAsState()
                        val warning by audio.warning.collectAsState()
                        val projectList by projects.projects.collectAsState()
                        val exportState by exports.state.collectAsState()
                        SynthKitApp(
                            engineState = state,
                            latency = latency.takeIf { BuildConfig.DEBUG },
                            library = libraryState,
                            onToggleFavourite = library::toggleFavourite,
                            onInstrumentOpened = library::opened,
                            warning = warning,
                            onDismissWarning = audio::dismissWarning,
                            projects = projectList,
                            projectActions = ProjectActions(
                                create = projects::create,
                                rename = projects::rename,
                                duplicate = projects::duplicate,
                                delete = projects::delete,
                                save = projects::update,
                            ),
                            sessions = sessions,
                            exportState = exportState,
                            exportActions = ExportActions(
                                export = exports::export,
                                cancel = exports::cancel,
                                reset = exports::reset,
                                saveTo = exports::saveTo,
                            ),
                            settings = loaded,
                            settingsActions = settingsModel.actions,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        // The scrims enableEdgeToEdge uses by default for button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    override fun onStart() {
        super.onStart()
        audio.onVisible()
    }

    override fun onStop() {
        audio.onHidden()
        super.onStop()
    }
}
