#pragma once

#include <memory>
#include <atomic>
#include <mutex>

#include <oboe/Oboe.h>

#include "SoundFontSynth.h"

namespace synthkit {

struct LatencyReport {
    bool running = false;
    // Negative when the platform cannot report it yet.
    double outputLatencyMs = -1.0;
    oboe::AudioApi audioApi = oboe::AudioApi::Unspecified;
    oboe::PerformanceMode performanceMode = oboe::PerformanceMode::None;
    oboe::SharingMode sharingMode = oboe::SharingMode::Shared;
    int32_t sampleRate = 0;
    int32_t framesPerBurst = 0;
    int32_t bufferFrames = 0;
    int32_t underruns = 0;
    // 0 when the platform does not report the output device.
    int32_t deviceId = 0;
    // Share of each buffer's time spent rendering it: smoothed, and the peak
    // since the previous report. Above 1 the callback misses its deadline.
    float loadAverage = 0.0f;
    float loadPeak = 0.0f;
    int32_t voices = 0;
};

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

    LatencyReport latencyReport();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openAndStartLocked();
    void closeLocked();

    std::mutex lock_;
    // Written by the audio callback, read with the report.
    std::atomic<float> loadAverage_{0.0f};
    std::atomic<float> loadPeak_{0.0f};
    std::shared_ptr<oboe::AudioStream> stream_;
    bool playing_ = false;
    std::unique_ptr<SoundFontSynth> synth_;
};

}  // namespace synthkit
