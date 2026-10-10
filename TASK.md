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
- [x] TASK-006: Instrument catalogue
  - All 128 GM programs with their standard names (string resources), family tab and layout from `docs/gm-layouts.md`, and hold-on-by-default flags.
  - Drum kits from SoundFont bank 128, except GS programs 56 and 127.
  - JVM unit tests for the mapping.
  - Depends on: TASK-005. Required by: TASK-007.
- [x] TASK-007: Home screen, instrument screen and browser
  - Home: four quick entries (Keys, Guitar/Bass, Drums/Percussion, Synth) and a Browse entry.
  - Browser: seven family tabs, search by name, favourites (star toggle) and a recents row. Favourites and recents persist in app-private storage. Default: recents keep the last 8 instruments.
  - Choosing an instrument opens its layout. Default: a quick entry opens the first program of its tab (Acoustic Grand Piano, Acoustic Guitar (nylon), Standard 1 kit, Lead 1 (square)).
  - Depends on: TASK-006. Required by: TASK-008, TASK-009, TASK-010, TASK-011.
- [x] TASK-008: Keys layout
  - White keys at least 48 dp wide and 72 dp tall, black keys about 30 dp wide. Octave shift buttons and a scroll strip. Hold toggle with sustain-pedal behaviour; replaying a held note restarts it.
  - Multi-touch. Notes sound while held. Default: a finger that slides onto another key keeps its first note.
  - Depends on: TASK-005, TASK-007.
- [x] TASK-009: Chords layout
  - Key and mode (major or minor) pickers, seven diatonic triad pads (I to vii).
  - Tap: short automatic strum. Holding a pad and swiping the strum strip plays string by string at the swipe speed. Default: six-string voicing from standard tuning (E A D G B E), each string taking the nearest chord tone at or above its open note; strum spacing 12 ms; chord notes stop when the pad is released.
  - Bass programs: the pad plays the chord root in a low octave, with octave shift.
  - Depends on: TASK-005, TASK-007.
- [x] TASK-010: Pads layout
  - Drum kits: GM percussion notes 35 to 81, first page in the agreed order, 4 x 2 on small screens and 4 x 4 where space allows, further pages swiped sideways. Kit picker.
  - Melodic pads (timpani, synth effects, percussive): chromatic from a root with octave shift, sounding while held. Sound effects 121 to 128 also sound while held, because several of their samples loop (see `docs/gm-layouts.md`).
  - Multi-touch on every pad layout.
  - Drum tab test (requested 2026-10-09): a UI test plays the user's two-part drum tab through the pad grid step by step, with simultaneous hits pressed together, and checks the notes. Lanes: C crash 49, HH closed hat 42, Rd ride 51, S snare 38, B kick 36, T high tom 50, F floor tom 43, Hf pedal hat 44. The tab is kept as a test fixture.
  - Depends on: TASK-005, TASK-007.
- [x] TASK-011: Latency warning
  - Dismissible banner when the stream is not in low-latency mode or the output device is Bluetooth. Suggests wired headphones or the speaker. Play is never blocked.
  - Re-evaluated when the output device changes.
  - Depends on: TASK-004, TASK-007.

## Milestone 3: projects, metronome, looper, track controls, loop editor

Branch `feat/milestone-3`. Defaults chosen by Claude where the plan is silent are marked "Default".

Timing model: notes are stored in ticks (480 per quarter note, 1920 per 4/4 bar), so a tempo change keeps the music. The native engine runs a tick clock, plays the loop and the metronome sample-accurately, and records live notes with their clock position minus the output latency, so a take lines up with what the player heard. Kotlin turns recorded notes into takes, snaps the start and rounds the length.

- [x] TASK-012: Project files and project list
  - Project, track, take and note model in ticks; JSON through kotlinx-serialization; every loaded file validated (tempo, bars, keys, velocities, ticks, track count); a broken file is skipped and logged, never fatal.
  - One file per project in app-private storage, written atomically (temporary file, then rename). Autosave after every change.
  - Project list on launch: new, rename, duplicate, delete (with confirmation). Default: new projects are named "Project 1", "Project 2" and so on, at 120 BPM.
  - JVM tests for the model, validation and store.
  - Depends on: TASK-007. Required by: TASK-014.
- [x] TASK-013: Native transport, sequencer and metronome
  - Tick clock at the project tempo; loop playback of every track's notes at their sample position; metronome click (bell on beat 1).
  - Recording captures live notes with clock position minus output latency into a lock-free queue for Kotlin.
  - Loop notes reach the audio thread as an immutable snapshot swapped atomically; the audio thread never allocates or frees memory.
  - Per-channel volume. Each track uses its own MIDI channel.
  - GoogleTest host tests.
  - Depends on: TASK-005. Required by: TASK-014.
- [x] TASK-014: Project screen and looper flow
  - Track list with Add track (quick entries and Browse), tempo (40 to 240), metronome-during-playback toggle, Record and Play/Stop.
  - First take: Record arms the click; recording starts on the first tap and snaps to the nearest click beat; Record again stops, rounds to the nearest whole bar (1 to 8) and the loop plays at once.
  - Later takes: Record records the selected track while the loop plays; notes wrap to the loop length and overdub; Record again ends the take and the loop keeps playing.
  - Track controls: undo last take (repeatable back to the first take), mute, solo, volume, clear, delete (with confirmation), instrument swap within kind.
  - The track's instrument layout is played with a compact transport bar above it.
  - Depends on: TASK-012, TASK-013. Required by: TASK-015, TASK-016.
- [x] TASK-015: Loop editor
  - Per-track quantise (off, 1/8, 1/16), non-destructive: raw timing is kept.
  - Step grid with 1/16 columns over the whole loop, scrolling sideways. Rows: one per pad for drum tracks, one per semitone for melodic tracks, scrolling, opened at the track's notes.
  - Default: tap an empty cell to add a note; tap a note to select it and show a velocity slider and Delete; tap the selected note again to remove it. Drag a note to move it in time and pitch.
  - Double loop: doubles the loop length (up to 8 bars) and repeats every track's notes.
  - Known limit: TalkBack reads a description of the grid, but cannot yet add, select or move notes cell by cell.
  - Depends on: TASK-014.
- [x] TASK-016: Track limit on the budget phone
  - Measure audio callback load and underruns on the Galaxy A03 with 4, 6 and 8 busy tracks; set the shipped limit (design 8, never below 4); record the result in `.learnings/`.
  - Depends on: TASK-014.

## Milestone 4: export to audio and MIDI

Branch `feat/milestone-4`. Defaults chosen by Claude where the plan is silent are marked "Default".

Default export settings: 44.1 kHz stereo, 16-bit; MP3 192 kbit/s CBR; AAC 192 kbit/s; FLAC level 5; loop passes 1 to 16, default 2, used by every format; audio adds a 2-second release tail. MIDI drum tracks use channel 10 with the kit as a GS program change; melodic tracks use channels 1 to 9 and 11 to 16.

- [x] TASK-017: Offline renderer
  - Renders the project's notes for N passes plus the tail, in blocks, on a worker thread, while live audio keeps playing.
  - Uses a TinySoundFont copy of an idle template made at load, so the 32 MB SoundFont is not loaded twice and the live instance is never read concurrently.
  - Streams: memory use does not grow with export length.
  - GoogleTest host tests. Depends on: TASK-013. Required by: TASK-018 to TASK-021.
- [x] TASK-018: WAV and MIDI writers
  - WAV: streaming 16-bit PCM writer, header fixed up at the end.
  - MIDI: Type 1 file, a tempo and time-signature track, one track per project track with program change and notes.
  - JVM tests on the bytes. Depends on: TASK-017.
- [x] TASK-019: MP3 and FLAC encoders
  - LAME 3.100 and libFLAC 1.5.0 vendored from verified official tarballs, built with CMake. LAME is a separate shared library (LGPL).
  - Streaming native encoders fed from the renderer.
  - Host tests decode the output (FLAC with libFLAC, MP3 frame headers) and check length and format.
  - Licence notices added. Depends on: TASK-017.
- [x] TASK-020: AAC in MP4
  - Platform AAC encoder through MediaCodec and MediaMuxer, fed from the renderer.
  - Device test reads the file back with MediaExtractor. Depends on: TASK-017.
- [x] TASK-021: Export screen
  - From the project screen: format, loop passes, Export with progress and Cancel, then Save (system file picker) or Share.
  - Export files live in app cache and the previous export is deleted at the next export. No storage permission.
  - Depends on: TASK-018, TASK-019, TASK-020.

## Redesign: studio-hardware look

Branch `feat/redesign`. Reference: the local mockups the user approved on 2026-10-09 (drums, keys, project, project list; light and dark). Every screen keeps its behaviour and tests; only the look changes, except the diagnostics panel.

- [x] TASK-022: Design foundation
  - Brand light and dark colour schemes, no dynamic colour; instrument family colours; shapes and spacing.
  - Bundled Archivo (variable) and JetBrains Mono Bold fonts, with origin and licence recorded.
  - Shared studio components: panel buttons, lit toggle, LCD transport display, round Record and Play/Stop, square M/S toggles, small fader, family chip, page dots.
  - Depends on: TASK-021. Required by: TASK-023 to TASK-025.
- [x] TASK-023: Play screens
  - Top bar: back, family chip that opens the instrument picker, LCD (beat lights, bar, BPM, REC), Record, Play/Stop, More menu (Diagnostics).
  - Lit drum, chord and chromatic pads; ivory keyboard on a dark keybed with lit HOLD, octave stepper and range mini-map; strum strip.
  - Diagnostics panel replaces the on-screen readout; the latency warning keeps its banner, restyled.
  - Depends on: TASK-022.
- [x] TASK-024: Project screen
  - LCD with tempo stepper, beat lights and bar; lit Click; Play/Stop; Export.
  - Track rows: family tab and icon tile, name, loop note preview with playhead, square M and S, fader, More; dashed Add track row.
  - Depends on: TASK-022.
- [x] TASK-025: Project list and remaining screens
  - Project list: brand mark, amber New project, cards with a preview of each project's tracks.
  - Add track, browser, loop editor, export screen and dialogs in the same look.
  - Depends on: TASK-022. Required by: TASK-026.

## Milestone 5: release

Branch `feat/milestone-5`. Device tests run on the 3 test phones only (Xperia 1 II, Xperia XZ Premium, Galaxy A03), not the Pixel 11 (user, 2026-10-09).

- [x] TASK-026: Settings screen
  - Gear button on the project list opens Settings.
  - Theme: System (default), Light or Dark, applied at once and kept after restart.
  - Haptic feedback on taps, on by default.
  - Metronome during playback and quantise, with the meaning the user chooses before work starts.
  - Settings stored on the device only. TASK-027 and TASK-028 add the links to the licence screen and the privacy policy.
  - Depends on: TASK-025. Required by: TASK-027, TASK-028.
- [ ] TASK-027: Licence screen
  - Opened from Settings. Lists every third-party part with its licence text: Oboe, TinySoundFont, LAME, libFLAC, GeneralUser GS, Material icons, Archivo, JetBrains Mono, the AndroidX, Jetpack Compose, DataStore, Okio and Kotlin libraries (Apache-2.0), and the app's own GPL-3.0.
  - Texts are bundled in the app; no network.
  - Device test: every listed part opens its full text.
  - Depends on: TASK-026.
- [ ] TASK-028: Privacy policy
  - Draft the full policy text in `docs/privacy-policy.md`: no data collected, no network, no ads, no accounts, files stay on the device unless the user shares them, microphone not used, publisher contact.
  - The same text shown in the app from Settings.
  - The user approves the text, then publishes it on their website.
  - Depends on: TASK-026. Required by: TASK-032.
- [ ] TASK-029: App icon
  - SVG source in the repository: abstract keys or pad-grid mark, flat, indigo/violet with amber.
  - Adaptive launcher icon (foreground, background and monochrome layers) replaces the placeholder.
  - 512 x 512 32-bit PNG with alpha for Play, at most 1024 KB.
  - The user approves the design from local previews before it goes in.
  - Required by: TASK-030, TASK-032.
- [ ] TASK-030: Header image
  - 1024 x 500 feature graphic, JPEG or 24-bit PNG without alpha, from an SVG source, matching the icon.
  - The user approves it from a local preview.
  - Depends on: TASK-029. Required by: TASK-032.
- [ ] TASK-031: GitHub Actions CI
  - On push and pull request: build, JVM tests, native host tests, lint. Device tests stay local.
  - No secrets needed for this workflow.
  - Required by: TASK-033.
- [ ] TASK-032: Play listing guide
  - `docs/play-listing.md`: title, short and full description, category, content rating answers, data-safety answers, target age groups (13-15, 16-17, 18+), privacy policy URL, asset list.
  - Phone screenshots taken from the app in the studio look.
  - The user approves the text in conversation.
  - Depends on: TASK-028, TASK-029, TASK-030.
- [ ] TASK-033: Release build and internal testing
  - Release signing read from properties or environment variables outside the repository; never committed.
  - Signed Android App Bundle built and checked: minified build runs on the 3 test phones.
  - Version set for the first release.
  - Step-by-step guide for the user: create the upload key, create the app in Play Console, upload to internal testing, then promote to production.
  - Depends on: TASK-031, TASK-032.
