#include "AudioEngine.h"

#include "Log.h"

namespace synthkit {

AudioEngine::AudioEngine(std::unique_ptr<SoundFontSynth> synth) : synth_(std::move(synth)) {}

bool AudioEngine::start() {
    std::lock_guard<std::mutex> guard(lock_);
    if (playing_) {
        return true;
    }
    playing_ = openAndStartLocked();
    return playing_;
}

void AudioEngine::stop() {
    std::lock_guard<std::mutex> guard(lock_);
    playing_ = false;
    closeLocked();
}

bool AudioEngine::openAndStartLocked() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setUsage(oboe::Usage::Media)
            ->setContentType(oboe::ContentType::Music)
            ->setDataCallback(shared_from_this())
            ->setErrorCallback(shared_from_this());

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGE("event=stream_open_failed result=%s", oboe::convertToText(result));
        stream_.reset();
        return false;
    }
    // Two bursts is the smallest buffer that usually avoids underruns.
    stream_->setBufferSizeInFrames(stream_->getFramesPerBurst() * 2);
    // The stream is not started yet, so render() cannot run concurrently.
    synth_->setSampleRate(stream_->getSampleRate());

    result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("event=stream_start_failed result=%s", oboe::convertToText(result));
        closeLocked();
        return false;
    }
    LOGI("event=stream_started api=%s performance_mode=%s sharing_mode=%s sample_rate=%d "
         "frames_per_burst=%d buffer_frames=%d",
         oboe::convertToText(stream_->getAudioApi()),
         oboe::convertToText(stream_->getPerformanceMode()),
         oboe::convertToText(stream_->getSharingMode()), stream_->getSampleRate(),
         stream_->getFramesPerBurst(), stream_->getBufferSizeInFrames());
    return true;
}

LatencyReport AudioEngine::latencyReport() {
    std::lock_guard<std::mutex> guard(lock_);
    LatencyReport report;
    if (!stream_ || !playing_) {
        return report;
    }
    report.running = true;
    const auto latency = stream_->calculateLatencyMillis();
    if (latency) {
        report.outputLatencyMs = latency.value();
    }
    report.audioApi = stream_->getAudioApi();
    report.performanceMode = stream_->getPerformanceMode();
    report.sharingMode = stream_->getSharingMode();
    report.sampleRate = stream_->getSampleRate();
    report.framesPerBurst = stream_->getFramesPerBurst();
    report.bufferFrames = stream_->getBufferSizeInFrames();
    const auto underruns = stream_->getXRunCount();
    if (underruns) {
        report.underruns = underruns.value();
    }
    return report;
}

void AudioEngine::closeLocked() {
    if (stream_) {
        stream_->stop();
        stream_->close();
        stream_.reset();
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* /*stream*/, void* audioData,
                                                   int32_t numFrames) {
    synth_->render(static_cast<float*>(audioData), numFrames);
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* /*stream*/, oboe::Result error) {
    std::lock_guard<std::mutex> guard(lock_);
    stream_.reset();
    if (!playing_ || error != oboe::Result::ErrorDisconnected) {
        LOGE("event=stream_closed_by_error result=%s", oboe::convertToText(error));
        playing_ = false;
        return;
    }
    LOGW("event=stream_disconnected action=reopen");
    playing_ = openAndStartLocked();
}

}  // namespace synthkit
