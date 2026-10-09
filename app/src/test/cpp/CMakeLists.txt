# Host build of the platform-independent audio code, tested with GoogleTest.
# Run through the Gradle task nativeHostTest.
cmake_minimum_required(VERSION 3.22)
project(synthkit_host_tests LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(googletest
        URL https://github.com/google/googletest/releases/download/v1.18.0/googletest-1.18.0.tar.gz
        URL_HASH SHA256=6e3191c1455468b3fc35a417fb565c1c5071aee1b7e7f85e30cf48a98d37d8b5
        DOWNLOAD_EXTRACT_TIMESTAMP TRUE)
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

set(MAIN_CPP ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp)

add_executable(synthkit_host_tests
        NoteQueueTest.cpp
        SoundFontSynthTest.cpp
        ${MAIN_CPP}/audio/SoundFontSynth.cpp
        ${MAIN_CPP}/audio/TinySoundFont.cpp)

target_include_directories(synthkit_host_tests PRIVATE
        ${MAIN_CPP}
        ${MAIN_CPP}/third_party/tinysoundfont)
target_compile_definitions(synthkit_host_tests PRIVATE
        SYNTHKIT_SOUND_FONT="${CMAKE_CURRENT_SOURCE_DIR}/../../main/assets/GeneralUser-GS.sf2")
target_compile_options(synthkit_host_tests PRIVATE -Wall -Wextra)
set_source_files_properties(${MAIN_CPP}/audio/TinySoundFont.cpp PROPERTIES COMPILE_OPTIONS "-w")
target_link_libraries(synthkit_host_tests PRIVATE GTest::gtest_main)

enable_testing()
include(GoogleTest)
gtest_discover_tests(synthkit_host_tests)
