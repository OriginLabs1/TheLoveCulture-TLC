# TLC v0.0.7 — Shared Plane, Android-first

**Status:** proposed · **Baseline:** v0.0.6 (`081aa46`), local-only, no `INTERNET`, 131 tests
**Doctrine:** `CONTEXT.md` v0.0.2 · **Contract:** `SPECS.md` (this plan must not contradict it)
**Release headline:** *A person, live and in control.*
**One-sentence goal:** a person can join with a username and a phrase they remember, choose exactly
who sees them online, talk and share a live token state with someone they chose, move real money
through a Transfer Method they can see and understand, carry their record to a new phone without a
server, and hold a receipt that proves it — while TLC holds nothing and never pretends to be a bank.

---

## 0. The recommendation in one page

v0.0.7 is **not a rewrite**. It adds a thin, replaceable shared plane to the existing local-first
shell and keeps the private plane exactly where it is: on the device, readable and writable offline.

The release is judged by **seven proofs**, one for each non-negotiable in `CONTEXT.md` v0.0.2:

| # | Mandate | The proof a reviewer can run |
|---|---|---|
| 1 | **"dues" is never user-facing** | `make copy-lint` scans every user-facing surface; the build fails on the word |
| 2 | **Presence is a choice** | New account shows *nobody*. Each signal has its own audience, preview, override and one-tap invisibility |
| 3 | **Live communication is real** | Two accounts exchange a message in under 3 s p95, offline queues honestly, and both sides see the same token state |
| 4 | **Transfer Methods are live and flexible** | A method sheet with provider, fee, payer, timing, limits, quote TTL and a working failure path appears in every token transfer |
| 5 | **TLC is an instrument, not a bank** | Money moves in the person's own bank; TLC shows instruction → proof → attestation → receipt, never a balance |
| 6 | **Username + word/phrase passphrase** | Sign-up, sign-in, logout and new-device recovery are seamless; no plaintext passphrase ever leaves the device |
| 7 | **Pear/Holepunch is maximised** | A new phone restores the encrypted record directly from the old phone, verifies it offline, and the relay is not required |

**The cut line is deliberate.** v0.0.7 is Android-first. PWA parity, group messaging, a remote
Steward Console, social recovery, FX conversion, crypto and any feature that only works when TLC
holds money are **out**. They are not "later in this release"; they are later releases.

**The one hard dependency:** a thin relay must exist. v0.0.6 has no server. v0.0.7 cannot keep its
promises without one. The relay is small, open, replaceable and never holds funds, plaintext or bank
credentials. If the relay cannot be operated, the shared plane does not ship — the app stays honest
and local rather than pretending.

---

## 1. What the person actually feels

This is the product, in six moments. Every engineering decision below serves one of them.

### Moment 1 — "That was easy." (≤ 60 seconds)

Open the app → **Create an account** → choose `@ada` → choose a phrase of four or more words →
the local verifier is created → a 12-word recovery phrase is shown once → three plain notices → done.
No phone number. No email. No password rules written by a security team. The strength guide says
*"four unrelated words is strong; a line you will remember is stronger"* — it never shames.

### Moment 2 — "It knows me."

Next morning: open the app → type your passphrase → the circle is there. No form, no fingerprint,
no face, no device shortcut. The passphrase is the only door, on this device and every other. It is
asked for again after a lock, after a long absence, for a high-risk action, or when the person
chooses. **Log out** is one tap and means: shared session ended, the local door closed, private
record still readable on this device.

### Moment 3 — "I decide who sees me."

A **Presence** card in Settings, and a first-run coach-mark. Four independent rows — *Online now ·
Last seen · Typing · Read receipts* — each defaulting to **Nobody**. The person picks **Selected
people**, **Circle** or **Everyone**, adds a per-person exception, and sees a real preview:
*"Ada will see: online now; not last seen."* One tap: **Go invisible**. It takes effect on every
device and the other side sees **"Presence not shared"**, never a guessed "offline".

### Moment 4 — "This is alive."

A purpose-tagged connection request → accepted → the conversation unlocks. Messages appear
instantly, with honest states: `Sending · Sent · Delivered · Read` (only if read receipts are on).
Typing appears only if the sender opted in. A dropped socket stops the animation and says
**"Reconnecting"**. Offline, messages queue and say **Queued**; they never pretend to be sent.

### Moment 5 — "The money really moved."

Inside a token flow: amount, currency, purpose, the one-line instrument notice, and a **Transfer
Method** sheet. The sheet is live: *Bank transfer · your bank · fee ₦0 · usually instant · limit
₦1,000,000 · quote valid 2:00*. The person can switch method before committing without losing the
draft. They leave for their bank app, come back with a reference, add proof, and both sides see the
same state: **Instructed → Proof added → Attested → Receipt issued**. The receipt is verifiable
offline. TLC never shows a balance, never holds the money, never says "paid" before attestation.

### Moment 6 — "My record came with me."

New phone → **Bring my record** → the old phone shows a QR → the encrypted Hypercore
segments replicate directly over the LAN or Hyperswarm → every block signature and the hash chain
are verified → the private record opens. No relay. No plaintext. If the wrong key or a corrupt block
appears, it aborts cleanly and says exactly what was and was not restored.

---

## 2. Architecture

```
┌──────────────────────────── Android app (Compose + KMP core) ────────────────────────────┐
│  Private plane (device-authoritative)          Shared plane (server-authoritative)        │
│  • record / timeline / receipts                • account, @handle, public profile          │
│  • bank vault (Keystore AES-256-GCM)           • consent edges + presence policy           │
│  • drafts, keys, peer backup                   • 1:1 conversations (ciphertext only)       │
│  • offline read/write/export                   • live token state + transfer intents       │
│         │                                              │                                   │
│         └──────── SQLDelight + durable outbox ─────────┘                                   │
│                                                                                            │
│  Ports: Identity · Storage · Sync · Presence · Communication · TransferMethod · Peer       │
│         Evidence · Rail · Notify · Link · Clock · Policy · Flag · Realtime                 │
└───────────────────────────────────────┬────────────────────────────────────────────────────┘
                                        │ HTTPS / WSS (pinned, signed config)
                          ┌─────────────▼──────────────┐
                          │        TLC Relay           │  thin · open · replaceable
                          │  auth · handle directory   │  no funds · no plaintext
                          │  consent · mailbox · state │  no bank credentials
                          │  receipts · signed config  │  OpenAPI 3.1
                          └──────┬──────────┬──────────┘
                                 │          │
                    Postgres (RLS)     R2 evidence        TURN (media only)
                    append-only facts  encrypted proofs
                                 │
                    ┌────────────▼────────────┐
                    │  Pear / Holepunch peer  │  optional · off by default
                    │  Hypercore · Hyperswarm │  never the authority
                    │  Hyperdrive · Autobase  │  never on the critical path
                    └─────────────────────────┘
```

### 2.1 The two-plane rule stays

- **Private facts** (record, vault, drafts, peer keys) are device-authoritative and work offline
  forever. They sync only by explicit encrypted backup.
- **Shared facts** (account, handle, consent, conversation membership, token state, receipts) are
  server-authoritative because two people need one agreed answer. The client never wins a money
  conflict.
- **Presence** is a server-filtered projection of a consent record. It is never a public field and
  never part of a profile response.

### 2.2 Android-first choices

| Concern | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3, existing TLC tokens | One shell, already shipping |
| Core | Kotlin Multiplatform `core/*` (domain, contracts, policy, ledger) | Shared with a later PWA/CLI without rewriting rules |
| Local DB | SQLDelight (Android driver) + one schema/migration source | Same schema everywhere; typed queries |
| HTTP/WS | Ktor client (OkHttp engine) + kotlinx.serialization | KMP-ready; one client for REST + WSS |
| Crypto | libsodium via JNI for primitives; OpenMLS (Rust/JNI) for messaging | Audited, no hand-rolled ratchet |
| Calls | `org.webrtc` + coturn, feature-flagged | Standard, replaceable |
| Peer | `PeerPort`: fake → native Kotlin adapter → embedded Bare → companion node | Honest go/no-go, no critical path |
| Relay | Cloudflare Workers + Durable Objects + Postgres (RLS) + R2 + TURN | Cheap, edge, WebSocket-native, replaceable |

**Rejected:** Flutter, app-level Java, a custodial payment SDK, a second messaging stack, a
hand-rolled ratchet, a generic admin framework, any peer runtime on the critical path.

---

## 3. Identity: username + word/phrase passphrase

### 3.1 The model

```
username  @ada            public, unique, changeable, never a secret
display   "Ada"           how people see you, freely changeable
passphrase 4+ words       never stored, never sent, never logged
device key Ed25519        Android Keystore/StrongBox, unlocked only by the passphrase
verifier  OPAQUE/SRP      server stores a verifier, not the phrase
recovery  12–24 words     shown once, printable, never uploaded
```

The passphrase is a **word/phrase**, not a password. The UI calls it a passphrase everywhere. The
server never receives it. Authentication uses **OPAQUE** (preferred) or an audited SRP-class PAKE;
**Argon2id verifier** is the minimum fallback where PAKE is unavailable. The port hides which one is
in use, so the fallback can be replaced without touching the UI.

Biometric sign-in is **never** offered: no fingerprint prompt, no face prompt, no passkey, and no
device-credential shortcut. The passphrase is the only door. This is a product rule, not a setting,
and it is enforced by the copy gate and an architecture test.

### 3.2 Seamless sign-up, sign-in, logout

| Moment | What happens | What the person sees |
|---|---|---|
| Sign-up | handle checked live; passphrase → PAKE registration; device key created; recovery phrase shown once | "You're in. Keep these words safe." |
| First sign-in | passphrase → PAKE → device key unlocked | The circle opens |
| New device | passphrase → PAKE → device key created → recovery phrase confirmed | "This device is now trusted" |
| Long absence / high-risk | passphrase re-entry — never biometrics | "Confirm it's you" |
| Logout | shared session ended; device key removed locally; server revokes the session | "Signed out. Your private record is still here." |
| Forgot passphrase | recovery phrase restores; never a security question | "Use your recovery words" |

### 3.3 Handle rules

- Unique, server-enforced; 3–24 characters; letters, digits, `_`, `-`; no leading/trailing `_`/`-`.
- Reserved list: `tlc`, `admin`, `steward`, `support`, `official`, `pioneer`, and confusable variants.
- Confusable-script and homoglyph check; no impersonation of a verified person.
- A changed handle leaves a signed 30-day forwarding tombstone; people are not silently impersonated.
- A failed sign-in never reveals whether a handle exists beyond the minimum needed to avoid
  enumeration. Rate limits and progressive delays apply per handle and per device.

### 3.4 Tests

`ci-identity` (required): handle uniqueness and reserved list, confusable rejection, PAKE/Argon2id
vectors, no-plaintext-passphrase scan (network, logs, crash, cache), wrong-passphrase rate limit,
new-device recovery drill, logout keeps the private plane, session revocation propagates.

---

## 4. Presence: a consent setting, not a default

### 4.1 The four signals

`online_now · last_seen · typing · read_receipts` — each has its own audience:

**Nobody · Selected people · Circle · Everyone using TLC**

Plus per-person widen/narrow overrides, an optional reciprocity rule, temporary invisibility and a
schedule ("share while I am in the app", "hide after 22:00"). Default for every signal: **Nobody**.

### 4.2 Server projection

The server stores only the consent record and the last accepted heartbeat. For each viewer it
computes a projection:

```
canSee(viewer, signal) =
    signal.audience == EVERYONE
 || (signal.audience == CIRCLE      && viewer in owner.circle)
 || (signal.audience == SELECTED    && viewer in signal.selected)
 || override(viewer, signal) == WIDEN
    && override(viewer, signal) != NARROW
```

Rules that are not negotiable:

- A hidden signal is returned as **"not shared"**, never inferred from activity.
- `last_seen` is coarse by default ("today", "this week"); exact time is opt-in.
- Withdrawal is a new server-accepted consent version; an older "visible" value can never resurrect.
- Blocked/declined people see nothing; a declined request does not reveal presence.
- No presence in a public API response, push payload, screenshot, export or log.
- No "who viewed you", no presence-based ranking, no streak pressure.

### 4.3 UI

A **Presence** screen with four rows, a per-person control, a preview naming a real person, and a
persistent **Go invisible** action. The first-run coach-mark explains it once; it is replayable in
Settings. Offline edits queue visibly; the old setting stays in force until the server acknowledges
the new one, and the UI says which is active.

### 4.4 Tests

`ci-presence` (required): default-private, each audience, per-person override, preview accuracy,
withdrawal propagation ≤ 2 s p95, block/decline, offline queue, leak scan (API/push/cache/export/log).

---

## 5. Live communication: real, not decorative

### 5.1 1:1 text

- **E2EE:** MLS (RFC 9420) via OpenMLS. Hard integration gate in Sprint 2. If it misses, fall back to
  an audited `vodozemac`/libsignal binding. Never a hand-rolled ratchet. If no audited binding is
  viable in the window, the release ships **text with libsodium sealed-box + rotating prekeys**,
  clearly labelled *E2EE alpha*, with group chat blocked — honest, not fake.
- **Envelope:** recipient inbox id, ciphertext, size, timestamp. No sender, no subject, no plaintext.
  Rotating inbox ids. The relay orders by sequence and expires undelivered envelopes.
- **Transport:** WSS to a Durable Object when foreground; opaque push wake (UnifiedPush/ntfy in
  `foss`, FCM optional in `full`); foreground poll fallback; cursor resume.
- **Offline:** durable outbox, idempotency key per message, in-order per conversation, duplicates
  resolve to one message. States: `Queued · Sending · Sent · Delivered · Read (if shared) · Failed`.
- **Consent:** chat unlocks only on mutual acceptance. Purpose-tagged request; accept/decline/block;
  instant block/report; rate limits. No unsolicited mass messaging, no public discovery.

### 5.2 1:1 calls (beta, feature-flagged)

- WebRTC DTLS-SRTP, direct first, TURN only when NAT demands it (TURN sees encrypted media only).
- Signalling travels inside the encrypted conversation; a call request is a request, never
  auto-answered. No recording without explicit consent. No group video.
- **Gate:** if the WebRTC/TURN spike does not pass battery, crash-free and call-setup budgets by
  week 9, calls are disabled in v0.0.7 and the store copy says "calls coming next". Text, presence
  and live token state still satisfy the "live communication" mandate honestly.

### 5.3 Live shared token state

Every token event carries an `aggregate_seq`. Sender and receiver see the same state and the same
freshness label within 5 s p95; a gap triggers a pull. The state ladder is
`Recorded → Instructed → Proof added → Attested → Receipt issued → Verified`. No side ever sees a
higher state than the server acknowledged.

### 5.4 Tests

`ci-comms` (required): MLS/audited vectors, envelope ciphertext check, offline queue, duplicate
delivery, out-of-order/gap, revocation, block/report, call state machine, no plaintext on relay.

---

## 6. Transfer Methods: live, flexible, honest

### 6.1 What a Transfer Method is

The real-world route by which value moves between two accounts. **"Payment Method" is banned.** The
method is chosen **inside the transfer flow**, never buried in Settings.

| Method | v0.0.7 | Who moves the money | Notes |
|---|---|---|---|
| **Bank transfer (manual)** | **Must** | The payer, in their own bank | Default; NGN always; any currency both banks support |
| **Peer handoff (Pear/Holepunch)** | **Must (beta)** | The payer's bank | Encrypted instruction/receipt delivered device-to-device; never settlement |
| **Hosted card checkout** | **Should (Pioneer's Token only, off by default)** | Licensed acquirer → Pioneer's merchant account | No PAN/CVV in TLC; never peer-to-peer |
| **Licensed provider rail** | **Adapter only** | The provider, as the regulated entity | `TransferMethodPort` + provider terms; enabled when a reviewed contract exists |
| **Mobile money / USSD / wallet** | **Adapter only** | The provider | Only where licensed; never a TLC balance |

### 6.2 The flow (C-10 in `SPECS.md`)

```
list      methods genuinely available for this person, currency, recipient, corridor
quote     fee, payer, timing, limits, expiry ≤ 120 s; show the total
select    person chooses; last-used may be suggested, never pre-selected
prepare   instruction + reference; destination freshness ≤ 24 h (Pioneer ≤ 15 min)
commit    person leaves for their bank/provider; TLC never debits
confirm   proof + attestation; method and fee recorded in the receipt
fail      name the stage and provider; keep the draft; offer another live method
expire    stale quote/destination forces a re-quote; offline cannot issue a new instruction
```

### 6.3 What "live" means without a bank API

A method can be genuinely live without TLC touching the money:

- **Availability** comes from signed runtime config + a provider status adapter, with a short TTL.
  A method that is down is disabled with a reason and an alternative, never a dead end.
- **Quote** is a signed descriptor (fee, payer, timing, limits, expiry). For manual bank transfer
  the fee is ₦0 and the timing is the bank's stated window; for a licensed rail it comes from the
  provider.
- **Destination freshness** is checked before every instruction; a cached destination older than
  24 h is re-fetched. Pioneer destinations are signed runtime config, never in the repo/APK/log.
- **Switching** preserves amount, purpose and note. No silent substitution. No hidden fee.
- **Failure** preserves the draft and never says "sent" when the provider did not accept.

### 6.4 Tests

`ci-transfer-methods` (required): method/currency matrix, live quote, quote expiry, outage fallback,
fee disclosure, no silent substitution, hosted-card scope (Pioneer only, no PAN/CVV), peer method
never presented as settlement.

---

## 7. The instrument feel: money moved, TLC held nothing

### 7.1 The sentence every surface makes true

> **TLC is an instrument for directing and proving real transfers between people's own accounts.
> It is not a bank, not a wallet, not a payment institution and not a custodian.**

### 7.2 The money moment

The person should feel the transfer because:

1. They see **their own bank app** open with the recipient and reference already correct.
2. They come back and see **the same reference** on both sides.
3. They add proof; the other side sees **Proof added**.
4. A Pioneer/Steward attests; both sides see **Attested** and a **Receipt issued**.
5. The receipt verifies offline, with the method and fee recorded.

TLC never shows a balance, a wallet, "top up", "withdraw funds", "deposit funds", "we received your
money" or "your payment is safe with us". "Attested" is never rendered as "Confirmed by bank" unless
a rail adapter actually confirmed it.

### 7.3 The copy lint is a build gate

`make copy-lint` scans `strings.xml` values, PWA copy, store metadata, release notes, notifications
and generated PDFs. It fails on **dues**, **Payment Method**, **balance**, **wallet**, **top up**,
**withdraw funds**, **deposit funds**, **Paid** as a transfer state, **Confirmed by bank** without an
adapter, and **Live** on local-only data. The only exceptions are reviewed explicit negations
("no balance", "not a wallet", "TLC holds nothing"), the ordinary verb "withdraw a pledge", and
internal identifiers that never render. The allow-list is itself reviewed.

**"Dues" is never user-facing.** The internal `Dues.kt` / `DuesScreen.kt` identifiers may remain
until a safe rename, but no value, notification, receipt, store string or release note may show the
word. The lint proves it on every PR.

---

## 8. Pear/Holepunch: maximised where it is safe

### 8.1 What we use, and why

| Piece | Use in v0.0.7 | Why it is the right tool |
|---|---|---|
| **Hypercore** | Encrypted append-only private-record segments; receipts; audit trail | Signed, verifiable, resumable replication without a server |
| **Hyperswarm / HyperDHT** | Device discovery for backup/restore and nearby handoff | No public contact graph; rotating keys; works on a LAN or the DHT |
| **Hyperdrive / Hyperblobs** | Encrypted evidence blobs (proof images, receipts) | Content-addressed, hash-checked, peer-fetchable |
| **Autobase** | Multi-writer private record across a person's own devices | Legitimate multi-device writes without a central order |
| **Bare / Pear** | Candidate runtime for the peer stack on Android | Small footprint; must pass the Android spike to ship |

### 8.2 The four maximised uses

1. **Device backup and restore (must).** A new phone receives the encrypted private plane directly
   from the old phone. Every block signature and the hash chain are verified before anything opens.
2. **Nearby handoff (must, beta).** An encrypted transfer instruction or receipt moves directly
   between two people on the same network, then reconciles with the relay when online.
3. **Community resilience (should).** A local node replicates encrypted receipts and public anchors
   so a community can verify records during a relay outage. No plaintext, no authority.
4. **Evidence availability (should).** A proof or receipt can be fetched from a peer when R2 is
   slow, with the same hash and signature check.

### 8.3 Android integration: a real go/no-go

The preferred order is explicit:

1. **Native Kotlin adapter** for the subset that is safe and testable (Noise/XX + TCP/UDP LAN,
   content-addressed blocks, signature checks). Lowest risk, no JS runtime.
2. **Embedded Bare runtime** running a small JS bundle with `hypercore`/`hyperswarm`/`hyperdrive`,
   bridged to Kotlin over a local socket. Only if startup, battery, APK delta and store policy pass.
3. **Companion desktop node** (Pear) for backup/restore when mobile embedding fails. The phone still
   verifies every block; the desktop is a courier, not an authority.

**Spike (2 weeks, Sprint 3) measures:** cold-start delta, battery per hour of replication, APK size
delta, crash-free sessions, restore time for a 50 MB record, LAN and DHT success rate, F-Droid
reproducibility. **Pass:** +≤ 8 MB APK, +≤ 2% battery/hour, restore ≤ 60 s for 50 MB, ≥ 99% block
verification, no critical-path dependency. **Fail:** ship use (1) only; record the verdict in
`docs/adr/` and keep the desktop node as the documented path.

### 8.4 What Pear must never do

- Order shared facts, mint receipts, attest a transfer or resolve a money conflict.
- Store plaintext private data on a peer without explicit, per-device opt-in.
- Become mandatory for sign-up, sign-in, messaging, transfers or export.
- Weaken the relay's append-only record or the "server-authoritative shared plane" rule.

### 8.5 Tests

`ci-peer` (required): peer-off equivalence, LAN restore, wrong-key rejection, corrupt-block abort,
battery/APK budget, no plaintext on a peer, no authority change when disabled.

---

## 9. Data model and contracts (minimum)

### 9.1 Relay tables (Postgres, RLS, append-only where money is involved)

```
accounts(id, handle, handle_norm, display_name, role, created_at, status)
handles(handle_norm, account_id, reserved, tombstone_until, signature)
devices(id, account_id, public_key, name, platform, last_seen, revoked_at)
recovery(account_id, public_key, wrapped_key, created_at)          -- no phrase
presence_consent(account_id, signal, audience, version, updated_at)
presence_overrides(account_id, viewer_id, signal, effect, version)
presence_heartbeat(account_id, last_seen_coarse, last_seen_exact?, version)
relationships(id, a, b, state, purpose, created_at, revoked_at)
conversations(id, kind, created_at, state)
conversation_members(conversation_id, account_id, joined_at, state)
envelopes(id, conversation_id, recipient_inbox, ciphertext, size, seq, expires_at)
token_events(id, aggregate_id, seq, kind, actor, payload_hash, created_at)
transfer_intents(id, token_event_id, payer, payee, amount_minor, currency,
                 method_id, provider, fee_minor, quote_id, reference, state, created_at)
transfer_methods(id, kind, provider, currencies, limits, fee_model, timing,
                 availability, terms_url, version, signature)
transfer_quotes(id, method_id, amount_minor, currency, fee_minor, payer, timing,
                expires_at, signature)
attestations(id, intent_id, attestor, decision, reason_code, created_at)
receipts(id, intent_id, vc_json, hash, prev_hash, issued_at)
```

Money is **integer minor units** (`Long`), ISO 4217 currency, no `Double`. Presence and method state
never live in a money row. Every persisted shape is versioned.

### 9.2 API (OpenAPI 3.1, versioned)

`POST /v1/accounts` · `POST /v1/auth/opaque/{start,finish}` · `POST /v1/auth/refresh` ·
`POST /v1/auth/logout` · `GET/PUT /v1/presence` · `GET /v1/presence/{handle}` (projection) ·
`POST /v1/relationships/{request,accept,decline,block}` · `GET/POST /v1/conversations` ·
`GET/POST /v1/conversations/{id}/messages` · `WSS /v1/stream` · `GET /v1/tokens/{id}` ·
`GET /v1/transfer-methods` · `POST /v1/transfer-methods/{id}/quote` ·
`POST /v1/transfer-intents` · `POST /v1/transfer-intents/{id}/proof` ·
`POST /v1/transfer-intents/{id}/attest` · `GET /v1/receipts/{id}` · `GET /v1/config` (signed).

### 9.3 Ports

`IdentityPort · StoragePort · SyncPort · PresencePort · CommunicationPort · TransferMethodPort ·
PeerPort · EvidencePort · RailPort · NotifyPort · LinkPort · ClockPort · PolicyPort · FlagPort ·
RealtimePort`. Each ships a fake, a local adapter, a production adapter and one conformance suite.

---

## 10. Android UX map

| Screen | Job | Key states |
|---|---|---|
| Welcome / Create | handle, phrase, recovery, notices | resumable, offline-capable until shared step |
| Sign in | device unlock or passphrase | wrong phrase, rate limit, new device |
| Home / Today | what is here, what is live | offline, syncing, conflict |
| Presence | four signals, audience, preview | default private, queued change, invisible |
| People / Requests | discover only by handle or invite; accept/decline/block | empty, pending, blocked |
| Conversations | list with honest last state | empty, queued, failed |
| Chat | 1:1 text, call, live token card | sending, delivered, read-if-shared, offline |
| Call | consent, connect, end | ringing, connecting, failed, ended |
| Transfer sheet | method, quote, fee, timing, limits, switch | loading, unavailable, expired, failed |
| Transfer detail | instruction, reference, proof, attestation, receipt | each state, exception |
| Receipt | verify offline, share, export | verified, tampered, unavailable |
| You → Devices | sessions, revoke, recovery drill | this device, revoked, new device |
| You → Peer backup | opt in, QR/key fingerprint, restore | off, replicating, verified, aborted |
| Settings → Privacy | presence, read/typing, block list, export/delete | default private |

One primary action per screen; skeleton loading; empty/error/offline/presence-hidden designed, not
defaulted; 48 dp targets; TalkBack labels; live regions for state changes; no animation implying a
state the server has not acknowledged.

---

## 11. Scope: must / should / won't

### Must ship (the seven proofs)

1. **Identity:** username + word/phrase passphrase, device key, recovery phrase, seamless sign-in,
   logout, new-device restore, handle uniqueness/impersonation guards.
2. **Presence:** default-private, four signals, four audiences, per-person overrides, preview,
   one-tap invisibility, withdrawal propagation, no leaks.
3. **Live communication:** consent-gated 1:1 text, E2EE, realtime delivery, honest states, offline
   outbox, block/report, and live shared token state on both sides.
4. **Transfer Methods:** method sheet in every token transfer, live availability, quote TTL, fee,
   payer, timing, limits, switching, failure path, method recorded in the receipt.
5. **Instrument:** instruction → proof → attestation → receipt; both sides; no balance/wallet; copy
   lint green; "dues" absent from every user-facing surface.
6. **Pear/Holepunch:** encrypted device backup/restore verified offline; nearby handoff beta;
   peer-off equivalence; go/no-go recorded.
7. **Honesty and release:** offline matrix, a11y AA, signed APK, SBOM, checksums, store copy updated,
   privacy notice updated in the same release.

### Should ship (cut before a must is at risk)

- 1:1 voice call beta (feature-flagged; disabled if the gate misses).
- Hosted card checkout for Pioneer's Tokens only (licensed acquirer; off by default).
- Community mirror via a Pear/Bare node.
- PWA companion (read/verify/export), not parity.
- Multi-currency quotes beyond NGN where both banks support it.
- On-device respectful-message suggestions; receipt redaction/OCR helper.

### Will not ship in v0.0.7

- Group messaging or group video.
- PWA feature parity.
- A remote Steward Console (local attestation remains).
- Social recovery, SMS-only or knowledge-based recovery.
- FX conversion, crypto, stablecoin, securities, yield, investment products.
- Custody, wallet, balance, escrow, stored value, card-on-file.
- Public discovery, feed, leaderboards, amount ranking, contact-graph export.
- Server-side scanning of E2EE content.
- iOS.

**Cut line:** if a feature threatens any of the seven proofs, it moves to v0.0.8. A release that
tries everything ships nothing well.

---

## 12. Sequence: four sprints, 12 weeks

### Sprint 1 — Contract and door (weeks 1–3)

Doctrine/spec alignment; threat model + DPIA delta; OpenAPI + schemas; copy-lint + CI; relay
skeleton (accounts, handles, devices, signed config); identity (OPAQUE gate + Argon2id fallback);
device key bound to the passphrase (never biometrics); recovery phrase; seamless sign-in/logout;
local guest upgrade.

**Exit:** two test accounts created, signed in, logged out, restored on a second device; no
plaintext passphrase in any scan; `make copy-lint` green.

### Sprint 2 — Consent and conversation (weeks 4–6)

Presence consent + projection; relationships/requests/block; realtime WSS + Durable Objects; 1:1 text
with MLS gate; durable outbox/inbox; delivery states; live shared token state; offline matrix;
presence/privacy tests.

**Exit:** two accounts exchange messages under 3 s p95; presence default-private and withdrawal
propagates; offline queue has no duplicates; no plaintext on the relay.

### Sprint 3 — Money and peer (weeks 7–9)

Transfer Method descriptors, availability, quote, switch, failure; transfer intents + references;
proof upload + attestation + receipt; both-sides UI; instrument copy; Pear spike and backup/restore;
nearby handoff; call gate.

**Exit:** a token transfer completes end-to-end with a method, quote, proof, attestation and
offline-verifiable receipt; a new phone restores over the LAN; peer-off equivalence passes.

### Sprint 4 — Calls, hardening, release (weeks 10–12)

1:1 call beta if green; a11y audit; performance/battery; security review; pen-test checklist;
privacy/store/README updates; signed release, SBOM, checksums, internal track smoke; rollback plan.

**Exit:** every gate in §13 green; release notes and store copy tell the truth; a physical-device
smoke test passes.

---

## 13. Release gates

| Gate | Must be true |
|---|---|
| **G-01 Doctrine** | `CONTEXT.md` v0.0.2 and `SPECS.md` agree; no unresolved INV conflict |
| **G-02 Identity** | Username + passphrase + recovery + device revoke pass; no plaintext secret |
| **G-03 Presence** | Default-private, per-signal, per-person, withdrawal, leak tests pass |
| **G-04 Communication** | 1:1 text + live state + offline queue + block/report; E2EE vectors |
| **G-05 Transfer Methods** | Live availability, quote TTL, fee/timing disclosure, switching, outage fallback |
| **G-06 Instrument** | No custody, no balance, notices in every money flow, copy lint green |
| **G-07 Offline** | Airplane mode, 7-day reconnect, duplicate/out-of-order, stale destination, revoked grant |
| **G-08 Peer** | Peer-off equivalence, LAN restore, wrong-key/corrupt-block abort, budgets; verdict recorded |
| **G-09 Privacy** | DPIA rows for presence, messages, verifier, methods, peer keys; network audit |
| **G-10 A11y** | WCAG 2.2 AA; live regions; large type; 48 dp; no colour-only meaning |
| **G-11 Security** | Threat model updated; pen-test checklist for money/identity; gitleaks clean |
| **G-12 Release** | Signed, reproducible, checksummed, SBOM, internal track smoke, rollback plan |
| **G-13 Copy** | `make copy-lint` green; "dues" absent from every user-facing surface |

The full test matrix is `SPECS.md` Part E (T-01…T-38). Every gate maps to a `make` target and a CI
job; a gate that only exists in prose is not a gate.

---

## 14. Top risks and mitigations

| Risk | Mitigation |
|---|---|
| Scope collapse | Seven proofs, hard cut line, feature flags, no PWA parity |
| No relay operator | Thin, open, self-hostable relay; deploy runbook; local-only mode stays honest |
| OPAQUE integration cost | `IdentityPort` hides the mechanism; Argon2id verifier fallback; PAKE upgrade behind the port |
| MLS integration cost | OpenMLS gate; audited `vodozemac`/libsignal fallback; no hand-rolled ratchet; honest "E2EE alpha" if needed |
| WebRTC/TURN cost | Feature flag; call gate at week 9; text/presence still real |
| Pear on Android | Native Kotlin adapter first; Bare spike time-boxed; companion node fallback; never critical path |
| Passphrase loss | Recovery phrase drill, second device, printable kit, runbook; no security questions |
| Presence leak | Server-side projection, default-private, leak scan, withdrawal versioning |
| Harassment/abuse | Consent-first requests, block/report, rate limits, no discovery by default |
| Relay breach | No plaintext, no bank data, minimal metadata, RLS, rotating inbox ids, short retention |
| Bank reconciliation gaps | Exception queue, statement import, weekly digest, no false "confirmed" |
| Store/reproducibility with native libs | Pinned prebuilt `.so`, checksums, SBOM, build recipe, F-Droid job |
| Legal/regulatory | Instrument-only position, no custody, licensed provider for card, jurisdiction review |

---

## 15. The standard this release is judged by

Not adjectives. These are the tests:

- A person joins with a username and a phrase they remember, in under a minute, without a phone.
- A stranger cannot see whether they are online, cannot message them without acceptance, and cannot
  infer a hidden presence.
- Two people talk in real time; the relay cannot read a word; offline messages queue and never
  pretend to be sent.
- Both sides of a token see the same truth, with the same honest label.
- A Transfer Method shows its provider, fee, payer, timing, limits and failure path before the person
  commits; switching never loses the draft.
- Money moves in the person's own bank; TLC holds nothing and never shows a balance.
- A receipt proves the transfer offline, and a new phone can restore the encrypted record from the
  old phone without a server.
- The word **dues** appears nowhere a user can see it.
- Every claim in the README, the store copy and the privacy notice is true in the build.

---

*End of plan · doctrine v0.0.2 · target release v0.0.7 · Android-first.*
