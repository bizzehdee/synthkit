# Vendored export encoders, shared by the app build and the host tests.
# Expects THIRD_PARTY and THIRD_PARTY_CONFIG to point at the source folders.

# LAME is LGPL, so it is its own shared library rather than part of ours.
file(GLOB LAME_SOURCES ${THIRD_PARTY}/lame/libmp3lame/*.c)
add_library(mp3lame SHARED ${LAME_SOURCES})
target_include_directories(mp3lame
        PUBLIC ${THIRD_PARTY}/lame/include
        PRIVATE ${THIRD_PARTY_CONFIG}/lame ${THIRD_PARTY}/lame/libmp3lame)
target_compile_definitions(mp3lame PRIVATE HAVE_CONFIG_H)
target_compile_options(mp3lame PRIVATE -w)
target_link_libraries(mp3lame PRIVATE m)

set(FLAC_SOURCE_NAMES bitmath bitreader bitwriter cpu crc fixed float format lpc md5 memory
        metadata_iterators metadata_object stream_decoder stream_encoder stream_encoder_framing window)
list(TRANSFORM FLAC_SOURCE_NAMES PREPEND ${THIRD_PARTY}/flac/src/libFLAC/)
list(TRANSFORM FLAC_SOURCE_NAMES APPEND .c)
add_library(FLAC STATIC ${FLAC_SOURCE_NAMES})
target_include_directories(FLAC
        PUBLIC ${THIRD_PARTY}/flac/include
        PRIVATE ${THIRD_PARTY_CONFIG}/flac ${THIRD_PARTY}/flac/src/libFLAC/include)
target_compile_definitions(FLAC PUBLIC FLAC__NO_DLL PRIVATE HAVE_CONFIG_H)
target_compile_options(FLAC PRIVATE -w)
set_target_properties(FLAC mp3lame PROPERTIES POSITION_INDEPENDENT_CODE ON)
