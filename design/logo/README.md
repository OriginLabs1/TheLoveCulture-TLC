# TLC logo

The v0.0.5 plan ([`docs/PLAN-v0.0.5.md`](../../docs/PLAN-v0.0.5.md), section 4.5) uses a
royal-blue, azure, white and silver-grey palette derived from the founder's description. This
folder holds the brand assets built from that description.

## Current files

| File | Status | Notes |
|---|---|---|
| `tlc-logo.svg` | first master | transparent emblem, 512×512 viewBox, true vector |
| `tlc-logo-icon.svg` | first master | emblem on a rounded midnight-navy app-icon tile |
| `preview.html` | review aid | opens the emblem on light/dark surfaces and the icon at 256px and 48px |
| `tlc-logo.png` | pending | export from the SVG, not a trace |
| `tlc-logo-mark.png` | pending | export from the SVG, not a trace |
| `../../apps/android/src/main/res/drawable/ic_launcher_foreground.xml` | shipped | adaptive-icon foreground; mirrors the master, including the crowned figure |
| `../../apps/android/src/main/res/drawable/ic_launcher_monochrome.xml` | shipped | single-colour silhouette for Android 13+ themed icons |

The SVG is the source of truth. Do not trace a PNG or screenshot.

## What the master contains

- a silver outer ring with an azure inner rule;
- a crowned circular figure in royal blue and azure;
- a split-tone heart (royal blue / azure) with a white centre gap;
- a white botanical sprout inside the heart;
- silver laurel leaves flanking the heart;
- celestial four-point stars and dots around the ring.

The geometry is deliberately bold so it survives 24–48px.

## Still needed

1. **Founder review** of the crown contours, the heart split and the internal sprout.
2. **Android 12 splash mark**, About mark and store listing icon.
3. Optimised **PNG/WebP exports** at 48/72/96/144/192/512px, exported from the SVG.

The monochrome/themed icon and the adaptive-icon foreground are in the Android module and are
generated from the same geometry as the master. The in-app `TlcMark` is a deliberately reduced
glyph for 44–52dp use; it keeps the ring, crown, heart and sprout and drops the stars, laurel and
face detail that would disappear at that size.

## Production direction

- refine the crown contours;
- standardise stroke widths and shape geometry;
- make the heart and internal symbols legible at 24–48px;
- prove the emblem still reads in one colour before it ships.

The design system keeps the accent in one token, so the palette can be swapped without touching
screens.
