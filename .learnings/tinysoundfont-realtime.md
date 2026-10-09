# TinySoundFont on a real-time audio thread

Established 2026-10-09 by reading `tsf.h` at commit 853a0a1 (vendored in
`app/src/main/cpp/third_party/tinysoundfont/`).

- `tsf_note_on` allocates new voices with `realloc` when none is free, unless
  `tsf_set_max_voices` was called. Call it once after loading.
- With a voice limit, a new note may only steal a voice that is in its release
  segment. If none is, the new note is silently dropped. One-shot drum voices
  with no note-off stay out of release until their sample ends.
- `tsf_channel_*` functions `realloc` the channel array the first time a channel
  number is used, including inside `tsf_channel_note_on`. Set up all 16 MIDI
  channels before audio starts.
- The library calls itself only "mostly thread safe" with a voice limit. Synth Kit
  therefore calls TinySoundFont only on the audio thread and passes notes through
  `NoteQueue`.
- `tsf_load` converts every sample to `float`, so RAM use is about twice the
  sample data in the SF2. GeneralUser GS (32 MB file) loads in about 200 ms on an
  Xperia 1 II and about 390 ms on an Xperia XZ Premium (logcat
  `event=soundfont_loaded`, 2026-10-09).
- Any channel plays drums once a bank 128 preset is set on it with
  `tsf_channel_set_bank_preset`; the `flag_mididrums` argument only affects
  `tsf_channel_set_presetnumber`. Host test `DrumKitBankMakesAnyChannelPlayDrums`
  (2026-10-09).
- `struct tsf` is defined only when `TSF_IMPLEMENTATION` is set, and there is no
  public getter for a preset's bank and program by index. `TinySoundFontExtras.h`
  exposes one from the implementation unit.
- TinySoundFont's sustain does not end a held note when the same key is
  replayed, which the agreed hold rule requires. Hold is therefore done in Kotlin.
