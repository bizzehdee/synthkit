# Synth Kit

Synth Kit is a free, open-source Android app. Tap the screen to play drums,
keyboards, guitar, bass and more.

- Free. No ads, no accounts, no in-app purchases.
- Works offline. The app does not use the internet.
- Collects no personal data.

## Status

Synth Kit is in early development. The current build cannot play sounds yet.
Instruments, recording and export are planned.
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
- For the C++ host tests: CMake, Ninja and a C++17 compiler on the build machine.

Steps:

1. Create `local.properties` in the project root with the path to your SDK:
   `sdk.dir=/path/to/android-sdk`
2. Build the app: `./gradlew assembleDebug`
3. Run the tests that need no device: `./gradlew verify`
4. Run the tests on a connected device: `./gradlew connectedDebugAndroidTest`

## Licence

The app's own code is licensed under GPL-3.0. See [LICENSE](LICENSE).
Third-party components keep their own licences.
