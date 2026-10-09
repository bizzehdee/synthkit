#pragma once

#include <android/asset_manager.h>

struct tsf;

namespace synthkit {

// Returns nullptr if the asset is missing or is not a valid SoundFont.
tsf* loadSoundFontAsset(AAssetManager* assets, const char* path);

}  // namespace synthkit
