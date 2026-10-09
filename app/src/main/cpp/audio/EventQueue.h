#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>

namespace synthkit {

struct SynthEvent {
    enum class Type : uint8_t { NoteOn, NoteOff, AllNotesOff, Program };

    Type type;
    uint8_t channel;
    // Key for note events, program number for Program.
    uint8_t value;
    uint16_t bank;
    float velocity;
    float delayMillis;
};

// Lock-free ring buffer that hands synth events to the audio thread without
// blocking it. Safe for exactly one producer thread and one consumer thread.
class EventQueue {
public:
    static constexpr size_t kCapacity = 256;

    bool push(const SynthEvent& event) {
        const size_t head = head_.load(std::memory_order_relaxed);
        const size_t next = (head + 1) % kSlots;
        if (next == tail_.load(std::memory_order_acquire)) {
            return false;
        }
        slots_[head] = event;
        head_.store(next, std::memory_order_release);
        return true;
    }

    bool pop(SynthEvent& event) {
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

    std::array<SynthEvent, kSlots> slots_{};
    std::atomic<size_t> head_{0};
    std::atomic<size_t> tail_{0};
};

}  // namespace synthkit
