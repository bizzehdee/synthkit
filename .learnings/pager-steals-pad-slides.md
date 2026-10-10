# A pager scrolls under pads that slide

Established 2026-10-09 by a device test that failed before the fix and passed after.

- Symptom: tapping drum pads moved the pad page sideways (user report).
- Cause: `PlayPad` consumes only the down and up events. The finger's move
  events stay unconsumed, so the parent `HorizontalPager` sees a drag once the
  slide passes touch slop. Evidence: `aTapThatSlidesALittleDoesNotMoveThePads`
  with a 30 dp slide moved the snare pad 37 px on the Xperia 1 II.
- Remedy chosen by the user: `userScrollEnabled = false` on the pager, and
  previous and next arrows beside the page dots.
- Rule: never put playable pads inside a container that scrolls by touch.
- The page bar is 48 dp tall, so a 4-row page needs 368 dp. That is taller than
  the Galaxy A03 window (about 352 to 360 dp in landscape), so 4-row tests lay out
  with `requiredSize`; the pads stay on screen and the page bar falls off it.
