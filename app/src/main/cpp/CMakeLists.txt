cmake_minimum_required(VERSION 3.22)
project(synthkit LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(synthkit SHARED
        audio/AssetSoundFont.cpp
        audio/AudioEngine.cpp
        audio/SoundFontSynth.cpp
        audio/TinySoundFont.cpp
        jni/NativeAudioEngine.cpp)

target_include_directories(synthkit PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/third_party/tinysoundfont)

target_compile_options(synthkit PRIVATE -Wall -Wextra)
# Third-party code is compiled as published; its warnings are not ours to fix.
set_source_files_properties(audio/TinySoundFont.cpp PROPERTIES COMPILE_OPTIONS "-w")

target_link_libraries(synthkit PRIVATE oboe::oboe android log)
