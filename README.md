# Union

**One connected environment for communication, community, learning, experiences, presence, intelligence, economic coordination and stewardship.**

Union is a mobile-first, local-first environment. Your identity, keys and records live on your device. There is no account to create on a server, no password to leak, and no feed engineered to hold you. When you want to reach other people, an optional relay carries sealed envelopes it cannot read.

Version **0.0.1** — the first genuinely usable release: installable PWA, signed Android APK, optional Cloudflare relay, and a complete activation flow.

---

## What is inside

| Capability | What it actually does |
| --- | --- |
| **Communication** | Direct messages sealed with X25519 + HKDF-SHA256 + AES-256-GCM. Notes to self always work offline. An outbox retries when a relay is reachable. |
| **Community** | Rooms with a stated purpose, public or private, local posts, and optional relay sync for public rooms. |
| **People & consent** | Signed identity cards, connection requests, and a consent record you control. Discovery is not endorsement. |
| **Learning** | Foundations course, lessons, practice, and progress that stays on the device. |
| **Experiences** | Gatherings and moments with dates, places and participation, kept as records rather than advertising. |
| **Digital presence** | A shareable identity card and self-contained signed links that work without a server. |
| **Intelligence** | A local assistant with memory, proposals and explicit approval before anything changes. |
| **Mentorship & stewardship** | Mentorships, commitments and stewardship notes. |
| **Economic coordination** | Activation, reactivation plans, Joy/Giving plans, and a hash-chained local ledger that is honest about test mode. |
| **Operations** | Relay configuration, sync state, backups, exports, audit entries and device controls. |

Plus: onboarding, encrypted backup and restore, a service worker for offline use, and an Android shell with a Keystore-backed secret store.

## Install

### Android APK (recommended on a phone)

Download [`Union-0.0.1.apk`](https://union-app-9ni.pages.dev/Union-0.0.1.apk) and open it. Android 9 or newer. The APK asks for **one** permission — `INTERNET` — used for the loopback asset server and the optional relay. No storage, contacts, camera, microphone, location or advertising ID.

Verify before installing:

```sh
sha256sum Union-0.0.1.apk
# compare with SHA256SUMS from the same release
apksigner verify --print-certs Union-0.0.1.apk
```

Signing certificate (SHA-256):
`35:58:51:C9:DB:74:76:8F:8C:2B:E7:F3:79:38:0B:7B:F9:68:AB:9C:F5:F3:6A:91:3E:F5:FA:CB:DB:81:F9:42`

### Progressive web app

Open **https://union-app-9ni.pages.dev** (or the custom address once its certificate is active) in a browser and choose **Add to Home screen**. The service worker caches the shell, fonts and WASM core, so it opens offline after the first load.

### From source

```sh
npm install
npm run dev        # http://127.0.0.1:5173
npm run build      # dist/ + service worker manifest + checksums
npm test           # 10 app tests (Rolldown bundle + node:test)
npm run test:relay # 9 relay tests (real handler, in-memory store)
npm run check      # svelte-check, 0 errors
```

The Android build needs a JDK, Android build-tools and the platform jars described in [docs/build.md](docs/build.md):

```sh
sh android/scripts/gen-keys.sh   # once; writes android/keys/ (gitignored)
sh scripts/build-apk.sh          # release/Union-0.0.1.apk + SHA256SUMS
```

## The economy, plainly

Union v0.0.1 runs its economic layer in **test mode**. Nothing inside the app moves real money, and the app says so on every economic screen.

- **Activation Token** — minimum **$80**, Naira equivalent accepted. An activation request starts a **14-day trial** immediately.
- **Activation Proof** — a receipt (screenshot, photo, `.txt` or `.md`) is attached, hashed with SHA-256, and packaged for the Union Assistant on Telegram ([@Oneorigin_bot](https://t.me/Oneorigin_bot)).
- **Verification** — the assistant forwards the package to the Creator. The Creator's WhatsApp number is never shown to users, in the app or in any package.
- **Reactivation Token (Plan)** — **$10/month** equivalent, available after activation is verified.
- **Joy/Giving Plans** — optional, exact-named plans requested by texting **“Joyful Giver”** to [@CollinsRoyalMedium](https://t.me/CollinsRoyalMedium). These also begin with a **2-week trial**.

Full wording, disclosures and the request-to-verify flow: [docs/economy.md](docs/economy.md).

## Privacy model

- Identity keys are generated on the device and never leave it unless you export an encrypted backup.
- Messages are sealed before they reach a relay; the relay sees routing metadata and ciphertext only.
- Backups are encrypted with PBKDF2-SHA256 (310,000 iterations) and AES-256-GCM.
- In the Android app, secrets are additionally encrypted with an AES-256-GCM key held by the Android Keystore, so a copied preferences file is useless elsewhere.
- No analytics, no advertising identifiers, no third-party scripts, no fonts fetched at runtime.

Details and threat model: [docs/privacy.md](docs/privacy.md).

## Optional relay

Union is complete without a server. A relay adds:

- handle lookup by signed card,
- store-and-forward delivery when both people are not online,
- shared public rooms,
- resolvable link URLs (`/l/<id>`).

The relay is a Cloudflare Worker + D1 database in [`server/`](server/), with every write authenticated by an Ed25519 signature. It runs at **https://union-relay.access1tutamailcom.workers.dev**; the intended first-party address is `https://union-relay.cculture.net`. It is optional and self-hostable. Deploy notes: [docs/relay.md](docs/relay.md).

## Repository layout

```
src/                 Svelte 5 + TypeScript app (screens, lib, design system)
  lib/crypto.ts      Ed25519, X25519, HKDF, AES-GCM, cards, sealing, backups
  lib/store.svelte.ts  local records, economy, sync, audit, outbox
  screens/           one screen per capability
public/              manifest, icons, service worker, WASM core
rust/                Union core compiled to WebAssembly
android/             no-Gradle Android shell (Java + resources + scripts)
server/              optional Cloudflare Worker relay + D1 schema + tests
scripts/             build, postbuild, APK, icons, WASM, tests
tests/               app tests (node:test)
docs/                economy, privacy, relay, build, distribution, design
```

## Design

**Lumen**: a warm porcelain ground, one ultramarine signal, champagne for value, selective liquid glass and lacquered hero cards. Material 3 Expressive shapes and spring motion, light by default with a tuned dark theme. Typography is **Google Sans** throughout, with **Google Sans Code** for hashes and keys. The type is bundled, so the interface renders identically offline.

The assistant also speaks **A2UI**: a small, versioned catalog of headings, stats, lists, forms and actions, validated before rendering and limited to four allowlisted actions. Nothing an agent composes can execute without approval, and the catalog is designed to map one-to-one onto Jetpack Compose + Material 3 later.

More: [docs/design.md](docs/design.md).

## Limitations, stated honestly

- v0.0.1 is a **single-device** identity. Multi-device key sync is not implemented; backups are the bridge.
- Relay delivery is store-and-forward, not real-time push. The app syncs when opened or when you tap Sync.
- The economic layer is test mode: records and plans are real, funds are not.
- The Union Assistant's proof ingestion depends on a bot backend; v0.0.1 packages the proof and opens the bot with the package ready to send.
- Google Sans is a Google typeface. If you fork Union for commercial distribution, review Google's font terms for your use.

## License

AGPL-3.0-only. See [LICENSE](LICENSE).
