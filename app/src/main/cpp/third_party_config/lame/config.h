/* Build configuration for the vendored LAME 3.100 library on Android and the
   build host, in place of the header its autotools build would generate. Only
   the library is built: no decoder (HAVE_MPGLIB unset), no frontend. */
#ifndef LAME_CONFIG_H
#define LAME_CONFIG_H

#define STDC_HEADERS 1
#define HAVE_ERRNO_H 1
#define HAVE_FCNTL_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_LIMITS_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_STRINGS_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define LAME_LIBRARY_BUILD 1
#define PACKAGE_VERSION "3.100"

/* Added by LAME's configure script when the platform has no such types. */
typedef float ieee754_float32_t;
typedef double ieee754_float64_t;

#endif
