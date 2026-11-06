# TLC — SPECS

> Executable contract. Doctrine and rationale: `CONTEXT.md` v0.0.2 (wins on conflict).
> **Baseline:** the shipped v0.0.6 local-only shell (no account, no sync, no `INTERNET`).
> **Target:** the v0.0.7 shared plane described here — **Android-first**, with the release cut line
> in §0.2 and the seven mandate proofs in §0.3. The working plan is `docs/PLAN-v0.0.7.md`.
> Normative keywords: **MUST / MUST NOT / SHOULD / MAY** (RFC 2119).
> This file has three split parts — **SKILLS** (capabilities), **TOOLS** (build & runtime) and
> **WORKFLOWS** (ordered procedures) — plus a traceability map and a test matrix that bind them to
> `CONTEXT.md`. Each split part begins with a `<!-- split:NAME.md -->` marker so
> `awk` (or any 5-line script) can extract `SKILLS.md`, `TOOLS.md`, `WORKFLOWS.md` without
> divergence: `csplit -sz SPECS.md '/<!-- split:/' '{*}'` then rename by first line.
> Single source, many views.

---

## 0. Conformance & Scope

**v0.0.6 baseline in scope:** Android alpha · 3 roles + dyad · Receptive Mode ·
Supportive/Recognition/Joy/Courtesy/Activation/Reactivation records · Growth Grant
pledge-and-witness · consent grants · local Steward Console personas · Pioneer Ledger and offline
verification · offline read/write/export · signed release.

**v0.0.7 target in scope:** username + word/phrase passphrase identity with seamless sign-up,
sign-in, logout and new-device recovery · consented presence and last seen, default-private and
per-signal · instant 1:1 messaging with E2EE and a durable outbox · live shared token state on both
sides · live, flexible Transfer Methods · 1:1 voice call (beta, feature-flagged, only if the
WebRTC/TURN gate passes) · server-authoritative shared plane · encrypted peer backup and nearby
handoff via Pear/Holepunch behind `PeerPort` · PWA companion (read/verify/export; feature parity is
deferred) · scoped local attestation · signed config and release gates.

**Out of scope until a reviewed design exists:** TLC custody or a TLC balance · a TLC wallet ·
proprietary/custodial payment SDK · peer-to-peer settlement without a bank · crypto or stablecoin ·
securities, yield or investment products · public leaderboards or amount ranking · group messaging
or group video · PWA feature parity · a remote Steward Console · social recovery · location
tracking · public contact-graph export · silent server-side scanning of E2EE content · FX
conversion · iOS · ML ranking · any feature that only works when TLC holds or controls money.

**A change is conformant only if:**

1. no INV-01…INV-16 violation;
2. every port conformance suite passes (fake, local, production);
3. the ledger chain and daily anchor verify;
4. the accessibility audit passes at WCAG 2.2 AA;
5. every user-visible money string matches §0.1 and the `CONTEXT.md` §5.3 notices;
6. the offline / reconnect / presence / method / peer-off tests in Part E pass;
7. the copy lint finds no banned term and no false state label.

### 0.1 Terminology law (build-breaking)

| Use | Never use | Why |
|---|---|---|
| **Pioneer's Token**, **membership contribution**, **gift** | **dues** | A contribution is an offering, not a debt; "dues" is banned user-facing |
| **Transfer Method** | **Payment Method** | The route is chosen and disclosed; TLC is not a payment institution |
| **TLC is an instrument, not a bank** | we hold / your balance / top up / withdraw funds / deposit funds | No custody, no stored value; "withdraw a pledge" is a different, allowed verb |
| **Username** (handle) | login, email, phone ID | A handle is public and changeable; it is not a secret |
| **Passphrase** (word/phrase) | password | It is a phrase, never stored or sent in plaintext |
| **Recovery phrase** | recovery kit in user copy | The words are the artifact the person keeps |
| **Presence not shared** | offline (as a guess) | A hidden signal is not a network state |
| **Attested** / **Receipt issued** | Paid / Confirmed by bank | Only a bank adapter may claim bank confirmation |
| **On this device** | Live (when offline) | "Live" means connected and acknowledged |
| **Queued** | Pending payment | A queue is not a settlement |
| **Steward** | admin / moderator | Authority is scoped and expiring |
| **Token** | coin / asset | A record, not a financial instrument |

The lint in SK-19 MUST fail the build on any banned term in `strings.xml`, PWA copy, store
metadata, release notes, notifications, or generated PDFs. **"Dues" MUST NOT appear in any value a
user can see** — including a notification, receipt, PDF, store listing or release note. Internal
identifiers that never render (for example a legacy Kotlin file name) are tolerated until a safe
rename; the lint proves they do not reach a surface.

### 0.2 v0.0.7 release shape (Android-first)

> The full working plan, rationale and sequence are in `docs/PLAN-v0.0.7.md`. This section is the
> contract: what the release must prove, what it may cut, and what it must not attempt.

**The seven proofs.** A v0.0.7 build is releasable only when a reviewer can run all seven:

1. **No "dues" user-facing.** `make copy-lint` scans every surface and fails the build on the word.
2. **Presence is a choice.** A new account shows nobody; four signals have independent audiences,
   per-person overrides, a real preview and one-tap invisibility; withdrawal propagates.
3. **Live communication is real.** Two accounts exchange a message under the p95 budget, offline
   queues honestly, and both sides see the same token state.
4. **Transfer Methods are live and flexible.** Every token transfer shows a method sheet with
   provider, fee, payer, timing, limits, quote TTL, switching and a failure path.
5. **TLC is an instrument, not a bank.** Money moves in the person's own bank; TLC shows
   instruction → proof → attestation → receipt, never a balance.
6. **Username + word/phrase passphrase.** Sign-up, sign-in, logout and new-device recovery are
   seamless; no plaintext passphrase leaves the device.
7. **Pear/Holepunch is maximised.** A new phone restores the encrypted record directly from the old
   phone and verifies it offline; the relay is not required for that path.

**Must ship:** the seven proofs; the offline/reconnect/presence/method/peer-off tests in Part E; a
signed, reproducible APK with SBOM and checksums; updated privacy notice and store copy in the same
release.

**May ship (cut before a must is at risk):** 1:1 voice call beta behind a flag; hosted card checkout
for Pioneer's Tokens only (licensed acquirer, off by default); community mirror via a Pear/Bare node;
PWA companion (read/verify/export); multi-currency quotes beyond NGN; on-device message suggestions;
receipt redaction/OCR.

**MUST NOT ship in v0.0.7:** group messaging or group video; PWA feature parity; a remote Steward
Console; social recovery or SMS-only recovery; FX conversion, crypto, stablecoin, securities, yield
or investment products; custody, wallet, balance, escrow, stored value or card-on-file; public
discovery, feed, leaderboards, amount ranking or contact-graph export; server-side scanning of E2EE
content; iOS.

**Cut line.** If a feature threatens any of the seven proofs, it moves to v0.0.8. A release that
tries everything ships nothing well.

### 0.3 Mandate traceability (user promise → contract → proof)

| User promise | Contract | Proof |
|---|---|---|
| "dues" never user-facing | §0.1, SK-19, C-4, T-28 | `make copy-lint`; copy snapshot per money surface |
| Choose who sees last seen / online | SK-14, C-8, T-13…T-17 | `PresencePort` conformance; default-private and withdrawal tests |
| Instant live communication / networking | SK-15, C-9, T-18…T-20 | 1:1 message p95; offline queue; block/report; call state machine |
| Live, flexible Transfer Methods | SK-16, C-10, T-21…T-26 | `TransferMethodPort` conformance; quote expiry; outage fallback |
| Real money, TLC as instrument not bank | SK-02, SK-04, C-4…C-6, T-27 | custody/instrument review; receipt verifies offline; copy lint |
| Username + word/phrase passphrase | SK-13, C-2, T-10…T-12 | `ci-identity`; no-plaintext scan; recovery drill; logout |
| Pear/Holepunch properly maximised | SK-17, C-13, T-29, T-30 | peer-off equivalence; LAN restore; wrong-key abort; go/no-go |

---

## PART A — SKILLS
<!-- split:SKILLS.md -->

> A skill is a bounded capability an implementer (human or agent) can execute without improvising.
> Each declares trigger, inputs, result, and hard guardrails.

### SK-01 `tlc-domain-model`
**Trigger:** touching roles, dyad, tokens, obligations, states, presence, Transfer Methods.
**Do:** model as pure KMP types; encode every state transition as an explicit, total function with
a reason code; forbid `Double` for money (integer minor units, `Long`); forbid nullable booleans;
version every persisted shape; keep presence and method state out of money state.
**Result:** `core/domain` with property-based tests over the full transition matrix.
**Guardrails:** no Android/HTTP imports; no clocks or randomness inside domain (inject ports); no
`TokenNature` member that implies money, custody or a balance.

### SK-02 `tlc-instrument-and-non-custody-review`
**Trigger:** any feature that mentions money, fees, grants, transfers, methods, "balance" or a
Pioneer's Token.
**Do:** run the custody test — *"can TLC end up holding, delaying, redirecting, netting or
guaranteeing value?"* If yes, redesign to instruction + attestation. Run the instrument test —
*"does this read like a bank, wallet, deposit or payment institution?"* If yes, rewrite the copy
and the model. Produce a 5-line custody note and a 3-line instrument note in the PR.
**Result:** PR section "Custody: none — <routing table row>"; "Instrument: instruction + proof +
attestation + receipt; bank moves the money".
**Guardrails:** vocabulary lint (dues, escrow, wallet, payout, yield, invest, refund, balance,
payment method) fails the build; no method adapter may hold funds.

### SK-03 `tlc-transfer-method-flow`
**Trigger:** building any support, recognition, Growth Grant or Pioneer's Token transfer.
**Do:** render the recipient/Pioneer destination from the correct source; show amount, currency,
purpose, one-line instrument notice, and a **Transfer Method** selector; fetch live availability
and quote; show provider, fee, payer, timing, limits and failure path; allow method switching
before commitment; then run SK-04. Never pre-fill a destination from cache older than 24h.
**Result:** flow + snapshot tests for every role/token/method/currency pair; quote-expiry test;
provider-outage fallback test.
**Guardrails:** Pioneer details appear **only** inside the Pioneer's Token flow; never in logs,
analytics, links, screenshots or push; no silent method substitution; no "Payment Method" string.

### SK-04 `tlc-evidence-attestation`
**Trigger:** a user claims they transferred.
**Do:** create `FeeIntent` (idempotency key, method, provider, fee) → signed PUT to R2 → SHA-256 +
MIME/size checks → scan → `Attestation` by Pioneer/Steward (4-eyes for amounts ≥ ₦50k) → chained
`Receipt` VC. Three-way match; mismatch ⇒ exception with a reason code.
**Result:** receipt verifiable offline; chain length +1; method and fee recorded in the receipt.
**Guardrails:** clients MUST NOT mint receipts; "Attested" never rendered as "Confirmed by bank"
unless a rail adapter actually confirmed it; the provider's terms are shown before opt-in.

### SK-05 `tlc-sync-conflict`
**Trigger:** backlog, conflicting edits, schema migration.
**Do:** classify the field (preference = LWW per column; relationship = both-signatures; money =
append-only server sequence; presence = server-filtered latest consent); write a conflict test that
reproduces the case before fixing.
**Result:** migration + regression test; no silent data loss; no lost presence withdrawal.
**Guardrails:** never resolve a money conflict client-side; never let a stale presence value
resurrect after a withdrawal.

### SK-06 `tlc-consent-link`
**Trigger:** invitations, dyad binding, steward delegation, bank-detail change, conversation
request.
**Do:** mint PASETO v4.public token `{sub, act, res, dyad, jti, exp≤15m}`; deliver by link; require
separate authentication to act; single-use; revocable; audit issuance and redemption.
**Result:** link + verifier endpoint + revocation test.
**Guardrails:** no PII/secrets in URL; no authority from possession; expired link shows a helpful
page; a conversation link never reveals presence or read state.

### SK-07 `tlc-compose-ui`
**Trigger:** any Android screen.
**Do:** Material 3 + TLC tokens (colour/type/space/motion); one primary action per screen; skeleton
loading; empty/error/offline/presence-hidden states designed, not defaulted; large-type + TalkBack
pass; 48 dp targets.
**Result:** screen + preview + a11y labels + screenshot test at phone/tablet/foldable widths.
**Guardrails:** no hardcoded colours/strings (resources only); no blocking work on the main thread;
no animation that implies a state the server has not acknowledged.

### SK-08 `tlc-pwa-shell`
**Trigger:** web/PWA work.
**Do:** SvelteKit (stable) with `#lib` aliases; route-level code splitting; service worker with
cache-first assets + network-first data; install prompt only after meaningful engagement; offline
route with queued intents visible.
**Result:** Lighthouse ≥ 95 perf/a11y/best-practices, PWA installable, works offline after first
load.
**Guardrails:** no business logic duplicated from `core/`; bundle budget enforced in CI; no bank
detail or presence in a service-worker cache.

### SK-09 `tlc-crypto`
**Trigger:** keys, signatures, VC issuance/verification, hashing, passphrase verification.
**Do:** use audited primitives (libsodium/BoringSSL via NDK, or platform Keystore); Ed25519 sign;
Argon2id for verifier fallback; OPAQUE/SRP-class PAKE where available; JCS/CBOR-LD
canonicalisation; key rotation + revocation list; test vectors from RFC.
**Result:** vectors in `core/crypto/testvectors`; cross-platform parity test (Kotlin ↔ C++ ↔ TS).
**Guardrails:** never hand-roll crypto; never store private keys or passphrases unencrypted; never
log key material or a passphrase.

### SK-10 `tlc-privacy-dpia`
**Trigger:** new data field, upload, analytics event, presence signal, or third-party service.
**Do:** fill the data ledger row (purpose, lawful basis, retention, access, export, delete path);
minimise; default to on-device and default-private; add the DSR handler; update the privacy notice.
**Result:** DPIA delta + retention job + deletion test.
**Guardrails:** no analytics on money amounts, names, bank details, presence or message content;
no third-party SDK without review; no presence history beyond feature need.

### SK-11 `tlc-release`
**Trigger:** cutting a version.
**Do:** run the full gate (Part E, C-14), bump `version.properties`, generate a changelog from commits,
sign the APK/AAB with the upload key from secrets, publish to the internal track, tag, attach SBOM
+ checksums, deploy the PWA.
**Result:** reproducible artifact + release notes + rollback path.
**Guardrails:** no release from a dirty tree; no unsigned artifact; no secret in build logs; no
release that fails the copy lint.

### SK-12 `tlc-incident`
**Trigger:** suspected leak, fraud, chain failure, harassment, presence leak or outage.
**Do:** contain → preserve evidence (hashes, not copies) → notify affected (NDPA 72h where
required) → fix → post-mortem with INV mapping → add a failing test that would have caught it.
**Result:** incident record in `docs/runbooks`; ledger integrity statement when attestations were
affected.
**Guardrails:** never delete audit logs; never silently rotate away evidence; never expose a
reporter's identity in a moderation action.

### SK-13 `tlc-identity-passphrase`
**Trigger:** sign-up, sign-in, sign-out, device add/revoke, username change, recovery.
**Do:** enforce a unique handle (reserved list, length, confusable-script check); collect a
word/phrase passphrase with an honest strength guide (minimum four words/equivalent entropy); send
only a PAKE/Argon2id verifier; create a device key in Keystore/StrongBox bound to the passphrase;
show a 12–24-word recovery phrase once with print/save; **never** offer biometric, face, passkey or
device-credential unlock — the passphrase is the only door; require the passphrase on a new device,
after a long absence, for high-risk actions or on request; make **sign-out** one tap that ends the
shared session and removes the device key locally while the private plane stays readable;
rate-limit and progressively delay failures; never reveal whether a handle exists beyond the
minimum.
**Result:** onboarding flow + sign-in flow + sign-out flow + recovery drill + device list/revoke +
tests for handle uniqueness, wrong passphrase, rate limit, offline unlock, sign-out and restore.
**Guardrails:** never store or transmit the passphrase; never use security questions; never make
SMS/email the only recovery; never add a biometric or passkey door; never log a recovery phrase;
never make sign-out delete the private record or its export.

### SK-14 `tlc-presence-consent`
**Trigger:** any online state, last seen, typing indicator, read receipt, profile "online" dot, or
discovery surface.
**Do:** default every signal to private; expose independent controls for online, last seen, typing
and read receipts; support audiences **Nobody / Selected people / Circle / Everyone** and per-person
overrides; support temporary invisibility and schedules; show a preview of what a named person will
see; keep last seen coarse by default ("today", "this week") with exact time opt-in; apply
reciprocity only as an optional rule, never as coercion; filter presence at the edge; propagate
withdrawal immediately across devices; cache only the minimum; never reveal presence through
discovery, a request, a decline or a block.
**Result:** `PresencePort` + a settings screen + a per-person control + tests for default-private,
each audience, override, preview accuracy, withdrawal, block, offline and leak.
**Guardrails:** no presence in a public API response, push payload, screenshot, export or log; no
"who viewed you"; no inference for a person who hid it; no condition of access; no presence history
beyond the chosen feature.

### SK-15 `tlc-live-communication`
**Trigger:** messaging, calls, live token state, connection requests, discovery.
**Do:** implement MLS (RFC 9420) or an audited equivalent; send ciphertext through the relay
mailbox; use WebRTC DTLS-SRTP for 1:1 calls with TURN fallback; signal inside the encrypted
conversation; queue when offline with idempotency keys; show honest states; gate chat on mutual
acceptance; show typing and read state only when that signal is shared; add rate limits, block and
report; use opaque push; make the first message arrive within the p95 budget and stop every
animation when the socket drops. Calls are beta and feature-flagged; if the WebRTC/TURN gate misses,
the release ships text, presence and live token state and says "calls next" rather than faking it.
**Result:** 1:1 text + live shared-state tests, including offline queue, duplicate delivery,
revocation, abuse report and peer fallback; call state machine tests when the call flag is on.
**Guardrails:** never hand-roll a ratchet; no plaintext or presence in the relay; no silent
server-side scanning; no auto-answer; no recording without explicit consent; no group video in
v0.0.7; no "Live" label on a cached or queued state.

### SK-16 `tlc-transfer-methods`
**Trigger:** adding, removing or changing a way money can move.
**Do:** define a `TransferMethod` descriptor (id, kind, provider, currencies, limits, fee model,
payer, timing, availability, confirmation mode, receipt type, terms URL, fallback); render the
method sheet **inside every token transfer**, never in Settings; fetch live availability and a
short-TTL quote; disable unavailable methods with a reason and an alternative; allow switching
before commitment; preserve the draft on failure; record the method in the intent, instruction and
receipt; keep manual bank transfer the default and peer handoff a convenience channel, never a
settlement rail.
**Result:** `TransferMethodPort` conformance suite (fake/local/production) + method/currency matrix
tests + quote-expiry, provider-outage and method-switch tests.
**Guardrails:** no adapter may hold funds; no silent substitution; no hidden fee; no stale quote;
no "Payment Method" copy; hosted card only for Pioneer's Tokens and only with a licensed acquirer;
no method is shown that is not genuinely available for this person, currency and corridor.

### SK-17 `tlc-pear-holepunch-peer`
**Trigger:** peer backup, device-to-device restore, nearby handoff, community node, Hypercore.
**Do:** implement `PeerPort` with a no-op fake and a production adapter; use Hypercore for
encrypted append-only private segments; Hyperswarm/HyperDHT for discovery; Hyperdrive/Hyperblobs
for evidence blobs; Autobase for a person's own multi-device private record; keep keys on-device;
time-box a Bare/Pear Android spike against startup, battery, APK size, crash-free rate and store
policy; prefer a native Kotlin adapter, then embedded Bare, then a companion desktop node; ship the
maximised uses in this order — **device backup/restore (must), nearby handoff (must, beta),
community mirror (should), evidence availability (should)** — and record the go/no-go in `docs/adr/`.
**Result:** peer-off equivalence test; LAN restore that verifies every block and the hash chain
offline; wrong-key rejection; corrupt-block abort; battery/APK budget; go/no-go decision recorded.
**Guardrails:** the peer layer never orders shared facts, authors attestations, mints receipts or
settles money; no plaintext, bank detail, contact graph or hidden presence over a peer; no critical
path depends on a DHT, relay or runtime; disabling the peer layer changes no authority and breaks no
shared or money behaviour.

### SK-18 `tlc-truthful-realtime`
**Trigger:** any "live" label, badge, counter, presence dot, delivery state or real-time event.
**Do:** bind every label to a state the server acknowledged or to a clearly named local state;
expose `Cached / Queued / Sent / Delivered / Acknowledged / Attested / Receipt issued / Verified`;
show age in words; stop animation when the socket drops; alert through an accessibility live region.
**Result:** label tests + no-fake-live test + no-fake-presence test + screen-reader live-region test.
**Guardrails:** no "Live" from a cache; no "Paid" before attestation; no "Read" unless read receipts
are shared; no animation implying a state the server has not acknowledged.

### SK-19 `tlc-copy-and-vocabulary-lint`
**Trigger:** every copy change, string resource, notification, store listing, PDF or release note.
**Do:** run the banned-term list and the required-term list; scan **rendered values**, not resource
keys; check every money surface against the §5.3 notices; check presence copy against §2.15 and
§3.13; check method copy against §2.17; require a reviewer from product and, for money copy, legal.
**Result:** `make copy-lint` green; a copy snapshot test for each money/presence/method surface.
**Guardrails:** build fails on **dues**, **Payment Method**, **balance**, **wallet**, **top up**,
**withdraw funds**, **deposit funds**, **Paid** (as a transfer state), **Confirmed by bank** (without
an adapter), or **Live** on local-only data. The only exceptions are a reviewed explicit negation
("no balance", "not a wallet", "TLC holds nothing"), the ordinary verb "withdraw a pledge", and
internal identifiers that never render; the lint's allow-list is itself reviewed. **The word "dues"
MUST NOT appear in any user-facing value, notification, receipt, generated PDF, store listing or
release note**, even if an internal identifier still carries it.

### SK-20 `tlc-network-offline-audit`
**Trigger:** any new network call, permission, sync path or third-party adapter.
**Do:** inventory the host, purpose and payload; prove it fails closed offline; prove no secret,
bank detail, proof image or hidden presence is sent or cached; test airplane mode, captive portal,
flaky 3G, clock skew, duplicate replay and revoked-grant fail-closed; record the result in the
data map.
**Result:** network-call audit + offline matrix + permission rationale in the privacy notice.
**Guardrails:** no undeclared host; no analytics on sensitive fields; no `INTERNET` permission
without a purpose assertion and a test.

---

## PART B — TOOLS
<!-- split:TOOLS.md -->

### B-1 Toolchain (pin in the version catalog; latest **stable** only)

| Concern | Tool | Notes |
|---|---|---|
| Core logic | Kotlin Multiplatform (stable) | domain, contracts, crypto bindings |
| Android | AGP stable · JDK 21 · Compose BOM · Material 3 | minSdk 26, target latest stable |
| Local DB | SQLDelight (Android) · SQLite WASM/OPFS (web) | same schema, one migration source |
| Sync | `SyncPort` adapter → Postgres server truth; PowerSync or equivalent | durable outbox/inbox |
| Realtime | `RealtimePort`: WSS/SSE; WebRTC for calls; TURN fallback | push is a wake hint only |
| Identity | OPAQUE/SRP-class PAKE; Argon2id fallback; **no biometrics, no passkeys** | username + word/phrase passphrase |
| Messaging | OpenMLS (RFC 9420) or audited equivalent | never hand-rolled |
| Peer | `PeerPort`: Hypercore/Hyperswarm/Hyperdrive/Autobase; Bare/Pear spike | off by default; no authority |
| PWA | SvelteKit stable · Vite stable · Svelte 5 · Tailwind | `#lib` subpath imports |
| Native | NDK + CMake, libsodium/BoringSSL, OpenMLS/WebRTC | crypto/media only; no business logic |
| Edge | Cloudflare Workers · R2 · Queues · Durable Objects · Turnstile · WAF | TS, strict mode |
| Data (server) | Postgres (managed) + row-level security | append-only tables for attestations |
| Contracts | JSON-LD 1.1 · W3C VC 2.0 · OpenAPI 3.1 · JSON Schema 2020-12 | versioned, published |
| Testing | JUnit5/Kotest · Turbine · Playwright · Compose UI test · Pact-style port contracts | property-based domain; presence/method matrices |
| Quality | ktlint/detekt · ESLint/Prettier · svelte-check · gitleaks · Renovate · Syft (SBOM) | fail-closed in CI |
| CI/CD | GitHub Actions | mobile-first (B-3) |

Rejected on purpose: app-level Java (drift), unmanaged pre-release majors (churn), any custodial
payment SDK, a second messaging stack beside MLS/WebRTC, a hand-rolled ratchet, a generic admin
framework, any peer runtime on the critical path.

### B-2 Repository layout

```
TLC App/
├─ apps/{android,pwa,console,cli}
├─ core/{domain,contracts,crypto,policy,ledger,presence,transfermethod,communication,peer}
├─ adapters/{storage,sync,rail,transfermethod,evidence,notify,identity,presence,comms,peer,flags,realtime}
├─ contracts/{jsonld,schemas,openapi,vc}
├─ infra/{cloudflare,turn,actions,iac}
├─ design/{tokens,icons,copy}
├─ docs/{CONTEXT.md,SPECS.md,adr,threat-model,runbooks}
└─ .github/workflows/
```

### B-3 GitHub Actions (mobile-first, mandatory)

> **Honest status:** today the repository ships `ci-core` through `make verify`, `ledger-verify`,
> `security`, `copy-lint` and `check-foss`. The identity, presence, comms, transfer-method and peer
> workflows below are **required targets of the v0.0.7 work**, not existing jobs. Do not cite them
> as present until they run green in CI. Every new workflow MUST be reproducible locally via
> `make <target>`.

| Workflow | Trigger | Gate |
|---|---|---|
| `ci-core.yml` | PR | ktlint/detekt, domain + crypto vectors, port conformance, presence/method transition tests, coverage ≥ 80% on `core/ledger` |
| `ci-android.yml` | PR | assemble, unit, Compose screenshot, emulator instrumented (API 26 + latest), lint, a11y scan, presence/method UI snapshots |
| `ci-pwa.yml` | PR | svelte-check, build, bundle budget, Playwright e2e, Lighthouse budget, offline test, no sensitive cache test |
| `ci-identity.yml` | PR (paths) | handle uniqueness, PAKE/Argon2id vectors, no-plaintext-passphrase scan, recovery drill, rate-limit test |
| `ci-presence.yml` | PR (paths) | default-private, audience matrix, per-person override, withdrawal propagation, no-leak test |
| `ci-comms.yml` | PR (paths) | MLS vectors, envelope ciphertext check, offline queue, duplicate delivery, revocation, call state machine |
| `ci-transfer-methods.yml` | PR (paths) | method/currency matrix, live quote, expiry, outage fallback, fee disclosure, no silent substitution |
| `ci-peer.yml` | PR (paths) | peer-off equivalence, LAN restore, wrong-key rejection, battery/APK budget |
| `ci-contracts.yml` | PR (paths) | JSON-LD valid, schemas backward-compatible, VC round-trip, OpenAPI compatibility |
| `security.yml` | PR + nightly | gitleaks, dependency review, SBOM, SAST, secret-scope check, permission audit |
| `ledger-verify.yml` | nightly | re-verify hash chain + last Merkle anchor; fail loudly |
| `copy-lint.yml` | PR | banned/required terms, money notices, presence copy, method copy |
| `release-android.yml` | tag `v*` | signed AAB, internal track, checksums, SBOM, permission purpose assertion |
| `deploy-edge.yml` | main (paths `infra/`) | Workers/R2/TURN deploy behind env approval |
| `docs.yml` | main | link check, docs build, doctrine/spec consistency |

Rules: least-privilege `permissions:` per job; OIDC where available; no long-lived cloud keys in
workflow files; concurrency groups; caching keyed on lockfiles; every job reproducible locally via
`make <target>`. Mobile is the default home for automation; non-mobile automation needs a reason.

### B-4 Secrets & configuration

| Name | Where | Never |
|---|---|---|
| `PIONEER_ACCOUNT_*` | CF Worker secret / Android runtime config | repo, APK, screenshots, logs |
| `CLOUDFLARE_API_TOKEN` | GH Actions secret (scoped: Workers, Pages, R2, TURN) | user API key, chat |
| `GITHUB_TOKEN` | auto (Actions) | PAT in code |
| `ANDROID_KEYSTORE_*` | GH Actions secret + offline backup | repo |
| `POSTGRES_URL`, `R2_*` | CF secret | client bundles |
| `TURN_*` | CF secret | client bundles, logs |
| `TLC_ENV` | build config (`dev/staging/prod`) | hardcoded prod URLs |

Pre-flight (MUST, before any deploy):
`if [ -z "$CLOUDFLARE_API_TOKEN" ]; then echo "✗ Token missing"; exit 1; else echo "✓ Token set"; fi`

### B-5 Ports (interface contract)

`IdentityPort · StoragePort · SyncPort · EvidencePort · RailPort · TransferMethodPort ·
PresencePort · CommunicationPort · PeerPort · NotifyPort · LinkPort · ClockPort · PolicyPort ·
FlagPort · RealtimePort`.

- `IdentityPort.register/signIn/verify/deviceKey/recover` exposes a handle, a PAKE challenge and a
  verifier; it never receives a plaintext passphrase or recovery phrase.
- `StoragePort.tx/query/migrate` — local truth, encrypted where sensitive.
- `SyncPort.status/push/pull/subscribe` — connectivity, cursors, queue depth, conflict metadata.
- `EvidencePort.putSigned/hash/scan` — proof bytes, hash-addressed.
- `RailPort.instructions/confirmIntent/subscribe(optional)` — read-only outside the licensed
  provider; no debit authority.
- `TransferMethodPort.list/quote/prepare/confirm/subscribe` — only genuinely available methods;
  quote TTL; never holds funds.
- `PresencePort.get/set/audience/subscribe` — default-private, per-signal, permission-filtered.
- `CommunicationPort.send/receive/call/request` — ciphertext and signalling; no plaintext.
- `PeerPort.status/join/replicate/fetch` — optional Pear/Holepunch; no authority.
- `NotifyPort.send(template, channel)` — opaque where sensitive.
- `LinkPort.mint/verify/revoke` — signed, scoped, single-use.
- `ClockPort.now` — server offset + monotonic time.
- `PolicyPort.evaluate` — scoped authority.
- `FlagPort.get` — fail-closed flags.
- `RealtimePort` — events and call signalling; an optimisation, never the source of truth.

Each port ships: fake, local adapter, production adapter, and a conformance suite all three pass.

### B-6 External integrations (all optional, all replaceable)

GitHub (repo/CI/issues) · Cloudflare (edge/storage/TURN) · Postgres host · transactional email/SMS
· bank-statement import (CSV) · licensed Transfer Method provider (opt-in) · hosted card acquirer
(Pioneer's Token only, opt-in) · push (UnifiedPush/ntfy in FOSS; FCM optional in `full`) · error
telemetry (PII-scrubbed). The GitHub MCP server is a convenience for issue triage only — CI and
delivery use `git` + `gh`/REST; no workflow may depend on a chat-side MCP connection.

### B-7 Performance & UX budgets

Cold start ≤ 2.0 s (mid-range Android) · TTI PWA ≤ 1.8 s on 4G · APK ≤ 30 MB with optional
media/WebRTC/peer modules (the current local-only APK is far smaller) · JS ≤ 180 KB gzip initial ·
60 fps scrolling · offline-capable read ≤ 300 ms · presence update ≤ 2 s p95 · message delivery
≤ 3 s p95 on 4G · shared state ≤ 5 s p95 · reconnect catch-up ≤ 3 s p95 for a 7-day delta · zero
layout shift on money surfaces · every async action shows a determinate state.

### B-8 Definition of Done (acceptance criteria → test)

Amazing/Reliable → crash-free ≥ 99.5% in alpha, flake-free CI.
Universal → one core, 3 shells, RTL + large-type pass.
Robust/Safe → INV-01…INV-16 suite green, threat model updated, pen-test checklist for money flows.
Structured/Cohesive → port conformance, schema compatibility, no orphan code.
Mature/Self-sufficient → runbooks, offline docs, single-command bootstrap.
Reproducible → pinned toolchain, locked deps, signed artifacts, checksums.
Elegant/Polished → design tokens only, motion spec, copy reviewed against §0.1 and §5.3.
Adaptive → phone/tablet/foldable/desktop-web layouts verified.
Economically capable → every Pioneer's Token traceable end-to-end (SK-04) with its Transfer Method.
Private by default → presence hidden until chosen; peer layer off until chosen; passphrase never
leaves the device.
Honest in real time → no false live, paid, delivered, read, typing or presence state.

---

## PART C — WORKFLOWS
<!-- split:WORKFLOWS.md -->

> **Honest status:** C-1 describes the target developer flow. `make bootstrap` is **not in the
> Makefile yet**; it is required before the v0.0.7 gate (C-14). Today the working entry point is
> `make doctor` → `make verify` → `make copy-lint`. C-2…C-13 describe required v0.0.7 behaviour and
> are contracts, not shipped features.

### C-1 First-run bootstrap
1. `pwd && ls` — verify repo root and structure.
2. `make doctor` — JDK 21, SDK/NDK, Node, git, gh, secrets present.
3. `make bootstrap` — **required v0.0.7 target**: install deps, generate local schema, seed dev
   fixtures (no real PII). Until it exists, follow the setup steps in `README.md`.
4. `make verify` — build + unit + chain check on fixtures. Must be green before any change.
   `make copy-lint` runs in the same step and fails on any banned user-facing term.

### C-2 Onboarding: username + word/phrase passphrase
1. **Welcome:** ≤ 4 slides, skippable, no account needed; ends with one promise and one notice.
2. **Choice:** "Create an account" or "Use without an account". The local guest path is first-class.
3. **Identity:** username (unique handle) + display name + role. The handle is checked live; the
   display name can change freely.
4. **Passphrase:** the person chooses a word/phrase; the UI shows entropy in plain language, not a
   shaming meter. The device derives a PAKE/verifier; the passphrase never leaves the device.
5. **Device key:** create a key in Keystore/StrongBox bound to the passphrase. There is no
   biometric, face, passkey or device-credential unlock; the passphrase is the only door.
6. **Recovery phrase:** show 12–24 words once; require a confirmation step; offer print/save; warn
   that anyone with the words controls the account.
7. **Notices:** Token Nature · Financial/Instrument · Connectivity · Presence & Communication ·
   Transfer Method · Privacy. Consent is explicit, never pre-checked.
8. **Coach-marks:** spotlight Receptive Mode, Presence (default private) and the Transfer Method
   selector. Dismissible, replayable in Settings.
9. **Return and sign-out:** after the first sign-in, the passphrase opens the app. Sign-out is one
   tap: the shared session ends, the device key is removed locally, the private plane remains
   readable, and the person can sign in again with the passphrase — never a biometric.
**Exit:** profile exists offline; sync queues; no Pioneer details shown; presence is private.
**Failure:** resumable at every step; nothing lost on kill; a forgotten passphrase on a new device
falls back to the recovery phrase, never to a security question; sign-out never deletes the private
record or its export.

### C-3 Dyad formation
Invite (signed, 15 min) → invitee authenticates with passphrase/device → both confirm →
`ConsentGrant` pair written → shared space created with two-signature policy → both see the same
ledger view. Rejection/expiry leaves no half-dyad. Dissolution requires both signatures or a 7-day
unilateral exit. Presence is **not** shared by dyad formation; each person opts in separately.

### C-4 Pioneer's Token flow — the money path
1. Resolve the obligation/kind (role, period, amount, currency, idempotency key) —
   server-authoritative.
2. Show the exact instrument notice, amount, currency, purpose and **Transfer Method** panel.
3. Fetch live methods and a short-TTL quote: provider, fee, payer, timing, limits, availability.
4. The person selects a method. Switching is allowed before commitment and preserves the draft.
5. The person transfers in their own bank/provider app (deep link out; return via universal link).
6. Upload proof → `FeeIntent` (method, provider, fee) + SHA-256 → R2 signed PUT.
7. Attestation: Pioneer/Steward matches the statement; 4-eyes ≥ ₦50k; exception on mismatch.
8. Chained `Receipt` VC issued with method and fee; user sees "Attested" + downloadable proof.
9. Daily Merkle root anchored; nightly `ledger-verify` re-checks.
**Never:** optimistic "paid", auto-retry of a transfer, storing card data, hiding the notice,
silently switching methods, or using the word "dues".

### C-5 Supportive / Recognition (peer, zero custody)
Recipient publishes verified transfer details (dual-signed; 24h cool-down on change) → sender sees
`bank · last4 · verified`, available Transfer Methods, amount and optional message → sender
transfers directly → sender may attach a self-declared receipt (optional, never required) →
recipient may *Acknowledge* (no amount shown publicly by default) → both sides keep an
offline-verifiable record.
**Never:** TLC in the middle, a fee, a ranking by amount, or a method the recipient did not
publish/accept.

### C-6 Growth Grant (pledge → evidence → direct payment)
Donor pledges (amount, milestone, deadline) → beneficiary accepts → milestone evidence uploaded
(permission-scoped) → steward verifies (reason code + audit) → donor receives a one-tap direct
transfer instruction to the **beneficiary's** verified account, with a live Transfer Method → donor
pays → receipt attested. Unfulfilled pledge expires cleanly (no penalty, no custody, no debt claim
by TLC).

### C-7 Steward delegation & revocation
Request → scope + expiry stated in plain language → delegate authenticates → signed `ConsentGrant`
→ console shows only scoped views → 90-day auto-expiry → revocation propagates offline within one
sync cycle. **Never:** standing superuser, cross-tenant read, bulk export of people/bank data,
visibility into presence settings, or content access without a report.

### C-8 Presence setup and change
1. First run: every signal is **Nobody**. The UI says "You are not visible to anyone."
2. The person chooses per signal: online now · last seen · typing · read receipts.
3. The person chooses an audience: Nobody / Selected people / Circle / Everyone.
4. The person may add per-person widen/narrow overrides and an optional reciprocity rule.
5. A preview names a real person and states exactly what they will see.
6. "Go invisible" takes effect immediately on every device; caches clear; others see "not shared".
7. A blocked person sees nothing; a declined request does not reveal presence.
8. Audit: the person can see their own recent changes; no one else can.
**Failure:** offline edits queue visibly; the old setting remains in force until the server
acknowledges the new one; the UI says which is active.

### C-9 Live communication
1. Discovery/request: purpose-tagged request → accept/decline/block → chat unlocks only on mutual
   acceptance.
2. Text: compose → local ciphertext → outbox → relay mailbox or direct peer path → delivery state →
   read state only if shared.
3. Call: request → accept → WebRTC connect (DTLS-SRTP) → live → end. TURN only when needed. No
   auto-answer, no recording without consent, no group video.
4. Live shared state: a token event arrives with `aggregate_seq`; both sides see the same state and
   freshness label; a gap triggers a pull.
5. Offline: messages queue; calls say unavailable; presence says "not shared" or "last seen" per
   consent; nothing pretends to be live.
6. Revocation/block stops new messages immediately; the local copy remains readable and exportable.
7. Report: the reporter may attach an excerpt; moderation never scans the rest.

### C-10 Transfer Method lifecycle
1. `list`: fetch methods for this person, currency, recipient and corridor; never show a method
   that is not genuinely available.
2. `quote`: fetch fee, payer, timing, limits and expiry (≤ 120 s); show the total.
3. `select`: the person chooses; the last-used method may be suggested, never pre-selected.
4. `prepare`: create/refresh the instruction and reference; check destination freshness (≤ 24h;
   Pioneer instruction ≤ 15 min).
5. `commit`: the person leaves for their bank/provider; TLC never debits.
6. `confirm`: proof + attestation; record the method and fee in the receipt.
7. `fail`: name the stage and provider; keep the draft; offer another live method; never say "sent".
8. `expire`: a stale quote or destination forces a re-quote; offline cannot issue a new instruction.

### C-11 Offline, queue and reconnect
Reads: always from local storage. Writes: queued with a visible per-item state
(`queued → sent → attested | failed(reason)`). Money-adjacent: an intent may queue; **settlement
never**; a method quote may not. Presence: the old consent remains in force until acknowledged.
Messages: queue in order per conversation; duplicates resolve by idempotency key. Reconnect order:
verify device key → signed config → pull deltas → verify chain/anchor → push outbox in order →
resolve conflicts → update freshness → resume stream. Conflict banner is honest, actionable, and
dismissible only after resolution.

### C-12 Sync recovery / conflict
Detect (backlog, divergence) → classify field class → for money facts: discard the client claim,
re-pull the server sequence → for preferences: LWW with `updated_at` + device tiebreak → for
relationships: require re-confirmation → for presence: the latest server-accepted consent wins; a
withdrawal can never be overwritten by an older "visible" value → migrate schema with forward and
backward tests → verify the chain again.

### C-13 Pear/Holepunch backup, restore and handoff
1. Opt in on both devices; derive a shared peer key from the account key (never upload it).
2. Discover over Hyperswarm/HyperDHT or a direct LAN address; show the peer fingerprint.
3. Replicate encrypted Hypercore segments; verify every block signature and the hash chain.
4. Restore into a fresh local store; run the same offline verification as a normal export.
5. Optionally replicate encrypted evidence blobs (Hyperdrive/Hyperblobs) with hash checks.
6. Optionally hand an encrypted transfer instruction or receipt directly to a nearby person; the
   relay reconciles it when online, and the bank still moves the money.
7. Optionally run a community mirror that replicates encrypted receipts and public anchors for
   offline verification during a relay outage; it holds no authority and no plaintext.
8. Disable the peer layer at any time; nothing breaks and no authority changes.
**Failure:** a wrong key, a corrupt block or a missing segment aborts cleanly; the existing local
record is never overwritten by an unverified peer; the UI says what was and was not restored.
**Gate:** the Android runtime choice (native Kotlin adapter → embedded Bare → companion node) is a
time-boxed go/no-go recorded in `docs/adr/`; if it fails, only the safe subset ships and the rest is
documented, never faked.

### C-14 Release gate (must all pass)

> **Today:** `make verify`, `make ledger-verify`, `make security`, `make copy-lint` and
> `make check-foss` exist and gate v0.0.6. The broader gate below is the **v0.0.7 contract**; the
> missing `make` targets MUST be implemented before a shared-plane release, not assumed.

`make verify` → `make e2e` → `make a11y` → `make ledger-verify` → `make security` →
`make copy-lint` → `make identity` → `make presence` → `make comms` → `make transfer-methods` →
`make peer-off` → version bump → signed build → internal track → smoke on a physical device → tag
→ deploy PWA → publish notes + checksums + SBOM → post-release watch 48h → rollback plan
documented.

### C-15 Incident response
Detect → contain (revoke grants, rotate secret, disable flag via `FlagPort`, force presence
private) → preserve hashes → assess NDPA 72h duty → notify → fix → post-mortem mapping to INV IDs →
regression test → publish a ledger integrity statement if attestations were affected. A presence
leak or a harassment report follows the same path with the affected person's safety first.

### C-16 Data subject request
Export (JSON + human PDF, offline-capable) → delete (crypto-shred where possible, hash-only residue
for receipts required by tax) → confirm with an audit record → never delete another person's
evidence. Presence history, message copies and peer keys are included in the export and the delete
path.

---

## PART D — Traceability map (obligation → evidence)

| Obligation | Artifact | Verifier |
|---|---|---|
| Account exists | username + PAKE verifier + device key | any device; server verifies without the passphrase |
| Device is trusted | device key + short session | device list + revoke test |
| Recovery is possible | recovery phrase + second device drill | restore test |
| Presence is consented | per-signal consent record | `PresencePort` conformance + leak test |
| Message sent | ciphertext envelope + idempotency key | relay sequence + duplicate test |
| Call connected | WebRTC session record (no content) | call state machine test |
| Transfer Method chosen | `TransferMethod` descriptor + quote | intent, instruction, receipt |
| Pioneer's Token owed | `FeeIntent` (signed, method, fee) | any device, offline |
| Pioneer's Token claimed | proof artifact hash | R2 metadata + ledger |
| Pioneer's Token settled | `Attestation` + statement ref | Pioneer/Steward (4-eyes ≥ ₦50k) |
| Pioneer's Token proven | `Receipt` VC, chained | offline chain verify + daily anchor |
| Delegation | `ConsentGrant` VC | scope/expiry check |
| Peer support | recipient-signed details + optional receipt | either party |
| Grant | pledge + evidence + steward decision | donor, beneficiary, auditor |
| Peer backup | Hypercore segments + signatures | offline hash-chain verify |

---

## PART E — Test matrix (offline / online) and release gates

### E-1 Mandatory test matrix

| # | Test | Pass condition |
|---|---|---|
| T-01 | Airplane mode, private plane | Read/write/export/verify all work; no false "sent" |
| T-02 | Airplane mode, shared write | Visible queue; no settlement; no method instruction |
| T-03 | Reconnect after 7 days | Cursor catch-up; no duplicate; no lost draft |
| T-04 | Kill mid-write | Reopen consistent; incomplete line dropped; next step shown |
| T-05 | Device clock ±1 hour | Server time wins; money actions block above 5 min skew |
| T-06 | Duplicate event / retry | One fact, one receipt; idempotency holds |
| T-07 | Out-of-order event | Buffer by `aggregate_seq`; gap triggers pull |
| T-08 | Revoked grant while offline | Fail closed; local read remains; explanation shown |
| T-09 | Stale destination | No new instruction beyond TTL; re-fetch required |
| T-10 | Passphrase wrong / rate limit | Progressive delay; no enumeration; no lockout trap |
| T-11 | New-device recovery | Recovery phrase restores; private record intact |
| T-12 | Passphrase never leaves device | Network, log, crash and cache scans find no plaintext |
| T-13 | Presence default | Nobody sees online/last-seen/typing/read |
| T-14 | Presence audience matrix | Nobody/selected/circle/everyone each behave exactly as chosen |
| T-15 | Presence withdrawal | Other clients show "not shared" within budget; no inference |
| T-16 | Presence leak scan | No presence in API, push, cache, export, screenshot or log |
| T-17 | Block / decline | No messages, no presence, no request pressure |
| T-18 | Live message offline | Queues; no duplicate on reconnect; honest state |
| T-19 | Read receipt not shared | Never rendered as "Read" |
| T-20 | Call declined / failed | Honest state; no auto-redial; no fake missed-call |
| T-21 | Method availability | Only live methods shown; unavailable disabled with reason |
| T-22 | Quote expiry | Re-quote before commitment; stale fee never used |
| T-23 | Method switch | Draft, amount, purpose and note preserved |
| T-24 | Provider outage | No false "sent"; draft preserved; alternative offered |
| T-25 | Method fee disclosure | Provider, fee, payer, timing and limits visible before commit |
| T-26 | Hosted card scope | Pioneer's Token only; no PAN/CVV; peer transfer never card-only |
| T-27 | Instrument copy | No balance/wallet/top-up/withdraw/deposit; notice present |
| T-28 | Banned terms | No "dues", "Payment Method", "Paid" state, false "Live" |
| T-29 | Peer-off equivalence | Every shared/money behaviour unchanged with `PeerPort` disabled |
| T-30 | Peer restore | LAN restore verifies; wrong key/corrupt block aborts cleanly |
| T-31 | A11y | Live regions announce state; 48 dp; contrast; no colour-only meaning |
| T-32 | Network audit | Only declared hosts; no sensitive payload; offline fails closed |
| T-33 | Permission purpose | `INTERNET`/network state/camera/mic each has a purpose and a test |
| T-34 | Ledger | Chain + anchor verify after every money flow |
| T-35 | Release | Signed artifact, checksums, SBOM, tag = version, rollback plan |
| T-36 | Sign-out | Shared session ends; device key removed; private record still readable and exportable |
| T-37 | Handle safety | Reserved/confusable handles rejected; a changed handle leaves a signed 30-day tombstone |
| T-38 | Presence preview | The preview names a real person and matches exactly what that person sees for each signal |
| T-39 | Copy surfaces | `make copy-lint` scans values, notifications, receipts, PDFs, store copy and release notes; "dues" absent |

### E-2 Release gates for v0.0.7

| Gate | Must be true |
|---|---|
| **G-01 Doctrine** | `CONTEXT.md` v0.0.2 and this file agree; no unresolved INV conflict |
| **G-02 Identity** | Username + passphrase + recovery + device revoke + sign-out pass; no plaintext secret |
| **G-03 Presence** | Default-private, per-signal, per-person, preview, withdrawal, leak tests pass |
| **G-04 Communication** | 1:1 text + call + live state; offline queue; block/report; E2EE vectors |
| **G-05 Transfer Methods** | Live availability, quote TTL, fee/timing disclosure, switching, outage fallback |
| **G-06 Instrument** | No custody, no balance, notices in every money flow, copy lint green |
| **G-07 Offline** | T-01…T-09, T-18, T-21…T-24 pass |
| **G-08 Peer** | T-29, T-30 pass; go/no-go recorded; off by default |
| **G-09 Privacy** | DPIA rows for presence, messages, passphrase verifier, methods, peer keys |
| **G-10 A11y** | WCAG 2.2 AA audit; live-region tests; large type |
| **G-11 Security** | Threat model updated; pen-test checklist for money/identity; gitleaks clean |
| **G-12 Release** | Signed, reproducible, checksummed, SBOM, internal track smoke, rollback plan |
| **G-13 Copy** | `make copy-lint` green; "dues" absent from every user-facing value, notification, receipt, PDF, store listing and release note |

### E-3 The one-sentence release standard

A person can join with a username and a phrase they can remember, decide exactly who sees them
online, talk and share a live token state with someone they chose, move real money through a
Transfer Method they can see and understand, and hold a receipt that proves it — while TLC holds
nothing, never pretends to be a bank, and still works when the network disappears.

---

*End of SPECS · doctrine v0.0.2 · target release v0.0.7.*
