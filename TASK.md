# Tasks

Source of scope: `plan.md`. One commit per task, on branch `feat/milestone-1`.

## Milestone 1: skeleton, low-latency audio, one playable instrument

- [x] TASK-001: Android project skeleton
  - Gradle wrapper, version catalogue, single `:app` module, package `com.bizzeh.synthkit`, minSdk 26.
  - Kotlin + Jetpack Compose, Material 3 theme: dynamic colour on Android 12+, indigo/violet and amber fallback, light and dark.
  - Landscape only. No INTERNET permission. All strings in resources.
  - JUnit 4 unit tests, Compose UI instrumented tests, `verify` Gradle task.
  - `README.md` for end users.
  - Required by: TASK-002, TASK-003, TASK-004.
- [x] TASK-002: Native audio engine
  - Oboe output stream: low-latency performance mode, exclusive sharing requested, float stereo.
  - TinySoundFont plays the bundled GeneralUser GS SoundFont from the APK assets.
  - Notes reach the audio thread through a lock-free single-producer queue. TinySoundFont is called only on the audio thread.
  - The stream reopens after a device change (for example, headphones unplugged).
  - Origin, version and licence recorded for every vendored file.
  - GoogleTest host tests for the queue and the synth renderer, run from `verify`.
  - Depends on: TASK-001. Required by: TASK-003, TASK-004.
- [x] TASK-003: Drum pad instrument
  - 4 x 2 pad grid on the GM drum channel, first-page order from `docs/gm-layouts.md`.
  - Multi-touch: two or more pads pressed at the same time each play.
  - Sound starts on finger down, not on release. Fixed velocity. One-shot.
  - Pads at least 48 dp wide and 72 dp tall. Each pad has a TalkBack description.
  - Depends on: TASK-001, TASK-002.
- [x] TASK-004: Latency readout and measurement
  - On-screen readout and logcat line: Oboe-reported output latency, audio API, performance mode, sharing mode, sample rate, burst and buffer size.
  - Measured on every attached test phone. Results recorded in `.learnings/`.
  - Depends on: TASK-002, TASK-003.
