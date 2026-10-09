package com.bizzeh.synthkit.audio

fun interface NotePlayer {
    /** Returns false when the note was not accepted. */
    fun noteOn(channel: Int, key: Int, velocity: Float): Boolean
}
