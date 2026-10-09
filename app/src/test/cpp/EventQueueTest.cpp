#include "audio/EventQueue.h"

#include <thread>

#include <gtest/gtest.h>

using synthkit::EventQueue;
using synthkit::SynthEvent;

namespace {

SynthEvent noteOn(uint8_t key, float velocity = 1.0f) {
    return {SynthEvent::Type::NoteOn, 0, key, 0, velocity, 0.0f};
}

}  // namespace

TEST(EventQueue, EmptyQueueHasNothingToPop) {
    EventQueue queue;
    SynthEvent event{};

    EXPECT_FALSE(queue.pop(event));
}

TEST(EventQueue, EventsComeOutInTheOrderTheyWentIn) {
    EventQueue queue;
    queue.push(noteOn(36, 0.8f));
    queue.push(noteOn(38, 0.5f));

    SynthEvent first{};
    SynthEvent second{};
    ASSERT_TRUE(queue.pop(first));
    ASSERT_TRUE(queue.pop(second));

    EXPECT_EQ(first.value, 36);
    EXPECT_FLOAT_EQ(first.velocity, 0.8f);
    EXPECT_EQ(second.value, 38);
}

TEST(EventQueue, FullQueueRejectsPushUntilAnEventIsPopped) {
    EventQueue queue;
    for (size_t i = 0; i < EventQueue::kCapacity; ++i) {
        ASSERT_TRUE(queue.push(noteOn(static_cast<uint8_t>(i % 128))));
    }

    EXPECT_FALSE(queue.push(noteOn(60)));

    SynthEvent event{};
    ASSERT_TRUE(queue.pop(event));
    EXPECT_TRUE(queue.push(noteOn(60)));
}

TEST(EventQueue, ConcurrentProducerAndConsumerDeliverEveryEventInOrder) {
    EventQueue queue;
    constexpr int kEvents = 100000;

    std::thread producer([&queue] {
        for (int i = 0; i < kEvents; ++i) {
            while (!queue.push(noteOn(static_cast<uint8_t>(i % 128)))) {
                std::this_thread::yield();
            }
        }
    });

    int received = 0;
    bool inOrder = true;
    SynthEvent event{};
    while (received < kEvents) {
        if (queue.pop(event)) {
            inOrder = inOrder && event.value == received % 128;
            ++received;
        }
    }
    producer.join();

    EXPECT_TRUE(inOrder);
}
