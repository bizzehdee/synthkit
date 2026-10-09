#pragma once

#include <memory>
#include <mutex>

#include <oboe/Oboe.h>

#include "SoundFontSynth.h"

namespace synthkit {

// Owns the Oboe output stream and feeds it from the synth. The stream is
// reopened automatically when the output device changes. An open stream holds
// a shared reference to its engine, so stop() must run before the last
// external reference is dropped.
class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback,
                    public std::enable_shared_from_this<AudioEngine> {
public:
    explicit AudioEngine(std::unique_ptr<SoundFontSynth> synth);

    bool start();
    void stop();

    SoundFontSynth& synth() { return *synth_; }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openAndStartLocked();
    void closeLocked();

    std::mutex lock_;
    std::shared_ptr<oboe::AudioStream> stream_;
    bool playing_ = false;
    std::unique_ptr<SoundFontSynth> synth_;
};

}  // namespace synthkit
