#include "audio/NoteQueue.h"

#include <thread>

#include <gtest/gtest.h>

using synthkit::NoteEvent;
using synthkit::NoteQueue;

TEST(NoteQueue, EmptyQueueHasNothingToPop) {
    NoteQueue queue;
    NoteEvent event{};

    EXPECT_FALSE(queue.pop(event));
}

TEST(NoteQueue, EventsComeOutInTheOrderTheyWentIn) {
    NoteQueue queue;
    queue.push({9, 36, 0.8f});
    queue.push({9, 38, 0.5f});

    NoteEvent first{};
    NoteEvent second{};
    ASSERT_TRUE(queue.pop(first));
    ASSERT_TRUE(queue.pop(second));

    EXPECT_EQ(first.key, 36);
    EXPECT_FLOAT_EQ(first.velocity, 0.8f);
    EXPECT_EQ(second.key, 38);
}

TEST(NoteQueue, FullQueueRejectsPushUntilAnEventIsPopped) {
    NoteQueue queue;
    for (size_t i = 0; i < NoteQueue::kCapacity; ++i) {
        ASSERT_TRUE(queue.push({0, static_cast<uint8_t>(i % 128), 1.0f}));
    }

    EXPECT_FALSE(queue.push({0, 60, 1.0f}));

    NoteEvent event{};
    ASSERT_TRUE(queue.pop(event));
    EXPECT_TRUE(queue.push({0, 60, 1.0f}));
}

TEST(NoteQueue, ConcurrentProducerAndConsumerDeliverEveryEventInOrder) {
    NoteQueue queue;
    constexpr int kEvents = 100000;

    std::thread producer([&queue] {
        for (int i = 0; i < kEvents; ++i) {
            while (!queue.push({0, static_cast<uint8_t>(i % 128), 1.0f})) {
                std::this_thread::yield();
            }
        }
    });

    int received = 0;
    bool inOrder = true;
    NoteEvent event{};
    while (received < kEvents) {
        if (queue.pop(event)) {
            inOrder = inOrder && event.key == received % 128;
            ++received;
        }
    }
    producer.join();

    EXPECT_TRUE(inOrder);
}
