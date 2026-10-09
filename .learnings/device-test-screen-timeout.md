# Device tests fail when the phone screen turns off

Established 2026-10-09 by elimination on a Galaxy A03 (screen timeout 30 s).

- Symptom: Compose UI tests fail with `IllegalStateException: No compose
  hierarchies found in the app`, partway through `connectedDebugAndroidTest`.
- Cause: the screen turned off during the run. Evidence: with the screen kept on,
  `DrumPadGridTest` passed 3 runs out of 3 (18 tests). With the screen turned off
  by `input keyevent KEYCODE_SLEEP`, 5 of 6 failed with the same message.
- Remedy: keep the screen on while tests run. Use the "Stay awake" developer
  option, or `adb shell svc power stayon usb`, and restore the previous
  `stay_on_while_plugged_in` value afterwards.
- `connectedDebugAndroidTest` uninstalls the app when it finishes. Reinstall the
  debug APK before a manual run.
- A showing lock screen causes the same failure even when the screen is on.
  Evidence (2026-10-09, Xperia XZ Premium): `dumpsys window` reported
  `isKeyguardShowing=true` and `SynthKitAppTest` failed 12 of 12; after
  `adb shell wm dismiss-keyguard` it passed 12 of 12.
- Since 2026-10-09 the three test phones are set to stay awake while plugged in
  (`stay_on_while_plugged_in` = 7), with the user's permission.
- Open: one `favouriteToggleAddsTheInstrumentToFavourites` failure on the XZ
  Premium, 2026-10-09, right after the lock screen was dismissed. Its message was
  not captured. It did not recur in 3 single runs and 4 full-class runs. If it
  recurs, record the message here.
