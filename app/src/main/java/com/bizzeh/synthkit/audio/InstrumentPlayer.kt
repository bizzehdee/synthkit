package com.bizzeh.synthkit.audio

/** A [NotePlayer] whose channels can switch to another SoundFont preset. */
interface InstrumentPlayer : NotePlayer {
    /** Silences the channel and selects the preset. Returns false when the preset does not exist. */
    fun selectInstrument(channel: Int, bank: Int, program: Int): Boolean

    /** Releases every note on the channel. */
    fun allNotesOff(channel: Int): Boolean
}
