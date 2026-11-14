# Design — “Quiet Signal”

Union should feel like a calm, well-made instrument: dark, precise, warm at the edges. Motion confirms what happened; it never performs for attention.

## Typography

**One family: Google Sans.** The variable text face (weights 400–700) is used for interface, display and body. **Google Sans Code** is used only for hashes, keys, references and other machine strings.

Both are bundled as `woff2` in `src/assets/fonts/` and loaded from `src/app.css`. There is no runtime font request, so the interface is identical offline and inside the APK.

Rules of thumb:

- Display: 500–600 weight, tight tracking (−0.02 to −0.045 em), line-height 1.04–1.18.
- Body: 400 weight, 15 px base, line-height 1.6–1.7.
- Labels: 500–600 weight, 0.68–0.78 rem, positive tracking for small caps.
- Numbers and hashes: Google Sans Code, tabular where it matters.

Google Sans is a Google typeface. The files are included so Union renders correctly without a network. If you redistribute Union commercially, review Google's font terms for your use.

## Palette

| Token | Value | Use |
| --- | --- | --- |
| `--bg` | `#06070c` | ground |
| `--panel` | `#10121c` | cards, sheets |
| `--ink` | `#f6f5f9` | primary text |
| `--muted` | `#a5aabf` | secondary text |
| `--faint` | `#747b95` | tertiary text, labels |
| `--accent` | `#8b8dff` | iris: action, focus, identity |
| `--accent-2` | `#c6c8ff` | hover, highlighted words |
| `--gold` | `#e8c690` | value, activation, plans |
| `--teal` | `#2dd4bf` | verified, delivered, live |
| `--rose` | `#fb7185` | destructive, failed |
| `--line` | `rgb(255 255 255 / 0.085)` | hairlines |

Iris carries action. Champagne carries value. Teal is reserved for states that are actually confirmed. Nothing else competes.

## Materials

- **Obsidian glass:** layered translucent panels with 1 px hairlines, subtle inner light, and a soft drop shadow. Glass is a material, not a gimmick: it separates floating controls from content.
- **Gradient borders:** a 1 px conic/linear blend on selected cards, never a full rainbow.
- **Aurora:** very slow, low-opacity colour fields behind the top of the app. It should be noticed only on a second look.
- **Grain:** a tiny inline noise texture at low opacity; prevents large dark areas from banding.
- **Floating dock:** the bottom navigation is a glass pill inset from the edges, with a moving active indicator.

## Motion

- Springs: `cubic-bezier(0.22, 1, 0.36, 1)` for entrances, `cubic-bezier(0.34, 1.56, 0.64, 1)` for playful confirmation.
- Durations: 140 ms micro-feedback, 260–420 ms transitions, 700 ms+ only for ambient movement.
- Staggered reveals on lists and cards, 40–60 ms apart, capped so long lists do not crawl.
- Every animation has a `prefers-reduced-motion: reduce` path that removes transforms and ambient loops.
- Press states scale to 0.97–0.98; no bounce on destructive actions.

## Layout

- Mobile-first, 320 px minimum, comfortable to 480 px.
- Content column max 560 px, centred on tablets and desktop.
- 20 px page gutters, 14–18 px card padding, 12 px grid gap.
- Bottom safe area respected; the dock never covers the last control.
- No horizontal overflow at any supported width.

## Voice

Plain, specific, unshowy. State what a control does and what it cannot do. “No relay configured” is better than a spinner that implies one exists. Test mode is labelled test mode. Consent is described as a choice, not a formality.

## Accessibility

- Contrast: body text meets WCAG AA on the obsidian ground; muted text stays above 4.5:1 where it carries meaning.
- Focus rings are visible and use the iris accent.
- Controls are real `button`, `input`, `select` and `textarea` elements with labels.
- Touch targets are at least 44 × 44 px where the layout allows.
- Status is never colour-only: chips carry words, icons and colour.
- Reduced motion is respected globally.

## What to avoid

- Serif display type, script logos, or a second brand colour family.
- Neon gradients, glass everywhere, or animation on scroll for its own sake.
- Placeholder copy such as “Lorem ipsum”, fake counts, or invented activity.
- Hidden destructive actions; every irreversible step asks first.
