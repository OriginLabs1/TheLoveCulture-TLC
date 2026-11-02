# TLC — v0.0.3

Android-first shell over a sovereign, non-custodial core.
Doctrine: [`CONTEXT.md`](CONTEXT.md) · Contract: [`SPECS.md`](SPECS.md) · Releases: [v0.0.3](https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/latest)

**Status: durable.** The rules and the ledger are proven by tests, and the record now survives the
process: entries are written to the app's private folder with `fsync`, re-verified on every launch,
and refused when the chain does not check out. The shell is a working journal — search, entry proof,
seal sharing and export. Accounts, sync, steward approvals and growth grants are the next milestone.
The store copy says the same thing — see [`fastlane/metadata`](fastlane/metadata/android/en-US/full_description.txt).

## What exists (and is proven by tests)

| Layer | Path | Status |
|---|---|---|
| Domain state machine (SK-01) | `core/domain` | 29 invariant tests green |
| Hash-chained Pioneer ledger (SK-04, INV-08) | `core/ledger` | 51 tests green — tamper, reorder, truncation, torn writes, failed writes, replay all caught |
| `ledger-verify` CLI (SPECS §B-3) | `tools/ledger-verify` | reads the app's own export; exit-code contract tested |
| Android shell (Compose M3) | `apps/android` | durable journal, entry proof, seal share, export; type-checks against the real Compose classpath |

No code path in this repository moves, holds, pools or escrows money. `Effect` has
`requiresCustody = false` on every member, and the ledger has no `BALANCE`/`TRANSFER`/`ESCROW`
entry kind — both enforced by tests, not by comment.

## Run it

```bash
make doctor        # toolchain + secret presence (never prints secrets)
make verify        # compile + all 89 invariant tests, no Android SDK needed
make ledger-verify # chain verification, including deliberate tampering
make check-foss    # prove the foss channel has no Play Services / Firebase / tracking
make shell-check   # type-check the shell without AAPT2 (any CPU)
make android-build # foss debug APK (x86_64 host: AAPT2 ships x86-64 only)
```

JDK 21 is the only hard requirement for `verify`. The Android module joins the build only when
`ANDROID_HOME` is set (see `settings.gradle.kts`), so the core stays testable everywhere.
On an arm64 host `assembleFossDebug` cannot run (Google ships AAPT2 as an x86-64 binary); use
`make shell-check` locally and let CI produce the APK.

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
| `foss` | no Play Services, no Firebase, no analytics, no proprietary SDK | GitHub Releases, F-Droid, Orion Store |
| `full` | optional first-party integrations, never required for a core flow | GitHub Releases |

Both use `culture.thelove.tlc` and one signing key, so a user can move between channels without
losing data. `verifyFossOnly` fails the build if a proprietary dependency reaches `foss`.

## Release

Version identity lives in one file (`version.properties`); the git tag must equal `v<versionName>`.

```bash
./scripts/bump-version.sh patch
git add version.properties && git commit -m "release: v0.0.4"
git tag v0.0.4 && git push origin main v0.0.4
```

`.github/workflows/release.yml` then verifies the tag against `version.properties`, rejects a
non-increasing `versionCode`, refuses to build without the signing key, runs the core invariants and
the FOSS guard, signs `assembleFossRelease`, verifies the signature, pins the signing certificate
against [`release-key.properties`](release-key.properties), proves the APK requests no `INTERNET`
permission and carries no Play Services classes, and publishes an **immutable** GitHub Release with
the APK, `SHA256SUMS`, the dependency manifest, the signature block and a build-provenance
attestation. The same signed APK is what F-Droid and Orion receive — never a different build.

Key handling, rotation and store submission: [`RELEASING.md`](RELEASING.md).

## Structure

```
apps/android      Compose shell — pixels, never rules
core/domain       roles, tokens, dyad authority, fee state machine
core/ledger       append-only hash chain, durable storage, Merkle anchor, verification
tools/ledger-verify  nightly proof that nothing was rewritten
docs/             (next) ADRs, threat model, runbooks
```

## Deliberately absent

Wallet, balance, escrow, card data, admin superuser, analytics on amounts.
Every one of those is a licence or a breach waiting to happen (CONTEXT §9).

## Licence

**GPL-3.0-or-later** — see [`LICENSE`](LICENSE). This is the licence F-Droid expects and it keeps the
reciprocity the doctrine asks for. Privacy: [`PRIVACY.md`](PRIVACY.md). Security: [`SECURITY.md`](SECURITY.md).
