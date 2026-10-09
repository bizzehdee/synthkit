#include "export/AudioEncoder.h"

#include <cmath>
#include <cstdio>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>

#include <FLAC/stream_decoder.h>
#include <gtest/gtest.h>

using synthkit::AudioEncoder;

namespace {

constexpr int kRate = 44100;

std::string outputPath(const std::string& name) { return std::string(SYNTHKIT_TEST_OUTPUT) + "/" + name; }

// A deterministic signal with different content in each channel.
std::vector<int16_t> testSignal(int32_t frames) {
    std::vector<int16_t> stereo(static_cast<size_t>(frames) * 2);
    for (int32_t i = 0; i < frames; ++i) {
        stereo[i * 2] = static_cast<int16_t>(12000 * std::sin(i * 0.031));
        stereo[i * 2 + 1] = static_cast<int16_t>((i * 37) % 20000 - 10000);
    }
    return stereo;
}

bool encodeAll(AudioEncoder& encoder, const std::vector<int16_t>& stereo, int32_t block) {
    const auto frames = static_cast<int32_t>(stereo.size() / 2);
    for (int32_t at = 0; at < frames; at += block) {
        if (!encoder.encode(stereo.data() + at * 2, std::min(block, frames - at))) return false;
    }
    return encoder.finish();
}

struct Decoded {
    std::vector<int16_t> stereo;
    unsigned rate = 0;
    unsigned channels = 0;
    unsigned bits = 0;
    uint64_t total = 0;
};

Decoded decodeFlac(const std::string& path) {
    Decoded out;
    FLAC__StreamDecoder* decoder = FLAC__stream_decoder_new();
    const auto write = [](const FLAC__StreamDecoder*, const FLAC__Frame* frame, const FLAC__int32* const buffer[],
                          void* data) {
        auto* decoded = static_cast<Decoded*>(data);
        for (unsigned i = 0; i < frame->header.blocksize; ++i) {
            decoded->stereo.push_back(static_cast<int16_t>(buffer[0][i]));
            decoded->stereo.push_back(static_cast<int16_t>(buffer[1][i]));
        }
        return FLAC__STREAM_DECODER_WRITE_STATUS_CONTINUE;
    };
    const auto metadata = [](const FLAC__StreamDecoder*, const FLAC__StreamMetadata* block, void* data) {
        if (block->type != FLAC__METADATA_TYPE_STREAMINFO) return;
        auto* decoded = static_cast<Decoded*>(data);
        decoded->rate = block->data.stream_info.sample_rate;
        decoded->channels = block->data.stream_info.channels;
        decoded->bits = block->data.stream_info.bits_per_sample;
        decoded->total = block->data.stream_info.total_samples;
    };
    const auto error = [](const FLAC__StreamDecoder*, FLAC__StreamDecoderErrorStatus, void*) {};
    FLAC__stream_decoder_init_file(decoder, path.c_str(), write, metadata, error, &out);
    FLAC__stream_decoder_process_until_end_of_stream(decoder);
    FLAC__stream_decoder_delete(decoder);
    return out;
}

std::vector<unsigned char> readFile(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    return {std::istreambuf_iterator<char>(in), {}};
}

}  // namespace

TEST(AudioEncoder, FlacIsLosslessAndDescribesItsFormat) {
    const auto signal = testSignal(kRate * 2);
    const auto path = outputPath("roundtrip.flac");
    auto encoder = AudioEncoder::open(AudioEncoder::Format::Flac, path, kRate, kRate * 2);
    ASSERT_NE(encoder, nullptr);

    ASSERT_TRUE(encodeAll(*encoder, signal, 1000));
    encoder.reset();
    const auto decoded = decodeFlac(path);

    EXPECT_EQ(decoded.rate, 44100u);
    EXPECT_EQ(decoded.channels, 2u);
    EXPECT_EQ(decoded.bits, 16u);
    EXPECT_EQ(decoded.total, static_cast<uint64_t>(kRate * 2));
    EXPECT_EQ(decoded.stereo, signal);
}

TEST(AudioEncoder, Mp3IsConstantBitrateMpeg1LayerThreeOfTheRightLength) {
    const auto signal = testSignal(kRate * 2);
    const auto path = outputPath("frames.mp3");
    auto encoder = AudioEncoder::open(AudioEncoder::Format::Mp3, path, kRate, kRate * 2);
    ASSERT_NE(encoder, nullptr);

    ASSERT_TRUE(encodeAll(*encoder, signal, 4096));
    encoder.reset();
    const auto bytes = readFile(path);

    // Walk the frames: 11 sync bits, MPEG-1, Layer III, 192 kbit/s (index 11), 44.1 kHz (index 0).
    size_t at = 0;
    int frames = 0;
    while (at + 4 <= bytes.size()) {
        ASSERT_EQ(bytes[at], 0xFF) << "frame " << frames;
        ASSERT_EQ(bytes[at + 1] & 0xFE, 0xFA) << "frame " << frames;
        ASSERT_EQ(bytes[at + 2] >> 4, 11) << "frame " << frames;
        ASSERT_EQ((bytes[at + 2] >> 2) & 0x3, 0) << "frame " << frames;
        const int padding = (bytes[at + 2] >> 1) & 1;
        at += 144 * 192000 / 44100 + padding;
        ++frames;
    }
    EXPECT_EQ(at, bytes.size());
    // 1152 samples a frame, plus the encoder delay and an info frame.
    EXPECT_GE(frames, kRate * 2 / 1152);
    EXPECT_LE(frames, kRate * 2 / 1152 + 4);
}

TEST(AudioEncoder, UnwritablePathsAreRefused) {
    const auto path = outputPath("missing-folder/out");

    EXPECT_EQ(AudioEncoder::open(AudioEncoder::Format::Mp3, path, kRate, 100), nullptr);
    EXPECT_EQ(AudioEncoder::open(AudioEncoder::Format::Flac, path, kRate, 100), nullptr);
    EXPECT_EQ(AudioEncoder::open(AudioEncoder::Format::Flac, outputPath("x.flac"), 0, 100), nullptr);
}
