package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.InstrumentPlayer
import com.bizzeh.synthkit.audio.RecordedEvent
import com.bizzeh.synthkit.audio.Transport
import com.bizzeh.synthkit.instruments.InstrumentCatalogue
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.ProjectValidation
import com.bizzeh.synthkit.project.BEATS_PER_BAR
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.TICKS_PER_BEAT
import com.bizzeh.synthkit.project.TICKS_PER_BAR
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Phase {
    /** No clock running. */
    STOPPED,

    /** Click running, waiting for the first note of the first take. */
    ARMED,

    /** First take running; its length becomes the loop. */
    RECORDING_FIRST,

    /** Loop playing. */
    PLAYING,

    /** Loop playing while a track records a new take. */
    OVERDUBBING,
}

data class LooperState(
    val project: Project,
    val phase: Phase = Phase.STOPPED,
    val recordingTrackId: String? = null,
    /** Waiting for the engine to confirm where recording stopped. */
    val finishing: Boolean = false,
    /**
     * Bar being played, from 0, counted from the loop start or the first take's
     * start. Only the bar is kept so the screen redraws once a bar, not every poll.
     */
    val bar: Int = 0,
    /** Beat in the bar, 0 to 3, while the clock runs; null when stopped. */
    val beat: Int? = null,
)

/**
 * The looper-pedal flow for one project. Every change to the project is
 * reported through [onProjectChange] so it is saved. Call [poll] often (every
 * 20 ms or so) while the session is open: it reads what the engine recorded.
 * Not thread safe; use it from the main thread.
 */
class LooperSession(
    private val transport: Transport,
    private val player: InstrumentPlayer,
    project: Project,
    private val onProjectChange: (Project) -> Unit,
    private val newId: () -> String,
) {
    private val mutableState = MutableStateFlow(LooperState(project))
    val state: StateFlow<LooperState> = mutableState.asStateFlow()

    private val project: Project get() = mutableState.value.project
    private val take = mutableListOf<RecordedEvent>()
    private var takeStart = 0L
    private var loopOrigin = 0L
    private var stopAfterFinish = false
    private var focusTrackId: String? = null

    init {
        transport.setTempo(project.tempoBpm)
        applyTracks()
    }

    // Recording ------------------------------------------------------------

    /** Record button: arms, starts or ends a take on [trackId]. */
    fun record(trackId: String) {
        val state = mutableState.value
        if (state.finishing || project.tracks.none { it.id == trackId }) return
        when (state.phase) {
            Phase.STOPPED -> {
                transport.setTempo(project.tempoBpm)
                transport.setClick(true)
                if (project.loopBars == 0) {
                    transport.setLoop(0, 0, playing = false)
                    transport.startTransport()
                    transport.setRecording(true)
                    update { it.copy(phase = Phase.ARMED, recordingTrackId = trackId) }
                } else {
                    loopOrigin = 0
                    transport.setLoop(0, project.loopTicks, playing = true)
                    transport.startTransport()
                    startOverdub(trackId)
                }
            }
            Phase.PLAYING -> {
                transport.setClick(true)
                startOverdub(trackId)
            }
            Phase.ARMED -> stopEverything()
            Phase.RECORDING_FIRST, Phase.OVERDUBBING -> {
                transport.setRecording(false)
                update { it.copy(finishing = true) }
            }
        }
    }

    /** Play/Stop button. Stopping during a take ends the take first. */
    fun playStop() {
        val state = mutableState.value
        when (state.phase) {
            Phase.STOPPED -> if (project.loopBars > 0) {
                loopOrigin = 0
                transport.setTempo(project.tempoBpm)
                transport.setClick(project.metronomeOnPlayback)
                transport.setLoop(0, project.loopTicks, playing = true)
                transport.startTransport()
                update { it.copy(phase = Phase.PLAYING, bar = 0) }
            }
            Phase.PLAYING, Phase.ARMED -> stopEverything()
            Phase.RECORDING_FIRST, Phase.OVERDUBBING -> {
                stopAfterFinish = true
                if (!state.finishing) record(state.recordingTrackId ?: return)
            }
        }
    }

    /**
     * Where the loop is now, in ticks from the loop start, or null when no loop
     * plays. Read it while drawing, not into UI state: it changes every frame.
     */
    fun playheadTick(): Int? = when (mutableState.value.phase) {
        Phase.PLAYING, Phase.OVERDUBBING -> LoopMath.wrap(transport.clockTicks().toLong(), loopOrigin, project.loopTicks)
        else -> null
    }

    /** Reads recorded notes and the clock. */
    fun poll() {
        transport.drainRecorded().forEach(::onRecorded)
        val clock = transport.clockTicks().toLong()
        val position = when (mutableState.value.phase) {
            Phase.PLAYING, Phase.OVERDUBBING -> LoopMath.wrap(clock, loopOrigin, project.loopTicks).toLong()
            Phase.RECORDING_FIRST -> (clock - takeStart).coerceAtLeast(0)
            else -> 0L
        }
        val bar = (position / TICKS_PER_BAR).toInt()
        val beat = when (mutableState.value.phase) {
            Phase.STOPPED -> null
            // Armed: the click counts from when recording was armed.
            Phase.ARMED -> ((clock / TICKS_PER_BEAT) % BEATS_PER_BAR).toInt()
            else -> ((position / TICKS_PER_BEAT) % BEATS_PER_BAR).toInt()
        }
        if (bar != mutableState.value.bar || beat != mutableState.value.beat) update { it.copy(bar = bar, beat = beat) }
    }

    private fun onRecorded(event: RecordedEvent) {
        val state = mutableState.value
        val channel = recordingChannel() ?: return
        if (event.isStopMarker) {
            when (state.phase) {
                Phase.RECORDING_FIRST -> finishFirstTake(event.tick)
                Phase.OVERDUBBING -> finishOverdub(event.tick)
                else -> Unit
            }
            return
        }
        if (event.channel != channel) return
        when (state.phase) {
            Phase.ARMED -> if (event.velocity > 0f) {
                takeStart = LoopMath.snapToBeat(event.tick)
                take.clear()
                take += event
                update { it.copy(phase = Phase.RECORDING_FIRST) }
            }
            Phase.RECORDING_FIRST, Phase.OVERDUBBING -> take += event
            else -> Unit
        }
    }

    private fun startOverdub(trackId: String) {
        take.clear()
        transport.setRecording(true)
        update { it.copy(phase = Phase.OVERDUBBING, recordingTrackId = trackId) }
    }

    private fun finishFirstTake(stopTick: Long) {
        val bars = LoopMath.roundedBars(stopTick - takeStart)
        loopOrigin = takeStart
        val withLoop = project.copy(loopBars = bars)
        commitTake(withLoop, stopTick)
        transport.setLoop(loopOrigin, project.loopTicks, playing = true)
        afterTake()
    }

    private fun finishOverdub(stopTick: Long) {
        commitTake(project, stopTick)
        afterTake()
    }

    private fun commitTake(base: Project, stopTick: Long) {
        val trackId = mutableState.value.recordingTrackId
        val track = base.tracks.firstOrNull { it.id == trackId }
        val notes = track?.let {
            TakeBuilder.build(take, loopOrigin, base.loopTicks, stopTick, oneShot = it.bank == InstrumentCatalogue.DRUM_KIT_BANK)
        }.orEmpty()
        take.clear()
        val updated = if (notes.isEmpty() || track == null) base else base.withTrack(track.copy(takes = track.takes + Take(notes)))
        change(updated)
    }

    private fun afterTake() {
        transport.setClick(project.metronomeOnPlayback)
        update { it.copy(phase = Phase.PLAYING, finishing = false) }
        if (stopAfterFinish) {
            stopAfterFinish = false
            stopEverything()
        }
    }

    /** Stops the clock and every note; used when the project is closed. */
    fun close() {
        if (mutableState.value.phase != Phase.STOPPED) stopEverything()
    }

    private fun stopEverything() {
        transport.setRecording(false)
        transport.stopTransport()
        transport.setClick(false)
        project.tracks.indices.forEach { player.allNotesOff(trackChannel(it)) }
        take.clear()
        update { it.copy(phase = Phase.STOPPED, recordingTrackId = null, finishing = false, bar = 0, beat = null) }
    }

    private fun recordingChannel(): Int? =
        project.tracks.indexOfFirst { it.id == mutableState.value.recordingTrackId }.takeIf { it >= 0 }?.let(::trackChannel)

    // Project changes -------------------------------------------------------

    val canAddTrack: Boolean get() = project.tracks.size < ProjectValidation.MAX_TRACKS

    /** Adds a track and returns its id, or null at the track limit. */
    fun addTrack(bank: Int, program: Int, quantise: Quantise = Quantise.OFF): String? {
        if (!canAddTrack) return null
        val track = Track(id = newId(), bank = bank, program = program, quantise = quantise)
        change(project.copy(tracks = project.tracks + track))
        applyTracks()
        return track.id
    }

    fun deleteTrack(trackId: String) {
        if (mutableState.value.recordingTrackId == trackId && mutableState.value.phase != Phase.PLAYING) stopEverything()
        project.tracks.indices.forEach { player.allNotesOff(trackChannel(it)) }
        change(project.copy(tracks = project.tracks.filter { it.id != trackId }))
        applyTracks()
    }

    /** Removes the newest take; call again to go further back. */
    fun undoTake(trackId: String) = editTrack(trackId) { it.copy(takes = it.takes.dropLast(1)) }

    fun clearTrack(trackId: String) = editTrack(trackId) { it.copy(takes = emptyList()) }

    fun setMuted(trackId: String, muted: Boolean) = editTrack(trackId) { it.copy(muted = muted) }

    fun setSolo(trackId: String, solo: Boolean) = editTrack(trackId) { it.copy(solo = solo) }

    fun setQuantise(trackId: String, quantise: Quantise) = editTrack(trackId) { it.copy(quantise = quantise) }

    fun setVolume(trackId: String, volume: Float) {
        val index = project.tracks.indexOfFirst { it.id == trackId }.takeIf { it >= 0 } ?: return
        val clamped = volume.coerceIn(0f, 1f)
        transport.setVolume(trackChannel(index), clamped)
        change(project.withTrack(project.tracks[index].copy(volume = clamped)), publish = false)
    }

    /** Swaps the instrument; the caller offers only instruments of the same kind (kit or melodic). */
    fun swapInstrument(trackId: String, bank: Int, program: Int) {
        val track = project.tracks.firstOrNull { it.id == trackId } ?: return
        val drum = InstrumentCatalogue.DRUM_KIT_BANK
        if ((track.bank == drum) != (bank == drum)) return
        change(project.withTrack(track.copy(bank = bank, program = program)), publish = false)
        applyTracks()
    }

    fun setTempo(bpm: Int) {
        val clamped = bpm.coerceIn(Project.MIN_TEMPO, Project.MAX_TEMPO)
        transport.setTempo(clamped)
        change(project.copy(tempoBpm = clamped), publish = false)
    }

    fun setMetronomeOnPlayback(on: Boolean) {
        if (mutableState.value.phase == Phase.PLAYING) transport.setClick(on)
        change(project.copy(metronomeOnPlayback = on), publish = false)
    }

    /** Replaces a track's notes from the loop editor as a single take. */
    fun replaceNotes(trackId: String, notes: List<Note>) =
        editTrack(trackId) { it.copy(takes = if (notes.isEmpty()) emptyList() else listOf(Take(notes))) }

    /** Plays only [trackId] in the loop, or every audible track again when null. Not saved. */
    fun focusTrack(trackId: String?) {
        if (focusTrackId == trackId) return
        focusTrackId = trackId
        publishLoop()
    }

    /** Plays one note on [trackId]'s instrument, for its length at the project tempo, so an edit is heard. */
    fun audition(trackId: String, note: Note) {
        val index = project.tracks.indexOfFirst { it.id == trackId }.takeIf { it >= 0 } ?: return
        val channel = trackChannel(index)
        val millis = note.lengthTicks * MILLIS_PER_MINUTE / (TICKS_PER_BEAT * project.tempoBpm)
        player.noteOn(channel, note.key, note.velocity.coerceIn(1, 127) / 127f)
        player.noteOff(channel, note.key, millis)
    }

    /** Gives a project with no take yet a one-bar loop, so a track can be edited before anything is recorded. */
    fun ensureLoop() {
        if (project.loopBars > 0 || mutableState.value.phase != Phase.STOPPED) return
        change(project.copy(loopBars = 1))
    }

    /** Doubles the loop up to 8 bars, repeating every track's notes in the new half. */
    fun doubleLoop() {
        val bars = project.loopBars
        if (bars == 0 || bars * 2 > Project.MAX_LOOP_BARS) return
        val shift = bars * TICKS_PER_BAR
        val doubled = project.copy(
            loopBars = bars * 2,
            tracks = project.tracks.map { track ->
                track.copy(takes = track.takes.map { take -> Take(take.notes + take.notes.map { it.copy(tick = it.tick + shift) }) })
            },
        )
        change(doubled)
        if (mutableState.value.phase == Phase.PLAYING || mutableState.value.phase == Phase.OVERDUBBING) {
            transport.setLoop(loopOrigin, project.loopTicks, playing = true)
        }
    }

    private fun editTrack(trackId: String, edit: (Track) -> Track) {
        val track = project.tracks.firstOrNull { it.id == trackId } ?: return
        change(project.withTrack(edit(track)))
    }

    private fun change(updated: Project, publish: Boolean = true) {
        update { it.copy(project = updated) }
        onProjectChange(updated)
        if (publish) publishLoop()
    }

    private fun publishLoop() {
        transport.publishLoopNotes(LoopSnapshot.build(project, focusTrackId))
        // Notes of the old loop may be sounding; their note offs are gone.
        project.tracks.indices.forEach { player.allNotesOff(trackChannel(it)) }
    }

    private fun applyTracks() {
        project.tracks.forEachIndexed { index, track ->
            player.selectInstrument(trackChannel(index), track.bank, track.program)
            transport.setVolume(trackChannel(index), track.volume)
        }
        publishLoop()
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000f
    }

    private fun update(transform: (LooperState) -> LooperState) {
        mutableState.value = transform(mutableState.value)
    }
}

private fun Project.withTrack(track: Track): Project = copy(tracks = tracks.map { if (it.id == track.id) track else it })
