#include "OfflineRenderer.h"

#include <algorithm>
#include <cmath>

#include "tsf.h"

namespace synthkit {

SharedFont::~SharedFont() {
    std::lock_guard<std::mutex> guard(lock);
    tsf_close(idle);
}

std::unique_ptr<OfflineRenderer> OfflineRenderer::create(std::shared_ptr<SharedFont> font, int maxVoices,
                                                         const ExportSpec& spec) {
    if (!font || spec.loopTicks <= 0 || spec.passes <= 0 || spec.sampleRate <= 0 || spec.bpm <= 0 ||
        spec.tailMillis < 0) {
        return nullptr;
    }
    tsf* copy = nullptr;
    {
        std::lock_guard<std::mutex> guard(font->lock);
        copy = tsf_copy(font->idle);
    }
    if (copy == nullptr) {
        return nullptr;
    }
    // tsf_copy keeps the voice limit but not the voices, so they are allocated again.
    bool ready = tsf_set_max_voices(copy, maxVoices) != 0;
    tsf_set_output(copy, TSF_STEREO_INTERLEAVED, spec.sampleRate, 0.0f);
    for (int channel = 0; ready && channel < 16; ++channel) {
        ready = tsf_channel_set_presetnumber(copy, channel, 0, 0) != 0;
    }
    for (const auto& track : spec.tracks) {
        ready = ready && tsf_channel_set_bank_preset(copy, track.channel, track.bank, track.program) != 0 &&
                tsf_channel_set_volume(copy, track.channel, track.volume) != 0;
    }
    if (!ready) {
        std::lock_guard<std::mutex> guard(font->lock);
        tsf_close(copy);
        return nullptr;
    }

    const double framesPerTick = spec.sampleRate * 60.0 / (static_cast<double>(spec.bpm) * kTicksPerBeat);
    std::vector<Event> events;
    events.reserve(spec.notes.size() * static_cast<size_t>(spec.passes));
    for (int pass = 0; pass < spec.passes; ++pass) {
        for (const auto& note : spec.notes) {
            const double tick = static_cast<double>(pass) * spec.loopTicks + static_cast<double>(note.tick);
            events.push_back({std::llround(tick * framesPerTick), note.channel, note.key, note.velocity});
        }
    }
    std::stable_sort(events.begin(), events.end(), [](const Event& a, const Event& b) {
        if (a.frame != b.frame) return a.frame < b.frame;
        return a.velocity == 0.0f && b.velocity > 0.0f;
    });
    const int64_t releaseFrame = std::llround(static_cast<double>(spec.passes) * spec.loopTicks * framesPerTick);
    const int64_t total = releaseFrame + static_cast<int64_t>(spec.tailMillis) * spec.sampleRate / 1000;
    return std::unique_ptr<OfflineRenderer>(new OfflineRenderer(std::move(font), copy, std::move(events), releaseFrame, total));
}

OfflineRenderer::OfflineRenderer(std::shared_ptr<SharedFont> font, tsf* copy, std::vector<Event> events,
                                 int64_t releaseFrame, int64_t totalFrames)
    : shared_(std::move(font)),
      font_(copy),
      events_(std::move(events)),
      releaseFrame_(releaseFrame),
      totalFrames_(totalFrames),
      scratch_(static_cast<size_t>(kBlockFrames) * 2),
      chunk_(static_cast<size_t>(kBlockFrames) * 2) {}

OfflineRenderer::~OfflineRenderer() {
    std::lock_guard<std::mutex> guard(shared_->lock);
    tsf_close(font_);
}

void OfflineRenderer::apply(const Event& event) {
    if (event.velocity > 0.0f) {
        tsf_channel_note_on(font_, event.channel, event.key, event.velocity);
    } else {
        tsf_channel_note_off(font_, event.channel, event.key);
    }
}

int32_t OfflineRenderer::render(int16_t* stereo, int32_t maxFrames) {
    int32_t written = 0;
    while (written < maxFrames) {
        if (chunkRead_ == chunkFrames_) {
            chunkFrames_ = renderChunk();
            chunkRead_ = 0;
            if (chunkFrames_ == 0) break;
        }
        const int32_t frames = std::min(maxFrames - written, chunkFrames_ - chunkRead_);
        std::copy_n(chunk_.data() + static_cast<ptrdiff_t>(chunkRead_) * 2, frames * 2,
                    stereo + static_cast<ptrdiff_t>(written) * 2);
        chunkRead_ += frames;
        written += frames;
    }
    delivered_ += written;
    return written;
}

// Renders the next chunk of up to kBlockFrames, stopping early only at a note
// event or the release point, so chunk edges depend on the music alone.
int32_t OfflineRenderer::renderChunk() {
    int32_t written = 0;
    while (written < kBlockFrames && position_ < totalFrames_) {
        // Everything still sounding is released once, at the end of the last pass.
        if (!released_ && position_ >= releaseFrame_) {
            tsf_note_off_all(font_);
            released_ = true;
        }
        while (next_ < events_.size() && events_[next_].frame <= position_) {
            if (events_[next_].frame < releaseFrame_) apply(events_[next_]);
            ++next_;
        }
        int64_t limit = std::min<int64_t>(kBlockFrames - written, totalFrames_ - position_);
        if (next_ < events_.size()) limit = std::min(limit, events_[next_].frame - position_);
        if (!released_) limit = std::min(limit, releaseFrame_ - position_);
        const auto frames = static_cast<int32_t>(std::max<int64_t>(limit, 1));
        tsf_render_float(font_, scratch_.data(), frames, 0);
        for (int32_t i = 0; i < frames * 2; ++i) {
            const float sample = std::clamp(scratch_[i], -1.0f, 1.0f);
            chunk_[static_cast<size_t>(written) * 2 + i] = static_cast<int16_t>(std::lrint(sample * 32767.0f));
        }
        written += frames;
        position_ += frames;
    }
    return written;
}

}  // namespace synthkit
