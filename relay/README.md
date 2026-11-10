# TLC Relay

The shared plane is **thin, open and replaceable**. It carries account handles, public keys, consent
records, connection state and opaque encrypted envelopes. It never receives a passphrase, a private
key, message plaintext, a bank detail or a proof image. It does not move, hold, pool or settle money.

- Runtime: Cloudflare Workers + one SQLite-backed Durable Object.
- Wire: HTTPS + one authenticated WebSocket (`GET /v1/stream`).
- Storage: `accounts`, `consent`, `connections`, `messages`; every message is ciphertext only.
- Retention: undelivered ciphertext is pruned at 30 days; `DELETE /v1/account` removes the account,
  its connections and queued messages immediately.
- Authority: the relay orders shared facts and filters presence. The phone owns the private record.

## Run it

```sh
cd relay
npm ci
npm test
npx wrangler deploy
```

`wrangler.toml` is intentionally free of an account id and route: a different operator can deploy
the same worker on their own account and build the app against it. The app's default relay is set in
`apps/android/build.gradle.kts` as `TLC_RELAY_URL` and shown read-only on the Live screen.

## Presence rule

Presence is never inferred from activity. Each of the four signals — online now, last seen, typing
and read receipts — has its own audience: Nobody, Selected people, My circle or Everyone. Every
signal starts at **Nobody**. Invisible overrides everything and hidden people win over any audience.
The relay stores the consent record and the last accepted heartbeat, then computes a projection for
one viewer. A hidden signal is absent (`null`) rather than "offline". The rule lives in
`src/presence.js` and has plain-Node tests.

## E2EE envelope

The client sends only:

```json
{
  "to": "grace",
  "clientId": "uuid-per-message",
  "ephemeralPublicKey": "base64 X.509 P-256",
  "nonce": "base64 12-byte GCM nonce",
  "ciphertext": "base64 AES-256-GCM",
  "aad": "conversation|sender|recipient|clientId",
  "signature": "base64 SHA256withECDSA over aad|ephemeral|nonce|ciphertext"
}
```

The sender derives an AES-256-GCM key with ephemeral-static ECDH P-256 and HKDF-SHA256. The
recipient opens it with their static private key and verifies the sender's signature with the
sender's pinned public key. The relay only routes the envelope; it cannot open it. This is honest
one-to-one E2EE and is labelled **E2EE alpha** because it is not a ratcheting group protocol.

## Limits

- Body: 64 KiB. Message ciphertext: 32 KiB. Undelivered envelopes are pruned at 30 days by the
  Durable Object; `DELETE /v1/account` removes the account, connections and queued ciphertext at once.
- Message rate: 90/minute per account, in-memory; a larger deployment should move the counter into
  the Durable Object storage or a rate-limit binding.
- No push notifications in the `foss` channel. A foreground WebSocket is the live path; reconnecting
  pulls the mailbox by sequence. Offline messages queue in the app's durable outbox.

## Privacy

The relay necessarily sees handles, connection state, timing, sizes and IP addresses. It does not
see message text or the private record. The full disclosure is in `../PRIVACY.md`.
