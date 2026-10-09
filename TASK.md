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

## Milestone 2: all instrument families, browser, latency warning

Branch `feat/milestone-2`. Defaults chosen by Claude where the plan is silent are marked "Default".

- [x] TASK-005: Synth events for every layout
  - The engine accepts program change (bank and program, bank 128 for kits) and note off, besides note on.
  - Events may carry a delay in frames, applied sample-accurately on the audio thread (used for strums now, the looper later).
  - The engine lists the SoundFont's presets (bank, program, name) after loading.
  - GoogleTest host tests.
  - Depends on: TASK-002. Required by: TASK-006 to TASK-010.
- [ ] TASK-006: Instrument catalogue
  - All 128 GM programs with their standard names (string resources), family tab and layout from `docs/gm-layouts.md`, and hold-on-by-default flags.
  - Drum kits from SoundFont bank 128, except GS programs 56 and 127.
  - JVM unit tests for the mapping.
  - Depends on: TASK-005. Required by: TASK-007.
- [ ] TASK-007: Home screen, instrument screen and browser
  - Home: four quick entries (Keys, Guitar/Bass, Drums/Percussion, Synth) and a Browse entry.
  - Browser: seven family tabs, search by name, favourites (star toggle) and a recents row. Favourites and recents persist in app-private storage. Default: recents keep the last 8 instruments.
  - Choosing an instrument opens its layout. Default: a quick entry opens the first program of its tab (Acoustic Grand Piano, Acoustic Guitar (nylon), Standard 1 kit, Lead 1 (square)).
  - Depends on: TASK-006. Required by: TASK-008, TASK-009, TASK-010, TASK-011.
- [ ] TASK-008: Keys layout
  - White keys at least 48 dp wide and 72 dp tall, black keys about 30 dp wide. Octave shift buttons and a scroll strip. Hold toggle with sustain-pedal behaviour; replaying a held note restarts it.
  - Multi-touch. Notes sound while held. Default: a finger that slides onto another key keeps its first note.
  - Depends on: TASK-005, TASK-007.
- [ ] TASK-009: Chords layout
  - Key and mode (major or minor) pickers, seven diatonic triad pads (I to vii).
  - Tap: short automatic strum. Holding a pad and swiping the strum strip plays string by string at the swipe speed. Default: six-string voicing from standard tuning (E A D G B E), each string taking the nearest chord tone at or above its open note; strum spacing 12 ms; chord notes stop when the pad is released.
  - Bass programs: the pad plays the chord root in a low octave, with octave shift.
  - Depends on: TASK-005, TASK-007.
- [ ] TASK-010: Pads layout
  - Drum kits: GM percussion notes 35 to 81, first page in the agreed order, 4 x 2 on small screens and 4 x 4 where space allows, further pages swiped sideways. Kit picker.
  - Melodic pads (timpani, synth effects, percussive): chromatic from a root with octave shift, sounding while held. Sound effects 121 to 128: one-shot.
  - Multi-touch on every pad layout.
  - Depends on: TASK-005, TASK-007.
- [ ] TASK-011: Latency warning
  - Dismissible banner when the stream is not in low-latency mode or the output device is Bluetooth. Suggests wired headphones or the speaker. Play is never blocked.
  - Re-evaluated when the output device changes.
  - Depends on: TASK-004, TASK-007.
