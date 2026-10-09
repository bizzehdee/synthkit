# Third-party components

Every component that is vendored into this repository or bundled into the app.
Licence texts shipped in the app live in `app/src/main/assets/licences/`.

| Component | Version | Source | Licence | Where |
|---|---|---|---|---|
| Oboe | 1.11.0 | Google Maven, `com.google.oboe:oboe` | Apache-2.0 | Gradle dependency |
| TinySoundFont | commit `853a0a171759f1ddba0de1442133a75912bbeffa` (2026-07-19) | https://github.com/schellingb/TinySoundFont | MIT | `app/src/main/cpp/third_party/tinysoundfont/` |
| GeneralUser GS | v2.0.3, commit `684543d5e5efaef08d02be50dcda8d552478fa60` (2026-02-23) | https://github.com/mrbumpy409/GeneralUser-GS | GeneralUser GS License v2.0 (permissive) | `app/src/main/assets/GeneralUser-GS.sf2` |
| GoogleTest | 1.18.0 | https://github.com/google/googletest/releases | BSD-3-Clause | Host tests only, fetched at configure time, not shipped |

## SHA-256 of vendored files

| File | SHA-256 |
|---|---|
| `app/src/main/cpp/third_party/tinysoundfont/tsf.h` | `70d55963c98f60ebb81518eaa1f25d46888d5180eb5f5289fd6b74ffc177d197` |
| `app/src/main/assets/GeneralUser-GS.sf2` | `9575028c7a1f589f5770fccc8cff2734566af40cd26ed836944e9a5152688cfe` |
| `googletest-1.18.0.tar.gz` | `6e3191c1455468b3fc35a417fb565c1c5071aee1b7e7f85e30cf48a98d37d8b5` |

## Not yet shipped in the app

The Oboe Apache-2.0 notice is not yet in `assets/licences/`. The licence screen
(milestone 5) must add it.
