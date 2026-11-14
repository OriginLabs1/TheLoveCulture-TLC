# Security policy

## Supported version

| Version | Supported |
| --- | --- |
| 0.0.1 | Yes |

## Reporting

Do **not** open a public issue containing:

- a working exploit or a step-by-step attack,
- identity keys, recovery passphrases or backup files,
- someone else's personal data, proofs or message contents,
- the Telegram bot token or any server credential.

Send a private report to the Creator through the public Telegram contact listed in the app, with the Union version, platform, and a minimal reproduction. If the report involves the relay, include the deployed relay origin but no user data.

You will get an acknowledgement when the report is seen. Please allow time for a fix before public disclosure.

## Scope

In scope:

- `src/lib/crypto.ts` — identity, cards, message sealing, backups.
- `src/lib/store.svelte.ts` — local records, economy, sync, audit.
- `src/lib/native.ts` and the Android shell in `android/`.
- The relay in `server/`, including signature verification and access control.
- The service worker and offline behaviour.

Out of scope for v0.0.1:

- The absence of multi-device key sync (documented limitation).
- Metadata visible to a relay by design.
- A hostile relay dropping or delaying messages.
- Physical access to an unlocked device.
- The external Telegram assistant's own backend, unless Union's package format is the cause.

## Security model in one paragraph

Identity keys never leave the device except in an encrypted backup. Every relay write is signed over `method`, path, timestamp and body hash with Ed25519, and the relay checks the card signature and that the identity id is derived from its public key. Message bodies are sealed with X25519 + HKDF-SHA256 + AES-256-GCM before they reach a relay. The Android app stores secrets with an AES-256-GCM key held by the Android Keystore. There are no accounts, cookies, passwords, analytics or third-party scripts.

## Hardening checklist for operators

- Use a release keystore; back it up offline.
- Publish the APK's certificate fingerprint next to the download.
- Keep `ALLOWED_ORIGINS` narrow on the relay.
- Never commit bot tokens, phone numbers, proofs or backups.
- Rotate any credential that has appeared in a chat, log or screenshot.
- Rebuild and re-test after dependency updates; `npm audit` is not a substitute for reading the diff.
