#pragma once

#include <array>
#include <atomic>
#include <cstddef>

namespace synthkit {

// Lock-free ring buffer for exactly one producer thread and one consumer
// thread. Neither side blocks or allocates.
template <typename T, size_t Capacity>
class SpscRing {
public:
    static constexpr size_t kCapacity = Capacity;

    bool push(const T& item) {
        const size_t head = head_.load(std::memory_order_relaxed);
        const size_t next = (head + 1) % kSlots;
        if (next == tail_.load(std::memory_order_acquire)) {
            return false;
        }
        slots_[head] = item;
        head_.store(next, std::memory_order_release);
        return true;
    }

    bool pop(T& item) {
        const size_t tail = tail_.load(std::memory_order_relaxed);
        if (tail == head_.load(std::memory_order_acquire)) {
            return false;
        }
        item = slots_[tail];
        tail_.store((tail + 1) % kSlots, std::memory_order_release);
        return true;
    }

private:
    // One slot stays empty so that a full ring and an empty ring differ.
    static constexpr size_t kSlots = Capacity + 1;

    std::array<T, kSlots> slots_{};
    std::atomic<size_t> head_{0};
    std::atomic<size_t> tail_{0};
};

}  // namespace synthkit
