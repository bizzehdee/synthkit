#define TSF_IMPLEMENTATION
#include "tsf.h"

#include "TinySoundFontExtras.h"

namespace synthkit {

void presetBankAndProgram(const tsf* font, int presetIndex, int* bank, int* program) {
    *bank = font->presets[presetIndex].bank;
    *program = font->presets[presetIndex].preset;
}

}  // namespace synthkit
