# Play Store graphic asset specs

Verified 2026-10-09 against Play Console Help, "Add preview assets to showcase your app"
(https://support.google.com/googleplay/android-developer/answer/9866151).

| Asset | Dimensions | Format | Max size |
|---|---|---|---|
| App icon | 512 x 512 px | 32-bit PNG with alpha | 1024 KB |
| Feature graphic | 1024 x 500 px | JPEG or 24-bit PNG, no alpha | not published |

Gotcha: the feature graphic must have no alpha channel, so export it flattened on a
solid background, unlike the icon. Re-check the page before release in case the
limits change.
