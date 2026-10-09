# Measured output latency

Oboe-reported output latency (`calculateLatencyMillis`), measured 2026-10-09 on
debug builds of milestone 1. Evidence: logcat `event=latency_measured` lines.
Output device was the built-in speaker on every phone (`dumpsys
media.audio_flinger`: `AUDIO_DEVICE_OUT_SPEAKER`).

| Phone | Android | API | Sharing | Burst | Buffer | Output latency |
|---|---|---|---|---|---|---|
| Sony Xperia 1 II (XQ-AT51) | 12 | AAudio | Exclusive | 96 | 192 | 9.5 ms |
| Sony Xperia XZ Premium (G8141, LineageOS) | 13 | AAudio | Shared | 192 | 384 | 23.0 ms |
| Samsung Galaxy A03 (SM-A035F, Unisoc ums9230) | 13 | AAudio | Shared | 240 | 480 | 29.5 ms |

All three use 48000 Hz and the LowLatency performance mode. The buffer is two
bursts.

- This is output latency only. Tap-to-sound latency also includes touch input
  latency, which needs the physical loopback test in `plan.md`. That test has
  not been run.
- The plan's target is 20 ms or less on a mainstream phone. Only the Xperia 1 II
  meets it from output latency alone.
- The two slower phones did not get exclusive mode. Their burst size is set by
  the platform.
- SoundFont load time: 241 ms (Xperia 1 II), 393 ms (XZ Premium), 518 ms
  (Galaxy A03).
- Decision (2026-10-09, user): these values are acceptable for now, but need more
  testing before the buffer size is final.
