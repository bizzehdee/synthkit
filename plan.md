# Synth Kit plan

## Purpose

Phone and tablet users who want to tap out music have no free, open-source Android
instrument app with low-latency drums, keyboards, guitar/bass and other instruments.
Synth Kit is that app: free on Google Play, fully open source.

## Users

- **General and casual users, aged 11 and over** (UK high school age and above). Play instruments by tapping the screen, for fun or to sketch ideas.
- **Not a production tool.** The app is not aimed at professional or studio use, but it is not a toy: the sounds and features are real, with a simple interface.

## Scope

In scope (v1):
- Tap-playable instruments: drums/percussion, keyboards, guitar/bass, misc.
- Sound from the GeneralUser GS SoundFont.
- Named projects, saved on-device.
- Record and loop, with layered tracks.
- Metronome and quantise.
- Export to audio and MIDI.
- Offline use, with no accounts, analytics or ads.

Out of scope:
- iOS and other platforms (Android only).
- Cloud sync, accounts, social features.
- Ads, analytics, in-app purchases.
- Built-in synthesiser (dropped from v1, see Features).
- Translations other than English (UK) in v1.
- Anything not listed above, until the user adds it.

## Features

| Feature | What it does | Status |
|---|---|---|
| Drums/percussion | Several kits, pad grid on GM drum notes | built |
| Keyboards | Piano, e-piano, organ, synth; on-screen keys with octave shift buttons, a scroll strip and a sustain-style hold toggle (decided 2026-10-09) | built |
| Guitar/bass | Acoustic, electric, bass. Chord pads only (decided 2026-10-09, replacing the earlier two-layout choice): seven diatonic triad pads for a chosen key and mode; tap plays a quick auto-strummed chord, swiping the strip while holding a pad strums string by string. Bass pads play the chord roots in a low octave. Layout detail in `docs/gm-layouts.md` | built |
| Misc instruments | Strings, brass, mallets, world; generic pad/key layout | built |
| Instrument browser | Every General MIDI program and drum kit in GeneralUser GS, grouped into a few family tabs, with search, favourites and a recents row; the four families are the quick entry points | built |
| Built-in synth | Synthesised sounds alongside the SoundFonts | dropped (2026-10-09; GM synth programs cover v1) |
| Projects | Project list on launch: new, rename, duplicate, delete. Each project autosaves on every change (decided 2026-10-09). Inside a project, Add track shows the four quick entries and Browse to pick the track's instrument; there is no separate free-play home screen (decided 2026-10-09) | built |
| Record and loop | Looper-pedal flow for every instrument (decided 2026-10-09, replacing fixed-bars-first): set tempo, press Start, play; recording and the clock begin on the first tap and you hear yourself as you play; press Stop and the take is rounded to the nearest whole bar (1 to 8) and becomes the project loop, playing immediately. Workflow: each track holds one instrument; add a track, pick its instrument (any family), press Start and play while the other tracks loop underneath (mute any you do not want to hear, or all of them); Stop ends the take. The metronome clicks while armed; the take's start snaps to the click beat nearest the first tap, so the loop lines up with the beat the player heard (decided 2026-10-09). Later takes wrap to the loop length and overdub. Repeat for as many tracks as the device allows. Per track: undo last take, mute, solo, volume, clear, delete, and instrument swap within kind (drum kit to drum kit, melodic to melodic) | built |
| Loop editor | Tidy a recorded track afterwards: quantise (off, 1/8, 1/16) applied non-destructively with raw timing kept; a step grid (rows per pad, or one row per semitone for melodic tracks, scrollable and opened at the track's notes; 1/16 columns) to tap hits in or out; drag to move a note in time and pitch; tap a note to select it and show a velocity slider (decided 2026-10-09); and a double-loop action that doubles the project loop length (up to 8 bars), repeating every existing track's content so a longer part can be recorded over it (decided 2026-10-09) | built |
| Metronome and quantise | Tempo 40-240 BPM, 4/4 only; the click uses the SoundFont's GS Metronome Bell (34) on beat 1 and Metronome Click (33) on other beats (decided 2026-10-09); click runs while armed and recording, toggle for playback. Quantise is per track and applied in the loop editor (see Loop editor), not forced at record time (decided 2026-10-09) | built |
| Export | Save or share files as MIDI (type 1), WAV, MP3, FLAC and audio-only MP4 (AAC). Audio is a mixdown of a user-chosen number of loop passes; MIDI has one track per layer plus tempo and program (decided 2026-10-09). Defaults: 1 to 16 passes (2 by default) for every format; 44.1 kHz 16-bit stereo; MP3 and AAC at 192 kbit/s; a 2-second release tail on audio; drum tracks on MIDI channel 10 | built |
| Latency warning | Dismissible banner when the audio stream is not low-latency or the output is Bluetooth, suggesting wired or speaker; play is never blocked (decided 2026-10-09). The latency and load readout moves from the play screens to a Diagnostics panel reached from the More menu (decided 2026-10-09) | built |
| Licence screen | In-app notices for Oboe, TinySoundFont, LAME, libFLAC, GeneralUser GS, Material icons, Archivo and JetBrains Mono | planned |
| Settings | Haptic feedback on taps (on by default), metronome during playback, quantise | planned |
| Privacy policy | Policy text for the Play listing and in-app, stating that the app collects no data, uses no network and has no ads or accounts | planned |
| Play Store listing guide | A markdown file in the repo with everything needed to fill in the Play Console listing: title, short and full description, category, content and data-safety answers, age declaration, links | planned |
| App icon | One icon design, used both as the Play Store icon (512 x 512, decided 2026-10-09) and as the app's launcher icon (same artwork, adaptive-icon layers for Android) | planned |
| Header image | The Play Store feature graphic (header banner), matching the icon and the app's design language | planned |
| FluidR3 download | Optional on-demand download of the FluidR3 GM SoundFont | dropped (2026-10-09, not in v1) |

## Stack

- **Kotlin + Jetpack Compose** for the UI. Native Android, because tap latency is the hard requirement.
- **Oboe (C++)** for low-latency audio output. Apache-2.0.
- **TinySoundFont** to play SF2 files from the audio callback. MIT, single header, simple to drive from Oboe.
- **Export encoders** (approved 2026-10-09): WAV written in-app; AAC in MP4 via the platform encoder and `MediaMuxer`; MP3 via LAME 3.100 built from official source as a shared library; FLAC via libFLAC 1.5.0. Each is pinned and its notice shipped (see `.learnings/export-encoders.md`).
- **minSdk 26** (Android 8), phones and tablets.
- **Hosting and CI** (decided 2026-10-09): public GitHub repository. GitHub Actions for build and tests is deferred to milestone 5 to save Action minutes; local builds and tests until then. Release bundles are signed with an upload key kept outside the repository; Google Play App Signing holds the app signing key.
- **Project files** (decided 2026-10-09): kotlinx-serialization-json 1.11.0 (JetBrains, Apache-2.0) reads and writes the project JSON. Every loaded file is validated before use.
- **Tests** (decided 2026-10-09): JUnit 4 for Kotlin unit tests, Compose UI tests on a device or emulator, GoogleTest for the C++ audio engine on the build host.
- **Licence of own code: GPL-3.0.** Soundfonts are data and keep their own licences.

## Data

- Projects (tracks, tempo, loop length, quantise, instrument choice): one JSON file per project in app-private storage (decided 2026-10-09).
- Exported audio/MIDI: files the user chooses to save or share.
- SoundFont: GeneralUser GS bundled in the APK.

## Integrations

- Google Play: distribution only.
- None otherwise. No analytics, ads or accounts.

## Constraints

- Tap-to-sound latency <= 20 ms on a mainstream phone (needs AAudio low-latency mode; not guaranteed on every device).
- No network access in v1: the app does not request the INTERNET permission.
- No personal data collected or transmitted.
- Play Console target age groups declared: 13-15, 16-17 and 18+ (decided 2026-10-09). The listing, icon and in-app wording must not be child-oriented, because that can override the declaration.
- Screens: primarily phones of 5-6.5 inches, also usable on 8-11 inch tablets.
- Orientation: landscape only (decided 2026-10-09). Android 16+ ignores the lock on screens 600 dp or wider, so a portrait window there shows only a message asking the user to rotate the device (decided 2026-10-09, see `.learnings/large-screen-orientation.md`).
- Touch targets: minimum 48 dp for every interactive control (decided 2026-10-09). One agreed exception: black piano keys may be about 30 dp wide so nearly two octaves fit on a 5-inch phone (decided 2026-10-09).
- UI: intuitive and uncluttered, yet every needed control reachable; no control or text so small it is hard to use.
- Themes: both light and dark mode (decided 2026-10-09).
- Design language: one standardised, cohesive UI design language across the whole app (shared components, spacing, typography, colour, iconography and interaction patterns), not per-screen styling. Built on Material 3 with a custom theme (colour, shape, type) (decided 2026-10-09). Studio-hardware look (decided 2026-10-09, after the first build looked like a tech demo): lit pads that glow when struck, a colour per instrument family (Keys violet, Drums teal, Guitar and Bass orange, Synth magenta, Strings blue, Brass gold, World green), a dark LCD-style transport display with amber beat, bar and tempo, red only for recording. Brand colours always: dynamic colour is dropped (decided 2026-10-09, reversing the earlier on-by-default decision). Fonts: Archivo for the interface and JetBrains Mono for the display, bundled under the SIL Open Font License (decided 2026-10-09). Reference mockups are local only, not in the repository.
- Pad and key sizing (delegated to Claude by the user on 2026-10-09, so a proposal the user may change): design for a 5-inch landscape phone (about 640 x 360 dp) as the minimum. Playable pads and keys are at least 48 dp wide and at least 72 dp tall, with at least 4 x 2 drum pads. Larger screens use window size classes to enlarge pads/keys and show more of them, never to shrink them.
- Audience is 11+, which includes minors: no personal data, ads or accounts (see `.learnings/play-families-policy.md`). Any later data, network or social feature needs a fresh child-safety review first.
- Fallback colours for Android 8-11 (dynamic colour needs Android 12, from memory): deep indigo/violet with a warm amber accent, with full light and dark schemes (decided 2026-10-09).
- Latency measurement (decided 2026-10-09): Oboe-reported latency on every build, plus a physical loopback test on 2-3 real devices. Available test devices (user-owned): Sony Xperia 1 II (confirmed), Sony Xperia XZ Premium, Google Pixel 10, Google Pixel 11, Samsung Galaxy A03 (old budget phone, used for budget testing, added 2026-10-09). Tablet layouts are tested on the Android emulator (decided 2026-10-09; emulator audio latency is not representative, so tablets are checked for layout only). The user will source a budget phone for low-end measurements; until then limits are provisional.
- Track limit (measured 2026-10-09): 8 tracks and 96 synth voices; no underruns on the Galaxy A03 budget phone with 8 busy tracks (see `.learnings/track-limit.md`).
- Latency status (decided 2026-10-09): milestone 1 output latency is accepted as it is (9.5 ms on the Xperia 1 II, 23.0 ms on the XZ Premium, 29.5 ms on the Galaxy A03, see `.learnings/latency-measurements.md`), but it needs more testing, including the physical loopback test, before the buffer size is final.
- Children's Code (decided 2026-10-09): design to its spirit (no data, no network, no ads, no nudges, neutral wording) with no legal review. Whether it formally applies to a no-data app is unconfirmed.
- Privacy policy (decided 2026-10-09): the master text is a raw markdown file in this repository, republished as a page on the user's own website, whose URL goes in the Play listing. Publisher is an individual named with a contact email; the values are recorded in `docs/privacy-policy.md` (supplied 2026-10-09), never in this plan.
- Play asset specs (verified 2026-10-09 against Play Console Help, see `.learnings/play-store-assets.md`): icon 512 x 512 32-bit PNG with alpha, max 1024 KB; feature graphic 1024 x 500 JPEG or 24-bit PNG without alpha, no file-size limit published.
- Icon and header (decided 2026-10-09): Claude drafts vector (SVG) sources and exports PNGs. Direction is an abstract keys/pad-grid mark, flat and minimal, indigo/violet with amber, one design reused for launcher and Play icon and echoed in the header.
- App name: "Synth Kit" (decided 2026-10-09), used as the display name in the launcher, Play listing and privacy policy.
- Package id: `com.bizzeh.synthkit`.
- Ease of use (decided 2026-10-09): the whole app is built for phones first, not a desktop DAW shrunk down. Any flow is start, play, stop, then tidy; no screen needs a manual. A feature that cannot be made simple on a 5-inch landscape screen is cut or deferred rather than crammed in.
- Multi-touch (decided 2026-10-09): every instrument reacts to two or more pads, keys or strings touched at the same time. Each finger plays its own note.
- Playing model (decided 2026-10-09): fixed note velocity when played, with per-track volume; velocity can be edited per note afterwards in the loop editor. Keys sustain while held, drum pads are one-shot.
- Accessibility and locale (decided 2026-10-09): every control has a TalkBack content description; English (UK) only, with all strings externalised for later translation.
- Release path (decided 2026-10-09): Play internal testing track first, then production; closed or open testing only if outside testers are wanted.
- Ship the GeneralUser GS licence with the app. The user confirmed on 2026-10-09 that the SoundFont licences are acceptable; Claude has not verified the licence text from the original source.
- Play Console declaration stays 13-15 and up while the design audience is 11+ (decided 2026-10-09). The store listing and icon stay neutral.

## Open questions

| Question | Owner |
|---|---|
| Budget phone: the user will buy a new sub-GBP-100 Android 13+ phone (decided 2026-10-09); model and date still open. Track limits are provisional until measured on it. A Samsung Galaxy A03 is available for budget testing meanwhile (2026-10-09). | User |
| The Play listing text: Claude drafts `docs/play-listing.md` in milestone 5 once screenshots exist, the user approves in conversation (decided 2026-10-09). | User |

## Milestones

1. Project skeleton, Oboe audio output, one playable instrument with measured latency. The instrument is the GM drum kit on a 4 x 2 pad grid, played through TinySoundFont and GeneralUser GS (SF2 playback moved forward from milestone 2, decided 2026-10-09).
2. All four instrument families with the three layouts in `docs/gm-layouts.md`; instrument browser with search, favourites and recents (decided 2026-10-09); latency warning.
3. Projects, metronome, looper-flow record and loop with layers, track controls, loop editor.
4. Export to audio and MIDI.
5. Licence screen, settings, privacy policy (drafted, hosted and linked in the listing), icon and header image, Play Store listing guide, GitHub Actions CI, internal testing then Play Store release. The icon is needed earlier for the launcher; a placeholder icon is used until it exists.

## Revision history

| Date | Change | Summary |
|---|---|---|
| 2026-10-09 | Created | Initial plan from requirements conversation. |
| 2026-10-09 | Changed | SF2 playback moved into milestone 1 (drum pads); multi-touch made a constraint for every instrument; test frameworks chosen; rotate message for large screens that ignore the landscape lock. |
| 2026-10-09 | Changed | Galaxy A03 added as a budget test phone; milestone 1 latency accepted pending more testing. |
| 2026-10-09 | Changed | Studio-hardware redesign before milestone 5: brand colours only (dynamic colour dropped), family colours, bundled Archivo and JetBrains Mono, diagnostics panel. |
| 2026-10-09 | Changed | Milestone 4 built: export marked built. |
| 2026-10-09 | Changed | Milestone 4 planned with export defaults. |
| 2026-10-09 | Changed | Milestone 3 built: projects, record and loop, loop editor, metronome and quantise marked built. |
| 2026-10-09 | Changed | Track limit set to 8 after measuring on the Galaxy A03; open question closed. |
| 2026-10-09 | Changed | Milestone 3 planned: project list replaces the home screen on launch; kotlinx-serialization approved for project files; metronome uses SoundFont click sounds. |
| 2026-10-09 | Changed | Milestone 2 built: four families, three layouts, browser and latency warning marked built. |
| 2026-10-09 | Changed | Instrument browser placed in milestone 2; loop start snaps to the nearest click beat; loop editor rows and velocity editing decided. |
