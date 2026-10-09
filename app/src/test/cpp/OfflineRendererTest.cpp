#include "audio/OfflineRenderer.h"

#include <cstdlib>
#include <thread>
#include <vector>

#include <gtest/gtest.h>

#include "audio/SoundFontSynth.h"
#include "TestFont.h"

using synthkit::ExportNote;
using synthkit::ExportSpec;
using synthkit::OfflineRenderer;
using synthkit::SoundFontSynth;

namespace {

constexpr int32_t kRate = 44100;

std::unique_ptr<SoundFontSynth> loadSynth() {
    return SoundFontSynth::create(loadTestFont());
}

// One bar at 120 BPM is 2 s, 88200 frames; the tail is 0.5 s.
ExportSpec drumSpec(std::vector<ExportNote> notes, int passes = 2) {
    return {kRate, 120, 1920, passes, 500, {{1, 128, 0, 1.0f}}, std::move(notes)};
}

std::vector<int16_t> renderAll(OfflineRenderer& renderer, int32_t block = 1000) {
    std::vector<int16_t> out;
    std::vector<int16_t> buffer(static_cast<size_t>(block) * 2);
    while (true) {
        const int32_t frames = renderer.render(buffer.data(), block);
        if (frames == 0) break;
        out.insert(out.end(), buffer.begin(), buffer.begin() + frames * 2);
    }
    return out;
}

bool soundsBetween(const std::vector<int16_t>& stereo, int64_t fromFrame, int64_t toFrame) {
    for (int64_t i = fromFrame * 2; i < toFrame * 2 && i < static_cast<int64_t>(stereo.size()); ++i) {
        if (std::abs(stereo[i]) > 50) return true;
    }
    return false;
}

}  // namespace

TEST(OfflineRenderer, LengthIsEveryPassPlusTheTail) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    auto renderer = synth->openExport(drumSpec({}, 3));
    ASSERT_NE(renderer, nullptr);

    const auto out = renderAll(*renderer);

    EXPECT_EQ(renderer->totalFrames(), 3 * 88200 + 22050);
    EXPECT_EQ(static_cast<int64_t>(out.size()), renderer->totalFrames() * 2);
    EXPECT_EQ(renderer->renderedFrames(), renderer->totalFrames());
}

TEST(OfflineRenderer, NoNotesRenderSilence) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    auto renderer = synth->openExport(drumSpec({}));

    const auto out = renderAll(*renderer);

    EXPECT_FALSE(soundsBetween(out, 0, renderer->totalFrames()));
}

TEST(OfflineRenderer, NotesPlayInEveryPass) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    // A crash at beat 3 of each pass: frame 44100 and 132300.
    auto renderer = synth->openExport(drumSpec({{960, 1, 49, 0.9f}}));

    const auto out = renderAll(*renderer);

    EXPECT_FALSE(soundsBetween(out, 0, 44000));
    EXPECT_TRUE(soundsBetween(out, 44100, 46000));
    EXPECT_TRUE(soundsBetween(out, 132300, 134300));
}

TEST(OfflineRenderer, OutputIsTheSameEveryTime) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    const auto spec = drumSpec({{0, 1, 36, 0.8f}, {480, 1, 38, 0.8f}, {960, 1, 42, 0.5f}});

    const auto first = renderAll(*synth->openExport(spec), 777);
    const auto second = renderAll(*synth->openExport(spec), 4096);

    EXPECT_EQ(first, second);
}

TEST(OfflineRenderer, MelodicNoteEndsAtItsNoteOff) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    ExportSpec spec{kRate, 120, 1920, 1, 0, {{2, 0, 0, 1.0f}}, {{0, 2, 60, 0.8f}, {240, 2, 60, 0.0f}}};
    auto renderer = synth->openExport(spec);

    const auto out = renderAll(*renderer);

    EXPECT_TRUE(soundsBetween(out, 0, 11025));
    EXPECT_FALSE(soundsBetween(out, 70000, 88200));
}

TEST(OfflineRenderer, InvalidSpecsAreRefused) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_EQ(synth->openExport({kRate, 120, 0, 1, 0, {}, {}}), nullptr);
    EXPECT_EQ(synth->openExport({kRate, 120, 1920, 0, 0, {}, {}}), nullptr);
    EXPECT_EQ(synth->openExport({kRate, 120, 1920, 1, -1, {}, {}}), nullptr);
    EXPECT_EQ(synth->openExport({kRate, 120, 1920, 1, 0, {{1, 77, 3, 1.0f}}, {}}), nullptr);
}

TEST(OfflineRenderer, AnExportCanOutliveTheSynth) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    auto renderer = synth->openExport(drumSpec({{0, 1, 36, 0.8f}}));

    synth.reset();

    EXPECT_TRUE(soundsBetween(renderAll(*renderer), 0, 4000));
}

TEST(OfflineRenderer, ExportRendersWhileTheLiveSynthPlays) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    synth->setSampleRate(48000);
    auto renderer = synth->openExport(drumSpec({{0, 1, 36, 0.8f}, {480, 1, 38, 0.8f}}, 4));

    std::thread exporter([&] { renderAll(*renderer); });
    std::vector<float> live(512 * 2);
    for (int block = 0; block < 400; ++block) {
        synth->noteOn(9, 36 + block % 10, 0.8f);
        synth->render(live.data(), 512);
    }
    exporter.join();

    EXPECT_EQ(renderer->renderedFrames(), renderer->totalFrames());
}
