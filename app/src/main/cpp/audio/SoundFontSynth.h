#pragma once

#include <array>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "EventQueue.h"

struct tsf;

namespace synthkit {

struct PresetInfo {
    int bank;
    int program;
    std::string name;
};

// Plays a SoundFont through TinySoundFont. Event calls may come from one
// producer thread; render() and setSampleRate() belong to the audio thread.
// TinySoundFont itself is only touched from render() once playing starts,
// because it is not thread safe.
class SoundFontSynth {
public:
    static constexpr int kMidiChannels = 16;
    static constexpr int kDrumChannel = 9;
    static constexpr int kDrumBank = 128;
    static constexpr int kMaxVoices = 64;
    static constexpr size_t kMaxScheduledEvents = 256;

    // Takes ownership of font. Returns nullptr if font is null or cannot be
    // prepared for real-time use.
    static std::unique_ptr<SoundFontSynth> create(tsf* font);

    ~SoundFontSynth();
    SoundFontSynth(const SoundFontSynth&) = delete;
    SoundFontSynth& operator=(const SoundFontSynth&) = delete;

    // Must not run while render() runs.
    void setSampleRate(int32_t sampleRate);

    // Each returns false when the arguments are out of range, the preset does
    // not exist, or the event queue is full. A delayed event plays that many
    // milliseconds after the start of the next rendered block.
    bool noteOn(int channel, int key, float velocity, float delayMillis = 0.0f);
    bool noteOff(int channel, int key, float delayMillis = 0.0f);
    bool allNotesOff(int channel);
    bool programChange(int channel, int bank, int program);

    // Preset data is fixed after loading, so this is safe from any thread.
    std::vector<PresetInfo> presets() const;

    // Writes frameCount stereo-interleaved frames.
    void render(float* stereoOut, int32_t frameCount);

private:
    struct ScheduledEvent {
        uint64_t dueFrame;
        SynthEvent event;
    };

    explicit SoundFontSynth(tsf* font);
    bool enqueue(const SynthEvent& event);
    void schedule(const SynthEvent& event);
    void apply(const SynthEvent& event);

    tsf* font_;
    EventQueue queue_;
    int32_t sampleRate_ = 0;
    uint64_t frameClock_ = 0;
    // Sorted by dueFrame; events due on the same frame keep arrival order.
    std::array<ScheduledEvent, kMaxScheduledEvents> scheduled_{};
    size_t scheduledCount_ = 0;
};

}  // namespace synthkit
