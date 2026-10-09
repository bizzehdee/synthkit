# Android 16 ignores the orientation lock on large screens

Established 2026-10-09 from the Android 16 behaviour-changes page
(https://developer.android.com/about/versions/16/behavior-changes-16, section
"Ignore orientation, resizability, and aspect ratio restrictions").

- Apps that target API 36 or higher cannot lock orientation, resizability or
  aspect ratio on displays with smallest width 600 dp or more. Tablets and
  unfolded foldables can therefore show the app in portrait.
- Phones below 600 dp still honour `android:screenOrientation`.
- Exceptions: apps with `android:appCategory="game"`, and users who opt the app
  out in the device's aspect-ratio settings.
- The manifest property `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` opts out,
  but it stops working once the app targets API 37.
- Synth Kit does not claim the game category, because it is a music app.
  Decision (2026-10-09, user): keep the phone lock, and show only a "rotate your
  device" message when the window is portrait (`LandscapeOnly`). With this in
  place the app can target API 37.
- Lint reports `DiscouragedApi` on the lock. It is suppressed on the activity
  for this reason.
