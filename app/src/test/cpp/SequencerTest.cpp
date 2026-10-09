#include "audio/Sequencer.h"

#include <cmath>
#include <memory>
#include <thread>
#include <vector>

#include <gtest/gtest.h>

#include "audio/SoundFontSynth.h"
#include "tsf.h"

using synthkit::LoopNote;
using synthkit::LoopNotes;
using synthkit::RecordedEvent;
using synthkit::Sequencer;
using synthkit::SoundFontSynth;

namespace {

constexpr int32_t kRate = 48000;
// At 120 BPM and 48 kHz a beat is 24000 frames and a bar 96000.
constexpr int64_t kBeatFrames = 24000;
constexpr int64_t kBarFrames = 96000;

struct Emitted {
    int64_t frame;
    uint8_t channel;
    uint8_t key;
    float velocity;
};

// Runs the sequencer for [total] frames in blocks of [block] and returns every
// emitted event with its absolute frame.
std::vector<Emitted> run(Sequencer& sequencer, int64_t total, int32_t block) {
    std::vector<Emitted> out;
    for (int64_t done = 0; done < total; done += block) {
        const auto frames = static_cast<int32_t>(std::min<int64_t>(block, total - done));
        sequencer.collect(frames, [&](int32_t offset, uint8_t channel, uint8_t key, float velocity) {
            out.push_back({done + offset, channel, key, velocity});
        });
    }
    return out;
}

std::unique_ptr<Sequencer> startedSequencer() {
    auto sequencer = std::make_unique<Sequencer>();
    sequencer->setSampleRate(kRate);
    sequencer->setTempo(120);
    sequencer->start();
    return sequencer;
}

LoopNotes* notes(std::vector<LoopNote> list) { return new LoopNotes{std::move(list)}; }

}  // namespace

TEST(Sequencer, StoppedSequencerEmitsNothingAndKeepsItsClock) {
    Sequencer sequencer;
    sequencer.setSampleRate(kRate);
    sequencer.setClick(true);

    EXPECT_TRUE(run(sequencer, kBarFrames, 512).empty());
    EXPECT_EQ(sequencer.clockTicks(), 0.0);
}

TEST(Sequencer, ClickFallsOnEveryBeatWithTheBellOnEachBar) {
    auto sequencer = startedSequencer();
    sequencer->setClick(true);

    const auto clicks = run(*sequencer, 2 * kBarFrames, 4800);

    ASSERT_EQ(clicks.size(), 8u);
    for (size_t beat = 0; beat < clicks.size(); ++beat) {
        EXPECT_EQ(clicks[beat].frame, static_cast<int64_t>(beat) * kBeatFrames);
        EXPECT_EQ(clicks[beat].channel, Sequencer::kClickChannel);
        EXPECT_EQ(clicks[beat].key, beat % 4 == 0 ? Sequencer::kBellKey : Sequencer::kClickKey);
    }
}

TEST(Sequencer, ClickOffIsSilentButTheClockRuns) {
    auto sequencer = startedSequencer();

    EXPECT_TRUE(run(*sequencer, kBarFrames, 1000).empty());
    EXPECT_NEAR(sequencer->clockTicks(), Sequencer::kTicksPerBeat * 4, 1e-6);
}

TEST(Sequencer, TempoSetsTheBeatLength) {
    auto sequencer = startedSequencer();
    sequencer->setTempo(60);
    sequencer->setClick(true);

    const auto clicks = run(*sequencer, 3 * 48000, 1024);

    ASSERT_EQ(clicks.size(), 3u);
    EXPECT_EQ(clicks[1].frame, 48000);
}

TEST(Sequencer, LoopNotesPlayAtTheirFramesEveryCycle) {
    auto sequencer = startedSequencer();
    sequencer->publishLoopNotes(notes({{0, 1, 36, 0.8f}, {960, 1, 38, 0.8f}}));
    sequencer->setLoop(0, 1920, true);

    const auto played = run(*sequencer, 2 * kBarFrames, 1000);

    ASSERT_EQ(played.size(), 4u);
    EXPECT_EQ(played[0].frame, 0);
    EXPECT_EQ(played[1].frame, 48000);
    EXPECT_EQ(played[2].frame, 96000);
    EXPECT_EQ(played[3].frame, 144000);
    EXPECT_EQ(played[3].key, 38);
}

TEST(Sequencer, NoteJustBeforeTheLoopEndAndTheNextStartBothPlayInOneBlock) {
    auto sequencer = startedSequencer();
    sequencer->publishLoopNotes(notes({{0, 1, 36, 0.8f}, {1919, 1, 42, 0.8f}}));
    sequencer->setLoop(0, 1920, true);

    const auto played = run(*sequencer, kBarFrames + 50, kBarFrames - 100);

    ASSERT_EQ(played.size(), 3u);
    EXPECT_EQ(played[1].key, 42);
    EXPECT_EQ(played[2].frame, kBarFrames);
    EXPECT_EQ(played[2].key, 36);
}

TEST(Sequencer, LoopDoesNotPlayBeforeItsOrigin) {
    auto sequencer = startedSequencer();
    sequencer->publishLoopNotes(notes({{0, 1, 36, 0.8f}}));
    sequencer->setLoop(480, 1920, true);

    const auto played = run(*sequencer, kBarFrames, 2000);

    ASSERT_EQ(played.size(), 1u);
    EXPECT_EQ(played[0].frame, kBeatFrames);
}

TEST(Sequencer, BarsOfAPlayingLoopCountFromItsOrigin) {
    auto sequencer = startedSequencer();
    sequencer->setLoop(480, 1920, false);
    sequencer->setClick(true);

    const auto clicks = run(*sequencer, kBarFrames, 1000);

    ASSERT_EQ(clicks.size(), 4u);
    EXPECT_EQ(clicks[0].key, Sequencer::kClickKey);
    EXPECT_EQ(clicks[1].key, Sequencer::kBellKey);
}

TEST(Sequencer, PausedLoopIsSilent) {
    auto sequencer = startedSequencer();
    sequencer->publishLoopNotes(notes({{0, 1, 36, 0.8f}}));
    sequencer->setLoop(0, 1920, false);

    EXPECT_TRUE(run(*sequencer, kBarFrames, 1000).empty());
}

TEST(Sequencer, NotesAtOneTickKeepTheirOrder) {
    auto sequencer = startedSequencer();
    sequencer->publishLoopNotes(notes({{240, 2, 60, 0.0f}, {240, 2, 60, 0.7f}}));
    sequencer->setLoop(0, 1920, true);

    const auto played = run(*sequencer, kBeatFrames, 512);

    ASSERT_EQ(played.size(), 2u);
    EXPECT_EQ(played[0].velocity, 0.0f);
    EXPECT_EQ(played[1].velocity, 0.7f);
}

TEST(Sequencer, LiveNotesAreRecordedAtTheirClockPositionMinusLatency) {
    auto sequencer = startedSequencer();
    sequencer->setRecording(true);
    run(*sequencer, kBeatFrames, kBeatFrames);  // block now starts at tick 480
    sequencer->setLatencyMillis(10.0f);          // 480 frames = 9.6 ticks

    sequencer->collect(512, [](int32_t, uint8_t, uint8_t, float) {});
    sequencer->recordLive(100, 3, 64, 0.8f);  // 480 + 2 - 9.6

    RecordedEvent event{};
    ASSERT_TRUE(sequencer->popRecorded(event));
    EXPECT_EQ(event.tick, 472);
    EXPECT_EQ(event.channel, 3);
    EXPECT_EQ(event.key, 64);
    EXPECT_FLOAT_EQ(event.velocity, 0.8f);
}

TEST(Sequencer, StoppingARecordingLeavesAMarker) {
    auto sequencer = startedSequencer();
    sequencer->setRecording(true);
    run(*sequencer, 2 * kBeatFrames, kBeatFrames);
    sequencer->collect(1, [](int32_t, uint8_t, uint8_t, float) {});

    sequencer->setRecording(false);

    RecordedEvent event{};
    ASSERT_TRUE(sequencer->popRecorded(event));
    EXPECT_EQ(event.channel, Sequencer::kStopMarkerChannel);
    EXPECT_EQ(event.tick, 960);
}

TEST(Sequencer, NothingIsRecordedWhileNotRecording) {
    auto sequencer = startedSequencer();
    sequencer->collect(512, [](int32_t, uint8_t, uint8_t, float) {});

    sequencer->recordLive(0, 1, 60, 0.8f);
    sequencer->setRecording(false);

    RecordedEvent event{};
    EXPECT_FALSE(sequencer->popRecorded(event));
}

TEST(Sequencer, SnapshotsCanBeReplacedWhileTheAudioThreadPlays) {
    auto sequencer = startedSequencer();
    sequencer->setLoop(0, 1920, true);

    std::thread audio([&] {
        for (int block = 0; block < 4000; ++block) {
            sequencer->collect(256, [](int32_t, uint8_t, uint8_t, float) {});
        }
    });
    for (int i = 0; i < 2000; ++i) {
        sequencer->publishLoopNotes(notes({{i % 1920, 1, static_cast<uint8_t>(36 + i % 40), 0.5f}}));
    }
    audio.join();

    sequencer->publishLoopNotes(notes({{0, 1, 99, 0.5f}}));
    const auto played = run(*sequencer, kBarFrames, 1024);
    ASSERT_FALSE(played.empty());
    EXPECT_EQ(played.back().key, 99);
}

namespace {

std::unique_ptr<SoundFontSynth> loadSynth() {
    auto synth = SoundFontSynth::create(tsf_load_filename(SYNTHKIT_SOUND_FONT));
    if (synth) {
        synth->setSampleRate(kRate);
        synth->programChange(Sequencer::kClickChannel, SoundFontSynth::kDrumBank, 0);
    }
    return synth;
}

double renderEnergy(SoundFontSynth& synth, int32_t frames) {
    std::vector<float> buffer(static_cast<size_t>(frames) * 2);
    synth.render(buffer.data(), frames);
    double energy = 0.0;
    for (float sample : buffer) {
        energy += static_cast<double>(sample) * sample;
    }
    return energy;
}

}  // namespace

TEST(SequencedSynth, StartedTransportWithClickIsAudible) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    ASSERT_TRUE(synth->setClick(true));
    ASSERT_TRUE(synth->startTransport());

    EXPECT_GT(renderEnergy(*synth, 4800), 0.0);
}

TEST(SequencedSynth, LivePlayingIsRecordedButLoopPlaybackIsNot) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);
    auto loop = std::make_unique<LoopNotes>();
    loop->notes = {{0, 2, 40, 0.8f}};
    synth->publishLoopNotes(std::move(loop));
    synth->programChange(2, 0, 0);
    synth->setLoop(0, 1920, true);
    synth->startTransport();
    synth->setRecording(true);
    renderEnergy(*synth, 256);

    synth->noteOn(1, 60, 0.8f);
    renderEnergy(*synth, 256);
    synth->noteOff(1, 60);
    renderEnergy(*synth, 256);

    std::vector<RecordedEvent> recorded;
    RecordedEvent event{};
    while (synth->popRecorded(event)) recorded.push_back(event);
    ASSERT_EQ(recorded.size(), 2u);
    EXPECT_EQ(recorded[0].key, 60);
    EXPECT_GT(recorded[0].velocity, 0.0f);
    EXPECT_EQ(recorded[1].velocity, 0.0f);
    EXPECT_GT(recorded[1].tick, recorded[0].tick);
}

TEST(SequencedSynth, ZeroVolumeSilencesAChannel) {
    auto loud = loadSynth();
    auto muted = loadSynth();
    ASSERT_NE(loud, nullptr);
    ASSERT_NE(muted, nullptr);

    ASSERT_TRUE(muted->setVolume(1, 0.0f));
    loud->noteOn(1, 60, 0.8f);
    muted->noteOn(1, 60, 0.8f);

    EXPECT_GT(renderEnergy(*loud, 4800), 0.0);
    EXPECT_EQ(renderEnergy(*muted, 4800), 0.0);
}

TEST(SequencedSynth, TransportInputsAreValidated) {
    auto synth = loadSynth();
    ASSERT_NE(synth, nullptr);

    EXPECT_FALSE(synth->setTempo(39));
    EXPECT_FALSE(synth->setTempo(241));
    EXPECT_TRUE(synth->setTempo(240));
    EXPECT_FALSE(synth->setLatency(-1.0f));
    EXPECT_FALSE(synth->setLatency(std::nanf("")));
    EXPECT_FALSE(synth->setLoop(-1, 1920, true));
    EXPECT_FALSE(synth->setLoop(0, -5, true));
    EXPECT_FALSE(synth->setVolume(0, 1.5f));
    EXPECT_FALSE(synth->setVolume(16, 0.5f));
}
