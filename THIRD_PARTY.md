# Third-party components

Every component that is vendored into this repository or bundled into the app.
Licence texts shipped in the app live in `app/src/main/assets/licences/`.

| Component | Version | Source | Licence | Where |
|---|---|---|---|---|
| Oboe | 1.11.0 | Google Maven, `com.google.oboe:oboe` | Apache-2.0 | Gradle dependency |
| TinySoundFont | commit `853a0a171759f1ddba0de1442133a75912bbeffa` (2026-07-19) | https://github.com/schellingb/TinySoundFont | MIT | `app/src/main/cpp/third_party/tinysoundfont/` |
| GeneralUser GS | v2.0.3, commit `684543d5e5efaef08d02be50dcda8d552478fa60` (2026-02-23) | https://github.com/mrbumpy409/GeneralUser-GS | GeneralUser GS License v2.0 (permissive) | `app/src/main/assets/GeneralUser-GS.sf2` |
| LAME | 3.100 (2017-10-13) | https://sourceforge.net/projects/lame/files/lame/3.100/ | LGPL-2.0-or-later, built as its own shared library `libmp3lame.so` | `app/src/main/cpp/third_party/lame/` (library sources and `lame.h` only) |
| libFLAC | 1.5.0 (2025-02-11) | https://downloads.xiph.org/releases/flac/ | BSD-3-Clause (COPYING.Xiph) | `app/src/main/cpp/third_party/flac/` (libFLAC sources and headers only) |
| GoogleTest | 1.18.0 | https://github.com/google/googletest/releases | BSD-3-Clause | Host tests only, fetched at configure time, not shipped |

## SHA-256 of vendored files

| File | SHA-256 |
|---|---|
| `app/src/main/cpp/third_party/tinysoundfont/tsf.h` | `70d55963c98f60ebb81518eaa1f25d46888d5180eb5f5289fd6b74ffc177d197` |
| `app/src/main/assets/GeneralUser-GS.sf2` | `9575028c7a1f589f5770fccc8cff2734566af40cd26ed836944e9a5152688cfe` |
| `lame-3.100.tar.gz` (also matches Debian's `lame_3.100.orig.tar.gz`) | `ddfe36cab873794038ae2c1210557ad34857a4b6bdc515785d1da9e175b1da1e` |
| `flac-1.5.0.tar.xz` (matches Xiph's `SHA256SUMS.txt`) | `f2c1c76592a82ffff8413ba3c4a1299b6c7ab06c734dee03fd88630485c2b920` |
| `googletest-1.18.0.tar.gz` | `6e3191c1455468b3fc35a417fb565c1c5071aee1b7e7f85e30cf48a98d37d8b5` |

## Build configuration of vendored code

`app/src/main/cpp/third_party_config/` holds our own `config.h` for LAME and
libFLAC (in place of their autotools and CMake output) and `codecs.cmake`. The
vendored files themselves are unchanged. SIMD, Ogg, decoder and frontend sources
were left out of the copies because they are not built.

## Not yet shipped in the app

The Oboe Apache-2.0 notice is not yet in `assets/licences/`. The licence screen
(milestone 5) must add it.
