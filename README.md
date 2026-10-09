# Synth Kit

Synth Kit is a free, open-source Android app. Tap the screen to play drums,
keyboards, guitar, bass and more.

- Free. No ads, no accounts, no in-app purchases.
- Works offline. The app does not use the internet.
- Collects no personal data.

## Status

Synth Kit is in early development. The current build plays every General MIDI
instrument in the bundled SoundFont:

- **Keys**: a piano keyboard with octave buttons, a scroll strip and a hold
  button.
- **Guitar and bass**: seven chord pads for a key and mode. Tap a pad to strum,
  or hold it and swipe the strum strip to play string by string.
- **Drums**: every General MIDI drum sound on pages of pads, with a kit picker.
- **Pitched percussion and effects**: pads that play one note each.

You can use several fingers at the same time on every instrument. A browser
lists every instrument by family, with search, favourites and recent
instruments. A banner warns you when Bluetooth audio or the device adds delay.

**Projects and loops.** The app opens on your projects. Each project saves
itself after every change. Inside a project:

1. Tap **Add track** and pick an instrument.
2. Tap **Record**. A click counts the beat. Recording starts on your first note.
3. Tap **Record** again. The take becomes a loop of whole bars (1 to 8) and
   plays at once.
4. Add more tracks and record over the loop. Each track can be muted, soloed,
   set louder or quieter, undone take by take, or cleared.
5. Use **Edit loop** to tidy a track: quantise it, add, move or delete notes on
   a grid, change how hard a note is played, or double the loop.

A project holds up to 8 tracks.

**Export.** Tap **Export** in a project to save or share it as MIDI, WAV, MP3,
FLAC or MP4 audio. Choose how many times the loop plays (1 to 16). Save uses the
system file picker, so the app never asks for storage permission.
See [plan.md](plan.md) for the full plan.

## Requirements

- Android 8.0 or newer.
- A phone or a tablet. The app runs in landscape.
- Wired headphones or the built-in speaker give the lowest delay between a tap
  and the sound. Bluetooth audio adds a noticeable delay.

## Building from source

You need:

- JDK 17 or newer.
- The Android SDK with platform 37, NDK r30 and CMake 4.1.
- For the C++ host tests: CMake, Ninja and Clang on the build machine. The host
  tests download GoogleTest when first configured.

Steps:

1. Create `local.properties` in the project root with the path to your SDK:
   `sdk.dir=/path/to/android-sdk`
2. Build the app: `./gradlew assembleDebug`
3. Run the tests that need no device: `./gradlew verify`
4. Run the tests on a connected device: `./gradlew connectedDebugAndroidTest`.
   Keep the device screen on during the run (developer option "Stay awake").
   UI tests fail if the screen turns off.

Debug builds show the measured audio output latency above the instrument.

## Licence

The app's own code is licensed under GPL-3.0. See [LICENSE](LICENSE).
Third-party components keep their own licences.
