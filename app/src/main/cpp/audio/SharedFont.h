#pragma once

#include <mutex>

struct tsf;

namespace synthkit {

// An idle copy of the loaded SoundFont that exports copy from, and the lock for
// closing any copy. All copies share one non-atomic reference count and the
// last close frees the sample data, so every tsf_close of a copy must hold
// [lock]. The live instance is never copied directly, because the audio
// thread writes to it while tsf_copy reads it.
struct SharedFont {
    std::mutex lock;
    tsf* idle = nullptr;

    ~SharedFont();
};

}  // namespace synthkit
