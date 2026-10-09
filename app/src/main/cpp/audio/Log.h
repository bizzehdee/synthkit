#pragma once

#include <android/log.h>

#define SYNTHKIT_LOG_TAG "SynthKit"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, SYNTHKIT_LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, SYNTHKIT_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, SYNTHKIT_LOG_TAG, __VA_ARGS__)
