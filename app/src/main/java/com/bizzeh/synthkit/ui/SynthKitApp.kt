package com.bizzeh.synthkit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bizzeh.synthkit.R
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.browser.BrowserScreen
import com.bizzeh.synthkit.browser.Library
import com.bizzeh.synthkit.home.HomeScreen
import com.bizzeh.synthkit.instruments.PlayLayout
import com.bizzeh.synthkit.play.PlayScreen
import com.bizzeh.synthkit.project.ProjectActions
import com.bizzeh.synthkit.project.ProjectListScreen
import com.bizzeh.synthkit.project.StoredProject

private val BackStackSaver = listSaver<SnapshotStateList<Route>, String>(
    save = { stack -> stack.map(Route::encode) },
    restore = { saved -> mutableStateListOf(*saved.map(Route::decode).toTypedArray()) },
)

@Composable
fun SynthKitApp(
    engineState: EngineState,
    latency: LatencyReport?,
    library: Library,
    onToggleFavourite: (String) -> Unit,
    onInstrumentOpened: (String) -> Unit,
    warning: LatencyWarning?,
    onDismissWarning: () -> Unit,
    projects: List<StoredProject>?,
    projectActions: ProjectActions,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
    ) {
        when (engineState) {
            EngineState.Loading -> {
                val loading = stringResource(R.string.loading_sounds)
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
            }
            EngineState.Failed -> Text(
                text = stringResource(R.string.sounds_failed),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            is EngineState.Ready -> Navigation(
                engineState, latency, library, onToggleFavourite, onInstrumentOpened, warning, onDismissWarning,
                projects, projectActions,
            )
        }
    }
}

@Composable
private fun Navigation(
    engine: EngineState.Ready,
    latency: LatencyReport?,
    library: Library,
    onToggleFavourite: (String) -> Unit,
    onInstrumentOpened: (String) -> Unit,
    warning: LatencyWarning?,
    onDismissWarning: () -> Unit,
    projects: List<StoredProject>?,
    projectActions: ProjectActions,
) {
    val stack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Route>(Route.Projects) }
    fun replaceWith(routes: List<Route>) {
        stack.clear()
        stack.addAll(routes)
    }

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    fun openInstrument(id: String) = replaceWith(Route.openInstrument(stack, id))

    when (val route = stack.last()) {
        Route.Projects -> ProjectListScreen(
            projects = projects,
            onOpen = { stack.add(Route.Project(it)) },
            onCreate = { stack.add(Route.Project(projectActions.create().id)) },
            onRename = projectActions.rename,
            onDuplicate = projectActions.duplicate,
            onDelete = projectActions.delete,
        )
        is Route.Project -> HomeScreen(
            onOpenFamily = { family -> engine.catalogue.quickEntry(family)?.let { openInstrument(it.id) } },
            onBrowse = { stack.add(Route.Browser) },
        )
        Route.Browser -> BrowserScreen(
            catalogue = engine.catalogue,
            library = library,
            onOpen = { openInstrument(it.id) },
            onToggleFavourite = onToggleFavourite,
        )
        is Route.Play -> {
            val instrument = engine.catalogue.byId(route.instrumentId)
            if (instrument == null) {
                LaunchedEffect(route) { replaceWith(listOf(Route.Projects)) }
            } else {
                PlayScreen(
                    instrument = instrument,
                    player = engine.player,
                    latency = latency,
                    onBack = { stack.removeAt(stack.lastIndex) },
                    onChangeInstrument = { stack.add(Route.Browser) },
                    onOpened = { onInstrumentOpened(it.id) },
                    kits = engine.catalogue.instruments.filter { it.layout == PlayLayout.DrumKit },
                    onOpenInstrument = { openInstrument(it.id) },
                    warning = warning,
                    onDismissWarning = onDismissWarning,
                )
            }
        }
    }
}
