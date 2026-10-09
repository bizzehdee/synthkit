package com.bizzeh.synthkit.audio

interface NotePlayer {
    /** Returns false when the note was not accepted. A delayed note plays [delayMillis] from now. */
    fun noteOn(channel: Int, key: Int, velocity: Float, delayMillis: Float = 0f): Boolean

    fun noteOff(channel: Int, key: Int, delayMillis: Float = 0f): Boolean
}
