#pragma once

struct tsf;

namespace synthkit {

// tsf.h has no public accessor for a preset's bank and program by index; the
// struct is only visible in the translation unit that compiles the library.
void presetBankAndProgram(const tsf* font, int presetIndex, int* bank, int* program);

}  // namespace synthkit
