#include <jni.h>

#include <string>
#include <vector>

#include "export/AudioEncoder.h"

using synthkit::AudioEncoder;

namespace {

AudioEncoder& encoder(jlong handle) { return *reinterpret_cast<AudioEncoder*>(handle); }

}  // namespace

extern "C" {

// format: 0 MP3, 1 FLAC. Must match NativeEncoder in Kotlin. Returns 0 on failure.
JNIEXPORT jlong JNICALL Java_com_bizzeh_synthkit_export_NativeEncoder_nativeOpen(
        JNIEnv* env, jclass /*clazz*/, jint format, jstring path, jint sampleRate, jlong totalFrames) {
    if (format != 0 && format != 1) return 0;
    const char* chars = env->GetStringUTFChars(path, nullptr);
    const std::string file(chars);
    env->ReleaseStringUTFChars(path, chars);
    auto opened = AudioEncoder::open(format == 0 ? AudioEncoder::Format::Mp3 : AudioEncoder::Format::Flac, file,
                                     sampleRate, totalFrames);
    return opened ? reinterpret_cast<jlong>(opened.release()) : 0;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_export_NativeEncoder_nativeEncode(
        JNIEnv* env, jclass /*clazz*/, jlong handle, jshortArray stereo, jint frames) {
    if (frames < 0 || frames * 2 > env->GetArrayLength(stereo)) return JNI_FALSE;
    std::vector<int16_t> samples(static_cast<size_t>(frames) * 2);
    env->GetShortArrayRegion(stereo, 0, frames * 2, samples.data());
    return encoder(handle).encode(samples.data(), frames) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_bizzeh_synthkit_export_NativeEncoder_nativeFinish(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    return encoder(handle).finish() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_bizzeh_synthkit_export_NativeEncoder_nativeClose(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    delete reinterpret_cast<AudioEncoder*>(handle);
}

}  // extern "C"
