#include <jni.h>

#include <memory>

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
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jint key, jfloat velocity) {
    return engine(handle).synth().noteOn(channel, key, velocity) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_audio_AudioEngine_nativeNoteOff(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle, jint channel, jint key) {
    return engine(handle).synth().noteOff(channel, key) ? JNI_TRUE : JNI_FALSE;
}

}  // extern "C"
