# TLC v0.0.1 — SPECS

> Executable contract. Doctrine and rationale: `CONTEXT.md` (wins on conflict).
> Normative keywords: **MUST / MUST NOT / SHOULD / MAY** (RFC 2119).
> This file contains three parts: **SKILLS** (capabilities), **TOOLS** (build & runtime),
> **WORKFLOWS** (ordered procedures). Each part begins with a `<!-- split:NAME.md -->` marker, so
> `awk` (or any 5-line script) can extract `SKILLS.md`, `TOOLS.md`, `WORKFLOWS.md` without divergence:
> `csplit -sz SPECS.md '/<!-- split:/' '{*}'` then rename by first line. Single source, many views.

---

## 0. Conformance & Scope

**v0.0.1 in scope:** Android app (alpha) · installable PWA · 3 roles + dyad · Receptive Mode ·
Supportive/Recognition/Joy/Courtesy/Activation/Reactivation flows (direct transfer + attestation) ·
Growth Grant pledge-and-witness · consent grants · Steward Console (read + attest + revoke) ·
Pioneer Ledger + verification export · offline read / queued intent · CI + signed release.

**Out of scope:** card acquiring, in-app wallet/balance, custodial escrow, chat, video, matching
algorithms, feed, payments API to third parties, iOS, ML ranking, crypto tokens.

**A change is conformant only if:** no INV-01…INV-10 violation, port conformance tests pass,
ledger chain verifies, a11y AA audit passes, and every user-visible money string matches §4 copy.

---

## PART A — SKILLS
<!-- split:SKILLS.md -->

> A skill = a bounded capability an implementer (human or agent) can execute without improvising.
> Each declares trigger, inputs, result, and hard guardrails.

### SK-01 `tlc-domain-model`
**Trigger:** touching roles, dyad, tokens, obligations, states.
**Do:** model as pure KMP types; encode every state transition as an explicit, total function with
a reason code; forbid `Double` for money (integer kobo, `Long`); forbid nullable booleans; version
every persisted shape.
**Result:** `core/domain` with property-based tests over the full transition matrix.
**Guardrails:** no Android/HTTP imports; no clocks or randomness inside domain (inject ports).

### SK-02 `tlc-non-custody-review`
**Trigger:** any feature that mentions money, fees, grants, transfers, or "balance".
**Do:** run the custody test — *"can TLC end up holding, delaying, redirecting or netting value?"*
If yes, redesign to instruction + attestation. Produce a 5-line custody note in the PR.
**Result:** PR section "Custody: none — <routing table row>".
**Guardrails:** vocabulary lint (escrow, wallet, payout, yield, invest, refund) fails the build.

### SK-03 `tlc-direct-transfer-flow`
**Trigger:** building Activation/Reactivation/Courtesy/Joy/Supportive/Recognition.
**Do:** render destination from the *correct* source (recipient-provided vs Pioneer runtime secret);
show amount, purpose, one-line notice, and "Transfer Method"; give copyable account + reference
code; then run SK-04. Never pre-fill a destination from cache older than 24h.
**Result:** flow + snapshot tests for every role/token pair.
**Guardrails:** Pioneer details appear **only** inside fee flows; never in logs, analytics, links, screenshots.

### SK-04 `tlc-evidence-attestation`
**Trigger:** a user claims they transferred.
**Do:** create `FeeIntent` (idempotency key) → signed PUT to R2 → SHA-256 + MIME/size checks →
scan → `Attestation` by Pioneer/Steward (4-eyes for amounts ≥ ₦50k) → chained `Receipt` VC.
Three-way match; mismatch ⇒ exception with reason code.
**Result:** receipt verifiable offline; chain length +1.
**Guardrails:** clients MUST NOT mint receipts; "Attested" never rendered as "Confirmed by bank"
unless a rail adapter actually confirmed it.

### SK-05 `tlc-sync-conflict`
**Trigger:** PowerSync backlog, conflicting edits, schema migration.
**Do:** classify field (preference = LWW per column; relationship = both-signatures; money = append-only
server sequence); write a conflict test that reproduces the case before fixing.
**Result:** migration + regression test; no silent data loss.
**Guardrails:** never resolve a money conflict client-side.

### SK-06 `tlc-consent-link`
**Trigger:** invitations, dyad binding, steward delegation, bank-detail change.
**Do:** mint PASETO v4.public token `{sub, act, res, dyad, jti, exp≤15m}`; deliver by link; require
separate authentication to act; single-use; revocable; audit both issuance and redemption.
**Result:** link + verifier endpoint + revocation test.
**Guardrails:** no PII/secrets in URL; no authority from possession; expired link shows a helpful page.

### SK-07 `tlc-compose-ui`
**Trigger:** any Android screen.
**Do:** Material 3 + TLC tokens (color/type/space/motion); one primary action per screen; skeleton
loading; empty/error/offline states designed, not defaulted; large-type + TalkBack pass; 48dp targets.
**Result:** screen + preview + a11y labels + screenshot test at phone/tablet/foldable widths.
**Guardrails:** no hardcoded colors/strings (resources only); no blocking work on main thread.

### SK-08 `tlc-pwa-shell`
**Trigger:** web/PWA work.
**Do:** SvelteKit (stable) with `#lib` aliases; route-level code splitting; service worker with
cache-first assets + network-first data; install prompt only after meaningful engagement; offline
route with queued intents visible.
**Result:** Lighthouse ≥ 95 perf/a11y/best-practices, PWA installable, works offline after first load.
**Guardrails:** no business logic duplicated from `core/`; bundle budget enforced in CI.

### SK-09 `tlc-crypto`
**Trigger:** keys, signatures, VC issuance/verification, hashing.
**Do:** use audited primitives (libsodium/BoringSSL via NDK, or platform Keystore); Ed25519 sign;
JCS/CBOR-LD canonicalisation; key rotation + revocation list; test vectors from RFC.
**Result:** vectors in `core/crypto/testvectors`, cross-platform parity test (Kotlin ↔ C++ ↔ TS).
**Guardrails:** never hand-roll crypto; never store private keys unencrypted; never log key material.

### SK-10 `tlc-privacy-dpia`
**Trigger:** new data field, upload, analytics event, or third-party service.
**Do:** fill the data ledger row (purpose, lawful basis, retention, access, export, delete path);
minimise; default to on-device; add DSR handler; update privacy notice.
**Result:** DPIA delta + retention job + test for deletion.
**Guardrails:** no analytics on money amounts, names, or bank details; no third-party SDK without review.

### SK-11 `tlc-release`
**Trigger:** cutting a version.
**Do:** run the full gate (§C-8), bump `manifest.version`, generate changelog from commits, sign APK
with upload key from secrets, publish to internal track, tag, attach SBOM + checksums, deploy PWA.
**Result:** reproducible artifact + release notes + rollback path.
**Guardrails:** no release from a dirty tree; no unsigned artifact; no secret in build logs.

### SK-12 `tlc-incident`
**Trigger:** suspected leak, fraud, chain failure, or outage.
**Do:** contain → preserve evidence (hashes, not copies) → notify affected (NDPA 72h) → fix →
post-mortem with INV mapping → add a failing test that would have caught it.
**Result:** incident record in `docs/runbooks`, ledger integrity statement.
**Guardrails:** never delete audit logs; never silently rotate away evidence.

---

## PART B — TOOLS
<!-- split:TOOLS.md -->

### B-1 Toolchain (pin in version catalog; latest **stable** only)

| Concern | Tool | Notes |
|---|---|---|
| Core logic | Kotlin Multiplatform (stable) | domain, contracts, crypto bindings |
| Android | AGP stable · JDK 21 · Compose BOM · Material 3 | minSdk 26, target latest stable |
| Local DB | SQLDelight (Android) · SQLite WASM/OPFS (web) | same schema, one migration source |
| Sync | PowerSync SDK | behind `SyncPort`; server truth = Postgres |
| PWA | SvelteKit stable · Vite stable · Svelte 5 · Tailwind | `#lib` subpath imports |
| Native | NDK + CMake, libsodium/BoringSSL | crypto only; no UI, no business logic |
| Edge | Cloudflare Workers · R2 · Queues · Durable Objects · Turnstile · WAF | TS, strict mode |
| Data (server) | Postgres (managed) + row-level security | append-only tables for attestations |
| Contracts | JSON-LD 1.1 · W3C VC 2.0 · OpenAPI 3.1 · JSON Schema 2020-12 | versioned, published |
| Testing | JUnit5/Kotest · Turbine · Playwright · Compose UI test · Pact-style port contracts | property-based for domain |
| Quality | ktlint/detekt · ESLint/Prettier · svelte-check · gitleaks · Renovate · Syft (SBOM) | fail-closed in CI |
| CI/CD | GitHub Actions | mobile-first (§B-3) |

Rejected on purpose: app-level Java (drift), unmanaged pre-release majors (churn), any custodial
payment SDK, heavy enterprise data lake, generic admin framework.

### B-2 Repository layout

```
TLC App/
├─ apps/{android,pwa,console,cli}
├─ core/{domain,contracts,crypto,policy,ledger}
├─ adapters/{storage,sync,rail,evidence,notify,identity,flags}
├─ contracts/{jsonld,schemas,openapi,vc}
├─ infra/{cloudflare,actions,iac}
├─ design/{tokens,icons,copy}
├─ docs/{CONTEXT.md,SPECS.md,adr,threat-model,runbooks}
└─ .github/workflows/
```

### B-3 GitHub Actions (mobile-first, mandatory)

| Workflow | Trigger | Gate |
|---|---|---|
| `ci-core.yml` | PR | ktlint/detekt, domain + crypto vectors, port conformance, coverage ≥ 80% on `core/ledger` |
| `ci-android.yml` | PR | assemble, unit, Compose screenshot, emulator instrumented (API 26 + latest), lint, a11y scan |
| `ci-pwa.yml` | PR | svelte-check, build, bundle budget, Playwright e2e, Lighthouse budget, offline test |
| `ci-contracts.yml` | PR (paths) | JSON-LD contexts valid, schemas backward-compatible, VC round-trip |
| `security.yml` | PR + nightly | gitleaks, dependency review, SBOM, SAST, secret-scope check |
| `ledger-verify.yml` | nightly | re-verify hash chain + last Merkle anchor; fail loudly |
| `release-android.yml` | tag `v*` | signed AAB, internal track, checksums, SBOM |
| `deploy-edge.yml` | main (paths `infra/`) | Workers/R2 deploy behind env approval |
| `docs.yml` | main | link check, docs build |

Rules: least-privilege `permissions:` per job; OIDC where available; no long-lived cloud keys in
workflow files; concurrency groups; caching keyed on lockfiles; every job reproducible locally via
`make <target>`. Mobile is the default home for automation; non-mobile automation needs a reason.

### B-4 Secrets & configuration

| Name | Where | Never |
|---|---|---|
| `PIONEER_ACCOUNT_*` | CF Worker secret / Android runtime config | repo, APK, screenshots, logs |
| `CLOUDFLARE_API_TOKEN` | GH Actions secret (scoped: Workers, Pages, R2) | user API key, chat |
| `GITHUB_TOKEN` | auto (Actions) | PAT in code |
| `ANDROID_KEYSTORE_*` | GH Actions secret + offline backup | repo |
| `POSTGRES_URL`, `R2_*` | CF secret | client bundles |
| `TLC_ENV` | build config (`dev/staging/prod`) | hardcoded prod URLs |

Pre-flight (MUST, before any deploy):
`if [ -z "$CLOUDFLARE_API_TOKEN" ]; then echo "✗ Token missing"; exit 1; else echo "✓ Token set"; fi`

### B-5 Ports (interface contract)

`IdentityPort.peek/verify/deviceKey` · `StoragePort.tx/query/migrate` ·
`SyncPort.status/push/pull/subscribe` · `EvidencePort.putSigned/hash/scan` ·
`RailPort.instructions/confirmIntent/subscribe(optional)` · `NotifyPort.send(template, channel)` ·
`LinkPort.mint/verify/revoke` · `ClockPort.now` · `PolicyPort.evaluate` · `FlagPort.get`.

Each port ships: fake, local adapter, prod adapter, and a conformance suite that all three pass.

### B-6 External integrations (all optional, all replaceable)

GitHub (repo/CI/issues) · Cloudflare (edge/storage) · Postgres host · transactional email/SMS ·
bank-statement import (CSV) · payment-provider webhook (opt-in) · push (FCM) · error telemetry
(PII-scrubbed). The GitHub MCP server is a convenience for issue triage only — CI and delivery use
`git` + `gh`/REST; no workflow may depend on a chat-side MCP connection.

### B-7 Performance & UX budgets

Cold start ≤ 2.0 s (mid-range Android) · TTI PWA ≤ 1.8 s on 4G · APK ≤ 12 MB download ·
JS ≤ 180 KB gzip initial · 60 fps scrolling · offline-capable read within 300 ms · zero layout shift
on money surfaces · every async action shows determinate state.

### B-8 Definition of Done (acceptance criteria → test)

Amazing/Reliable → crash-free ≥ 99.5% in alpha, flake-free CI.
Universal → one core, 3 shells, RTL + large-type pass.
Robust/Safe → INV suite green, threat model updated, pen-test checklist for money flows.
Structured/Cohesive → port conformance, schema compatibility, no orphan code.
Mature/Self-sufficient → runbooks, offline docs, single-command bootstrap.
Reproducible → pinned toolchain, locked deps, signed artifacts, checksums.
Elegant/Polished → design tokens only, motion spec, copy reviewed against §4.
Adaptive → phone/tablet/foldable/desktop-web layouts verified.
Economically capable → fee obligations traceable end-to-end (SK-04).

---

## PART C — WORKFLOWS
<!-- split:WORKFLOWS.md -->

### C-1 First-run bootstrap
1. `pwd && ls` — verify repo root and structure.
2. `make doctor` — JDK 21, SDK/NDK, Node, git, gh, secrets present.
3. `make bootstrap` — install deps, generate local schema, seed dev fixtures (no real PII).
4. `make verify` — build + unit + chain check on fixtures. Must be green before any change.

### C-2 Onboarding (The Reveal → The Choice → The Form → Coach-marks)
1. **Reveal:** ≤ 4 slides, skippable, no account needed, ends with one promise + one notice.
2. **Choice:** three roles with plain-language consequences (cost, visibility, veto).
3. **Form:** minimal fields; dyad invite issued as SK-06 link; consent captured with timestamp + version.
4. **Coach-marks:** spotlight *Receptive Mode* and *Acknowledge* — dismissible, never blocking, replayable in Settings.
**Exit:** profile exists offline; sync queued; no Pioneer details shown.
**Failure:** resumable at every step; nothing lost on kill.

### C-3 Dyad formation
Invite (signed, 15 min) → invitee authenticates → both confirm → `ConsentGrant` pair written →
shared space created with two-signature policy → both see the same ledger view.
Rejection/expiry leaves no half-dyad. Dissolution requires both signatures or a 7-day unilateral exit.

### C-4 Fee flow (Activation / Reactivation / Courtesy / Joy) — the money path
1. Resolve obligation (role, period, amount, idempotency key) — server-authoritative.
2. Show notice + transfer method; fetch Pioneer details from runtime secret (TTL 15 min).
3. User transfers in their banking app (deep link out; return via universal link).
4. Upload proof → `FeeIntent` + SHA-256 → R2 signed PUT.
5. Attestation: Pioneer/Steward matches statement; 4-eyes ≥ ₦50k; exception path on mismatch.
6. Chained `Receipt` VC issued; user sees "Attested" + downloadable proof-of-payment.
7. Daily Merkle root anchored; nightly `ledger-verify` re-checks.
**Never:** optimistic "paid", auto-retry of a transfer, storing card data, hiding the notice.

### C-5 Supportive / Recognition (peer, zero custody)
Recipient publishes verified transfer details (dual-signed; 24h cool-down on change) →
sender sees `bank · last4 · verified` + amount + optional message → sender transfers directly →
sender may attach a self-declared receipt (optional, never required) → recipient may *Acknowledge*
(no amount shown publicly by default) → both sides keep an offline-verifiable record.
**Never:** TLC in the middle, no fee, no ranking by amount.

### C-6 Growth Grant (pledge → evidence → direct payment)
Donor pledges (amount, milestone, deadline) → beneficiary accepts → milestone evidence uploaded
(permission-scoped) → steward verifies (reason code + audit) → donor receives a one-tap direct
transfer instruction to the **beneficiary's** account → donor pays → receipt attested.
Unfulfilled pledge expires cleanly (no penalty, no custody, no debt claim by TLC).

### C-7 Steward delegation & revocation
Request → scope + expiry stated in plain language → delegate authenticates → signed `ConsentGrant`
→ console shows only scoped views → 90-day auto-expiry → revocation propagates offline within one sync cycle.
**Never:** standing superuser, cross-tenant read, bulk export of people/bank data.

### C-8 Release gate (must all pass)
`make verify` → `make e2e` → `make a11y` → `make ledger-verify` → `make security` → version bump →
signed build → internal track → smoke on a physical device → tag → deploy PWA → publish notes +
checksums + SBOM → post-release watch 48h → rollback plan documented.

### C-9 Sync recovery / conflict
Detect (backlog, divergence) → classify field class → for money facts: discard client claim, re-pull
server sequence → for preferences: LWW with `updated_at` + device tiebreak → for relationships:
require re-confirmation → migrate schema with forward+backward test → verify chain again.

### C-10 Offline behaviour
Reads: always from local SQLite. Writes: queued with visible "pending" badge and a per-item state
(`queued → sent → attested | failed(reason)`). Money-adjacent: intent may queue; **settlement never**.
Conflict banner is honest, actionable, and dismissible only after resolution.

### C-11 Incident response
Detect → contain (revoke grants, rotate secret, disable flag via `FlagPort`) → preserve hashes →
assess NDPA 72h duty → notify → fix → post-mortem mapping to INV IDs → regression test → publish
ledger integrity statement if attestations were affected.

### C-12 Data subject request
Export (JSON + human PDF, offline-capable) → delete (crypto-shred where possible, hash-only residue
for receipts required by tax) → confirm with audit record → never delete another person's evidence.

---

## PART D — Traceability map (obligation → evidence)

| Obligation | Artifact | Verifier |
|---|---|---|
| Fee owed | `FeeIntent` (signed) | any device, offline |
| Fee claimed | Proof artifact hash | R2 metadata + ledger |
| Fee settled | `Attestation` + statement ref | Pioneer/Steward (4-eyes ≥ ₦50k) |
| Fee proven | `Receipt` VC, chained | offline chain verify + daily anchor |
| Delegation | `ConsentGrant` VC | scope/expiry check |
| Peer support | recipient-signed details + optional receipt | either party |
| Grant | pledge + evidence + steward decision | donor, beneficiary, auditor |

*End of SPECS v0.0.1.*
