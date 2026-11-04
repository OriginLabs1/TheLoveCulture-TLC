# TLC — v0.0.5

Android-first shell over a sovereign, non-custodial core.
Doctrine: [`CONTEXT.md`](CONTEXT.md) · Contract: [`SPECS.md`](SPECS.md) · Releases: [v0.0.5](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/latest)

**Status: trust and contact.** The record is proven, and v0.0.5 makes it usable between people.
Names, numbers and a way to reach the Pioneer: `@handles` for people and reserved roles, a local
address book, an encrypted bank vault that saves a destination once and reveals it only on purpose,
Send Support, the renamed **Pioneer's Token**, and a purpose-first **Pioneer Desk**. The visual
language is new — royal blue, azure and silver, no pink — with four tabs (Today, Record, People,
Settings) and a Create sheet. Everything is still local-only: no account, no sync, no `INTERNET`
permission, no custody. Accounts, sync, the SEF economy and the social layer are the next milestone.
The store copy says the same thing — see
[`fastlane/metadata`](fastlane/metadata/android/en-US/full_description.txt).

## Install v0.0.5

**[Download `tlc-0.0.5-foss.apk`](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.5/tlc-0.0.5-foss.apk)**
· [install page](https://originlabs1.github.io/TheLoveCulture-TLC/) ·
[release page](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/tag/v0.0.5) ·
[SHA256SUMS](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.5/SHA256SUMS) ·
[signature](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/download/v0.0.5/tlc-0.0.5-foss.signature.txt)

```sh
sha256sum -c SHA256SUMS
apksigner verify --print-certs tlc-0.0.5-foss.apk
# certificate SHA-256: e04cb8aa2cf5fdeec043d917a74a5ba634121f2a0b98df92869a42bb4acb732c
```

Android 7.0 or newer. The APK asks for no `INTERNET` permission. Add
`https://github.com/OriginLabs1/TheLoveCulture-TLC` to Obtainium for update checks straight from
these releases. F-Droid, Orion Store and Uptodown receive the same signed file; see
[`docs/DISTRIBUTION.md`](docs/DISTRIBUTION.md) for their status and the submission rules.

## What exists (and is proven by tests)

| Layer | Path | Status |
|---|---|---|
| Domain state machine (SK-01) | `core/domain` | 34 invariant tests green — roles, tokens, handles, NUBAN, dyad authority, fee state machine |
| Hash-chained Pioneer ledger (SK-04, INV-08) | `core/ledger` | 53 tests green — tamper, reorder, truncation, torn writes, failed writes, replay and restore all caught |
| Experience layer | `core/experience` | 31 tests green — onboarding, preferences, dyad, proposals, grants, tokens, Support, people, contact log, state and bundle codecs |
| `ledger-verify` CLI (SPECS §B-3) | `tools/ledger-verify` | 9 tests — reads the app's own export; exit-code contract tested |
| Android shell (Compose M3) | `apps/android` | onboarding, Today, Record, People, Bank vault, Support, Pioneer Desk, Grants, Token, Circle, Settings, Verify and About; type-checks against the real Compose classpath |

No code path in this repository moves, holds, pools or escrows money. `Effect` has
`requiresCustody = false` on every member, and the ledger has no `BALANCE`/`TRANSFER`/`ESCROW`
entry kind — both enforced by tests, not by comment.

## Run it

```bash
make doctor        # toolchain + secret presence (never prints secrets)
make verify        # compile + all 127 invariant tests, no Android SDK needed
make ledger-verify # chain verification, including deliberate tampering
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
git add version.properties && git commit -m "release: v0.0.5"
git tag v0.0.5 && git push origin main v0.0.5
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

Wallet, balance, escrow, card data, admin superuser, analytics on amounts.
Every one of those is a licence or a breach waiting to happen (CONTEXT §9).

## Licence

**GPL-3.0-or-later** — see [`LICENSE`](LICENSE). This is the licence F-Droid expects and it keeps the
reciprocity the doctrine asks for. Privacy: [`PRIVACY.md`](PRIVACY.md). Security: [`SECURITY.md`](SECURITY.md).

Pioneer: [sircollins.coreconcept1.net](https://sircollins.coreconcept1.net) ·
[Telegram](https://t.me/CollinsRoyalMedium) · [tlcfounder@protonmail.com](mailto:tlcfounder@protonmail.com)
