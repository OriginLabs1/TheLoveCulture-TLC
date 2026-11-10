# TLC v0.0.8 — the shared plane

**Status:** shipped · **Baseline:** v0.0.7 (`faf8552`), local-only, no `INTERNET`
**Doctrine:** `CONTEXT.md` v0.0.2 · **Contract:** `SPECS.md`
**Release headline:** *Two phones, one honest record.*
**One-sentence goal:** two people who chose each other can exchange end-to-end encrypted messages and
see only the presence each has consented to, through a thin relay that can be replaced, while the
private record still works offline and nothing about money becomes custodial.

---

## 0. The recommendation in one page

v0.0.8 does not rewrite the record. It adds a **shared plane beside it** and leaves the private
plane exactly where it was: on the device, readable and writable offline.

The release is judged by these proofs, each one runnable:

| # | Mandate | Proof |
|---|---|---|
| 1 | **Presence is a choice** | Every signal starts at Nobody; the relay projects consent and returns `null` for a hidden signal; a test asserts hidden is not `false` |
| 2 | **Messages are sealed on the devices** | P-256 ECDH + HKDF-SHA256 + AES-256-GCM in `core:shared`, 11 tests; relay stores no plaintext field |
| 3 | **Live means live** | `ready` is the only path to the Live label; dropped sockets show Offline/Reconnecting, never a false Live |
| 4 | **The relay is thin and replaceable** | `relay/src/worker.js` is one Worker + one SQLite Durable Object; the URL is build configuration; `DELETE /v1/account` removes every row |
| 5 | **Permissions match the feature** | The APK requests `INTERNET` and `ACCESS_NETWORK_STATE`; cleartext is disabled; the workflow now requires `INTERNET` and rejects cleartext |
| 6 | **Pear/Holepunch is real, not a badge** | A companion Hyperswarm/Hypercore node ships in `peer/`, tested on two local peers over the public DHT, with an Android handoff adapter |
| 7 | **TLC is still not a bank** | No custody, no transfer, no balance; the relay carries envelopes and consent, never funds or bank data |

**Cut line.** No group messaging, no calls, no multi-device key sharing, no server-side account
recovery, no embedded Pear runtime in the APK, no push notifications. These are later releases, not
half-built claims in this one.

---

## 1. What the person feels

### Moment 1 — "It is still mine."

Open the app offline. Today, Record, People, the vault, receipts and Verify work exactly as before.
The Live tab exists but says Offline. Nothing is uploaded and no request is made until the person
taps **Go live**.

### Moment 2 — "I chose who sees me."

Four signals — Online now, Last seen, Typing, Read receipts — each with Nobody / Selected people /
Circle / Everyone. Invisible wins over everything. Hidden people see nothing. The other side sees
**Not shared**, not "offline". The relay is not trusted to invent a default: it receives the consent
record and applies it before a projection leaves the server.

### Moment 3 — "This is alive."

A request by exact username → the other person accepts → the conversation unlocks. A message leaves
as **Queued**, becomes **Sent**, then **Delivered**, then **Read** only if read receipts are shared.
A dropped socket closes the Live label and reconnects quietly. Typing appears only from someone who
chose to share it.

### Moment 4 — "The key changed; stop."

The app pins the signing and messaging key of every accepted connection. If a later directory
answer presents a different key, the app keeps the pinned key, marks the contact **Key changed** and
refuses to send until the person verifies it on another channel. The relay cannot silently swap a
key and keep the conversation.

### Moment 5 — "I can leave."

**Delete live account** erases the handle, public keys, consent, connections and ciphertext from the
relay, then clears the live state on the device. The private record remains. The action has a
confirmation that names what is removed.

### Moment 6 — "I can carry the shape without a server."

**Export Pear handoff** writes a JSON file with sequence, kind, hash and time only. The companion
node in `peer/` validates it, appends it to a Hypercore and replicates it to peers that know the
same circle code. No subjects, photos, receipts or vault references travel.

---

## 2. Architecture

```
┌─────────────────────────────┐        ┌─────────────────────────────┐
│ Android (Compose)           │        │ Android (Compose)           │
│  private record (offline)   │        │  private record (offline)   │
│  SharedPlane client         │        │  SharedPlane client         │
│  keys wrapped by Keystore   │        │  keys wrapped by Keystore   │
└──────────────┬──────────────┘        └──────────────┬──────────────┘
               │ HTTPS / WSS                         │
               ▼                                     ▼
      ┌──────────────────────────────────────────────────┐
      │ tlc-relay: one Worker + one SQLite Durable Object│
      │ accounts · consent · connections · ciphertext    │
      │ presence projection · WebSocket fan-out          │
      └──────────────────────────────────────────────────┘

      ┌──────────────────────────────────────────────────┐
      │ peer/: Hyperswarm + Hypercore companion           │
      │ circle code → topic · validated handoff records   │
      └──────────────────────────────────────────────────┘
```

Modules:

- `core/shared` — pure Kotlin crypto and presence projection, no Android dependency. 11 tests.
- `apps/android/.../data/SharedPlane.kt` — registration, keys, HTTP/WS, outbox, persistence.
- `relay/` — Worker, presence policy, 6 tests, `wrangler.toml`, self-host README.
- `peer/` — Hyperswarm/Hypercore node, 5 tests, CLI, README.
- `apps/android/.../data/PeerAdapter.kt` — record-shape handoff only.

---

## 3. Cryptographic contract

- Key agreement: P-256 ECDH, ephemeral sender key against the recipient's static messaging key.
- KDF: HKDF-SHA256, 32-byte key, fixed info `tlc.e2ee.v1`.
- AEAD: AES-256-GCM, 12-byte nonce, 128-bit tag.
- Signature: SHA256withECDSA over `aad|ephemeral|nonce|ciphertext`.
- AAD: sorted `conversation|sender|recipient|clientId`, where conversation is the sorted handles
  joined by `~`.
- Device key protection: P-256 private keys wrapped by an AES-256-GCM key in the Android Keystore.
- Pinning: accepted contact keys are stored locally; a change is surfaced, not trusted.

**Not claimed:** forward secrecy per message, post-compromise security, deniability, multi-device
key agreement. The UI and release notes say **E2EE alpha** for that reason.

---

## 4. Test matrix

| Layer | Command | Result |
|---|---|---|
| Core invariants | `make verify` | green |
| Shell type-check | `make shell-check` | 292 classes, 831 resources |
| Relay policy | `cd relay && npm test` | 6/6 |
| Peer node | `cd peer && npm test` | 5/5 |
| Copy lint | `make copy-lint` | clean |
| FOSS guard | `make check-foss` | clean |
| Relay end-to-end | live script | presence online, hidden `null`, message over WS, sent/delivered/typing/read, inbox |
| Pear end-to-end | two local peers | handoff imported on A, received and persisted on B |

---

## 5. Deployment

- Relay: `https://tlc-relay.access1tutamailcom.workers.dev` (Cloudflare Worker, SQLite DO).
- The address is a BuildConfig default and a setting; a self-hoster changes it without a new build.
- Release: GitHub Actions builds the signed APK on x86-64, verifies signature and certificate,
  requires `INTERNET`, rejects cleartext, and publishes an immutable release.

---

## 6. Known limits

- No offline message expiry sweep beyond the 30-day prune on mailbox reads.
- No push wake-up: the socket must be open while the app is in the foreground.
- No multi-device key sharing; each device is its own live identity.
- The relay sees connection metadata and IP; the Pear path avoids the relay but requires both peers
  online and leaks DHT metadata.
- The Pear node is a companion, not an Android runtime.
