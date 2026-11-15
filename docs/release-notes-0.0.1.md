# Union v0.0.1 — first usable release

**One connected environment for communication, community, learning, experiences, presence, intelligence, economic coordination and stewardship.**

This is the first release that is meant to be used, not demonstrated: installable PWA, signed Android APK, offline storage, encrypted backup, and an honest activation flow.

## Highlights

- **Premium mobile interface** — “Lumen”: warm porcelain ground, ultramarine signal, champagne for value, selective liquid glass, lacquered hero cards, Material 3 Expressive shapes, spring motion and reduced-motion support. Light by default, with a tuned dark theme and matching native system bars. Google Sans throughout, Google Sans Code for keys and hashes.
- **Adaptive surface** — a floating glass dock on phones and a vertical rail from 1080 px, a flexible app bar that collapses on scroll, grouped and split buttons, and an “Ask Union” command bar.
- **A2UI agent surfaces** — the assistant can compose a safe, validated interface beside the conversation (headings, stats, lists, forms, actions) from a fixed catalog. Only four allowlisted actions exist, nothing executes without approval, and invalid surfaces fall back to text.
- **Ten working capability areas** — communication, community, people/consent, learning, experiences, presence, assistant, mentorship, economy, operations.
- **Local-first identity** — Ed25519 identity derived from its public key; X25519 + HKDF + AES-256-GCM sealed messages; the relay sees ciphertext and routing metadata only.
- **Android shell** — 375 KB signed APK, Android 9+, one permission (`INTERNET`), Keystore-backed secret store, loopback asset server for a secure WebView origin.
- **Offline PWA** — service worker, bundled fonts and WASM core; no runtime third-party requests.
- **Activation flow** — request → 14-day trial → proof with SHA-256 → package → Union Assistant → verification → $10/month reactivation.
- **Joy/Giving Plans** — optional, “Joyful Giver”, 2-week trial, no automatic charge.
- **Hash-chained ledger** — every economic record is local and verifiable.
- **Optional relay** — Cloudflare Worker + D1, Ed25519-signed writes, 8 passing relay tests.

## Install

### Android

Download `Union-0.0.1.apk`, verify the checksum, open it.

```
package      com.oneorigin.union
version      0.0.1 (1)
minSdk       28 (Android 9)
targetSdk    34
permission   android.permission.INTERNET
size         ~375 KB
```

Signing certificate SHA-256:

```
35:58:51:C9:DB:74:76:8F:8C:2B:E7:F3:79:38:0B:7B:F9:68:AB:9C:F5:F3:6A:91:3E:F5:FA:CB:DB:81:F9:42
```

### Web / PWA

Open the Pages site and choose **Add to Home screen**. Works offline after the first load.

## Verify

```sh
sha256sum Union-0.0.1.apk          # compare with SHA256SUMS
apksigner verify --print-certs Union-0.0.1.apk
```

## Economy in v0.0.1

Test mode. No real funds move inside the app; no withdrawable value is promised. Activation is $80 minimum equivalent, reactivation is $10/month after verification, and Joy/Giving plans are optional with a 2-week trial. The Creator's WhatsApp number is never shown.

Read [docs/economy.md](../docs/economy.md) before requesting activation.

## Known limits

- Single-device identity; multi-device sync is not implemented.
- Relay delivery is store-and-forward, not push.
- The activation assistant's backend is separate from this repository; the app prepares the package and opens [@Oneorigin_bot](https://t.me/Oneorigin_bot).
- No payments, balances, refunds or automatic charges inside the app.

## Checks

- `npm run check` — 0 errors, 0 warnings
- `npm test` — 10/10
- `npm run test:relay` — 8/8
- All 14 routes audited at a 360 px phone viewport: no horizontal overflow, no console errors
- Light and dark contrast audits: text meets WCAG AA on paper, tonal chips and the hero card
- Desktop layout at 1280 px: rail navigation, centred column, composer offset, no overflow
- Fresh install → onboarding → encrypted-backup step → Home verified in a browser
- Assistant flow verified: prompt → A2UI presence form → submit → tailored draft → approve → page appears in Presence with a badge
- Activation request → proof → verified → reactivation section verified in a browser
- APK signature, permissions, `minSdk` and packaged assets inspected

## Thank you

Built for people who want a place to belong without being owned.
