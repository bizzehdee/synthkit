#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>

namespace synthkit {

struct NoteEvent {
    uint8_t channel;
    uint8_t key;
    // 0 means note off.
    float velocity;
};

// Lock-free ring buffer that hands note events to the audio thread without
// blocking it. Safe for exactly one producer thread and one consumer thread.
class NoteQueue {
public:
    static constexpr size_t kCapacity = 256;

    bool push(const NoteEvent& event) {
        const size_t head = head_.load(std::memory_order_relaxed);
        const size_t next = (head + 1) % kSlots;
        if (next == tail_.load(std::memory_order_acquire)) {
            return false;
        }
        slots_[head] = event;
        head_.store(next, std::memory_order_release);
        return true;
    }

    bool pop(NoteEvent& event) {
        const size_t tail = tail_.load(std::memory_order_relaxed);
        if (tail == head_.load(std::memory_order_acquire)) {
            return false;
        }
        event = slots_[tail];
        tail_.store((tail + 1) % kSlots, std::memory_order_release);
        return true;
    }

private:
    // One slot stays empty so that a full queue and an empty queue differ.
    static constexpr size_t kSlots = kCapacity + 1;

    std::array<NoteEvent, kSlots> slots_{};
    std::atomic<size_t> head_{0};
    std::atomic<size_t> tail_{0};
};

}  // namespace synthkit
