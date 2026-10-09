package com.bizzeh.synthkit.export

import com.bizzeh.synthkit.instruments.InstrumentCatalogue
import com.bizzeh.synthkit.looper.LoopMath
import com.bizzeh.synthkit.looper.LoopSnapshot
import com.bizzeh.synthkit.project.BEATS_PER_BAR
import com.bizzeh.synthkit.project.Project
import com.bizzeh.synthkit.project.TICKS_PER_BEAT
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * Standard MIDI File, Type 1: a tempo track, then one track per audible project
 * track. Ticks are 480 per quarter note, as in the project. Drums use channel 10
 * (index 9) with the kit as a GS program change; melodic tracks use the others.
 */
object MidiWriter {
    const val DRUM_CHANNEL = 9
    private const val MICROSECONDS_PER_MINUTE = 60_000_000
    private const val VOLUME_CONTROLLER = 7
    private const val NOTE_OFF_VELOCITY = 64

    private data class Event(val tick: Long, val order: Int, val bytes: ByteArray)

    fun write(project: Project, passes: Int, trackName: (bank: Int, program: Int) -> String, out: OutputStream) {
        require(project.loopBars > 0) { "Nothing to export before the first take" }
        require(passes in ExportPlan.MIN_PASSES..ExportPlan.MAX_PASSES) { "Passes out of range: $passes" }
        val audible = LoopSnapshot.audible(project).map { it.id }.toSet()
        val tracks = project.tracks.filter { it.id in audible }
        val melodicChannels = (0 until 16).filter { it != DRUM_CHANNEL }.iterator()

        out.write("MThd".toByteArray(Charsets.US_ASCII))
        out.write(int32(6))
        out.write(int16(1))
        out.write(int16(tracks.size + 1))
        out.write(int16(TICKS_PER_BEAT))
        out.write(chunk(tempoTrack(project.tempoBpm)))

        tracks.forEach { track ->
            val drums = track.bank == InstrumentCatalogue.DRUM_KIT_BANK
            val channel = if (drums) DRUM_CHANNEL else melodicChannels.next()
            val events = mutableListOf(
                Event(0, 0, meta(0x03, trackName(track.bank, track.program).toByteArray(Charsets.UTF_8))),
                Event(0, 1, byteArrayOf((0xC0 or channel).toByte(), track.program.toByte())),
                Event(0, 2, byteArrayOf((0xB0 or channel).toByte(), VOLUME_CONTROLLER.toByte(), (track.volume * 127).roundToInt().toByte())),
            )
            repeat(passes) { pass ->
                val offset = pass.toLong() * project.loopTicks
                track.notes.forEach { note ->
                    val start = offset + LoopMath.quantise(note.tick, track.quantise.gridTicks, project.loopTicks)
                    events += Event(start, 4, byteArrayOf((0x90 or channel).toByte(), note.key.toByte(), note.velocity.toByte()))
                    events += Event(start + note.lengthTicks, 3, byteArrayOf((0x80 or channel).toByte(), note.key.toByte(), NOTE_OFF_VELOCITY.toByte()))
                }
            }
            out.write(chunk(encode(events)))
        }
    }

    private fun tempoTrack(bpm: Int): ByteArray {
        val tempo = MICROSECONDS_PER_MINUTE / bpm
        return encode(
            listOf(
                Event(0, 0, meta(0x51, byteArrayOf((tempo shr 16).toByte(), (tempo shr 8).toByte(), tempo.toByte()))),
                // 4/4, a click every quarter note, 8 thirty-second notes per quarter.
                Event(0, 1, meta(0x58, byteArrayOf(BEATS_PER_BAR.toByte(), 2, 24, 8))),
            ),
        )
    }

    // Events sorted by tick; at one tick note offs (order 3) come before note ons (4).
    private fun encode(events: List<Event>): ByteArray {
        val body = ByteArrayOutputStream()
        var last = 0L
        events.sortedWith(compareBy({ it.tick }, { it.order })).forEach { event ->
            body.write(variableLength(event.tick - last))
            body.write(event.bytes)
            last = event.tick
        }
        body.write(variableLength(0))
        body.write(meta(0x2F, ByteArray(0)))
        return body.toByteArray()
    }

    private fun chunk(body: ByteArray): ByteArray =
        "MTrk".toByteArray(Charsets.US_ASCII) + int32(body.size) + body

    private fun meta(type: Int, data: ByteArray): ByteArray =
        byteArrayOf(0xFF.toByte(), type.toByte()) + variableLength(data.size.toLong()) + data

    internal fun variableLength(value: Long): ByteArray {
        require(value in 0..0x0FFFFFFF) { "Out of range for a MIDI length: $value" }
        val bytes = mutableListOf((value and 0x7F).toByte())
        var rest = value shr 7
        while (rest > 0) {
            bytes.add(0, ((rest and 0x7F) or 0x80).toByte())
            rest = rest shr 7
        }
        return bytes.toByteArray()
    }

    private fun int32(value: Int) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    private fun int16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())
}
