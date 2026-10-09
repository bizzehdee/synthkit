#pragma once

#include <cstdint>

#include "SpscRing.h"

namespace synthkit {

struct SynthEvent {
    enum class Type : uint8_t {
        NoteOn,
        NoteOff,
        AllNotesOff,
        Program,
        Volume,
        TransportStart,
        TransportStop,
        Tempo,
        Click,
        Recording,
        Loop,
        Latency,
    };

    Type type;
    uint8_t channel;
    // Key for note events, program for Program, 0 or 1 for Click and Recording.
    uint8_t value;
    // Bank for Program, BPM for Tempo.
    uint16_t bank;
    // Velocity for notes, volume for Volume, milliseconds for Latency.
    float velocity;
    float delayMillis;
    // Loop origin in ticks for Loop.
    int64_t position;
    // Loop length in ticks for Loop.
    int32_t length;
    // Played by a person, so a recording captures it; false for loop playback.
    bool live;
};

using EventQueue = SpscRing<SynthEvent, 256>;

}  // namespace synthkit
