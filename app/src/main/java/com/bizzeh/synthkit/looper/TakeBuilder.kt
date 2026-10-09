package com.bizzeh.synthkit.looper

import com.bizzeh.synthkit.audio.RecordedEvent
import com.bizzeh.synthkit.project.Note
import com.bizzeh.synthkit.project.TICKS_PER_BEAT
import kotlin.math.roundToInt

/** Turns the live events of one take into loop notes. */
object TakeBuilder {
    /** Drum pads send no note off; their hits are stored as 1/16 notes. */
    const val ONE_SHOT_TICKS = TICKS_PER_BEAT / 4

    /**
     * Pairs each note on with the next note off of the same key. A note still
     * held at [stopTick] ends there. Positions wrap into the loop, so a note
     * played just before the loop start lands at the loop end.
     */
    fun build(events: List<RecordedEvent>, origin: Long, loopTicks: Int, stopTick: Long, oneShot: Boolean): List<Note> {
        val open = mutableMapOf<Int, ArrayDeque<RecordedEvent>>()
        val notes = mutableListOf<Note>()
        fun add(on: RecordedEvent, endTick: Long) {
            val length = if (oneShot) ONE_SHOT_TICKS.toLong() else endTick - on.tick
            notes += Note(
                tick = LoopMath.wrap(on.tick, origin, loopTicks),
                key = on.key,
                velocity = (on.velocity * 127).roundToInt().coerceIn(1, 127),
                lengthTicks = length.coerceIn(1, loopTicks - 1L).toInt(),
            )
        }
        events.filter { !it.isStopMarker }.forEach { event ->
            if (event.velocity > 0f) {
                if (oneShot) add(event, event.tick) else open.getOrPut(event.key) { ArrayDeque() }.addLast(event)
            } else {
                open[event.key]?.removeFirstOrNull()?.let { add(it, event.tick) }
            }
        }
        open.values.flatten().forEach { add(it, stopTick) }
        return notes.sortedBy { it.tick }
    }
}
