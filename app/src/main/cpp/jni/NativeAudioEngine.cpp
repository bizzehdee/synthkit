#include <jni.h>

#include <memory>
#include <string>

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
