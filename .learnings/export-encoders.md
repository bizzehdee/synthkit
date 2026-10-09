# Export encoders on Android

Established 2026-10-09 from web search extracts (not archived under `research/`). Approved in `plan.md`; vendored in milestone 4 (see `THIRD_PARTY.md`).

- MP3: Android decodes MP3 but lists no MP3 encoder, and the CDD requires none. Encoding needs a library. Inferred from the supported-formats table; verify on a device with `MediaCodecList`.
- AAC: the platform AAC LC encoder exists. `MediaMuxer` writes MP4, WebM, 3GP, HEIF and Ogg, so audio-only MP4 (AAC) needs no dependency.
- FLAC: a platform encoder exists (listed from Android 4.1; the CDD requires it only on devices with a microphone). `MediaMuxer` cannot write a `.flac` file, so a raw FLAC writer or libFLAC is needed.
- LAME 3.100 (October 2017) is LGPL-2+. The MP3 patents expired in 2017. Upstream fixed a batch of memory-safety CVEs by 3.100, and I found no newer release or later advisory. A GPL-3.0 app can use LAME under the LGPL's conversion-to-GPL clause (my reading, not legal advice). A shared library avoids the LGPL relinking question.
- libFLAC is BSD-like (COPYING.Xiph). Latest release found: 1.5.0 (11 February 2025). Fixes for older CVEs landed in 1.3.4 and 1.4.0; none found for 1.5.0.
- WAV needs no dependency (PCM plus a header).

Sources:
- https://developer.android.com/media/platform/supported-formats
- https://developer.android.com/reference/android/media/MediaMuxer
- https://seclists.org/oss-sec/2017/q4/121
- https://newreleases.io/project/github/xiph/flac/release/1.5.0

Building them (2026-10-09, milestone 4):

- Neither library is built with its own build system. LAME's autotools output
  and libFLAC's CMake probe for Ogg, iconv and SIMD; our `third_party_config/`
  holds a fixed `config.h` per library and `codecs.cmake` compiles only the
  library sources.
- LAME's generated config normally adds `typedef float ieee754_float32_t` (and
  the 64-bit one); without it `util.h` does not compile. `fft.c` always includes
  `vector/lame_intrin.h`, so that header is vendored though the SSE source is not.
- libFLAC includes `deduplication/*.c` from `bitreader.c` and `lpc.c`; the
  folder must be vendored even with `FLAC__NO_ASM`.
- Host tests prove FLAC is lossless (decoded samples equal the input) and walk
  every MP3 frame header (MPEG-1 Layer III, 192 kbit/s, 44.1 kHz).
