#include "Sequencer.h"


namespace synthkit {

Sequencer::~Sequencer() {
    delete current_;
    delete pending_.exchange(nullptr);
    LoopNotes* retired = nullptr;
    while (retired_.pop(retired)) {
        delete retired;
    }
}

void Sequencer::publishLoopNotes(LoopNotes* notes) {
    LoopNotes* retired = nullptr;
    while (retired_.pop(retired)) {
        delete retired;
    }
    // A snapshot the audio thread never adopted was never read, so it is safe to free.
    delete pending_.exchange(notes, std::memory_order_acq_rel);
}

void Sequencer::adoptPendingNotes() {
    LoopNotes* next = pending_.exchange(nullptr, std::memory_order_acq_rel);
    if (next == nullptr) {
        return;
    }
    // If the ring were ever full the old snapshot leaks: freeing it here would
    // block the audio thread, and the ring cannot fill (see retired_).
    if (current_ != nullptr) {
        retired_.push(current_);
    }
    current_ = next;
}

void Sequencer::setSampleRate(int32_t sampleRate) { sampleRate_ = sampleRate; }

void Sequencer::start() {
    frame_ = 0;
    tempoFrame_ = 0;
    tempoTick_ = 0.0;
    blockStart_ = 0.0;
    running_ = true;
    publishedClock_.store(0.0, std::memory_order_relaxed);
}

void Sequencer::stop() {
    running_ = false;
    recording_ = false;
}

void Sequencer::setTempo(int bpm) {
    tempoTick_ = ticksAt(frame_);
    tempoFrame_ = frame_;
    bpm_ = bpm;
}

void Sequencer::setRecording(bool on) {
    if (recording_ && !on) {
        const double latencyTicks = latencyMillis_ / 1000.0 * sampleRate_ * ticksPerFrame();
        recorded_.push({static_cast<int64_t>(std::llround(blockStart_ - latencyTicks)), kStopMarkerChannel, 0, 0.0f});
    }
    recording_ = on;
}

void Sequencer::setLoop(int64_t origin, int32_t length, bool playing) {
    loopOrigin_ = origin;
    loopLength_ = std::max(length, 0);
    playing_ = playing;
}

void Sequencer::recordLive(int32_t frameOffset, uint8_t channel, uint8_t key, float velocity) {
    if (!recording_ || !running_) {
        return;
    }
    const double latencyTicks = latencyMillis_ / 1000.0 * sampleRate_ * ticksPerFrame();
    const double tick = blockStart_ + frameOffset * ticksPerFrame() - latencyTicks;
    recorded_.push({static_cast<int64_t>(std::llround(tick)), channel, key, velocity});
}

double Sequencer::ticksPerFrame() const {
    return static_cast<double>(bpm_) * kTicksPerBeat / (60.0 * sampleRate_);
}

double Sequencer::ticksAt(int64_t frame) const {
    return tempoTick_ + static_cast<double>(frame - tempoFrame_) * ticksPerFrame();
}

int32_t Sequencer::frameOffset(double tick, int32_t frames) const {
    const auto offset = static_cast<int32_t>(std::llround((tick - blockStart_) / ticksPerFrame()));
    return std::clamp(offset, 0, frames - 1);
}

}  // namespace synthkit
