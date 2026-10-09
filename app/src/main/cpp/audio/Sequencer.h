#pragma once

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <vector>

#include "SpscRing.h"

namespace synthkit {

// velocity 0 is a note off.
struct LoopNote {
    int32_t tick;
    uint8_t channel;
    uint8_t key;
    float velocity;
};

// Sorted by tick; at the same tick, note offs come before note ons.
struct LoopNotes {
    std::vector<LoopNote> notes;
};

struct RecordedEvent {
    int64_t tick;
    uint8_t channel;
    uint8_t key;
    float velocity;
};

// Transport clock, loop playback and metronome. Ticks are 480 per beat. Control
// and collect() run on the audio thread; publishLoopNotes(), popRecorded() and
// clockTicks() run on one UI thread.
class Sequencer {
public:
    static constexpr int kTicksPerBeat = 480;
    static constexpr int kBeatsPerBar = 4;
    static constexpr uint8_t kClickChannel = 15;
    // GS Metronome Bell and Metronome Click.
    static constexpr uint8_t kBellKey = 34;
    static constexpr uint8_t kClickKey = 33;
    static constexpr float kClickVelocity = 0.9f;
    // A recorded event on this channel marks where recording stopped.
    static constexpr uint8_t kStopMarkerChannel = 255;
    static constexpr size_t kRecordCapacity = 2048;
    static constexpr double kEdgeTolerance = 1e-6;

    Sequencer() = default;
    ~Sequencer();
    Sequencer(const Sequencer&) = delete;
    Sequencer& operator=(const Sequencer&) = delete;

    // UI thread. Takes ownership; frees snapshots the audio thread retired.
    void publishLoopNotes(LoopNotes* notes);
    bool popRecorded(RecordedEvent& event) { return recorded_.pop(event); }
    double clockTicks() const { return publishedClock_.load(std::memory_order_relaxed); }

    // Audio thread.
    void setSampleRate(int32_t sampleRate);
    void start();
    void stop();
    void setTempo(int bpm);
    void setClick(bool on) { click_ = on; }
    void setRecording(bool on);
    void setLoop(int64_t origin, int32_t length, bool playing);
    void setLatencyMillis(float millis) { latencyMillis_ = millis; }
    bool running() const { return running_; }

    // Records a live note played [frameOffset] frames into the current block.
    void recordLive(int32_t frameOffset, uint8_t channel, uint8_t key, float velocity);

    // Emits this block's clicks and loop notes as emit(frameOffset, channel,
    // key, velocity), then advances the clock by [frames].
    template <typename Emit>
    void collect(int32_t frames, Emit&& emit);

private:
    double ticksPerFrame() const;
    double ticksAt(int64_t frame) const;
    int32_t frameOffset(double tick, int32_t frames) const;
    void adoptPendingNotes();
    template <typename Emit>
    void emitLoopRange(double from, double to, double base, int32_t frames, Emit& emit);

    int32_t sampleRate_ = 48000;
    int bpm_ = 120;
    bool running_ = false;
    bool click_ = false;
    bool recording_ = false;
    bool playing_ = false;
    float latencyMillis_ = 0.0f;
    // The clock counts whole frames and derives ticks from them, so rounding
    // never accumulates. A tempo change rebases it at the current tick.
    int64_t frame_ = 0;
    int64_t tempoFrame_ = 0;
    double tempoTick_ = 0.0;
    double blockStart_ = 0.0;
    int64_t loopOrigin_ = 0;
    int32_t loopLength_ = 0;

    LoopNotes* current_ = nullptr;
    std::atomic<LoopNotes*> pending_{nullptr};
    // Snapshots replaced on the audio thread wait here for the UI thread to free.
    // The UI thread empties it before every publish, so it never holds more than
    // two; eight slots cannot fill.
    SpscRing<LoopNotes*, 8> retired_;
    SpscRing<RecordedEvent, kRecordCapacity> recorded_;
    std::atomic<double> publishedClock_{0.0};
};

template <typename Emit>
void Sequencer::collect(int32_t frames, Emit&& emit) {
    adoptPendingNotes();
    blockStart_ = ticksAt(frame_);
    if (!running_) {
        return;
    }
    const double start = blockStart_;
    // An event on the block edge belongs to the next block.
    const double end = ticksAt(frame_ + frames) - kEdgeTolerance;

    if (click_) {
        const double barOrigin = loopLength_ > 0 ? static_cast<double>(loopOrigin_) : 0.0;
        auto beat = static_cast<int64_t>(std::ceil((start - barOrigin) / kTicksPerBeat));
        for (double tick = barOrigin + beat * kTicksPerBeat; tick < end; tick += kTicksPerBeat, ++beat) {
            const bool barStart = ((beat % kBeatsPerBar) + kBeatsPerBar) % kBeatsPerBar == 0;
            emit(frameOffset(tick, frames), kClickChannel, barStart ? kBellKey : kClickKey, kClickVelocity);
        }
    }

    if (playing_ && loopLength_ > 0 && current_ != nullptr && end > static_cast<double>(loopOrigin_)) {
        const double from = std::max(start, static_cast<double>(loopOrigin_));
        const double into = std::fmod(from - static_cast<double>(loopOrigin_), loopLength_);
        const double base = from - into;
        const double span = end - from;
        emitLoopRange(into, std::min(into + span, static_cast<double>(loopLength_)), base, frames, emit);
        if (into + span > loopLength_) {
            emitLoopRange(0.0, into + span - loopLength_, base + loopLength_, frames, emit);
        }
    }

    frame_ += frames;
    publishedClock_.store(ticksAt(frame_), std::memory_order_relaxed);
}

template <typename Emit>
void Sequencer::emitLoopRange(double from, double to, double base, int32_t frames, Emit& emit) {
    const auto& notes = current_->notes;
    auto it = std::lower_bound(notes.begin(), notes.end(), from,
                               [](const LoopNote& note, double tick) { return note.tick < tick; });
    for (; it != notes.end() && it->tick < to; ++it) {
        emit(frameOffset(base + it->tick, frames), it->channel, it->key, it->velocity);
    }
}

}  // namespace synthkit
