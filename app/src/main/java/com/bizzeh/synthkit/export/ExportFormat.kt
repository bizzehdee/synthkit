package com.bizzeh.synthkit.export

enum class ExportFormat(val extension: String, val mimeType: String) {
    MIDI("mid", "audio/midi"),
    WAV("wav", "audio/wav"),
    MP3("mp3", "audio/mpeg"),
    FLAC("flac", "audio/flac"),
    AAC("m4a", "audio/mp4"),
}
