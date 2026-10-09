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
    tsf_set_output(font_, TSF_STEREO_INTERLEAVED, sampleRate, 0.0f);
}

bool SoundFontSynth::noteOn(int channel, int key, float velocity, float delayMillis) {
    if (!(velocity > 0.0f && velocity <= 1.0f) || !validChannel(channel) || !validKey(key) ||
        !validDelay(delayMillis)) {
        return false;
    }
    return enqueue({SynthEvent::Type::NoteOn, static_cast<uint8_t>(channel),
                    static_cast<uint8_t>(key), 0, velocity, delayMillis});
}

bool SoundFontSynth::noteOff(int channel, int key, float delayMillis) {
    if (!validChannel(channel) || !validKey(key) || !validDelay(delayMillis)) {
        return false;
    }
    return enqueue({SynthEvent::Type::NoteOff, static_cast<uint8_t>(channel),
                    static_cast<uint8_t>(key), 0, 0.0f, delayMillis});
}

bool SoundFontSynth::allNotesOff(int channel) {
    if (!validChannel(channel)) {
        return false;
    }
    return enqueue({SynthEvent::Type::AllNotesOff, static_cast<uint8_t>(channel), 0, 0, 0.0f, 0.0f});
}

bool SoundFontSynth::programChange(int channel, int bank, int program) {
    if (!validChannel(channel) || bank < 0 || bank > kMaxBank || !validKey(program) ||
        tsf_get_presetindex(font_, bank, program) < 0) {
        return false;
    }
    return enqueue({SynthEvent::Type::Program, static_cast<uint8_t>(channel),
                    static_cast<uint8_t>(program), static_cast<uint16_t>(bank), 0.0f, 0.0f});
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
    if (scheduledCount_ == kMaxScheduledEvents) {
        return;
    }
    const auto delayFrames = static_cast<uint64_t>(event.delayMillis * sampleRate_ / 1000.0f);
    const ScheduledEvent entry{frameClock_ + delayFrames, event};
    size_t position = scheduledCount_;
    while (position > 0 && scheduled_[position - 1].dueFrame > entry.dueFrame) {
        scheduled_[position] = scheduled_[position - 1];
        --position;
    }
    scheduled_[position] = entry;
    ++scheduledCount_;
}

void SoundFontSynth::apply(const SynthEvent& event) {
    switch (event.type) {
        case SynthEvent::Type::NoteOn:
            tsf_channel_note_on(font_, event.channel, event.value, event.velocity);
            break;
        case SynthEvent::Type::NoteOff:
            tsf_channel_note_off(font_, event.channel, event.value);
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
    SynthEvent event{};
    while (queue_.pop(event)) {
        schedule(event);
    }

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
