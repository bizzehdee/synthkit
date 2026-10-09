# View model clear order and polled UI state

Established 2026-10-09 from device test failures.

- When the activity finishes, Android clears its view models in no defined
  order. `AudioEngineViewModel` closed the engine before
  `SessionViewModel.onCleared` asked the closed engine to stop, which threw
  `IllegalStateException: AudioEngine is closed` and crashed the app on exit
  (`MainActivityTest`, all three phones). Rule: `onCleared` must not call into
  an object another view model owns.
- The looper polls the engine every 15 ms. Writing the exact tick position to UI
  state on every poll recomposed the screen about 66 times a second; on the
  Galaxy A03 Compose never went idle (`ComposeNotIdleException ... pending
  recompositions`). Rule: polled values go into UI state only at the resolution
  the screen shows (here, the bar number).
- The plain `ComponentActivity` used by `createAndroidComposeRule` is not
  landscape-locked, so UI tests can run in portrait (about 400 dp wide). A top
  bar that relies on landscape width squeezed buttons to zero width there.
