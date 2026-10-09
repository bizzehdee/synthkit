#include "SoundFontSynth.h"

#include "tsf.h"

namespace synthkit {

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
    tsf_set_output(font_, TSF_STEREO_INTERLEAVED, sampleRate, 0.0f);
}

bool SoundFontSynth::noteOn(int channel, int key, float velocity) {
    if (!(velocity > 0.0f && velocity <= 1.0f)) {
        return false;
    }
    return enqueue(channel, key, velocity);
}

bool SoundFontSynth::noteOff(int channel, int key) { return enqueue(channel, key, 0.0f); }

bool SoundFontSynth::enqueue(int channel, int key, float velocity) {
    if (channel < 0 || channel >= kMidiChannels || key < 0 || key > 127) {
        return false;
    }
    return queue_.push({static_cast<uint8_t>(channel), static_cast<uint8_t>(key), velocity});
}

void SoundFontSynth::render(float* stereoOut, int32_t frameCount) {
    NoteEvent event{};
    while (queue_.pop(event)) {
        if (event.velocity > 0.0f) {
            tsf_channel_note_on(font_, event.channel, event.key, event.velocity);
        } else {
            tsf_channel_note_off(font_, event.channel, event.key);
        }
    }
    tsf_render_float(font_, stereoOut, frameCount, 0);
}

}  // namespace synthkit
