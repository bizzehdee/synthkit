# General MIDI instrument layouts

Status: agreed 2026-10-09 after review (first proposal 2026-10-09). It maps every
General MIDI (GM) program and the drum channel to one of three play layouts and one
browser family tab. The program numbers are the standard GM list. The drum kits
available in GeneralUser GS have not been checked against the SoundFont file yet.

## Play layouts

| Layout | What the player sees | Rules |
|---|---|---|
| **Keys** | A piano keyboard with octave shift buttons and a scroll strip above the keys for finer movement, plus a hold toggle | White keys at least 48 dp wide and 72 dp tall. Black keys sit on top at about 30 dp wide: an agreed exception to the 48 dp touch minimum so that nearly two octaves fit on a 5-inch phone. Hold is sustain-pedal behaviour: released notes keep ringing until hold is turned off or the note is replayed |
| **Chords** | Seven chord pads for a chosen key and mode (major or minor), showing the diatonic triads I to vii, plus a strum strip | Tapping a pad plays the chord with a short automatic strum. Swiping the strip while a pad is held plays the chord string by string at the swipe speed. Pads are at least 48 dp wide and 72 dp tall. Bass programs show the same seven pads but play the chord root in a low octave, with octave shift |
| **Pads** | A grid of pads, 4 x 2 minimum on a 5-inch phone, 4 x 4 where space allows, further pages swiped sideways | Each pad plays one note or sound; pads are at least 48 dp wide and 72 dp tall. Melodic pad instruments are chromatic: consecutive semitones from a root, with octave shift |

The fretboard grid (strings as lanes, frets as columns) from the first proposal is
dropped: 6 x N cells cannot meet the touch minimum on a 5-inch phone.

## Browser family tabs

Seven tabs: Keys, Guitar/Bass, Drums/Percussion, Strings/Orchestra, Brass/Winds,
Synth, World/Misc. The four quick entries on the home screen open Keys, Guitar/Bass,
Drums/Percussion and Synth.

## Mapping by GM group

| GM programs | Group | Family tab | Layout |
|---|---|---|---|
| 1-8 | Piano | Keys | Keys |
| 9-16 | Chromatic percussion (glockenspiel, vibraphone, marimba, bells) | Keys | Keys |
| 17-24 | Organ | Keys | Keys |
| 25-32 | Guitar | Guitar/Bass | Chords |
| 33-40 | Bass | Guitar/Bass | Chords (root notes) |
| 41-47 | Strings (violin to pizzicato, harp) | Strings/Orchestra | Keys |
| 48 | Timpani | Drums/Percussion | Pads (tuned drum, chromatic) |
| 49-56 | Ensemble and voices | Strings/Orchestra | Keys, hold on by default |
| 57-64 | Brass | Brass/Winds | Keys |
| 65-72 | Reed | Brass/Winds | Keys |
| 73-80 | Pipe (flute, recorder, whistle) | Brass/Winds | Keys |
| 81-88 | Synth lead | Synth | Keys |
| 89-96 | Synth pad | Synth | Keys, hold on by default |
| 97-104 | Synth effects | Synth | Pads (chromatic) |
| 105-108 | Sitar, banjo, shamisen, koto | World/Misc | Chords |
| 109-112 | Kalimba, bagpipe, fiddle, shanai | World/Misc | Keys |
| 113-120 | Percussive (agogo, steel drums, woodblock, taiko, melodic tom, synth drum) | Drums/Percussion | Pads |
| 121-128 | Sound effects | World/Misc | Pads (one-shot sounds) |
| Drum channel | Drum kits | Drums/Percussion | Pads, GM percussion notes 35-81 |

Wind and brass programs use plain Keys with no expression control. All eight sound
effects, including Gunshot, are kept under their standard GM names.

## Drum kit pad grids

- First page, in order: kick, snare, closed hat, open hat, low tom, high tom, crash,
  ride. On a 4 x 4 grid the same page continues with clap, cowbell, tambourine,
  rimshot, side stick, floor tom and two spare pads.
- Further pages hold the remaining GM percussion notes, swiped sideways.
- Kits to list: whichever GM-compatible kits the bundled SoundFont provides. The list is
  built from the file at start-up, not hard-coded.

## Decisions log

| Date | Decision |
|---|---|
| 2026-10-09 | Guitar family uses Chords only; fretboard layout dropped. |
| 2026-10-09 | Keys navigation is octave buttons plus a scroll strip. |
| 2026-10-09 | Black keys may be about 30 dp wide, an exception to the 48 dp minimum. |
| 2026-10-09 | Seven browser tabs kept. |
| 2026-10-09 | All GM sound effects kept with standard names. |
| 2026-10-09 | No breath or expression control for winds and brass. |
| 2026-10-09 | Synth effects are chromatic pads. |
| 2026-10-09 | First drum page order accepted. |
| 2026-10-09 | Bass pads play chord roots. Tap strums, strip strums manually. Triads only. Hold is sustain-pedal style. |
