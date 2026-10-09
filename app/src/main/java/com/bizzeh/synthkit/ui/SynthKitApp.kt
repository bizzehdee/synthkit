package com.bizzeh.synthkit.ui

import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.bizzeh.synthkit.export.CreateExportDocument
import com.bizzeh.synthkit.export.ExportActions
import com.bizzeh.synthkit.export.ExportFormat
import com.bizzeh.synthkit.export.ExportPlan
import com.bizzeh.synthkit.export.ExportScreen
import com.bizzeh.synthkit.export.ExportState
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.bizzeh.synthkit.instruments.Instrument
import com.bizzeh.synthkit.instruments.InstrumentCatalogue
import com.bizzeh.synthkit.looper.TransportLcd
import com.bizzeh.synthkit.looper.PlayStopButton
import com.bizzeh.synthkit.looper.RecordButton
import com.bizzeh.synthkit.looper.SessionHost
import com.bizzeh.synthkit.looper.isPlaying
import com.bizzeh.synthkit.looper.isRecording
import com.bizzeh.synthkit.looper.trackChannel
import com.bizzeh.synthkit.project.ProjectScreen
import com.bizzeh.synthkit.project.ProjectValidation
import com.bizzeh.synthkit.project.Track
import com.bizzeh.synthkit.project.TrackActions
import kotlinx.coroutines.delay
import com.bizzeh.synthkit.audio.EngineState
import com.bizzeh.synthkit.audio.LatencyReport
import com.bizzeh.synthkit.audio.LatencyWarning
import com.bizzeh.synthkit.browser.BrowserScreen
import com.bizzeh.synthkit.editor.LoopEditorScreen
import com.bizzeh.synthkit.browser.Library
import com.bizzeh.synthkit.home.HomeScreen
import com.bizzeh.synthkit.instruments.PlayLayout
import com.bizzeh.synthkit.play.PlayScreen
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.ui.theme.StudioTheme
import com.bizzeh.synthkit.ui.theme.familyColor
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
    sessions: SessionHost,
    exportState: ExportState,
    exportActions: ExportActions,
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
                projects, projectActions, sessions, exportState, exportActions,
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
    sessions: SessionHost,
    exportState: ExportState,
    exportActions: ExportActions,
) {
    val stack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Route>(Route.Projects) }
    fun replaceTop(route: Route) {
        stack[stack.lastIndex] = route
    }
    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val route = stack.last()
    val projectId = route.projectId
    LaunchedEffect(projectId) { if (projectId == null) sessions.close() }

    if (route == Route.Projects) {
        ProjectListScreen(
            projects = projects,
            familyOf = { track -> engine.catalogue.byId("${track.bank}:${track.program}")?.family },
            onOpen = { stack.add(Route.Project(it)) },
            onCreate = { stack.add(Route.Project(projectActions.create().id)) },
            onRename = projectActions.rename,
            onDuplicate = projectActions.duplicate,
            onDelete = projectActions.delete,
        )
        return
    }

    val project = projects?.firstOrNull { it.project.id == projectId }?.project
    if (project == null) {
        LaunchedEffect(route) {
            stack.clear()
            stack.add(Route.Projects)
        }
        return
    }
    val session = remember(project.id) { sessions.open(project, engine.transport, engine.player, projectActions.save) }
    LaunchedEffect(session) {
        while (true) {
            session.poll()
            delay(POLL_MILLIS)
        }
    }
    val looper by session.state.collectAsState()
    val catalogue = engine.catalogue
    fun instrumentOf(track: Track) = catalogue.byId("${track.bank}:${track.program}")
    fun addTrack(instrument: Instrument) {
        session.addTrack(instrument.bank, instrument.program)?.let { replaceTop(Route.Track(project.id, it)) }
    }

    when (route) {
        is Route.Project -> ProjectScreen(
            state = looper,
            instrumentName = { instrumentOf(it)?.name.orEmpty() },
            familyOf = { instrumentOf(it)?.family },
            canAddTrack = looper.project.tracks.size < ProjectValidation.MAX_TRACKS,
            onBack = ::pop,
            onAddTrack = { stack.add(Route.AddTrack(project.id)) },
            onTempo = session::setTempo,
            onClickOnPlayback = session::setMetronomeOnPlayback,
            onPlayStop = session::playStop,
            onExport = { stack.add(Route.Export(project.id)) },
            tracks = TrackActions(
                open = { stack.add(Route.Track(project.id, it)) },
                setMuted = session::setMuted,
                setSolo = session::setSolo,
                setVolume = session::setVolume,
                undo = session::undoTake,
                clear = session::clearTrack,
                changeInstrument = { stack.add(Route.Browser(project.id, it)) },
                delete = session::deleteTrack,
                edit = { stack.add(Route.Editor(project.id, it)) },
            ),
        )
        is Route.AddTrack -> HomeScreen(
            title = stringResource(R.string.add_track),
            onOpenFamily = { family -> catalogue.quickEntry(family)?.let(::addTrack) },
            onBrowse = { stack.add(Route.Browser(project.id)) },
            onBack = ::pop,
            quickName = { catalogue.quickEntry(it)?.name },
        )
        is Route.Browser -> {
            val swapping = looper.project.tracks.firstOrNull { it.id == route.swapTrackId }
            val drums = swapping?.bank == InstrumentCatalogue.DRUM_KIT_BANK
            BrowserScreen(
                catalogue = catalogue,
                library = library,
                onOpen = { chosen ->
                    if (swapping == null) {
                        // Browser was opened from Add track; the new track replaces both screens.
                        pop()
                        addTrack(chosen)
                    } else {
                        session.swapInstrument(swapping.id, chosen.bank, chosen.program)
                        pop()
                    }
                },
                onToggleFavourite = onToggleFavourite,
                filter = { swapping == null || (it.bank == InstrumentCatalogue.DRUM_KIT_BANK) == drums },
            )
        }
        is Route.Track -> {
            val index = looper.project.tracks.indexOfFirst { it.id == route.trackId }
            val instrument = looper.project.tracks.getOrNull(index)?.let(::instrumentOf)
            if (instrument == null) {
                LaunchedEffect(route) { pop() }
                return
            }
            LaunchedEffect(instrument.id) { onInstrumentOpened(instrument.id) }
            PlayScreen(
                instrument = instrument,
                channel = trackChannel(index),
                player = engine.player,
                latency = latency,
                warning = warning,
                onDismissWarning = onDismissWarning,
                onBack = ::pop,
                onChangeInstrument = { stack.add(Route.Browser(project.id, route.trackId)) },
            ) {
                // The track view leaves the tempo to the project screen to fit a 640 dp phone.
                TransportLcd(looper, inTrack = true, showTempo = false)
                RecordButton(
                    recording = looper.isRecording && looper.recordingTrackId == route.trackId,
                    enabled = !looper.finishing,
                    onClick = { session.record(route.trackId) },
                )
                PlayStopButton(
                    playing = looper.isPlaying,
                    enabled = looper.isPlaying || looper.project.loopBars > 0,
                    onClick = session::playStop,
                )
            }
        }
        is Route.Editor -> {
            val track = looper.project.tracks.firstOrNull { it.id == route.trackId }
            if (track == null || looper.project.loopBars == 0) {
                LaunchedEffect(route) { pop() }
                return
            }
            DisposableEffect(session, track.id) {
                session.focusTrack(track.id)
                onDispose { session.focusTrack(null) }
            }
            LoopEditorScreen(
                color = instrumentOf(track)?.let { familyColor(it.family) } ?: StudioTheme.palette.muted,
                project = looper.project,
                track = track,
                instrumentName = instrumentOf(track)?.name.orEmpty(),
                onBack = ::pop,
                onQuantise = { session.setQuantise(track.id, it) },
                onNotes = { session.replaceNotes(track.id, it) },
                onDoubleLoop = session::doubleLoop,
                onAudition = { session.audition(track.id, it) },
                playhead = if (looper.isPlaying) session::playheadTick else null,
                transport = {
                    PlayStopButton(
                        playing = looper.isPlaying,
                        enabled = looper.isPlaying || looper.project.loopBars > 0,
                        onClick = session::playStop,
                    )
                },
            )
        }
        is Route.Export -> ExportRoute(
            project = looper.project,
            engine = engine,
            state = exportState,
            actions = exportActions,
            onBack = {
                exportActions.cancel()
                exportActions.reset()
                pop()
            },
        )
        Route.Projects -> Unit
    }
}

@Composable
private fun ExportRoute(
    project: Project,
    engine: EngineState.Ready,
    state: ExportState,
    actions: ExportActions,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var format by rememberSaveable { mutableStateOf(ExportFormat.WAV) }
    var passes by rememberSaveable { mutableIntStateOf(ExportPlan.DEFAULT_PASSES) }
    var message by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.export_saved)
    val saveFailed = stringResource(R.string.export_save_failed)
    val save = rememberLauncherForActivityResult(CreateExportDocument()) { uri ->
        uri?.let { actions.saveTo(it) { ok -> message = if (ok) saved else saveFailed } }
    }
    BackHandler(onBack = onBack)
    ExportScreen(
        projectName = project.name,
        state = state,
        format = format,
        passes = passes,
        onFormat = { format = it },
        onPasses = { passes = it.coerceIn(ExportPlan.MIN_PASSES, ExportPlan.MAX_PASSES) },
        onExport = {
            message = null
            actions.export(project, format, passes, engine.openExport) { bank, program ->
                engine.catalogue.byId("$bank:$program")?.name.orEmpty()
            }
        },
        onCancel = actions.cancel,
        onSave = {
            (state as? ExportState.Done)?.let { save.launch(it.format.mimeType to it.file.name) }
        },
        onShare = {
            (state as? ExportState.Done)?.let { done ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", done.file)
                val send = Intent(Intent.ACTION_SEND)
                    .setType(done.format.mimeType)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, null))
            }
        },
        onBack = onBack,
        message = message,
    )
}

// The session reads recorded notes and the clock this often while a project is open.
private const val POLL_MILLIS = 15L
