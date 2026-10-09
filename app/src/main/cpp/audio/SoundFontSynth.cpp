#include "SoundFontSynth.h"

#include <algorithm>
#include <cmath>

#include "TinySoundFontExtras.h"
#include "tsf.h"

namespace synthkit {
namespace {

constexpr int kMaxBank = 16383;

bool validChannel(int channel) { return channel >= 0 && channel < SoundFontSynth::kMidiChannels; }

bool validKey(int key) { return key >= 0 && key <= 127; }

bool validDelay(float delayMillis) { return delayMillis >= 0.0f && std::isfinite(delayMillis); }

bool isTransport(SynthEvent::Type type) {
    switch (type) {
        case SynthEvent::Type::TransportStart:
        case SynthEvent::Type::TransportStop:
        case SynthEvent::Type::Tempo:
        case SynthEvent::Type::Click:
        case SynthEvent::Type::Recording:
        case SynthEvent::Type::Loop:
        case SynthEvent::Type::Latency:
            return true;
        default:
            return false;
    }
}

}  // namespace

std::unique_ptr<SoundFontSynth> SoundFontSynth::create(tsf* font) {
    if (font == nullptr) {
        return nullptr;
    }
    // Pre-allocating voices and every channel keeps TinySoundFont from
    // allocating memory on the audio thread.
    bool ready = tsf_set_max_voices(font, kMaxVoices) != 0;
    for (int channel = 0; ready && channel < kMidiChannels; ++channel) {
        const int midiDrums = channel == kDrumChannel ? 1 : 0;
        ready = tsf_channel_set_presetnumber(font, channel, 0, midiDrums) != 0;
    }
    if (!ready) {
        tsf_close(font);
        return nullptr;
    }
    return std::unique_ptr<SoundFontSynth>(new SoundFontSynth(font));
}

SoundFontSynth::SoundFontSynth(tsf* font) : font_(font) {}

SoundFontSynth::~SoundFontSynth() { tsf_close(font_); }

void SoundFontSynth::setSampleRate(int32_t sampleRate) {
    sampleRate_ = sampleRate;
    sequencer_.setSampleRate(sampleRate);
    tsf_set_output(font_, TSF_STEREO_INTERLEAVED, sampleRate, 0.0f);
}

bool SoundFontSynth::noteOn(int channel, int key, float velocity, float delayMillis) {
    if (!(velocity > 0.0f && velocity <= 1.0f) || !validChannel(channel) || !validKey(key) ||
        !validDelay(delayMillis)) {
        return false;
    }
    return enqueue({SynthEvent::Type::NoteOn, static_cast<uint8_t>(channel), static_cast<uint8_t>(key), 0,
                    velocity, delayMillis, 0, 0, true});
}

bool SoundFontSynth::noteOff(int channel, int key, float delayMillis) {
    if (!validChannel(channel) || !validKey(key) || !validDelay(delayMillis)) {
        return false;
    }
    return enqueue({SynthEvent::Type::NoteOff, static_cast<uint8_t>(channel), static_cast<uint8_t>(key), 0,
                    0.0f, delayMillis, 0, 0, true});
}

bool SoundFontSynth::allNotesOff(int channel) {
    if (!validChannel(channel)) {
        return false;
    }
    return enqueue({SynthEvent::Type::AllNotesOff, static_cast<uint8_t>(channel), 0, 0, 0.0f, 0.0f, 0, 0, false});
}

bool SoundFontSynth::programChange(int channel, int bank, int program) {
    if (!validChannel(channel) || bank < 0 || bank > kMaxBank || !validKey(program) ||
        tsf_get_presetindex(font_, bank, program) < 0) {
        return false;
    }
    return enqueue({SynthEvent::Type::Program, static_cast<uint8_t>(channel), static_cast<uint8_t>(program),
                    static_cast<uint16_t>(bank), 0.0f, 0.0f, 0, 0, false});
}

bool SoundFontSynth::setVolume(int channel, float volume) {
    if (!validChannel(channel) || !(volume >= 0.0f && volume <= 1.0f)) {
        return false;
    }
    return enqueue({SynthEvent::Type::Volume, static_cast<uint8_t>(channel), 0, 0, volume, 0.0f, 0, 0, false});
}

bool SoundFontSynth::control(SynthEvent::Type type, uint8_t value, uint16_t bank, float velocity, int64_t position,
                             int32_t length) {
    return enqueue({type, 0, value, bank, velocity, 0.0f, position, length, false});
}

bool SoundFontSynth::startTransport() { return control(SynthEvent::Type::TransportStart); }

bool SoundFontSynth::stopTransport() { return control(SynthEvent::Type::TransportStop); }

bool SoundFontSynth::setTempo(int bpm) {
    if (bpm < kMinTempo || bpm > kMaxTempo) {
        return false;
    }
    return control(SynthEvent::Type::Tempo, 0, static_cast<uint16_t>(bpm));
}

bool SoundFontSynth::setClick(bool on) { return control(SynthEvent::Type::Click, on ? 1 : 0); }

bool SoundFontSynth::setRecording(bool on) { return control(SynthEvent::Type::Recording, on ? 1 : 0); }

bool SoundFontSynth::setLoop(int64_t origin, int32_t length, bool playing) {
    if (origin < 0 || length < 0) {
        return false;
    }
    return control(SynthEvent::Type::Loop, playing ? 1 : 0, 0, 0.0f, origin, length);
}

bool SoundFontSynth::setLatency(float millis) {
    if (!(millis >= 0.0f && millis < kMaxLatencyMillis)) {
        return false;
    }
    return control(SynthEvent::Type::Latency, 0, 0, millis);
}

std::vector<PresetInfo> SoundFontSynth::presets() const {
    std::vector<PresetInfo> result;
    const int count = tsf_get_presetcount(font_);
    result.reserve(count);
    for (int index = 0; index < count; ++index) {
        int bank = 0;
        int program = 0;
        presetBankAndProgram(font_, index, &bank, &program);
        result.push_back({bank, program, tsf_get_presetname(font_, index)});
    }
    return result;
}

bool SoundFontSynth::enqueue(const SynthEvent& event) { return queue_.push(event); }

void SoundFontSynth::schedule(const SynthEvent& event) {
    const auto delayFrames = static_cast<uint64_t>(event.delayMillis * sampleRate_ / 1000.0f);
    scheduleAt(frameClock_ + delayFrames, event);
}

void SoundFontSynth::scheduleAt(uint64_t dueFrame, const SynthEvent& event) {
    if (scheduledCount_ == kMaxScheduledEvents) {
        return;
    }
    const ScheduledEvent entry{dueFrame, event};
    size_t position = scheduledCount_;
    while (position > 0 && scheduled_[position - 1].dueFrame > entry.dueFrame) {
        scheduled_[position] = scheduled_[position - 1];
        --position;
    }
    scheduled_[position] = entry;
    ++scheduledCount_;
}

void SoundFontSynth::apply(const SynthEvent& event) {
    if (event.live && (event.type == SynthEvent::Type::NoteOn || event.type == SynthEvent::Type::NoteOff)) {
        sequencer_.recordLive(static_cast<int32_t>(frameClock_ - blockStartFrame_), event.channel, event.value,
                              event.type == SynthEvent::Type::NoteOn ? event.velocity : 0.0f);
    }
    switch (event.type) {
        case SynthEvent::Type::NoteOn:
            tsf_channel_note_on(font_, event.channel, event.value, event.velocity);
            break;
        case SynthEvent::Type::NoteOff:
            tsf_channel_note_off(font_, event.channel, event.value);
            break;
        case SynthEvent::Type::Volume:
            tsf_channel_set_volume(font_, event.channel, event.velocity);
            break;
        case SynthEvent::Type::TransportStart:
            sequencer_.start();
            break;
        case SynthEvent::Type::TransportStop:
            sequencer_.stop();
            break;
        case SynthEvent::Type::Tempo:
            sequencer_.setTempo(event.bank);
            break;
        case SynthEvent::Type::Click:
            sequencer_.setClick(event.value != 0);
            break;
        case SynthEvent::Type::Recording:
            sequencer_.setRecording(event.value != 0);
            break;
        case SynthEvent::Type::Loop:
            sequencer_.setLoop(event.position, event.length, event.value != 0);
            break;
        case SynthEvent::Type::Latency:
            sequencer_.setLatencyMillis(event.velocity);
            break;
        case SynthEvent::Type::AllNotesOff:
            tsf_channel_note_off_all(font_, event.channel);
            break;
        case SynthEvent::Type::Program:
            tsf_channel_set_bank_preset(font_, event.channel, event.bank, event.value);
            break;
    }
}

void SoundFontSynth::render(float* stereoOut, int32_t frameCount) {
    blockStartFrame_ = frameClock_;
    SynthEvent event{};
    while (queue_.pop(event)) {
        // Transport changes are state, not sound: they apply before this block's
        // loop notes and clicks are collected. Sound events keep their order.
        if (isTransport(event.type)) {
            apply(event);
        } else {
            schedule(event);
        }
    }
    sequencer_.collect(frameCount, [this](int32_t offset, uint8_t channel, uint8_t key, float velocity) {
        const auto type = velocity > 0.0f ? SynthEvent::Type::NoteOn : SynthEvent::Type::NoteOff;
        scheduleAt(blockStartFrame_ + static_cast<uint64_t>(offset),
                   {type, channel, key, 0, velocity, 0.0f, 0, 0, false});
    });

    int32_t rendered = 0;
    while (true) {
        size_t due = 0;
        while (due < scheduledCount_ && scheduled_[due].dueFrame <= frameClock_) {
            apply(scheduled_[due].event);
            ++due;
        }
        if (due > 0) {
            std::move(scheduled_.begin() + due, scheduled_.begin() + scheduledCount_,
                      scheduled_.begin());
            scheduledCount_ -= due;
        }
        if (rendered == frameCount) {
            break;
        }
        int32_t segment = frameCount - rendered;
        if (scheduledCount_ > 0) {
            const uint64_t untilNext = scheduled_[0].dueFrame - frameClock_;
            segment = static_cast<int32_t>(std::min<uint64_t>(segment, untilNext));
        }
        tsf_render_float(font_, stereoOut + static_cast<ptrdiff_t>(rendered) * 2, segment, 0);
        rendered += segment;
        frameClock_ += static_cast<uint64_t>(segment);
    }
}

}  // namespace synthkit
