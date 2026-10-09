package com.bizzeh.synthkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bizzeh.synthkit.audio.AudioEngineViewModel
import com.bizzeh.synthkit.browser.LibraryViewModel
import com.bizzeh.synthkit.looper.SessionViewModel
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.ProjectsViewModel
import com.bizzeh.synthkit.ui.LandscapeOnly
import com.bizzeh.synthkit.ui.SynthKitApp
import com.bizzeh.synthkit.ui.theme.SynthKitTheme

class MainActivity : ComponentActivity() {
    private val audio: AudioEngineViewModel by viewModels()
    private val library: LibraryViewModel by viewModels()
    private val projects: ProjectsViewModel by viewModels()
    private val sessions: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SynthKitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LandscapeOnly {
                        val state by audio.state.collectAsState()
                        val latency by audio.latency.collectAsState()
                        val libraryState by library.library.collectAsState()
                        val warning by audio.warning.collectAsState()
                        val projectList by projects.projects.collectAsState()
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
                        )
                    }
                }
            }
        }
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
