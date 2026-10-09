cmake_minimum_required(VERSION 3.22)
project(synthkit LANGUAGES C CXX)

# C++20 defines left shifts of negative values, which TinySoundFont uses when
# reading coarse sample offsets; in C++17 they are undefined behaviour.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

set(THIRD_PARTY ${CMAKE_CURRENT_SOURCE_DIR}/third_party)
set(THIRD_PARTY_CONFIG ${CMAKE_CURRENT_SOURCE_DIR}/third_party_config)
include(${THIRD_PARTY_CONFIG}/codecs.cmake)

add_library(synthkit SHARED
        audio/AssetSoundFont.cpp
        audio/AudioEngine.cpp
        audio/OfflineRenderer.cpp
        audio/Sequencer.cpp
        audio/SoundFontSynth.cpp
        audio/TinySoundFont.cpp
        export/AudioEncoder.cpp
        jni/NativeAudioEngine.cpp
        jni/NativeEncoders.cpp)

target_include_directories(synthkit PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/third_party/tinysoundfont)

target_compile_options(synthkit PRIVATE -Wall -Wextra)
# Third-party code is compiled as published; its warnings are not ours to fix.
set_source_files_properties(audio/TinySoundFont.cpp PROPERTIES COMPILE_OPTIONS "-w")

target_link_libraries(synthkit PRIVATE oboe::oboe android log mp3lame FLAC)
