# Host build of the platform-independent audio code, tested with GoogleTest.
# Run through the Gradle task nativeHostTest.
cmake_minimum_required(VERSION 3.22)
project(synthkit_host_tests LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(googletest
        URL https://github.com/google/googletest/releases/download/v1.18.0/googletest-1.18.0.tar.gz
        URL_HASH SHA256=6e3191c1455468b3fc35a417fb565c1c5071aee1b7e7f85e30cf48a98d37d8b5
        DOWNLOAD_EXTRACT_TIMESTAMP TRUE)
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

set(MAIN_CPP ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp)
set(THIRD_PARTY ${MAIN_CPP}/third_party)
set(THIRD_PARTY_CONFIG ${MAIN_CPP}/third_party_config)
include(${THIRD_PARTY_CONFIG}/codecs.cmake)

add_executable(synthkit_host_tests
        EventQueueTest.cpp
        SoundFontSynthTest.cpp
        SequencerTest.cpp
        OfflineRendererTest.cpp
        AudioEncoderTest.cpp
        ${MAIN_CPP}/export/AudioEncoder.cpp
        ${MAIN_CPP}/audio/OfflineRenderer.cpp
        ${MAIN_CPP}/audio/SoundFontSynth.cpp
        ${MAIN_CPP}/audio/Sequencer.cpp
        ${MAIN_CPP}/audio/TinySoundFont.cpp)

target_include_directories(synthkit_host_tests PRIVATE
        ${MAIN_CPP}
        ${MAIN_CPP}/third_party/tinysoundfont)
# Encoder tests write into the build folder, never the system temporary folder.
file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/test-output)
target_compile_definitions(synthkit_host_tests PRIVATE
        SYNTHKIT_SOUND_FONT="${CMAKE_CURRENT_SOURCE_DIR}/../../main/assets/GeneralUser-GS.sf2"
        SYNTHKIT_TEST_OUTPUT="${CMAKE_CURRENT_BINARY_DIR}/test-output")
target_compile_options(synthkit_host_tests PRIVATE -Wall -Wextra)

# Sanitizers catch memory and lifetime errors in the lock-free code on the host.
include(CheckLinkerFlag)
check_linker_flag(CXX -fsanitize=address,undefined SYNTHKIT_HAS_SANITIZERS)
if(SYNTHKIT_HAS_SANITIZERS)
    target_compile_options(synthkit_host_tests PRIVATE -fsanitize=address,undefined -fno-omit-frame-pointer)
    target_link_options(synthkit_host_tests PRIVATE -fsanitize=address,undefined)
else()
    message(WARNING "This compiler cannot link the address and undefined-behaviour sanitizers; tests run without them.")
endif()
set_source_files_properties(${MAIN_CPP}/audio/TinySoundFont.cpp PROPERTIES COMPILE_OPTIONS "-w")
target_link_libraries(synthkit_host_tests PRIVATE GTest::gtest_main mp3lame FLAC)

enable_testing()
include(GoogleTest)
gtest_discover_tests(synthkit_host_tests)
