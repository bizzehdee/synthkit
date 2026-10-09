#include <jni.h>

#include <memory>
#include <algorithm>
#include <string>
#include <vector>

#include <android/asset_manager_jni.h>

#include "audio/AssetSoundFont.h"
#include "audio/AudioEngine.h"

using synthkit::AudioEngine;

namespace {

// The Kotlin side holds a pointer to this heap shared_ptr; Oboe needs shared
// ownership of the engine for its callbacks.
using EngineHandle = std::shared_ptr<AudioEngine>;

AudioEngine& engine(jlong handle) { return **reinterpret_cast<EngineHandle*>(handle); }

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeCreate(
        JNIEnv* env, jclass /*clazz*/, jobject assetManager, jstring assetPath) {
    AAssetManager* assets = AAssetManager_fromJava(env, assetManager);
    const char* path = env->GetStringUTFChars(assetPath, nullptr);
    tsf* font = synthkit::loadSoundFontAsset(assets, path);
    env->ReleaseStringUTFChars(assetPath, path);

    auto synth = synthkit::SoundFontSynth::create(font);
    if (!synth) {
        return 0;
    }
    synth->programChange(synthkit::Sequencer::kClickChannel, synthkit::SoundFontSynth::kDrumBank, 0);
    auto* handle = new EngineHandle(std::make_shared<AudioEngine>(std::move(synth)));
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT void JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeDestroy(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    auto* engineHandle = reinterpret_cast<EngineHandle*>(handle);
    (*engineHandle)->stop();
    delete engineHandle;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeStart(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return engine(handle).start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeStop(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    engine(handle).stop();
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeNoteOn(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jint key, jfloat velocity,
        jfloat delayMillis) {
    return engine(handle).synth().noteOn(channel, key, velocity, delayMillis) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeNoteOff(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jint key, jfloat delayMillis) {
    return engine(handle).synth().noteOff(channel, key, delayMillis) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeAllNotesOff(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel) {
    return engine(handle).synth().allNotesOff(channel) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeProgramChange(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jint bank, jint program) {
    return engine(handle).synth().programChange(channel, bank, program) ? JNI_TRUE : JNI_FALSE;
}

// Three strings per preset: bank, program, name. Must match Preset.fromNative in Kotlin.
JNIEXPORT jobjectArray JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativePresets(
        JNIEnv* env, jclass /*clazz*/, jlong handle) {
    const auto presets = engine(handle).synth().presets();
    const auto count = static_cast<jsize>(presets.size() * 3);
    jobjectArray result = env->NewObjectArray(count, env->FindClass("java/lang/String"), nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    jsize index = 0;
    for (const auto& preset : presets) {
        const std::string fields[] = {std::to_string(preset.bank), std::to_string(preset.program),
                                      preset.name};
        for (const auto& field : fields) {
            jstring value = env->NewStringUTF(field.c_str());
            env->SetObjectArrayElement(result, index++, value);
            env->DeleteLocalRef(value);
        }
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetVolume(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jfloat volume) {
    return engine(handle).synth().setVolume(channel, volume) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeStartTransport(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return engine(handle).synth().startTransport() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeStopTransport(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return engine(handle).synth().stopTransport() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetTempo(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint bpm) {
    return engine(handle).synth().setTempo(bpm) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetClick(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jboolean on) {
    return engine(handle).synth().setClick(on == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetRecording(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jboolean on) {
    return engine(handle).synth().setRecording(on == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetLoop(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jlong origin, jint length, jboolean playing) {
    return engine(handle).synth().setLoop(origin, length, playing == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeSetLatency(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jfloat millis) {
    return engine(handle).synth().setLatency(millis) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeClockTicks(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return engine(handle).synth().clockTicks();
}

// Arrays from Kotlin are checked here: a bad value would otherwise reach the
// audio thread. Notes are sorted by tick with note offs first, as the sequencer needs.
JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativePublishLoopNotes(
        JNIEnv* env, jclass /*clazz*/, jlong handle, jintArray ticks, jintArray channels, jintArray keys,
        jfloatArray velocities) {
    const jsize count = env->GetArrayLength(ticks);
    if (env->GetArrayLength(channels) != count || env->GetArrayLength(keys) != count ||
        env->GetArrayLength(velocities) != count) {
        return JNI_FALSE;
    }
    std::vector<jint> tickValues(count), channelValues(count), keyValues(count);
    std::vector<jfloat> velocityValues(count);
    env->GetIntArrayRegion(ticks, 0, count, tickValues.data());
    env->GetIntArrayRegion(channels, 0, count, channelValues.data());
    env->GetIntArrayRegion(keys, 0, count, keyValues.data());
    env->GetFloatArrayRegion(velocities, 0, count, velocityValues.data());

    auto notes = std::make_unique<synthkit::LoopNotes>();
    notes->notes.reserve(count);
    for (jsize i = 0; i < count; ++i) {
        const bool valid = tickValues[i] >= 0 && channelValues[i] >= 0 &&
                           channelValues[i] < synthkit::SoundFontSynth::kMidiChannels && keyValues[i] >= 0 &&
                           keyValues[i] <= 127 && velocityValues[i] >= 0.0f && velocityValues[i] <= 1.0f;
        if (!valid) {
            return JNI_FALSE;
        }
        notes->notes.push_back({tickValues[i], static_cast<uint8_t>(channelValues[i]),
                                static_cast<uint8_t>(keyValues[i]), velocityValues[i]});
    }
    std::stable_sort(notes->notes.begin(), notes->notes.end(), [](const auto& a, const auto& b) {
        if (a.tick != b.tick) return a.tick < b.tick;
        return a.velocity == 0.0f && b.velocity > 0.0f;
    });
    engine(handle).synth().publishLoopNotes(std::move(notes));
    return JNI_TRUE;
}

// Four values per event: tick, channel, key, velocity. Must match RecordedEvent.fromNative.
JNIEXPORT jdoubleArray JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeDrainRecorded(
        JNIEnv* env, jclass /*clazz*/, jlong handle) {
    std::vector<jdouble> values;
    synthkit::RecordedEvent event{};
    while (engine(handle).synth().popRecorded(event)) {
        values.insert(values.end(), {static_cast<jdouble>(event.tick), static_cast<jdouble>(event.channel),
                                     static_cast<jdouble>(event.key), static_cast<jdouble>(event.velocity)});
    }
    jdoubleArray result = env->NewDoubleArray(static_cast<jsize>(values.size()));
    if (result != nullptr) {
        env->SetDoubleArrayRegion(result, 0, static_cast<jsize>(values.size()), values.data());
    }
    return result;
}

namespace {

constexpr int kMinExportRate = 8000;
constexpr int kMaxExportRate = 96000;
constexpr int kMaxPasses = 16;
constexpr int kMaxTailMillis = 10000;

}  // namespace

// Everything from Kotlin is checked before the renderer sees it. Returns 0 on
// a bad request; otherwise a handle owned by the caller until nativeExportClose.
JNIEXPORT jlong JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeOpenExport(
        JNIEnv* env, jclass /*clazz*/, jlong handle, jint sampleRate, jint bpm, jint loopTicks, jint passes,
        jint tailMillis, jintArray trackChannels, jintArray banks, jintArray programs, jfloatArray volumes,
        jintArray ticks, jintArray channels, jintArray keys, jfloatArray velocities) {
    if (sampleRate < kMinExportRate || sampleRate > kMaxExportRate || bpm < synthkit::SoundFontSynth::kMinTempo ||
        bpm > synthkit::SoundFontSynth::kMaxTempo || loopTicks <= 0 || passes < 1 || passes > kMaxPasses ||
        tailMillis < 0 || tailMillis > kMaxTailMillis) {
        return 0;
    }
    const jsize trackCount = env->GetArrayLength(trackChannels);
    const jsize noteCount = env->GetArrayLength(ticks);
    if (trackCount > synthkit::SoundFontSynth::kMidiChannels || env->GetArrayLength(banks) != trackCount ||
        env->GetArrayLength(programs) != trackCount || env->GetArrayLength(volumes) != trackCount ||
        env->GetArrayLength(channels) != noteCount || env->GetArrayLength(keys) != noteCount ||
        env->GetArrayLength(velocities) != noteCount) {
        return 0;
    }
    std::vector<jint> trackChannelValues(trackCount), bankValues(trackCount), programValues(trackCount);
    std::vector<jfloat> volumeValues(trackCount);
    env->GetIntArrayRegion(trackChannels, 0, trackCount, trackChannelValues.data());
    env->GetIntArrayRegion(banks, 0, trackCount, bankValues.data());
    env->GetIntArrayRegion(programs, 0, trackCount, programValues.data());
    env->GetFloatArrayRegion(volumes, 0, trackCount, volumeValues.data());
    std::vector<jint> tickValues(noteCount), channelValues(noteCount), keyValues(noteCount);
    std::vector<jfloat> velocityValues(noteCount);
    env->GetIntArrayRegion(ticks, 0, noteCount, tickValues.data());
    env->GetIntArrayRegion(channels, 0, noteCount, channelValues.data());
    env->GetIntArrayRegion(keys, 0, noteCount, keyValues.data());
    env->GetFloatArrayRegion(velocities, 0, noteCount, velocityValues.data());

    const auto validChannel = [](jint channel) { return channel >= 0 && channel < synthkit::SoundFontSynth::kMidiChannels; };
    synthkit::ExportSpec spec{sampleRate, bpm, loopTicks, passes, tailMillis, {}, {}};
    for (jsize i = 0; i < trackCount; ++i) {
        if (!validChannel(trackChannelValues[i]) || bankValues[i] < 0 || bankValues[i] > 16383 || programValues[i] < 0 ||
            programValues[i] > 127 || !(volumeValues[i] >= 0.0f && volumeValues[i] <= 1.0f)) {
            return 0;
        }
        spec.tracks.push_back({static_cast<uint8_t>(trackChannelValues[i]), static_cast<uint16_t>(bankValues[i]),
                               static_cast<uint8_t>(programValues[i]), volumeValues[i]});
    }
    spec.notes.reserve(noteCount);
    for (jsize i = 0; i < noteCount; ++i) {
        if (tickValues[i] < 0 || tickValues[i] >= 2 * loopTicks || !validChannel(channelValues[i]) || keyValues[i] < 0 ||
            keyValues[i] > 127 || !(velocityValues[i] >= 0.0f && velocityValues[i] <= 1.0f)) {
            return 0;
        }
        spec.notes.push_back({tickValues[i], static_cast<uint8_t>(channelValues[i]), static_cast<uint8_t>(keyValues[i]),
                              velocityValues[i]});
    }
    auto renderer = engine(handle).synth().openExport(spec);
    return renderer ? reinterpret_cast<jlong>(renderer.release()) : 0;
}

JNIEXPORT jint JNICALL Java_com_bizzeh_synthkit_audio_ExportRender_nativeRender(
        JNIEnv* env, jclass /*clazz*/, jlong exportHandle, jshortArray buffer) {
    const jsize frames = env->GetArrayLength(buffer) / 2;
    std::vector<int16_t> samples(static_cast<size_t>(frames) * 2);
    const int32_t rendered = reinterpret_cast<synthkit::OfflineRenderer*>(exportHandle)->render(samples.data(), frames);
    env->SetShortArrayRegion(buffer, 0, rendered * 2, samples.data());
    return rendered;
}

JNIEXPORT jlong JNICALL Java_com_bizzeh_synthkit_audio_ExportRender_nativeTotalFrames(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong exportHandle) {
    return reinterpret_cast<synthkit::OfflineRenderer*>(exportHandle)->totalFrames();
}

JNIEXPORT void JNICALL Java_com_bizzeh_synthkit_audio_ExportRender_nativeClose(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong exportHandle) {
    delete reinterpret_cast<synthkit::OfflineRenderer*>(exportHandle);
}

// Field order must match LatencyReport.fromNative in Kotlin.
JNIEXPORT jobjectArray JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeLatencyReport(
        JNIEnv* env, jclass /*clazz*/, jlong handle) {
    const synthkit::LatencyReport report = engine(handle).latencyReport();
    const std::string fields[] = {
            report.running ? "1" : "0",
            std::to_string(report.outputLatencyMs),
            oboe::convertToText(report.audioApi),
            oboe::convertToText(report.performanceMode),
            oboe::convertToText(report.sharingMode),
            std::to_string(report.sampleRate),
            std::to_string(report.framesPerBurst),
            std::to_string(report.bufferFrames),
            std::to_string(report.underruns),
            std::to_string(report.deviceId),
            std::to_string(report.loadAverage),
            std::to_string(report.loadPeak),
            std::to_string(report.voices),
    };
    constexpr jsize kCount = sizeof(fields) / sizeof(fields[0]);
    jobjectArray result = env->NewObjectArray(kCount, env->FindClass("java/lang/String"), nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    for (jsize i = 0; i < kCount; ++i) {
        jstring field = env->NewStringUTF(fields[i].c_str());
        env->SetObjectArrayElement(result, i, field);
        env->DeleteLocalRef(field);
    }
    return result;
}

}  // extern "C"
