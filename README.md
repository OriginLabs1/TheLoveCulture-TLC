# TLC — v0.0.1 scaffold

Android-first shell over a sovereign, non-custodial core.
Doctrine: [`CONTEXT.md`](CONTEXT.md) · Contract: [`SPECS.md`](SPECS.md)

## What exists (and is proven by tests)

| Layer | Path | Status |
|---|---|---|
| Domain state machine (SK-01) | `core/domain` | 29 invariant tests green |
| Hash-chained Pioneer ledger (SK-04, INV-08) | `core/ledger` | 21 tests green — tamper, reorder, truncation, replay all caught |
| `ledger-verify` CLI (SPECS §B-3) | `tools/ledger-verify` | exit-code contract tested, `--demo` proves the invariants |
| Android shell (Compose M3) | `apps/android` | type-checks against the real Compose classpath; APK built by CI |

No code path in this repository moves, holds, pools or escrows money. `Effect` has
`requiresCustody = false` on every member, and the ledger has no `BALANCE`/`TRANSFER`/`ESCROW`
entry kind — both enforced by tests, not by comment.

## Run it

```bash
make doctor        # toolchain + secret presence (never prints secrets)
make verify        # compile + all 56 invariant tests, no Android SDK needed
make ledger-verify # chain verification, including deliberate tampering
make shell-check   # type-check the shell without AAPT2 (any CPU)
make android-build # debug APK (x86_64 host: AAPT2 ships x86-64 only)
```

JDK 21 is the only hard requirement for `verify`. The Android module joins the build only when
`ANDROID_HOME` is set (see `settings.gradle.kts`), so the core stays testable everywhere.
On an arm64 host `assembleDebug` cannot run (Google ships AAPT2 as an x86-64 binary); use
`make shell-check` locally and let CI produce the APK.

## Structure

```
apps/android      Compose shell — pixels, never rules
core/domain       roles, tokens, dyad authority, fee state machine
core/ledger       append-only hash chain, Merkle anchor, verification
tools/ledger-verify  nightly proof that nothing was rewritten
docs/             (next) ADRs, threat model, runbooks
```

## Deliberately absent

Wallet, balance, escrow, card data, admin superuser, analytics on amounts.
Every one of those is a licence or a breach waiting to happen (CONTEXT §9).
