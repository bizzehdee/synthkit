#pragma once

#include <cstdint>
#include <memory>
#include <string>

namespace synthkit {

// Writes 16-bit interleaved stereo to a compressed file, a block at a time.
class AudioEncoder {
public:
    enum class Format { Mp3, Flac };

    static constexpr int kMp3BitrateKbps = 192;
    static constexpr int kFlacCompressionLevel = 5;

    // Returns nullptr when the file cannot be created or the encoder refuses the settings.
    static std::unique_ptr<AudioEncoder> open(Format format, const std::string& path, int sampleRate, int64_t totalFrames);

    virtual ~AudioEncoder() = default;
    virtual bool encode(const int16_t* stereo, int32_t frames) = 0;
    // Flushes and closes the file; the encoder may not be used afterwards.
    virtual bool finish() = 0;
};

}  // namespace synthkit
