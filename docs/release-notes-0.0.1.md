# Union v0.0.1 — first usable release

**One connected environment for communication, community, learning, experiences, presence, intelligence, economic coordination and stewardship.**

This is the first release that is meant to be used, not demonstrated: installable PWA, signed Android APK, offline storage, encrypted backup, and an honest activation flow.

## Highlights

- **Premium mobile interface** — “Quiet Signal”: obsidian glass, iris accent, champagne for value, spring motion, reduced-motion support. Google Sans throughout, Google Sans Code for keys and hashes.
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
- Fresh install → onboarding → encrypted-backup step → Home verified in a browser
- Activation request → proof → verified → reactivation section verified in a browser
- APK signature, permissions, `minSdk` and packaged assets inspected

## Thank you

Built for people who want a place to belong without being owned.
