# Privacy and threat model

Union is local-first. The honest version of that sentence is: **your records live on your device, and the only things that leave it are the things you deliberately send.**

## What stays on the device

- Identity keys (Ed25519 signing key, X25519 key-agreement key).
- Messages, threads, notes and attachments.
- Communities, posts, learning progress, experiences, presence cards.
- Activation requests, proofs, plans and the ledger.
- The assistant's memory, projects and proposals.
- Audit entries and outbox jobs.

On the web/PWA these live in IndexedDB; secrets use `localStorage` only when no native store exists. In the Android app, secrets are encrypted with an AES-256-GCM key held by the **Android Keystore** and stored in app-private preferences.

## What can leave the device

| Data | When | Who can see it |
| --- | --- | --- |
| Signed identity card | You register with a relay, or share a card | Anyone with the card; it is public by design |
| Sealed message envelope | A relay is configured and you send a direct message | Relay operators: sender, recipient, time, size, ciphertext |
| Link record | You publish a link to a relay | Anyone with the URL, until revoked or expired |
| Public room post | You publish to a public room with a relay | Anyone who can read that room |
| Activation package | You choose copy/save and send it to the assistant | The assistant and the Creator's review process |
| Encrypted backup | You export it | Whoever you give the file to; without the passphrase it is unreadable |

Nothing else is sent. There is no telemetry, crash reporting, advertising SDK or remote font fetch.

## Cryptography

- **Identity:** Ed25519. The identity id is derived from the public key (`did:union:<base64url(public key)>`), so a card cannot claim a key it does not hold.
- **Cards:** canonical JSON (sorted keys) signed with Ed25519.
- **Key agreement:** X25519, ephemeral per message.
- **Message encryption:** HKDF-SHA256 → AES-256-GCM. A self-copy is sealed separately so your own sent messages are readable only by you.
- **Backups:** PBKDF2-SHA256 (310,000 iterations) → AES-256-GCM.
- **Ledger:** SHA-256 hash chain over canonical entries.

All primitives come from `@noble/curves`, `@noble/hashes`, `@scure/base` and the platform WebCrypto. Union does not invent a cryptographic protocol.

## Relay metadata

A relay necessarily learns some metadata: which identity sent to which identity, when, and how large the ciphertext is. It cannot read message bodies or attachments. It cannot forge a card because cards are signed. It can withhold or drop messages; the outbox will retry, but a hostile relay can deny service.

Use your own relay if metadata matters to you. The Worker and schema are in [`server/`](../server/).

## What Union does not protect against

- A compromised device, unlocked phone or malicious keyboard.
- Someone who knows your backup passphrase.
- A hostile relay that drops or delays messages.
- Traffic analysis of relay metadata.
- A recipient who screenshots a message you sent them.
- Social engineering: someone impersonating the Creator or the assistant. The Creator's WhatsApp number is never published; verify public contacts through the links in the app.

## Android specifics

- One permission: `INTERNET`. Used for the loopback asset server (`127.0.0.1`) and the optional relay.
- Cleartext traffic is disabled except for loopback.
- The WebView blocks file access, mixed content, geolocation and multiple windows.
- External links leave for the system browser; the shell only accepts `https`, `mailto` and `tel`.
- `allowBackup=false`: Android cloud backup does not copy Union's records. Use Union's own encrypted backup.
- The native bridge exposes only: encrypted secret get/set/delete, info, share, copy, open external link, theme colour, exit, and save-to-Downloads. No contacts, SMS, camera, filesystem browsing or arbitrary intents.

## Data retention

- Local records remain until you delete them, reset the app, or uninstall.
- A relay keeps an unacknowledged message for up to 30 days and an acknowledged message for up to 1 day; links expire when their owner says they do.
- Activation proofs exist in the local request record and in whatever package you send. There is no server-side proof archive in v0.0.1.

## Your controls

- Export an encrypted backup with or without keys.
- Delete records, clear the ledger, reset the app.
- Revoke published links.
- Remove the relay URL at any time; the app returns to local-only behaviour.
- Choose whether a room is public or private; private rooms are not registered with a relay.

## Reporting a problem

Do not open a public issue containing a working exploit, keys, a backup file or someone's personal data. Use a private channel to the Creator and include the version, platform and a minimal reproduction.
