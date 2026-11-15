# Design — “Lumen”

Union should feel like a quiet instrument in a warm room: precise where it matters, soft where it rests, and never louder than the work. **Lumen** replaces the earlier dark “Quiet Signal” shell with a light-first system: porcelain ground, ink typography, one ultramarine signal, champagne for value, and dark lacquered cards used as moments rather than decoration.

The design borrows **Material 3 Expressive** principles — large tonal shapes, flexible app bars, grouped controls, spring motion — without pretending to be a Google app. Lumen is Union's own language; it is built to port cleanly to Jetpack Compose + Material 3 later.

## Typography

**One family: Google Sans.** The variable text face (weights 400–700) is used for interface, display and body. **Google Sans Code** is used only for hashes, keys, references and other machine strings.

Both are bundled as `woff2` in `src/assets/fonts/` and loaded from `src/app.css`. There is no runtime font request, so the interface is identical offline and inside the APK.

Rules of thumb:

- Display: 500–580 weight, tight tracking (−0.02 to −0.05 em), line-height 1.04–1.18.
- Body: 400 weight, 15.5 px base, line-height 1.58–1.7.
- Labels: 600–640 weight, 0.68–0.78 rem, slight positive tracking for small caps.
- Numbers and hashes: Google Sans Code, tabular where it matters.

Google Sans is a Google typeface. The files are included so Union renders correctly without a network. If you redistribute Union commercially, review Google's font terms for your use.

## Palette

Light is the default. Dark follows `prefers-color-scheme: dark`, and the native Android shell switches with the same signal.

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| `--bg` | `#f4f1ea` | `#101116` | warm porcelain ground |
| `--surface` | `#fbfaf6` | `#191a21` | cards, sheets |
| `--surface-2` | `#f1eee6` | `#202129` | inset wells, grouped controls |
| `--ink` | `#17181d` | `#f6f4ef` | primary text |
| `--muted` | `#54575f` | `#bcbbc5` | secondary text |
| `--faint` | `#64676f` | `#9a99a3` | tertiary text, labels |
| `--accent` | `#4a3aff` | `#9790ff` | ultramarine: action, focus, identity |
| `--accent-soft` | `#e9e6ff` | `rgb(143 134 255 / 0.18)` | selected washes, chips |
| `--gold` | `#8a5c14` | `#e2bc7b` | value, activation, plans |
| `--teal` | `#0b7a6e` | `#5fe0d0` | verified, delivered, live |
| `--rose` | `#b2344f` | `#ff92a7` | destructive, failed |
| `--line` | `10% ink` | `10% white` | hairlines |

Ultramarine carries action. Champagne carries value. Teal is reserved for states that are actually confirmed. Nothing else competes. Deep tones and soft tones are paired per hue so a chip or a tile never depends on colour alone.

## Surfaces and materials

- **Porcelain ground.** A warm off-white with three very slow radial washes (peach, ultramarine, mint) at low opacity. It should be felt, not read as a gradient.
- **Paper cards.** `--surface` with a 1 px hairline, 30 px radius, and a soft ambient shadow. Tonal, not glassy.
- **Selective liquid glass.** Glass is reserved for things that float above content: the bottom dock, the collapsible app bar, modal scrims, the command bar. Cards are paper.
- **Lacquered hero card.** One per screen at most: a dark, slightly reflective slab with a conic halo, used for identity, progress and the assistant. It gives the light interface a centre of gravity.
- **Champagne rule.** Gold never fills a large area. It marks value, plans and activation only.
- **No ambient noise or neon.** Texture is structural (hairlines, tonal steps, one inner light), not decorative.

## Shape, layout and adaptation

- Mobile-first, 320 px minimum, comfortable to 480 px.
- Content column max 760 px, centred on tablets and desktop.
- 20 px page gutters (26 px ≥ 760 px), 20–24 px card padding, 12 px grid gap.
- M3 Expressive radii: 30 px cards, 21 px compact cards, 14 px fields, full pills for buttons and chips.
- **Flexible app bar:** collapses from a large masthead into a compact sticky bar on scroll; back affordance is explicit.
- **Adaptive navigation:** a floating glass dock below 1080 px; a vertical rail from 1080 px, with the same order, labels and badges.
- **Floating command bar:** “Ask Union” is always one tap from Home and sits above the composer in the assistant.
- Bottom safe area respected; the dock never covers the last control.
- No horizontal overflow at any supported width; long identifiers wrap instead of widening the viewport.

## Components

- **Buttons:** primary (ultramarine, sheen sweep), soft/tonal, ghost, danger; sizes `lg`, default, `sm`, `xs`; `block` for full width.
- **Button groups and segmented controls:** one pill shell, tonal track, elevated selected segment.
- **Split button:** a primary action with a revealed secondary half, used where a choice is genuinely adjacent (Home identity card).
- **Floating action button:** 20 px radius, 54 px tall, for the single next action on a screen.
- **Tiles:** tonal grid tiles with an icon well, title and one honest line; `feature` variant for a full-width entry point.
- **Chips:** accent, gold, teal, green, amber, rose and plain; always carry words, never colour alone.
- **Fields:** 44 px minimum, clear labels, 14 px radius, visible focus ring in ultramarine.
- **Lists:** `listitem` rows with a title, one line of context and a trailing affordance; never fake activity.
- **Empty states:** one sentence, one action, no illustration noise.

Every component is a real `button`, `a`, `input`, `select` or `textarea`. There are no clickable `div`s.

## Motion

- Springs: `cubic-bezier(0.2, 0.9, 0.2, 1)` for movement, `cubic-bezier(0.16, 1.24, 0.3, 1)` for confirmation.
- Durations: 140 ms micro-feedback, 220–420 ms transitions, 700 ms+ only for ambient movement.
- Staggered reveals on lists and cards, 40–60 ms apart, capped so long lists do not crawl.
- Press states scale to 0.93–0.97; destructive actions do not bounce.
- Haptics accompany primary confirmations in the Android shell.
- Every animation has a `prefers-reduced-motion: reduce` path that removes transforms and ambient loops.

## A2UI — the agent surface language

Union's assistant can compose a small interface beside the conversation. It does this with **A2UI**, a declarative catalog, not HTML or JavaScript.

- **Version:** `a2ui/1`, catalog constants in `src/lib/a2ui.ts`, renderer in `src/components/A2UISurface.svelte`.
- **Components:** `heading`, `text`, `stats`, `list`, `actions`, `form`, `note`, `progress`, `divider`, `card`.
- **Actions:** `navigate` (safe internal paths only), `ask` (send a prepared prompt), `copy`, `dismiss`.
- **Limits:** 80 nodes, depth 4, 600-character strings, 12 list items, 6 form fields, 4 stats.
- **Validation:** every surface is validated before rendering. Unknown components, actions, paths with `..`, over-long strings and over-deep trees are rejected; invalid surfaces are not shown. A form is a real form with named fields and one submit action.
- **Host control:** the surface never executes an action on its own. The host receives the intent, may require approval, and decides what changes.
- **Composed surfaces:** briefing, consent, economy, messages, learning, people, presence and a presence-page draft form. The surface for a reply is chosen by scoring intent words in the person's prompt and the reply, so a passing mention does not hijack the screen.

**Portability to Compose.** Each catalog node maps to one composable (`Heading`, `Text`, `Stats`, `List`, `Actions`, `Form`, `Note`, `Progress`, `Divider`, `Card`), each action maps to one host intent, and the validator is plain data logic. The catalog can move to `androidx.compose.material3` without changing the wire format. A2UI is treated as an evolving protocol (`1.0.0-alpha02` at the time of writing), so the app tolerates its absence: the assistant works without a surface, and an invalid surface degrades to text.

## Voice

Plain, specific, unshowy. State what a control does and what it cannot do. “No relay configured” is better than a spinner that implies one exists. Test mode is labelled test mode. Consent is described as a choice, not a formality. The assistant says when it is on-device and when it used a configured provider.

## Accessibility

- Contrast: body and interface text meet WCAG AA in both light and dark themes, including on tonal chips and the dark hero card. Muted and faint tokens were tuned against the soft surfaces they actually sit on.
- Focus rings are visible and use ultramarine.
- Controls are real elements with labels; forms use `label for`.
- Touch targets are at least 44 × 44 px where the layout allows.
- Status is never colour-only: chips carry words, icons and colour.
- Reduced motion is respected globally.
- Native system bars follow light/dark and are not forced to one theme.

## What to avoid

- A second display typeface, serif display type, script logos, or a second brand colour family.
- Glass everywhere. If it does not float, it is paper.
- Neon gradients, scroll-triggered animation for its own sake, or motion that delays a control.
- Placeholder copy such as “Lorem ipsum”, fake counts, or invented activity.
- Hidden destructive actions; every irreversible step asks first.
- Agent output that can run code, inject markup, or change data without approval.
