package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.Quantise
import com.bizzeh.synthkit.project.Take
import com.bizzeh.synthkit.project.Track
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class MidiWriterTest {
    private data class Event(val tick: Long, val status: Int, val data: List<Int>)

    private class Midi(val format: Int, val division: Int, val tracks: List<List<Event>>)

    // Reads back what MidiWriter writes: no running status, meta events kept as status 0xFF.
    private fun parse(bytes: ByteArray): Midi {
        var at = 0
        fun u8() = bytes[at++].toInt() and 0xFF
        fun u16() = (u8() shl 8) or u8()
        fun u32() = (u16() shl 16) or u16()
        fun variable(): Long {
            var value = 0L
            while (true) {
                val b = u8()
                value = (value shl 7) or (b and 0x7F).toLong()
                if (b and 0x80 == 0) return value
            }
        }
        check(String(bytes, 0, 4) == "MThd")
        at = 8
        val format = u16()
        val count = u16()
        val division = u16()
        val tracks = (0 until count).map {
            check(String(bytes, at, 4) == "MTrk")
            at += 4
            val end = u32() + at
            var tick = 0L
            buildList {
                while (at < end) {
                    tick += variable()
                    val status = u8()
                    if (status == 0xFF) {
                        val type = u8()
                        val length = variable().toInt()
                        add(Event(tick, 0xFF, listOf(type) + (0 until length).map { u8() }))
                    } else {
                        val size = if (status and 0xF0 == 0xC0) 1 else 2
                        add(Event(tick, status, (0 until size).map { u8() }))
                    }
                }
            }
        }
        return Midi(format, division, tracks)
    }

    private val drums = Track("a", 128, 25, takes = listOf(Take(listOf(Note(130, 36, 100, 120)))), volume = 0.5f)
    private val bass = Track("b", 0, 33, takes = listOf(Take(listOf(Note(0, 40, 90, 480)))))
    private val project = Project(id = "p", name = "P", tempoBpm = 100, loopBars = 1, tracks = listOf(drums, bass))

    private fun written(project: Project = this.project, passes: Int = 2): Midi {
        val out = ByteArrayOutputStream()
        MidiWriter.write(project, passes, { bank, program -> "$bank:$program" }, out)
        return parse(out.toByteArray())
    }

    @Test
    fun type1WithATempoTrackAndOneTrackPerProjectTrack() {
        val midi = written()

        assertEquals(1, midi.format)
        assertEquals(480, midi.division)
        assertEquals(3, midi.tracks.size)
    }

    @Test
    fun tempoTrackHoldsTempoAndFourFour() {
        val tempo = written().tracks[0]

        // 60,000,000 / 100 BPM = 600,000 microseconds = 0x0927C0.
        assertEquals(Event(0, 0xFF, listOf(0x51, 0x09, 0x27, 0xC0)), tempo[0])
        assertEquals(listOf(0x58, 4, 2, 24, 8), tempo[1].data)
        assertEquals(listOf(0x2F), tempo.last().data)
    }

    @Test
    fun drumsUseChannelTenWithTheKitAsProgram() {
        val track = written().tracks[1]

        assertEquals(listOf(0x03) + "128:25".map { it.code }, track[0].data)
        assertEquals(Event(0, 0xC9, listOf(25)), track[1])
        assertEquals(Event(0, 0xB9, listOf(7, 64)), track[2])
        assertEquals(Event(130, 0x99, listOf(36, 100)), track[3])
    }

    @Test
    fun melodicTracksUseTheFirstFreeChannel() {
        val track = written().tracks[2]

        assertEquals(Event(0, 0xC0, listOf(33)), track[1])
    }

    @Test
    fun notesRepeatEveryPassWithOffsBeforeOnsAtTheSameTick() {
        val notes = written(passes = 2).tracks[2].filter { it.status and 0xE0 == 0x80 }

        assertEquals(
            listOf(
                Event(0, 0x90, listOf(40, 90)),
                Event(480, 0x80, listOf(40, 64)),
                Event(1920, 0x90, listOf(40, 90)),
                Event(2400, 0x80, listOf(40, 64)),
            ),
            notes,
        )
    }

    @Test
    fun quantiseAndMuteApplyAsOnPlayback() {
        val midi = written(project.copy(tracks = listOf(drums.copy(quantise = Quantise.SIXTEENTH), bass.copy(muted = true))), 1)

        assertEquals(2, midi.tracks.size)
        assertEquals(120, midi.tracks[1].first { it.status == 0x99 }.tick)
    }

    @Test
    fun variableLengthQuantitiesMatchTheSpecification() {
        assertArrayEquals(byteArrayOf(0x00), MidiWriter.variableLength(0))
        assertArrayEquals(byteArrayOf(0x7F), MidiWriter.variableLength(0x7F))
        assertArrayEquals(byteArrayOf(0x81.toByte(), 0x00), MidiWriter.variableLength(0x80))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F), MidiWriter.variableLength(0x0FFFFFFF))
    }
}
