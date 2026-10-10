package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LooperSessionTest {
    private val transport = FakeTransport()
    private val player = FakePlayer()
    private val saved = mutableListOf<Project>()
    private var nextId = 0
    private val session = LooperSession(transport, player, Project(id = "p", name = "Song"), { saved += it }, { "t${nextId++}" })

    private val state get() = session.state.value
    private val project get() = state.project

    // Records a two-bar first take of kicks on the first track (channel 1), started just before beat 2.
    private fun recordFirstTake(): String {
        val track = session.addTrack(128, 0)!!
        session.record(track)
        transport.on(470, 1, 36)
        session.poll()
        transport.on(1440, 1, 36)
        session.record(track)
        transport.stopMarker(480 + 2 * 1920 + 100)
        session.poll()
        return track
    }

    @Test
    fun recordArmsTheClickAndWaitsForTheFirstNote() {
        val track = session.addTrack(0, 0)!!

        session.record(track)

        assertEquals(Phase.ARMED, state.phase)
        assertTrue(transport.running && transport.click && transport.recording)
    }

    @Test
    fun firstTakeSnapsToTheBeatRoundsToBarsAndLoopsAtOnce() {
        recordFirstTake()

        assertEquals(Phase.PLAYING, state.phase)
        assertEquals(2, project.loopBars)
        assertEquals(Triple(480L, 3840, true), transport.loop)
        // 470 is just before the snapped start at 480, so it wraps to the loop end.
        assertEquals(listOf(960, 3830), project.tracks[0].notes.map { it.tick }.sorted())
        assertFalse(transport.click)
        assertFalse(transport.recording)
        assertEquals(project, saved.last())
        assertEquals(4, transport.loopNotes.size)
    }

    @Test
    fun overdubAddsATakeThatUndoRemoves() {
        val track = recordFirstTake()

        session.record(track)
        assertEquals(Phase.OVERDUBBING, state.phase)
        assertTrue(transport.click)
        transport.on(480 + 3840 + 240, 1, 38)
        session.record(track)
        transport.stopMarker(480 + 3840 + 1000)
        session.poll()

        assertEquals(Phase.PLAYING, state.phase)
        assertEquals(2, project.tracks[0].takes.size)
        assertEquals(240, project.tracks[0].takes[1].notes.single().tick)

        session.undoTake(track)
        assertEquals(1, project.tracks[0].takes.size)
        session.undoTake(track)
        assertTrue(project.tracks[0].takes.isEmpty())
    }

    @Test
    fun stopDuringATakeEndsTheTakeThenStops() {
        val track = recordFirstTake()
        session.record(track)
        transport.on(480 + 3840 + 240, 1, 38)

        session.playStop()
        assertTrue(state.finishing)
        transport.stopMarker(480 + 3840 + 500)
        session.poll()

        assertEquals(Phase.STOPPED, state.phase)
        assertEquals(2, project.tracks[0].takes.size)
        assertFalse(transport.running)
    }

    @Test
    fun recordingAgainBeforeAnyNoteCancels() {
        val track = session.addTrack(0, 0)!!
        session.record(track)

        session.record(track)
        transport.stopMarker(500)
        session.poll()

        assertEquals(Phase.STOPPED, state.phase)
        assertEquals(0, project.loopBars)
    }

    @Test
    fun notesOnOtherChannelsAreNotRecorded() {
        val first = session.addTrack(0, 0)!!
        session.addTrack(0, 24)
        session.record(first)

        transport.on(480, 2, 60)
        session.poll()

        assertEquals(Phase.ARMED, state.phase)
    }

    @Test
    fun playFromStoppedRestartsTheLoopFromItsStart() {
        recordFirstTake()
        session.playStop()
        assertEquals(Phase.STOPPED, state.phase)

        session.playStop()

        assertEquals(Phase.PLAYING, state.phase)
        assertEquals(Triple(0L, 3840, true), transport.loop)
        assertTrue(transport.running)
    }

    @Test
    fun recordFromStoppedWithALoopOverdubsAtOnce() {
        val track = recordFirstTake()
        session.playStop()

        session.record(track)

        assertEquals(Phase.OVERDUBBING, state.phase)
        assertTrue(transport.running && transport.recording)
    }

    @Test
    fun barFollowsTheLoop() {
        recordFirstTake()
        transport.clock = 480.0 + 3840 + 100
        session.poll()
        assertEquals(0, state.bar)

        transport.clock = 480.0 + 3840 + 1920 + 100
        session.poll()

        assertEquals(1, state.bar)
        assertEquals(0, state.beat)
        transport.clock = 480.0 + 3840 + 1920 + 3 * 480 + 10
        session.poll()
        assertEquals(3, state.beat)
    }

    @Test
    fun beatIsClearedWhenStopped() {
        recordFirstTake()
        session.poll()

        session.playStop()

        assertEquals(null, state.beat)
    }

    @Test
    fun tracksGetChannelsInstrumentsAndAreLimitedToEight() {
        repeat(8) { assertTrue(session.addTrack(0, it) != null) }

        assertNull(session.addTrack(0, 9))
        assertFalse(session.canAddTrack)
        assertEquals(0 to 7, player.selected[8])
    }

    @Test
    fun deletingATrackShiftsTheOthersToNewChannels() {
        val first = session.addTrack(128, 0)!!
        session.addTrack(0, 24)

        session.deleteTrack(first)

        assertEquals(1, project.tracks.size)
        assertEquals(0 to 24, player.selected[1])
    }

    @Test
    fun muteAndSoloChangeWhatThePlaybackSnapshotHolds() {
        val track = recordFirstTake()

        session.setMuted(track, true)
        assertTrue(transport.loopNotes.isEmpty())
        session.setSolo(track, true)
        assertEquals(4, transport.loopNotes.size)
    }

    @Test
    fun volumeGoesToTheTracksChannelAndIsSaved() {
        val track = session.addTrack(0, 0)!!

        session.setVolume(track, 1.7f)

        assertTrue("volume 1 1.0" in transport.calls)
        assertEquals(1f, project.tracks[0].volume)
    }

    @Test
    fun instrumentSwapStaysWithinItsKind() {
        val drums = session.addTrack(128, 0)!!

        session.swapInstrument(drums, 0, 24)
        assertEquals(128, project.tracks[0].bank)
        session.swapInstrument(drums, 128, 25)
        assertEquals(25, project.tracks[0].program)
    }

    @Test
    fun tempoIsClampedAndSentToTheEngine() {
        session.setTempo(500)

        assertEquals(Project.MAX_TEMPO, project.tempoBpm)
        assertTrue("tempo 240" in transport.calls)
    }

    @Test
    fun doubleLoopRepeatsEveryNoteInTheNewHalf() {
        val track = recordFirstTake()

        session.doubleLoop()

        assertEquals(4, project.loopBars)
        assertEquals(listOf(960, 3830, 960 + 3840, 3830 + 3840), project.tracks[0].notes.map { it.tick }.sorted())
        assertEquals(Triple(480L, 7680, true), transport.loop)
        session.doubleLoop()
        session.doubleLoop()
        assertEquals(8, project.loopBars)
        session.undoTake(track)
    }

    @Test
    fun ensureLoopGivesAnEmptyProjectAOneBarLoopToEdit() {
        val track = session.addTrack(128, 0)!!

        session.ensureLoop()
        session.replaceNotes(track, listOf(Note(0, 36, 90, 120)))

        assertEquals(1, project.loopBars)
        assertEquals(listOf(Note(0, 36, 90, 120)), saved.last().tracks[0].notes)
    }

    @Test
    fun ensureLoopLeavesAnArmedFirstTakeAlone() {
        val track = session.addTrack(0, 0)!!
        session.record(track)

        session.ensureLoop()

        assertEquals(0, project.loopBars)
    }

    @Test
    fun ensureLoopKeepsTheRecordedLoopLength() {
        recordFirstTake()

        session.ensureLoop()

        assertEquals(2, project.loopBars)
    }

    @Test
    fun quantiseAndNoteEditsAreSaved() {
        val track = recordFirstTake()

        session.setQuantise(track, Quantise.EIGHTH)
        session.replaceNotes(track, listOf(Note(0, 36, 90, 120)))

        assertEquals(Quantise.EIGHTH, saved.last().tracks[0].quantise)
        assertEquals(listOf(Note(0, 36, 90, 120)), saved.last().tracks[0].notes)
    }

    @Test
    fun clearingRemovesEveryTake() {
        val track = recordFirstTake()

        session.clearTrack(track)

        assertTrue(project.tracks[0].takes.isEmpty())
        assertEquals(2, project.loopBars)
    }

    @Test
    fun focusPublishesOnlyTheFocusedTrackUntilCleared() {
        val drums = recordFirstTake()
        val keys = session.addTrack(0, 0)!!
        session.replaceNotes(keys, listOf(Note(0, 60, 100, 120)))

        session.focusTrack(drums)
        assertEquals(setOf(1), transport.loopNotes.map { it.channel }.toSet())
        session.focusTrack(null)

        assertEquals(setOf(1, 2), transport.loopNotes.map { it.channel }.toSet())
        assertTrue(saved.none { project -> project.tracks.any { it.solo || it.muted } })
    }

    @Test
    fun auditionPlaysTheNoteForItsLengthOnTheTrackChannel() {
        session.addTrack(0, 0)
        val keys = session.addTrack(0, 0)!!

        session.audition(keys, Note(0, 60, 127, 960))
        session.audition("gone", Note(0, 60, 127, 960))

        // Two beats at 120 BPM.
        assertEquals(listOf("on 2 60 1.0", "off 2 60 1000.0ms"), player.played)
    }

    @Test
    fun playheadFollowsTheClockFromTheLoopStartOnlyWhilePlaying() {
        recordFirstTake()
        transport.clock = 480.0 + 3840 + 100

        assertEquals(100, session.playheadTick())
        session.playStop()
        assertNull(session.playheadTick())
    }

    @Test
    fun aNewTrackStartsWithTheGivenQuantise() {
        val track = session.addTrack(0, 0, Quantise.EIGHTH)!!

        assertEquals(Quantise.EIGHTH, project.tracks.single { it.id == track }.quantise)
    }
}
