#include "audio/SoundFontSynth.h"

#include <algorithm>
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

namespace {

// Index of the first frame whose left or right sample is not zero, or -1.
int firstSoundingFrame(const std::vector<float>& stereo) {
    for (size_t i = 0; i < stereo.size(); ++i) {
        if (stereo[i] != 0.0f) {
            return static_cast<int>(i / 2);
        }
    }
    return -1;
}

std::vector<float> renderFrames(SoundFontSynth& synth, int32_t frames) {
    std::vector<float> buffer(static_cast<size_t>(frames) * 2);
    synth.render(buffer.data(), frames);
    return buffer;
}

}  // namespace

TEST(SoundFontSynth, DelayedNoteStartsAtItsFrameInsideABlock) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    constexpr float kDelayMillis = 10.0f;  // 480 frames at 48 kHz

    ASSERT_TRUE(synth->noteOn(SoundFontSynth::kDrumChannel, kSnare, kVelocity, kDelayMillis));

    const int first = firstSoundingFrame(renderFrames(*synth, 1024));
    EXPECT_GE(first, 480);
    EXPECT_LT(first, 480 + 16);
}

TEST(SoundFontSynth, DelayedNoteWaitsAcrossBlocks) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    constexpr float kDelayMillis = 50.0f;  // 2400 frames at 48 kHz

    synth->noteOn(SoundFontSynth::kDrumChannel, kSnare, kVelocity, kDelayMillis);

    EXPECT_EQ(firstSoundingFrame(renderFrames(*synth, 1024)), -1);
    EXPECT_EQ(firstSoundingFrame(renderFrames(*synth, 1024)), -1);
    EXPECT_GE(firstSoundingFrame(renderFrames(*synth, 1024)), 2400 - 2048);
}

TEST(SoundFontSynth, DelayedNoteOffEndsANoteLater) {
    auto held = loadSynth();
    auto released = loadSynth();
    ASSERT_NE(held, nullptr);
    ASSERT_NE(released, nullptr);
    constexpr int kMiddleC = 60;
    held->noteOn(0, kMiddleC, kVelocity);
    released->noteOn(0, kMiddleC, kVelocity);
    released->noteOff(0, kMiddleC, 20.0f);

    for (int block = 0; block < 20; ++block) {
        energyOf(*held);
        energyOf(*released);
    }

    EXPECT_LT(energyOf(*released), energyOf(*held));
}

TEST(SoundFontSynth, ProgramChangeSelectsAnotherInstrument) {
    auto piano = loadSynth();
    auto guitar = loadSynth();
    ASSERT_NE(piano, nullptr);
    ASSERT_NE(guitar, nullptr);
    constexpr int kNylonGuitar = 24;

    ASSERT_TRUE(guitar->programChange(0, 0, kNylonGuitar));
    piano->noteOn(0, 60, kVelocity);
    guitar->noteOn(0, 60, kVelocity);

    EXPECT_NE(renderFrames(*piano, kFrames), renderFrames(*guitar, kFrames));
}

TEST(SoundFontSynth, DrumKitBankMakesAnyChannelPlayDrums) {
    auto drumChannel = loadSynth();
    auto otherChannel = loadSynth();
    ASSERT_NE(drumChannel, nullptr);
    ASSERT_NE(otherChannel, nullptr);

    ASSERT_TRUE(otherChannel->programChange(3, SoundFontSynth::kDrumBank, 0));
    drumChannel->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity);
    otherChannel->noteOn(3, kKick, kVelocity);

    EXPECT_EQ(renderFrames(*drumChannel, kFrames), renderFrames(*otherChannel, kFrames));
}

TEST(SoundFontSynth, ProgramChangeToAMissingPresetIsRejected) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_FALSE(synth->programChange(0, 77, 3));
    EXPECT_FALSE(synth->programChange(0, -1, 0));
    EXPECT_FALSE(synth->programChange(0, 0, 128));
    EXPECT_FALSE(synth->programChange(16, 0, 0));
}

TEST(SoundFontSynth, AllNotesOffReleasesEveryNoteOnTheChannel) {
    auto held = loadSynth();
    auto released = loadSynth();
    ASSERT_NE(held, nullptr);
    ASSERT_NE(released, nullptr);
    for (int key : {60, 64, 67}) {
        held->noteOn(0, key, kVelocity);
        released->noteOn(0, key, kVelocity);
    }
    energyOf(*held);
    energyOf(*released);

    ASSERT_TRUE(released->allNotesOff(0));
    for (int block = 0; block < 20; ++block) {
        energyOf(*held);
        energyOf(*released);
    }

    EXPECT_LT(energyOf(*released), energyOf(*held));
    EXPECT_FALSE(released->allNotesOff(16));
}

TEST(SoundFontSynth, PresetsIncludeEveryGmProgramAndTheDrumKits) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    const auto presets = synth->presets();
    const auto inBank = [&presets](int bank) {
        return std::count_if(presets.begin(), presets.end(),
                             [bank](const synthkit::PresetInfo& p) { return p.bank == bank; });
    };

    EXPECT_EQ(inBank(0), 128);
    EXPECT_GE(inBank(SoundFontSynth::kDrumBank), 1);
    const auto standardKit = std::find_if(presets.begin(), presets.end(), [](const auto& p) {
        return p.bank == SoundFontSynth::kDrumBank && p.program == 0;
    });
    ASSERT_NE(standardKit, presets.end());
    EXPECT_FALSE(standardKit->name.empty());
}

TEST(SoundFontSynth, RejectsInvalidDelays) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_FALSE(synth->noteOn(0, 60, kVelocity, -1.0f));
    EXPECT_FALSE(synth->noteOn(0, 60, kVelocity, std::nanf("")));
    EXPECT_FALSE(synth->noteOff(0, 60, INFINITY));
}

TEST(SoundFontSynth, MoreScheduledEventsThanTheLimitAreDroppedSafely) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    for (size_t i = 0; i < SoundFontSynth::kMaxScheduledEvents + 10; ++i) {
        synth->noteOn(SoundFontSynth::kDrumChannel, kKick, kVelocity, 5.0f + static_cast<float>(i % 7));
        if (i % 200 == 199) {
            renderFrames(*synth, 1);
        }
    }

    EXPECT_GT(energyOf(*synth), 0.0);
}
