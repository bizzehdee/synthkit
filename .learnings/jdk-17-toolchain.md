# JDK toolchain: any JDK 17 or newer

Established 2026-10-10.

- `app/build.gradle.kts` used to pin `JavaLanguageVersion.of(17)`. With only
  JDK 25 installed, every test task failed with `Cannot find a Java installation
  ... languageVersion=17`, because no toolchain download repository is configured.
- The pin was removed. Gradle runs on whichever JDK launches it; bytecode still
  targets Java 17 through `sourceCompatibility`/`targetCompatibility`.
- Observed: `./gradlew testDebugUnitTest` passes on JDK 25 with the pin removed.
- Risk the pin guarded against: a JRE with no compiler, or a JDK newer than AGP
  supports. If a build fails for that reason, use a full JDK 17-25.
- `--offline` fails: the Android Gradle plugin is not in the local cache.
