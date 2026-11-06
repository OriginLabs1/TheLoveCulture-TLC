# TLC — v0.0.7

Android-first shell over a sovereign, non-custodial core.
Doctrine: [`CONTEXT.md`](CONTEXT.md) · Contract: [`SPECS.md`](SPECS.md) · Releases: [v0.0.7](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/latest)

**Status: the door, the face and the route.** v0.0.7 adds the identity the next milestone needs: a
distinct username and a word/phrase passphrase, with no biometric or passkey door anywhere. An
avatar studio frames your photo before it is saved, presence is four independent choices that start
at Nobody, and a token transfer asks which real route the value will take. Everything is still
local-only: no server account, no sync, no `INTERNET` permission, no custody. Presence choices are
stored on this device and are the instruction the shared plane must obey; the Transfer Method sheet
describes real routes honestly and does not invent a provider quote. The store copy says the same
thing — see [`fastlane/metadata`](fastlane/metadata/android/en-US/full_description.txt).

**Doctrine v0.0.2 — the direction the next build must earn.** TLC is an **instrument, not a bank**:
people move real money between their own accounts, and TLC records the instruction, proof,
attestation and receipt. The shared plane adds encrypted messaging, calls where supported, and the
same token state on both sides; the identity, presence and Transfer Method surfaces in this build
are the local half of that. **Pear/Holepunch** (Hypercore/Hyperswarm) is evaluated only for
encrypted backup and nearby handoff — never as the authority for a shared fact. None of the shared
plane is claimed for this build; see [`CONTEXT.md`](CONTEXT.md), [`SPECS.md`](SPECS.md) and the
working plan [`docs/PLAN-v0.0.7.md`](docs/PLAN-v0.0.7.md).

## Install v0.0.7

**[Download `tlc-0.0.7-foss.apk`](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.7/tlc-0.0.7-foss.apk)**
· [install page](https://originlabs1.github.io/TheLoveCulture-TLC/) ·
[release page](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/tag/v0.0.7) ·
[SHA256SUMS](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.7/SHA256SUMS) ·
[signature](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.7/tlc-0.0.7-foss.signature.txt)

```sh
sha256sum -c SHA256SUMS
apksigner verify --print-certs tlc-0.0.7-foss.apk
# certificate SHA-256: e04cb8aa2cf5fdeec043d917a74a5ba634121f2a0b98df92869a42bb4acb732c
```

Android 8.0 or newer (API 26). The APK asks for no `INTERNET` permission. Add
`https://github.com/OriginLabs1/TheLoveCulture-TLC` to Obtainium for update checks straight from
these releases. F-Droid, Orion Store and Uptodown receive the same signed file; see
[`docs/DISTRIBUTION.md`](docs/DISTRIBUTION.md) for their status and the submission rules.

## What exists (and is proven by tests)

| Layer | Path | Status |
|---|---|---|
| Domain state machine (SK-01) | `core/domain` | 34 invariant tests green — roles, tokens, handles, NUBAN, dyad authority, fee state machine |
| Hash-chained Pioneer ledger (SK-04, INV-08) | `core/ledger` | 53 tests green — tamper, reorder, truncation, torn writes, failed writes, replay and restore all caught |
| Experience layer | `core/experience` | 35 tests green — onboarding, preferences, dyad, proposals, grants, tokens, Support, people, contact log, profile and receipts, state and bundle codecs |
| `ledger-verify` CLI (SPECS §B-3) | `tools/ledger-verify` | 9 tests — reads the app's own export; exit-code contract tested |
| Android shell (Compose M3) | `apps/android` | onboarding with passphrase, Today, Record, People, Bank vault, Support with Transfer Methods, Pioneer Desk, Grants, Token, Tokens, Profile with avatar studio, Presence, Circle, Settings, Verify and About; type-checks against the real Compose classpath |

No code path in this repository moves, holds, pools or escrows money. `Effect` has
`requiresCustody = false` on every member, and the ledger has no `BALANCE`/`TRANSFER`/`ESCROW`
entry kind — both enforced by tests, not by comment.

## Run it

```bash
make doctor        # toolchain + secret presence (never prints secrets)
make verify        # compile + all 131 invariant tests, no Android SDK needed
make ledger-verify # chain verification, including deliberate tampering
make copy-lint     # fail on banned user-facing terms (SPECS §0.1 / SK-19)
make check-foss    # prove the foss channel has no Play Services / Firebase / tracking
make shell-check   # type-check the shell without AAPT2 (any CPU)
make android-build # foss debug APK
```

JDK 21 is the only hard requirement for `verify`. The Android module joins the build only when
`ANDROID_HOME` is set (see `settings.gradle.kts`), so the core stays testable everywhere.
Google ships AAPT2 as an x86-64 binary, so on an arm64 host the Android build runs it through
`qemu-user-static` with an amd64 libc; [`RELEASING.md`](RELEASING.md) records the local workaround.
CI produces the release APK on x86-64.

## How the record stays honest

- One header line, then one pipe-delimited line per entry (`#tlc-ledger-v1`). A person can read it
  without this app.
- Every entry hashes its sequence, kind, time, amount (when present), subject and the hash of the
  entry before it. Editing a past entry breaks every later hash.
- A local seal (`length:head:root`) detects a tail removed from this device. A published seal detects
  a tail removed and the local seal rewritten.
- Writes are durable before they are acknowledged. A failed write is not shown as saved and the
  session becomes read-only rather than diverging from the disk.
- A crash mid-write is repaired by dropping only the incomplete line; an edit is reported, never
  repaired away.

## Two flavours, one identity

| Flavour | Contains | Ships to |
|---|---|---|
| `foss` | no Play Services, no Firebase, no analytics, no proprietary SDK | GitHub Releases, F-Droid, Orion Store, Uptodown |
| `full` | optional first-party integrations, never required for a core flow | GitHub Releases |

Both use `culture.thelove.tlc` and one signing key, so a user can move between channels without
losing data. `verifyFossOnly` fails the build if a proprietary dependency reaches `foss`.

## Release

Version identity lives in one file (`version.properties`); the git tag must equal `v<versionName>`.

```bash
./scripts/bump-version.sh patch
git add version.properties && git commit -m "release: v0.0.7"
git tag v0.0.7 && git push origin main v0.0.7
```

`.github/workflows/release.yml` then verifies the tag against `version.properties`, rejects a
non-increasing `versionCode`, refuses to build without the signing key, runs the core invariants and
the FOSS guard, signs `assembleFossRelease`, verifies the signature, pins the signing certificate
against [`release-key.properties`](release-key.properties), proves the APK requests no `INTERNET`
permission and carries no Play Services classes, and publishes an **immutable** GitHub Release with
the APK, `SHA256SUMS`, the dependency manifest, the signature block and a build-provenance
attestation. The same signed APK is what F-Droid, Orion and Uptodown receive — never a different build.

Key handling, rotation and store submission: [`RELEASING.md`](RELEASING.md).

## Structure

```
apps/android      Compose shell — pixels, never rules
core/domain       roles, tokens, handles, NUBAN, dyad authority, fee state machine
core/experience   use cases, durable state, people, Support, contact log, grant and token lifecycles, backup bundles
core/ledger       append-only hash chain, durable storage, Merkle anchor, verification
tools/ledger-verify  nightly proof that nothing was rewritten
docs/SEF.md       the Token direction, and the non-custody line it must keep
design/logo       vector logo masters and the store-icon source
```

## Deliberately absent

No wallet, no balance, no escrow, no card data, no admin superuser, no analytics on amounts.
Every one of those is a licence or a breach waiting to happen (CONTEXT §9).

## Licence

**GPL-3.0-or-later** — see [`LICENSE`](LICENSE). This is the licence F-Droid expects and it keeps the
reciprocity the doctrine asks for. Privacy: [`PRIVACY.md`](PRIVACY.md). Security: [`SECURITY.md`](SECURITY.md).

Pioneer: [sircollins.coreconcept1.net](https://sircollins.coreconcept1.net) ·
[Telegram](https://t.me/CollinsRoyalMedium) · [tlcfounder@protonmail.com](mailto:tlcfounder@protonmail.com)
