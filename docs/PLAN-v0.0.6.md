# TLC v0.0.6 — Remote Foundation

**Status:** proposed · **Baseline:** v0.0.5 (`487772c`), local-only, no `INTERNET`, 127 tests
**Doctrine delta:** `local-first` → **remote-first for relationship, local-first for identity**.
Remote-first does not mean remote-only. The app must remain fully readable, writable and honest
when the network is gone.

**Release headline:** *A person, reached with consent.*
**One-sentence goal:** prove that a real person can join without a phone number, be reached only
with permission, keep a simple record, and see the same live truth as the other side of every
token — with every Pioneer token traceable from intent to receipt, without TLC ever holding money,
plaintext messages, or bank credentials.

---

## 0. The architecture in one page

```
┌───────────────────────────── Android (Compose + KMP core) ─────────────────────────────┐
│  Private plane (device-authoritative)          Shared plane (server-authoritative)      │
│  • Record / timeline                           • account, handle, public profile       │
│  • bank vault (Keystore-encrypted)             • consent + contact policy              │
│  • drafts, private notes, keys                 • 1:1 conversations (ciphertext only)   │
│  • optional encrypted backup                   • live token circulation (two-sided)     │
│  • receipts + visibility controls              • token intents, proofs, receipts        │
│         │                                              │                               │
│         └──────── SQLDelight mirror + outbox ──────────┘                               │
└───────────────────────────────────────┬────────────────────────────────────────────────┘
                                        │ HTTPS / WSS (pinned, signed config)
                              ┌─────────▼──────────┐
                              │     TLC Relay      │  thin, replaceable, OpenAPI 3.1
                              │  auth · directory  │  no funds · no plaintext · no bank data
                              │  consent · mailbox │
                              │  receipts · config │
                              └─────────┬──────────┘
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              │                         │                         │
        Postgres (RLS)            R2 evidence               WebRTC / TURN
        append-only facts         encrypted proofs          P2P chat + 1:1 video
```

**The two-plane rule is the core refinement.** Shared, relational facts need a server so two
people can trust the same answer. Private, personal facts do not. This resolves the false choice
between "local-first" and "remote-first" and keeps the blast radius of a breach small.

---

## 1. Review: what holds, what breaks

**Holds (do not redesign):** non-custody invariants, hash-chained record, direct transfer +
attestation, three roles, scoped stewardship, plain-language notices, fail-closed release gates.

**Breaks (must change in v0.0.6):**

| # | Problem | Consequence |
|---|---|---|
| 1 | "No account, no network" is the current shipped promise | Remote-first is impossible without retiring it explicitly in manifest, onboarding, privacy, store copy and README |
| 2 | Pioneer account number described as "hardcoded" | A bank number in a repo or APK is a permanent leak. It must be signed runtime config, rotatable, never committed |
| 3 | Growth Grant described as "escrow" | Contradicts INV-01 and creates a licensing problem. Keep pledge → evidence → direct payment → receipt |
| 4 | "Primary male / Co-partner female" | Discriminatory and brittle. Keep **Primary Steward / Co-Steward**, symmetric veto, chosen by the couple |
| 5 | "Record" is a tab, a verb, a receipt, a payment, a proof and a promise | The most overloaded surface in the app. Users cannot tell what it is for |
| 6 | Stack list includes C++, Java, Kotlin, KMP, Flutter and SDK/NDK/JDK | A kitchen sink. Flutter would fork the UI and double the work; app-level Java is drift |
| 7 | "Zero-server" and "metadata-free" stated as absolutes | Neither is true for asynchronous remote communication. Say **server-minimal** and **metadata-minimized**, then design for it |
| 8 | Roles described as "strictly 3" while stewards and managers exist | Confuses role (identity) with authority (delegated grant). Separate them |

---

## 2. Decisions (the selective refinements)

1. **Two planes, one app.** Shared plane is remote-first and server-authoritative. Private plane
   is device-authoritative and works offline forever. No shared action is shown as complete until
   the server acknowledges it.
2. **Identity without phone.** Signup = handle + display name + role + device key + recovery kit.
   Phone is optional, verified only if supplied, used only for recovery/notices, never for
   discovery. No email required either.
3. **Passkeys where available, Keystore everywhere.** Credential Manager passkeys on devices that
   provide them; a device-bound Keystore key plus a high-entropy recovery kit on every device,
   including the FOSS build with no Play Services. Never make passkeys the only door.
4. **Local guest mode.** "Use without an account" is a first-class path. It can write the private
   record and vault. Social and Pioneer actions prompt an upgrade; the guest record merges.
5. **Thin trust server.** It knows accounts, handles, public profiles, consent edges, opaque
   envelopes and receipts. It never knows message plaintext, the private record, bank credentials
   or the bank vault. It never holds funds.
6. **Record simplification.** One timeline. One manual entry type. System events append
   themselves. Proof and export move to **You → Proof & backup**. "Record" stays a noun; the verbs
   become *Add*, *Save*, *Confirm*.
7. **Pioneer details are configuration, not code.** Signed, pinned, 24-hour cache maximum, with a
   signed rotation history. No account number in the repo, APK, logs, links or screenshots.
8. **Stewardship is authority, not gender or rank.** Primary Steward and Co-Steward; symmetric
   veto on irreversible acts; every grant scoped, expiring, revocable, audited.
9. **Growth Grants never enter TLC.** Pledge → evidence → steward witness → donor pays beneficiary
   directly → receipt. No escrow, no float, no pooled balance.
10. **Holepunch is selective, not foundational.** Hypercore for append-only record replication and
    peer backup; Hyperswarm for a companion/desktop node and optional discovery. A time-boxed
    Android runtime spike decides whether it reaches mobile. No critical path depends on it.
11. **Realtime is WebRTC-first, relay-fallback.** 1:1 chat data and video use DTLS-SRTP P2P.
    Signaling travels inside the encrypted chat. TURN is used only when NAT demands it and sees
    only encrypted media.
12. **No Flutter.** Kotlin Multiplatform core + Jetpack Compose (Compose Multiplatform later for
    desktop/web). C++/NDK only for libsodium/MLS/WebRTC; Java only where Android forces it.
13. **AI is on-device and optional.** A small downloaded model (Gemma 3 270M or Qwen3 0.6B class)
    suggests respectful wording and flags abuse signals locally. No server scanning of E2EE.
14. **"Metadata-free" becomes "metadata-minimized".** Sealed envelopes, rotating inbox IDs, no
    sender field in the relay, no read receipts by default, no contact-graph export. The relay and
    TURN still see IP and timing; say so in the privacy notice.
15. **Token circulation is a shared, live surface.** The sender and the receiver see the same
    transfer, the same state and the same receipt — in real time. It is a record of movement, not
    a wallet: no balance, no pooled total, no leaderboard.
16. **Receipts are user-owned evidence.** A receipt image can be uploaded once and shared with
    the other party, a circle, a signed link or the public Record. Visibility is explicit,
    revocable and off by default outside the transfer. Redaction is offered before sharing;
    originals stay encrypted.

---

## 3. Shared plane: account, profile, consent

### 3.1 Signup (no phone by default)

```
Welcome
  ├─ Create account
  │    1. Handle (@ada) + display name
  │    2. Role: Single · Intentional Partner · Love Model
  │    3. Device key created in Keystore; passkey if the device supports it
  │    4. Recovery kit: high-entropy code + QR, printable/saveable
  │    5. Notices: Token Nature · Financial · Privacy (plain language)
  │    6. Optional: phone, email, photo, locality, boundaries
  └─ Use without an account
        → private record only; upgrade later merges without loss
```

- Server-side handle uniqueness, reserved handles, and an abuse-resistant invite/rate-limit path.
- No phone = no exclusion. Anti-abuse uses invite codes, per-device rate limits, proof-of-work on
  web, and human review of reports — not identity gatekeeping.
- Recovery: recovery kit + second device. Social recovery is deliberately out of scope for v0.0.6.
- Structured profile: versioned JSON document validated by JSON Schema, signed by the account key.
  Public projection (name, handle, role, bio, photo, verification badges) is separated from private
  fields (boundaries, contact policy internals, recovery data).
- "Intentional Partner" appears verbatim in role labels, copy and the dyad invitation. The dyad is
  two real accounts with signed invitations and symmetric veto — not local personas.

### 3.2 Consent and contact

- Discovery is opt-in: open to connection, limited, follow only, or private.
- A request carries a purpose tag — **Friendship, Courtship, Collaboration, Guidance** — and a
  short note. Limits: requests per day, cooldown after declines, instant block/report.
- Chat unlocks only on mutual acceptance. Declining, ignoring or revoking is never punished.
- Love Models receive structured **mentorship requests** (reason, expected guidance, category)
  with accept / decline / redirect to resource / suggest community discussion.
- Revocation is server-enforced: new messages stop immediately; the local copy remains readable
  and exportable. The UI says exactly that.
- Moderation only acts on user reports. Because messages are E2EE, the reporter may attach a
  decrypted excerpt; there is no silent content scanning.

---

## 4. Messaging and realtime

- **1:1 text:** MLS (RFC 9420) via OpenMLS (Rust/JNI) — forward secrecy, post-compromise security,
  a path to groups. If the integration gate fails, fall back to an audited libsignal/vodozemac
  binding. **Never ship a hand-rolled ratchet.**
- **Envelope:** recipient inbox ID, ciphertext, size, timestamp. No sender, no subject, no
  plaintext. Rotating inbox IDs. Server orders by sequence and expires undelivered envelopes.
- **Delivery:** WebSocket when foreground; UnifiedPush (`ntfy`) for FOSS, FCM optional in `full`;
  opaque wake payload only.
- **Video:** 1:1 WebRTC, DTLS-SRTP, signaling inside the MLS conversation, TURN fallback. No
  recording, no server-side mixing, consent-gated like a call request. Group video is v0.0.7.
- **P2P mode:** when both peers are online, chat data and video go direct. When one is offline,
  the relay mailbox holds ciphertext. "Zero-server" is a mode, not a guarantee.
- **Abuse protection:** local classifier + user reports + metadata rate limits; no plaintext
  server-side detection.

---

## 5. Record: reduce, don't delete

The record is the trust anchor. The problem is the surface, not the chain.

**Before:** a Record tab with journal, dues, grants, circle, capture, verify, export, and eight
entry kinds, plus "record" used as a verb in five other flows.

**After:**

| Surface | Content |
|---|---|
| **Record** (tab) | One chronological timeline. System events (support, token, consent, dyad, receipt) appear automatically. One manual entry: note + optional photo + optional person tag. Three filters: All · People · Tokens. |
| **Composer** | One sheet: *Add a note*, *Give support*, *Pioneer's Token*. No type picker, no category tree. |
| **You → Proof & backup** | Verify, export, restore, share a seal. Advanced, out of the main path. |
| **Everywhere else** | Verbs become Add / Save / Confirm / Send. "Record a proof" becomes "Add proof". "Record a payment" becomes "I have sent it". |

- The hash chain, Merkle anchor and tamper tests remain unchanged underneath.
- The private record does not sync by default. Multi-device continuity is an optional encrypted
  backup; Hypercore replication is the later peer-to-peer path.
- Migration from v0.0.5 imports and re-verifies the existing record before anything else.

---

## 6. Pioneer traceability (the hard requirement)

**What can be guaranteed:** every token intent has a unique reference, a clear instruction, a
stored proof, an attestation and a signed receipt; every bank credit is matched or flagged; the
whole chain is auditable and tamper-evident.

**What cannot be guaranteed honestly:** automatic bank confirmation without a bank/rail API.
"Attested" must never be rendered as "confirmed by bank".

```
Intent (client ULID)
  → Reference (TLC-ACT-7F3K9Q)
  → Instruction (amount, signed Pioneer destination, reference, notice)
  → User pays via their own bank
  → Proof upload (image/PDF; EXIF stripped; encrypted in R2; hash recorded)
  → Attestation (Pioneer; 4-eyes ≥ ₦50k; reason code on mismatch)
  → Signed receipt (W3C VC; verifiable offline and at /verify/<id>)
  → Monthly reconciliation (bank statement import; unmatched queue; signed summary)
  → Public transparency page (pseudonymous receipt IDs, totals per token type, Merkle root)
```

Controls:

- Runtime destination config is signed and pinned; rotation is a signed, audited event.
- The relay's audit log is append-only and hash-chained; every read of a proof is logged.
- Four-eyes on large amounts; scoped, expiring steward access; no bulk export of people or banks.
- Exceptions are first-class: unknown credit, duplicate reference, wrong amount, late proof.
- Weekly Pioneer digest: receipts issued, open exceptions, unmatched credits, median time-to-receipt.
- A payer can verify their own receipt with the reference; the public page never shows payer PII.
- Retention: proofs 90 days then hash-only; receipts 7 years; logs 90 days.

This is observability and accountability to the maximum degree the banking rail allows, without
custody and without pretending the bank is integrated.

---

## 7. Live token circulation and receipts

### 7.1 The shared truth

Every transfer has one `transferId`. The sender and the receiver see the same amount, kind,
counterparty, reference, state and receipt. Each side sees only the actions it is allowed to take.
Neither side can edit history; a correction is a new event.

**Today — a live strip, not a wallet:**

```
Circulation · this month
↑ 2 sent    ↓ 1 received    · 1 awaiting receipt
```

Tap it to open **Tokens**. No balance, no pooled total, no ranking.

**Tokens screen**

- Segmented view: **All · Sent · Received**.
- Status chips: *Queued* (offline), *Instructed*, *Sent* (proof added), *Received*, *Receipt ready*,
  *Needs attention*.
- Card: direction, `@handle` + role badge, token name, amount (tabular numerals), time, reference.
- Detail: a vertical state timeline naming the actor and time; proof and receipt thumbnails; the
  one action that is valid next.
- Live updates arrive over WSS (`transfer.updated`). The client shows "Sending…" until the server
  acknowledges, and "Queued" when offline. It never shows "Sent" before acknowledgement.
- Accessibility: status changes are announced through a live region; the pulse is disabled when
  reduced motion is on.

**States**

```
Community:  DRAFT → INSTRUCTED → PROOF_ADDED → RECEIVED → CLOSED
Pioneer:    DRAFT → INSTRUCTED → PROOF_ADDED → ATTESTED → RECEIPT_ISSUED
                     ↘ DISPUTED / CANCELLED
```

- Sender: **I have sent it** + optional proof.
- Receiver: **I have received it** or **Not received**. "Received" is a person's acknowledgment,
  not a bank confirmation, and the UI says so.
- Pioneer or Steward: **Confirm** (attest) or **Needs attention** (exception).
- Community tokens close on receiver acknowledgment. Pioneer tokens close on a signed receipt.

**Privacy**

- Amount, counterparty and receipt are visible only to the two parties.
- Public or aggregate circulation is opt-in, needs at least 10 transfers in a group, and never
  names a counterparty or shows free text.
- No leaderboards, popularity scores or "top supporter" surfaces.

### 7.2 Receipts and visibility

- Upload from camera, gallery or PDF when adding proof or a personal receipt.
- The client strips EXIF, downscales, computes SHA-256; the server stores an encrypted original and
  a thumbnail. The original is never served directly.
- Visibility: **Only me** (default for a personal receipt) · **This transfer** (default for a token)
  · **My circle** · **Anyone with the link** (expiring, revocable, `noindex`) · **Public in Record**
  (opt-in).
- Shared views use a redacted derivative. A redaction tool marks account numbers before sharing;
  the UI warns that screenshots cannot be recalled.
- TLC reviewers see a proof only inside an attestation case, just in time, and every read is
  audited.
- Delete anytime. Attestation retains the hash and metadata, not the image. No receipt bytes in
  URLs, logs or analytics.
- Keep the words distinct: **Proof** is the image the user uploads; **Receipt** is the signed TLC
  record issued after confirmation.

**Data:** `token_transfer` (parties, kind, amount, reference, state, seq), `transfer_event`
(append-only timeline), `receipt_asset` (encrypted blob, hash, redacted derivative), `receipt_acl`
(subject, permission, expiry, revocation). Amount visibility and receipt visibility are separate
grants.

### 7.3 Colour semantics for circulation

| Token | Light | Dark | Use |
|---|---|---|---|
| **Forest green** | `#1E5B3E` | `#7FD1A8` | Received, verified, growth |
| Forest container | `#DCEFE3` | `#123A2A` | Chips and quiet panels |
| On forest | `#FFFFFF` | `#052015` | Text on the solid accent |
| **Royal purple** | `#4B2E83` | `#C3A6FF` | Pioneer, stewardship, recognition |
| Purple container | `#E9E1FA` | `#2E1F52` | Chips and quiet panels |
| On purple | `#FFFFFF` | `#1B0F33` | Text on the solid accent |

Both families clear WCAG 2.2 AA comfortably (lowest measured pair 7.54:1). They are **ink stamps,
never a surface wash**: chips, badges, icons, status dots and one-line accents only. Royal blue
remains the action colour, azure the link/focus colour, silver the hairline, amber the warning and
red the error. Implemented as `TlcSemanticColors` / `TlcPalette` in the theme so a status can never
be mistaken for a button.

**The test:** both sides can answer *what happened, when, and what is next* without messaging each
other.

---

## 8. Holepunch: the selective verdict

| Use | Fit | Decision |
|---|---|---|
| Hypercore / Autobase for the append-only record | Excellent: signed, sparse, multi-writer, offline | **Adopt** for encrypted record replication and peer backup, behind `RecordSyncPort` |
| Hyperswarm DHT for peer discovery | Good, but needs a runtime | **Spike** on Android via Bare; otherwise run in the companion/desktop node |
| Hyperdrive for evidence blobs | Good | Optional; R2 remains the v0.0.6 evidence store |
| Bare/Pear on Android | Uncertain startup, battery and store-policy cost | **Time-boxed go/no-go**, no critical path |
| Replacing WebRTC for video | Poor fit | **Do not**; WebRTC is the mature choice |

The strategic use is **record replication and peer backup**, not a second messaging stack. One
transport for realtime (WebRTC), one envelope relay for offline delivery, one append-only log for
the record.

---

## 9. Stack and CI

### 9.1 Stack (refined)

| Layer | Choice | Why |
|---|---|---|
| Core | Kotlin Multiplatform (stable) | One domain, sync and crypto contract across Android, desktop, web |
| Android UI | Jetpack Compose + Material 3 | Existing, accessible, no rewrite |
| Local store | SQLDelight (+ SQLCipher/Tink for secrets) | KMP, migrations, testable |
| Network | Ktor client, kotlinx.serialization | KMP, no Retrofit/Room drift |
| Crypto | libsodium (X25519/Ed25519/XChaCha20) + OpenMLS | Audited primitives, standard E2EE |
| Realtime | org.webrtc + coturn | Mature P2P audio/video/data |
| Backend | TypeScript + Hono on Cloudflare Workers; Durable Objects; Postgres (RLS) via Hyperdrive; R2; Queues; Turnstile | Small, global, replaceable behind OpenAPI; matches existing doctrine |
| Auth | SimpleWebAuthn passkeys + device keys + recovery kit | No phone, no password database |
| Push | UnifiedPush/ntfy (foss); FCM optional (full) | No Google dependency in FOSS |
| AI | On-device Gemma 3 270M / Qwen3 0.6B via MediaPipe or llama.cpp; downloaded on demand | Privacy, size control |
| CI | GitHub Actions, mobile-first | Mandatory |

**Rejected:** Flutter (forked UI, double work), app-level Java (drift), hand-rolled crypto,
PowerSync in v0.0.6 (unnecessary before the mutation contract is proven), any custodial payment
SDK, any server-side plaintext scanning.

### 9.2 GitHub Actions (mobile-first)

| Workflow | Trigger | Gate |
|---|---|---|
| `ci-core.yml` | PR | ktlint/detekt, domain + crypto vectors, sync conflict tests, coverage |
| `ci-android.yml` | PR | assemble, unit, Compose screenshot, emulator API 26 + 35, lint, a11y, APK size budget |
| `ci-contracts.yml` | PR (paths) | OpenAPI/JSON Schema compatibility, generated client compiles, MLS vectors |
| `security.yml` | PR + nightly | gitleaks, dependency review, CodeQL, SBOM, secret-scope check |
| `ledger-verify.yml` | nightly | chain + Merkle anchor re-verification, restore drill |
| `release-android.yml` | tag `v*` | tag = version, signed APK, signature pin, no-INTERNET assertion removed deliberately, provenance, immutable release |
| `ci-relay.yml` | path-filtered | contract tests, migration dry-run, auth/consent integration (folded into the mobile pipeline where possible) |

Every job reproducible locally via `make`; least-privilege permissions; no long-lived cloud keys;
concurrency groups; Gradle and AVD caching.

### 9.3 New manifest reality

`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, camera/mic only when a call or photo is
started. No background location, no contacts permission, no SMS read. The old "no INTERNET"
assertion in the release workflow is replaced by a **purpose assertion**: the permission exists,
the privacy policy says why, and a network-call audit test proves no unexpected host is contacted.

---

## 10. Scope: must / should / will not

### Must ship (the four proofs)

1. **Signup without phone**, local guest upgrade, structured profile, device management, recovery.
2. **Consent-gated 1:1 messaging**, E2EE, realtime delivery, revocation, block/report, offline
   outbox. "Intentional Partner" dyad invitations with two real accounts.
3. **Simplified Record**: one timeline, one manual entry, automatic events, proof moved to You.
4. **Token truth, both sides**: live circulation for sender and receiver (queued, instructed, sent,
   received, receipt), receipt upload with explicit visibility and redaction, and a remote Pioneer
   Desk with runtime destination, unique reference, attestation, signed receipt, public verifier
   and reconciliation queue.
5. **Migration and honesty**: v0.0.5 record imports and verifies; README, privacy, store copy and
   onboarding updated; signed release with SBOM and provenance.

### Should ship (cut before the four proofs are at risk)

- 1:1 video call (beta, feature-flagged, P2P + TURN).
- Direct P2P chat mode (WebRTC data channel) when both peers are online.
- Hypercore record backup spike.
- On-device respectful-message suggestions.
- Multi-device session list and remote revoke.
- Opt-in aggregate circulation and a redaction/OCR helper for receipts.

### Will not ship in v0.0.6

- Group video, federation, public feed, discovery algorithm.
- Escrow, wallet, balance, card acquiring, crypto tokens.
- Server-side message scanning, analytics on amounts, contact-graph export.
- Public leaderboards, "top supporter" scores or default-public receipt amounts.
- iOS, Flutter, PWA parity.
- Phone-required signup, email-required signup, social recovery.

**Cut line:** if a feature threatens any of the four proofs, it moves to v0.0.7. A release that
tries everything ships nothing well.

---

## 11. Sequence (three sprints, urgent but ordered)

**Sprint 1 — Contract and door.** Doctrine update; threat model/DPIA delta; OpenAPI + schema;
account/passkey/device-key/recovery; handle registry; local guest upgrade; CI skeleton; signed
config for Pioneer destination.

**Sprint 2 — Consent and conversation.** Sync engine + outbox; consent requests and policy; 1:1
MLS chat; push; token circulation state machine and live UI; receipt upload and visibility; record
simplification; migration; a11y and offline states.

**Sprint 3 — Receipts and release.** Pioneer intents, attestation, signed receipts, public
verifier, reconciliation; receipt redaction; 1:1 video beta; P2P spike verdict; performance,
security and release gates.

---

## 12. Release gates (acceptance criteria, measurable)

| Area | Gate |
|---|---|
| Non-custody | Every `Effect` still `requiresCustody = false`; no balance/transfer/escrow kind; vocabulary lint passes |
| Signup | New account in ≤ 2 minutes with no phone and no email; restore on a second device with the recovery kit |
| Consent | A message cannot be delivered before acceptance; revocation stops new delivery within 5 seconds; block is immediate |
| Privacy | No plaintext message, bank number or private record in relay logs, analytics or crash reports; network audit shows only declared hosts |
| Record | One tap to add a note; all system events appear without user action; export verifies; migration from v0.0.5 lossless |
| Circulation | Sender and receiver see the same transfer state within 5 seconds; offline shows Queued, never Sent; no balance or public amount by default |
| Receipts | Only the selected audience can open a receipt; revoking hides it within 5 seconds; shared views are redacted; no receipt bytes in URLs or logs |
| Pioneer | 100% of token intents have a receipt or an open exception; unmatched credits visible within 24 hours; payer can verify a receipt |
| Colour | Forest and royal pairs meet AA in light and dark; neither is used as a background wash; status is never styled as an action |
| Offline | Airplane mode: read everything cached, write the private record, queue social actions; no data loss, no false "sent" |
| Reliability | Crash-free sessions ≥ 99.5%; p95 API < 300 ms; sync conflict tests cover every field class |
| UI quality | Every clickable node has a test or a documented action; no dead buttons; TalkBack labels; 48 dp targets; AA contrast; large type |
| Performance | Cold start < 2.5 s on a mid-range device; APK within budget (target ≤ 30 MB with WebRTC); no main-thread I/O |
| Release | Tag = version, signed APK, SBOM, provenance, immutable release, F-Droid metadata updated for network permission |
| Language | No user-facing "ledger", "hash", "attestation", "dyad", "NUBAN", "non-custodial"; plain words instead |

---

## 13. Top risks and mitigations

| Risk | Mitigation |
|---|---|
| Scope collapse | Four proofs, hard cut line, feature flags |
| E2EE integration cost | MLS gate in Sprint 2; fallback to audited binding; no hand-rolled ratchet |
| Passkey absence in FOSS | Device key + recovery kit is the baseline; passkeys are an upgrade |
| Recovery failure | Printable kit, second-device link, tested restore drill, support runbook |
| Relay lock-in | OpenAPI contract, Postgres/R2 adapters, Docker fallback, export |
| NDPA cross-border | DPIA, data map, retention, processor terms, Nigeria/EU region option |
| Harassment | Consent-first design, limits, block/report, no discovery by default |
| P2P NAT/battery | Relay fallback, TURN, foreground-only video, spike verdict |
| Bank reconciliation gaps | Exception queue, statement import, weekly digest, no false "confirmed" |
| Receipt PII leak | Default-closed visibility, redacted derivatives, short-lived links, access audit, no URLs/logs |
| Realtime fan-out cost | Durable Object per account, coalesced updates, 5s target, fallback to pull on reconnect |
| Colour confusion | Separate semantic tokens, no status-as-action styling, AA test in both themes |
| F-Droid reproducibility with WebRTC/MLS | Prebuilt reproducible `.so`, pinned checksums, SBOM, build recipe |

---

## 14. The standard this release is judged by

Not adjectives. These are the tests: a person can join without a phone; a stranger cannot message
without permission; a message is unreadable to the relay; a record survives a lost phone; both
sides of a token see the same truth and only the chosen audience sees the receipt; a Pioneer token
can be traced from intent to receipt; the app still works in a tunnel; no one at TLC can move a
naira; and every claim in the README is true in the build.
