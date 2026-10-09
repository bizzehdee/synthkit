#pragma once

#include <cstdint>
#include <memory>
#include <vector>

#include "SharedFont.h"

struct tsf;

namespace synthkit {

struct ExportTrack {
    uint8_t channel;
    uint16_t bank;
    uint8_t program;
    float volume;
};

// [tick] is inside one pass and may pass the loop end for a note off. velocity 0 is a note off.
struct ExportNote {
    int64_t tick;
    uint8_t channel;
    uint8_t key;
    float velocity;
};

struct ExportSpec {
    int32_t sampleRate;
    int bpm;
    int32_t loopTicks;
    int passes;
    int32_t tailMillis;
    std::vector<ExportTrack> tracks;
    std::vector<ExportNote> notes;
};

// Renders a project to 16-bit stereo, block by block, on its own SoundFont
// copy, so live playing continues. Memory does not grow with export length.
class OfflineRenderer {
public:
    static constexpr int32_t kBlockFrames = 4096;
    static constexpr int kTicksPerBeat = 480;

    // Returns nullptr if the copy cannot be prepared.
    static std::unique_ptr<OfflineRenderer> create(std::shared_ptr<SharedFont> font, int maxVoices,
                                                    const ExportSpec& spec);
    ~OfflineRenderer();
    OfflineRenderer(const OfflineRenderer&) = delete;
    OfflineRenderer& operator=(const OfflineRenderer&) = delete;

    // Writes up to [maxFrames] interleaved stereo frames; returns how many, 0 at
    // the end. The audio does not depend on [maxFrames]: TinySoundFont updates
    // envelopes per internal block, so rendering is always done in the same
    // chunks and callers are served from them.
    int32_t render(int16_t* stereo, int32_t maxFrames);

    int64_t totalFrames() const { return totalFrames_; }
    int64_t renderedFrames() const { return delivered_; }

private:
    struct Event {
        int64_t frame;
        uint8_t channel;
        uint8_t key;
        float velocity;
    };

    OfflineRenderer(std::shared_ptr<SharedFont> font, tsf* copy, std::vector<Event> events, int64_t releaseFrame,
                    int64_t totalFrames);
    void apply(const Event& event);
    int32_t renderChunk();

    std::shared_ptr<SharedFont> shared_;
    tsf* font_;
    std::vector<Event> events_;
    size_t next_ = 0;
    int64_t releaseFrame_;
    bool released_ = false;
    int64_t totalFrames_;
    int64_t position_ = 0;
    int64_t delivered_ = 0;
    std::vector<float> scratch_;
    std::vector<int16_t> chunk_;
    int32_t chunkFrames_ = 0;
    int32_t chunkRead_ = 0;
};

}  // namespace synthkit
