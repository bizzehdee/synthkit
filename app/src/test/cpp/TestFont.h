#pragma once

#include <cstdio>

#include "tsf.h"

// Loads the bundled SoundFont from disk through correctly typed callbacks.
// tsf_load_filename calls stdio functions through mismatched pointer types,
// which UndefinedBehaviorSanitizer reports; the app loads assets the same
// typed way (AssetSoundFont.cpp).
inline tsf* loadTestFont() {
    FILE* file = std::fopen(SYNTHKIT_SOUND_FONT, "rb");
    if (file == nullptr) return nullptr;
    tsf_stream stream{
        file,
        [](void* data, void* ptr, unsigned int size) {
            return static_cast<int>(std::fread(ptr, 1, size, static_cast<FILE*>(data)));
        },
        [](void* data, unsigned int count) {
            return std::fseek(static_cast<FILE*>(data), count, SEEK_CUR) == 0 ? 1 : 0;
        },
    };
    tsf* font = tsf_load(&stream);
    std::fclose(file);
    return font;
}
