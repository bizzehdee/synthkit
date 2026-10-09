#include "AssetSoundFont.h"

#include <chrono>
#include <cstdio>

#include "Log.h"
#include "tsf.h"

namespace synthkit {
namespace {

int readAsset(void* asset, void* buffer, unsigned int size) {
    const int read = AAsset_read(static_cast<AAsset*>(asset), buffer, size);
    return read < 0 ? 0 : read;
}

int skipAsset(void* asset, unsigned int count) {
    return AAsset_seek(static_cast<AAsset*>(asset), count, SEEK_CUR) == -1 ? 0 : 1;
}

}  // namespace

tsf* loadSoundFontAsset(AAssetManager* assets, const char* path) {
    const auto started = std::chrono::steady_clock::now();
    AAsset* asset = AAssetManager_open(assets, path, AASSET_MODE_STREAMING);
    if (asset == nullptr) {
        LOGE("event=soundfont_load_failed asset=%s reason=missing", path);
        return nullptr;
    }
    tsf_stream stream{asset, &readAsset, &skipAsset};
    tsf* font = tsf_load(&stream);
    AAsset_close(asset);
    if (font == nullptr) {
        LOGE("event=soundfont_load_failed asset=%s reason=invalid", path);
        return nullptr;
    }
    const auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now() - started);
    LOGI("event=soundfont_loaded asset=%s presets=%d load_ms=%lld", path,
         tsf_get_presetcount(font), static_cast<long long>(elapsed.count()));
    return font;
}

}  // namespace synthkit
