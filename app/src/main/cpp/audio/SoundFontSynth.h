#pragma once

#include <cstdint>
#include <memory>

#include "NoteQueue.h"

struct tsf;

namespace synthkit {

// Plays a SoundFont through TinySoundFont. Note calls may come from one
// producer thread; render() and setSampleRate() belong to the audio thread.
// TinySoundFont itself is only touched from render() once playing starts,
// because it is not thread safe.
class SoundFontSynth {
public:
    static constexpr int kMidiChannels = 16;
    static constexpr int kDrumChannel = 9;
    static constexpr int kMaxVoices = 64;

    // Takes ownership of font. Returns nullptr if font is null or cannot be
    // prepared for real-time use.
    static std::unique_ptr<SoundFontSynth> create(tsf* font);

    ~SoundFontSynth();
    SoundFontSynth(const SoundFontSynth&) = delete;
    SoundFontSynth& operator=(const SoundFontSynth&) = delete;

    // Must not run while render() runs.
    void setSampleRate(int32_t sampleRate);

    // Returns false when the arguments are out of range or the queue is full.
    bool noteOn(int channel, int key, float velocity);
    bool noteOff(int channel, int key);

    // Writes frameCount stereo-interleaved frames.
    void render(float* stereoOut, int32_t frameCount);

private:
    explicit SoundFontSynth(tsf* font);
    bool enqueue(int channel, int key, float velocity);

    tsf* font_;
    NoteQueue queue_;
};

}  // namespace synthkit
