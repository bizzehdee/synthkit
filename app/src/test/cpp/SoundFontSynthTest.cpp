#include "audio/SoundFontSynth.h"

#include <cmath>
#include <vector>

#include <gtest/gtest.h>

#include "tsf.h"

using synthkit::SoundFontSynth;

namespace {

constexpr int32_t kSampleRate = 48000;
constexpr int32_t kFrames = 4800;
constexpr int kKick = 36;
constexpr int kSnare = 38;
constexpr float kVelocity = 0.8f;

std::unique_ptr<SoundFontSynth> loadSynth() {
    auto synth = SoundFontSynth::create(tsf_load_filename(SYNTHKIT_SOUND_FONT));
    if (synth) {
        synth->setSampleRate(kSampleRate);
    }
    return synth;
}

double energyOf(SoundFontSynth& synth) {
    std::vector<float> buffer(kFrames * 2);
    synth.render(buffer.data(), kFrames);
    double energy = 0.0;
    for (float sample : buffer) {
        energy += static_cast<double>(sample) * sample;
    }
    return energy;
}

}  // namespace

TEST(SoundFontSynth, CreateRejectsAMissingFont) {
    EXPECT_EQ(SoundFontSynth::create(nullptr), nullptr);
}

TEST(SoundFontSynth, CreateRejectsDataThatIsNotASoundFont) {
    const char notASoundFont[] = "RIFF....WAVEfmt ";

    tsf* font = tsf_load_memory(notASoundFont, sizeof(notASoundFont));

    EXPECT_EQ(SoundFontSynth::create(font), nullptr);
}

TEST(SoundFontSynth, RendersSilenceWhenNoNoteIsPlaying) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_EQ(energyOf(*synth), 0.0);
}

TEST(SoundFontSynth, DrumNoteOnTheDrumChannelProducesSound) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    ASSERT_TRUE(synth->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity));

    EXPECT_GT(energyOf(*synth), 0.0);
}

TEST(SoundFontSynth, DrumChannelPlaysTheDrumKitNotTheDefaultPiano) {
    auto drums = loadSynth();
    auto piano = loadSynth();
    ASSERT_NE(drums, nullptr);
    ASSERT_NE(piano, nullptr);

    drums->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity);
    piano->noteOn(0, kKick, kVelocity);

    std::vector<float> drumOut(kFrames * 2);
    std::vector<float> pianoOut(kFrames * 2);
    drums->render(drumOut.data(), kFrames);
    piano->render(pianoOut.data(), kFrames);

    EXPECT_NE(drumOut, pianoOut);
}

TEST(SoundFontSynth, TwoNotesStartedTogetherBothSound) {
    auto kickOnly = loadSynth();
    auto kickAndSnare = loadSynth();
    ASSERT_NE(kickOnly, nullptr);
    ASSERT_NE(kickAndSnare, nullptr);

    kickOnly->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity);
    kickAndSnare->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity);
    kickAndSnare->noteOn(SoundFontSynth::kDrumChannel, kSnare, kVelocity);

    EXPECT_GT(energyOf(*kickAndSnare), energyOf(*kickOnly));
}

TEST(SoundFontSynth, NoteOffReleasesASustainedNote) {
    auto held = loadSynth();
    auto released = loadSynth();
    ASSERT_NE(held, nullptr);
    ASSERT_NE(released, nullptr);
    constexpr int kMiddleC = 60;
    held->noteOn(0, kMiddleC, kVelocity);
    released->noteOn(0, kMiddleC, kVelocity);
    energyOf(*held);
    energyOf(*released);

    ASSERT_TRUE(released->noteOff(0, kMiddleC));
    for (int block = 0; block < 20; ++block) {
        energyOf(*held);
        energyOf(*released);
    }

    EXPECT_LT(energyOf(*released), energyOf(*held));
}

TEST(SoundFontSynth, RejectsNotesOutsideTheMidiRange) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_FALSE(synth->noteOn(-1, kKick, kVelocity));
    EXPECT_FALSE(synth->noteOn(SoundFontSynth::kMidiChannels, kKick, kVelocity));
    EXPECT_FALSE(synth->noteOn(0, -1, kVelocity));
    EXPECT_FALSE(synth->noteOn(0, 128, kVelocity));
    EXPECT_FALSE(synth->noteOn(0, 60, 0.0f));
    EXPECT_FALSE(synth->noteOn(0, 60, 1.5f));
    EXPECT_FALSE(synth->noteOn(0, 60, std::nanf("")));
    EXPECT_FALSE(synth->noteOff(0, 128));
}

TEST(SoundFontSynth, ManyNotesBeyondTheVoiceLimitDoNotStopPlayback) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    for (int key = 35; key <= 81; ++key) {
        synth->noteOn(SoundFontSynth::kDrumChannel, key, kVelocity);
        synth->noteOn(SoundFontSynth::kDrumChannel, key, kVelocity);
    }

    EXPECT_GT(energyOf(*synth), 0.0);
}
