# Sequencer clock and host sanitizers

Established 2026-10-09.

- Adding `frames * ticksPerFrame` to a double clock every block drifts: at 60 BPM
  and 48 kHz (0.01 ticks per frame, not exact in binary) a beat on a block edge
  fell one frame early into the earlier block. Evidence: host test
  `Sequencer.TempoSetsTheBeatLength` emitted 4 clicks instead of 3.
- Fix in `Sequencer`: count whole frames as an integer, derive ticks from them,
  rebase on a tempo change, treat an event within 1e-6 ticks of the block end as
  belonging to the next block, and round frame offsets.
- On this Fedora build machine GCC cannot link `-fsanitize=address,undefined`:
  `libasan` and `libubsan` are not installed (`ld: cannot find
  /usr/lib64/libasan.so.8.0.0`). Clang 22 ships its own runtime, so the host
  tests build with `clang++`. CMake falls back to no sanitizers, with a
  warning, where the flags do not link.
