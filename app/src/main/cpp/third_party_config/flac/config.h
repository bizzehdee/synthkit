/* Build configuration for the vendored libFLAC 1.5.0 on Android and the build
   host, in place of the header its CMake build would generate. Portable C
   only: no SIMD paths, no Ogg, no threads. Every supported Android ABI is
   little endian. */
#ifndef FLAC_CONFIG_H
#define FLAC_CONFIG_H

#define CPU_IS_BIG_ENDIAN 0
#define WORDS_BIGENDIAN 0
#define ENABLE_64_BIT_WORDS 1
#define OGG_FOUND 0
#define FLAC__HAS_OGG 0
#define FLAC__HAS_X86INTRIN 0
#define FLAC__HAS_NEONINTRIN 0
#define FLAC__HAS_A64NEONINTRIN 0
#define FLAC__NO_ASM 1
#define HAVE_LROUND 1
#define HAVE_INTTYPES_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_SYS_PARAM_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define HAVE_FSEEKO 1
#define PACKAGE_VERSION "1.5.0"

#endif
