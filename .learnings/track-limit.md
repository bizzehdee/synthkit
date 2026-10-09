# Track limit and voice limit

Measured 2026-10-09 with `TrackLoadBenchmark` (manual test; logcat tag
`SynthKitBench`): each track plays a dense one-bar loop at 120 BPM (drums: 16th
hats, kick, snare; melodic: a three-note chord on every 8th), 10 s per count.
Load is render time divided by the buffer's time; above 1 a callback missed
its own deadline, which the two-burst buffer can absorb.

Galaxy A03 (budget phone), by voice limit, 8 tracks:

| Voice limit | Voices used (max) | Load average | Load peak | Underruns |
|---|---|---|---|---|
| 64 | 64 | 0.46 | 1.39 | 0 |
| 96 | 96 | 0.53 | 1.47 | 0 |
| 128 | 104 | 0.66 | 1.97 | 0 |

With 96 voices, 4 / 6 / 8 tracks: A03 peaks 1.35 / 1.42 / 1.47; Xperia XZ
Premium 1.04 / 1.09 / 1.02; Xperia 1 II 1.02 / 1.21 / 1.16. No underruns on any
phone.

- Polyphony, not CPU, limited the 64-voice build: 4 busy tracks already used
  all 64 voices.
- 128 voices pushed the A03's peak to 1.97, close to the 2-burst buffer: too
  little margin. The engine uses 96 voices.
- Decision (2026-10-09): ship 8 tracks, the design target. With all 96 voices
  busy, TinySoundFont takes voices that are fading out first, so a dense
  8-track loop may cut some release tails.
