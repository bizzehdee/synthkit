# Learnings index

- [export-encoders.md](export-encoders.md): which export formats need a library (MP3, FLAC) and their licences. Read before adding an encoder dependency.
- [play-families-policy.md](play-families-policy.md): Play Families policy and UK Children's Code duties for a child-inclusive audience. Read before choosing age groups, adding data or network features, or writing the store listing.
- [soundfont-licences.md](soundfont-licences.md): licence terms of GeneralUser GS and FluidR3 GM. Read before bundling or shipping SoundFonts.
- [play-store-assets.md](play-store-assets.md): verified Play icon and feature graphic dimensions, formats and size limits. Read before exporting store artwork.
- [large-screen-orientation.md](large-screen-orientation.md): Android 16+ ignores the landscape lock on screens 600 dp or wider, and the rotate-message decision. Read before changing orientation handling or targetSdk.
- [tinysoundfont-realtime.md](tinysoundfont-realtime.md): what TinySoundFont allocates and when, voice stealing, and load cost. Read before changing the synth or calling tsf from a new thread.
- [latency-measurements.md](latency-measurements.md): Oboe-reported output latency and SoundFont load time per test phone. Read before changing buffer sizes or judging the 20 ms target.
- [device-test-screen-timeout.md](device-test-screen-timeout.md): why device UI tests fail with "No compose hierarchies found", how to keep the screen on, and the Android 17 Espresso failure.  Read before running or debugging connected tests.
- [short-landscape-menus.md](short-landscape-menus.md): long dropdown menus scroll and hide items on landscape phones. Read before adding a menu or picker.
- [sequencer-timing.md](sequencer-timing.md): why the sequencer clock counts frames, and why host tests use Clang for sanitizers. Read before changing transport timing or the host test build.
- [viewmodel-clear-order-and-polling.md](viewmodel-clear-order-and-polling.md): view model clear order, how often polled state may change, and portrait test activities. Read before adding a view model, polling, or a crowded top bar.
- [track-limit.md](track-limit.md): measured audio load by track and voice count on the three phones, and why 8 tracks and 96 voices. Read before changing the track or voice limit.
- [pager-steals-pad-slides.md](pager-steals-pad-slides.md): why drum pads scrolled when tapped, and the 4-row test window on the Galaxy A03. Read before putting pads in a scrolling container.
