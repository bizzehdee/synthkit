# Long menus hide items on landscape phones

Established 2026-10-09 by elimination on a Sony Xperia XZ Premium (landscape
height 393 dp, density 2.5625).

- A `DropdownMenu` with 12 rows of 48 dp (576 dp) is taller than a landscape
  phone. The menu scrolls, and the last rows are not laid out: the "A" item of
  the key picker had zero bounds and `assertIsDisplayed` failed.
- Effect: the key picker test clicked an item that was not on screen; the key
  stayed C. A player would have to scroll to reach A, A♯ and B.
- Fix: the key picker shows the 12 keys in a 6 x 2 grid. Keep any menu or
  picker under about 300 dp tall, or lay it out in columns.
- Regression test: `ChordsLayoutTest.choosingAMinorKeyChangesThePads`.
