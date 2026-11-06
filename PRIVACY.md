# Privacy Notice — TLC

**Version:** 0.0.7 (current build) · §2a previews the shared plane, which is not in this APK
**Applies to:** the TLC Android app and `tlc.app`
**Data regime:** Nigeria Data Protection Act 2023 (NDPA), GDPR-shaped by design (INV-10)
**Contact:** tlcfounder@protonmail.com · https://sircollins.coreconcept1.net · https://t.me/CollinsRoyalMedium

This notice is short because the app collects little. It is written to be read, not to be survived.

---

## 1. Who is responsible

TLC is operated by **Concept Pioneer** (Collins Christopher), Nigeria — the *data controller* for the
purposes below. There is no separate analytics company, ad network or data broker in the chain.

## 2. What the app handles

| Category | Examples | Why | Stored where |
|---|---|---|---|
| Account | display name, handle, role (Single / I-Partner / Love Model), device-held identifier | Sign-in, addressing, dyad membership | On your device; server only if you enable sync |
| Identity secret | passphrase **verifier** (never the passphrase), device public key, recovery public key | Authenticate without storing or sending your words | Verifier/public keys on the server in v0.0.7; the passphrase and recovery phrase stay on your device |
| Presence | online now, last seen, typing, read receipts — each with its own audience | Show connection only when you consent | Shared plane in v0.0.7; private and ephemeral; no history beyond the feature |
| Messages and calls | ciphertext envelopes, conversation id, time, size; call session metadata | Deliver your message or connect your call | Ciphertext in the relay mailbox; session metadata in v0.0.7; no plaintext, no recording |
| Relationship | dyad membership, Receptive Mode, the people you add and their handles | Route support to the right person | On your device |
| Bank destination | bank name, account holder, last four digits, fingerprint, and the full account number | Save a destination once and transfer directly | Full number only in the encrypted vault on your device; the rest in local state |
| Transfer Method | method kind, provider, quoted fee, payer, timing, reference | Show the real route and keep the receipt | Local state; the minimum needed for reconciliation on the shared plane in v0.0.7; never PAN or CVV |
| Contact log | purpose, channel and time of a Pioneer Desk contact | Keep an honest record that you reached out | On your device; never the message itself |
| Fee evidence | proof image/PDF, bank reference, amount, timestamp | Prove *you* transferred directly to the Concept Pioneer | Encrypted object storage, 90 days, then hash only |
| Receipts | obligation id, amount, attestation, hash chain | Your permanent record of what was attested | On your device + append-only ledger |
| Images | profile photo, receipt image, image fingerprint | Show your face; keep the evidence for a token | App-private storage on your device; a file name and fingerprint in local state |
| Peer backup key | device-local replication key material | Optional encrypted backup to your own devices | On your device; never uploaded in plaintext |
| Technical | app version, OS version | Fix defects | On your device only, in this build |

**In this build (0.0.7):** the app stores, on the device only, the recognition record you write, the
profile name, handle and circle name you enter, your username and a salted PBKDF2 passphrase
verifier, your presentation preferences, your presence choices and the selected and hidden people
you name, the people and handles you add, the Support and Pioneer's Token records you create, the
local contact log, any bank destination you deliberately save, and any profile or receipt image you
pick. The passphrase itself is never stored, logged or sent; only a salt and verifier remain on the
device. Presence choices are stored locally and are not transmitted in this build. A saved
destination keeps the full account number only in the encrypted vault (AES-256-GCM with a key held
by the Android Keystore); the state file, the record, a backup bundle and the contact log see only
the bank name, the last four digits and a fingerprint. An image is framed in the avatar studio,
downscaled, re-encoded as JPEG and stripped of metadata before it is stored in the app's private
media folder; the state and the record keep a file name and a SHA-256 fingerprint, never the bytes,
and the image never enters a backup bundle or an export. Removing an image deletes the file. The
state file, vault, record and media folder live in the app's private folder. There is no server
account, no sync and no crash reporting, so the account, relationship and fee-evidence rows above
describe the product's intended scope, not data this version sends anywhere. Backup means exporting
a bundle through the system file picker; TLC does not upload it. Sharing a receipt is a deliberate
action through the system chooser and grants one file, not the folder.

**Not collected, ever:** card numbers, bank passwords, full bank account numbers in plaintext or in
any export, contacts from your address book, location, advertising identifiers, clipboard contents,
the contents of your messages, your passphrase or recovery phrase, or the presence of a person who
chose not to share it.

### 2a. The shared plane (future direction, not in this build)

A later release adds an account on a server, sync, live presence, live communication, Transfer
Methods connected to providers and optional peer backup. These commitments are doctrine, not
aspirations:

- **Presence is consented and private by default.** Online now, last seen, typing and read receipts
  are four separate signals. You choose **Nobody / Selected people / Circle / Everyone** for each,
  can add per-person exceptions, and can withdraw at any time. A hidden signal is shown as "not
  shared", never guessed. Presence is never sold, used for ranking or exposed to a steward.
- **Passphrase never leaves the device.** The server receives a verifier produced by a
  password-authenticated key exchange (OPAQUE or an audited SRP-class protocol), or an Argon2id
  verifier as the minimum fallback. It cannot reconstruct your words. Your recovery phrase never
  leaves the device.
- **Messages and calls are end-to-end encrypted.** The relay holds ciphertext, a conversation id,
  size and time — not the content. Push payloads are opaque. A reporter may choose to attach an
  excerpt; nothing else is scanned.
- **Transfer Methods disclose their provider.** TLC stores the method kind, provider, fee and a
  reference for the receipt. It never receives or stores a card PAN/CVV or a bank password. Where a
  licensed provider or acquirer is used, that provider's own privacy notice applies and is shown
  before you opt in.
- **Peer backup is opt-in and encrypted.** Hypercore/Hyperswarm replication carries encrypted
  segments and never grants authority over a shared fact. You can disable it at any time.

When this plane ships, the retention table, lawful bases and network section below are updated in
the same release, before the feature is enabled.

`TLC` never holds, pools, escrows or moves money. It records claims about transfers that already
happened between two private bank accounts (INV-01).

## 3. The `foss` build is telemetry-free

The `foss` flavour ships with **no** Google Play Services, Firebase, Crashlytics, ad SDK or
third-party analytics. Version 0.0.7 contains no networking code at all and its APK does not request
the `INTERNET` permission — the release workflow checks both. When the shared plane arrives, this
notice changes in the same release, and the app asks only for what the feature needs:
`INTERNET` and network state for sync; notifications for an opaque wake hint; camera and microphone
only when you enable a call, requested at the moment of use. The `full` flavour may add first-party
integrations; each one is opt-in and listed in the release notes for that version.

## 4. Lawful bases

- **Contract / steps before contract** — account, dyad, receipts, messages, calls.
- **Legitimate interest (fraud prevention, dispute resolution)** — fee evidence and the ledger.
- **Consent** — Receptive Mode, presence sharing, optional sync, optional peer backup, any optional
  integration, camera/microphone during a call. Withdrawable at any time.
- **Legal obligation** — financial-record retention and lawful orders.

## 5. Retention

| Data | Retention |
|---|---|
| Saved bank destination | Until you remove it in the vault, or uninstall the app; the key never leaves the Android Keystore |
| Profile photo and receipt images | Until you replace or remove them, or uninstall the app |
| Local contact log | Until you clear it, or uninstall the app |
| Fee proofs (image/PDF) | 90 days, then deleted; only the SHA-256 hash and the attestation remain |
| Receipts and ledger entries | 7 years (financial-record expectation) |
| Presence (online, last seen, typing, read) | Only as long as the chosen feature needs it; cleared on withdrawal; no history |
| Ciphertext messages | Until delivered plus the mailbox expiry window; your local copy until you delete it |
| Passphrase verifier and device public keys | Until you remove the device or delete the account |
| Peer backup keys | On your device until you disable the peer layer or uninstall |
| Crash/technical logs | 90 days |
| Optional product analytics (never in `foss`) | 13 months |
| Account data | Until you delete your account, then 30 days in backups |

Deleting your account deletes your account data. It does not delete an attestation that a payment
was made — that record belongs to the Concept Pioneer's financial history and to the other member of
the dyad. It contains no proof image after 90 days.

## 6. Who else sees data

- **Hosting / object storage** — one infrastructure provider, under a data-processing agreement.
- **Transfer Methods** — none that TLC operates. Money moves bank-to-bank between you and the
  recipient; TLC is not a party to that transfer and receives no payment data from your bank. In
  v0.0.7 a licensed provider or hosted card acquirer may be used for a Pioneer's Token; the provider
  is the regulated party, its notice is shown before you opt in, and TLC never sees your card PAN or
  CVV.
- **Peer nodes** — only if you enable peer backup. Other nodes hold encrypted segments, not
  plaintext, and never the authority to change a shared fact.
- **Authorities** — only on a valid legal order, and we will tell you unless legally barred.

We do not sell data. We do not share it for advertising. We do not use it to train third-party models.

## 7. Your rights (NDPA ss. 34–38)

Access, correction, deletion, restriction, portability, objection, and withdrawal of consent.
**Export is always available offline** — your ledger is a file on your device (INV-05).

Make a request to **tlcfounder@protonmail.com**. We answer within **30 days**. If you are
not satisfied you may complain to the **Nigeria Data Protection Commission (NDPC)**.

## 8. Security

TLS in transit, encryption at rest, short-lived signed URLs for proofs, hashed bank references,
no secrets in URLs or logs. A saved bank destination is encrypted with AES-256-GCM under a key
generated inside the Android Keystore and never exported; the full account number is not written to
the state file, the record, a backup bundle, the contact log, a notification or an export. The
Pioneer account number is never compiled into the app (CONTEXT §0). Your passphrase and recovery
phrase are never transmitted or logged; only a verifier reaches the server. Messages and calls are
end-to-end encrypted, and presence is filtered by consent before it leaves the server. See
[`SECURITY.md`](SECURITY.md) to report a vulnerability.

## 9. Children

TLC is for adults (18+). We do not knowingly process data of anyone under 18.

## 10. Changes

Material changes are announced in the release notes and in-app before they take effect. The version
and date at the top of this file always identify the notice you accepted.
