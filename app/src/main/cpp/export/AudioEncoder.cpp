#include "AudioEncoder.h"

#include <cstdio>
#include <vector>

#include <FLAC/stream_encoder.h>
#include <lame.h>

namespace synthkit {
namespace {

class Mp3Encoder : public AudioEncoder {
public:
    Mp3Encoder(lame_t lame, FILE* file) : lame_(lame), file_(file) {}

    ~Mp3Encoder() override {
        lame_close(lame_);
        if (file_ != nullptr) std::fclose(file_);
    }

    bool encode(const int16_t* stereo, int32_t frames) override {
        // LAME's documented worst case for one call's output.
        buffer_.resize(static_cast<size_t>(frames) * 5 / 4 + 7200);
        // lame_encode_buffer_interleaved takes a non-const pointer but does not write to it.
        const int bytes = lame_encode_buffer_interleaved(lame_, const_cast<int16_t*>(stereo), frames, buffer_.data(),
                                                         static_cast<int>(buffer_.size()));
        return bytes >= 0 && std::fwrite(buffer_.data(), 1, bytes, file_) == static_cast<size_t>(bytes);
    }

    bool finish() override {
        buffer_.resize(7200);
        const int bytes = lame_encode_flush(lame_, buffer_.data(), static_cast<int>(buffer_.size()));
        const bool written = bytes >= 0 && std::fwrite(buffer_.data(), 1, bytes, file_) == static_cast<size_t>(bytes);
        const bool closed = std::fclose(file_) == 0;
        file_ = nullptr;
        return written && closed;
    }

private:
    lame_t lame_;
    FILE* file_;
    std::vector<unsigned char> buffer_;
};

class FlacEncoder : public AudioEncoder {
public:
    explicit FlacEncoder(FLAC__StreamEncoder* encoder) : encoder_(encoder) {}

    ~FlacEncoder() override { FLAC__stream_encoder_delete(encoder_); }

    bool encode(const int16_t* stereo, int32_t frames) override {
        samples_.assign(stereo, stereo + static_cast<ptrdiff_t>(frames) * 2);
        return FLAC__stream_encoder_process_interleaved(encoder_, samples_.data(), frames) != 0;
    }

    bool finish() override { return FLAC__stream_encoder_finish(encoder_) != 0; }

private:
    FLAC__StreamEncoder* encoder_;
    std::vector<FLAC__int32> samples_;
};

std::unique_ptr<AudioEncoder> openMp3(const std::string& path, int sampleRate) {
    lame_t lame = lame_init();
    if (lame == nullptr) return nullptr;
    lame_set_in_samplerate(lame, sampleRate);
    lame_set_out_samplerate(lame, sampleRate);
    lame_set_num_channels(lame, 2);
    lame_set_mode(lame, JOINT_STEREO);
    lame_set_brate(lame, AudioEncoder::kMp3BitrateKbps);
    lame_set_VBR(lame, vbr_off);
    lame_set_quality(lame, 2);
    if (lame_init_params(lame) < 0) {
        lame_close(lame);
        return nullptr;
    }
    FILE* file = std::fopen(path.c_str(), "wb");
    if (file == nullptr) {
        lame_close(lame);
        return nullptr;
    }
    return std::make_unique<Mp3Encoder>(lame, file);
}

std::unique_ptr<AudioEncoder> openFlac(const std::string& path, int sampleRate, int64_t totalFrames) {
    FLAC__StreamEncoder* encoder = FLAC__stream_encoder_new();
    if (encoder == nullptr) return nullptr;
    const bool configured = FLAC__stream_encoder_set_channels(encoder, 2) &&
                            FLAC__stream_encoder_set_bits_per_sample(encoder, 16) &&
                            FLAC__stream_encoder_set_sample_rate(encoder, sampleRate) &&
                            FLAC__stream_encoder_set_compression_level(encoder, AudioEncoder::kFlacCompressionLevel) &&
                            FLAC__stream_encoder_set_total_samples_estimate(encoder, totalFrames);
    if (!configured ||
        FLAC__stream_encoder_init_file(encoder, path.c_str(), nullptr, nullptr) != FLAC__STREAM_ENCODER_INIT_STATUS_OK) {
        FLAC__stream_encoder_delete(encoder);
        return nullptr;
    }
    return std::make_unique<FlacEncoder>(encoder);
}

}  // namespace

std::unique_ptr<AudioEncoder> AudioEncoder::open(Format format, const std::string& path, int sampleRate,
                                                 int64_t totalFrames) {
    if (sampleRate <= 0 || totalFrames < 0) return nullptr;
    return format == Format::Mp3 ? openMp3(path, sampleRate) : openFlac(path, sampleRate, totalFrames);
}

}  // namespace synthkit
