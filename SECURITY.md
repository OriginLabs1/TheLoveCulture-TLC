# Security Policy — TLC

## Supported versions

| Version | Supported |
|---|---|
| 0.0.x (pre-release) | ✅ latest patch only |

Pre-release builds are for testing. Keep an independent copy of any bank destination you save until
v1.0; a lost or reset keystore makes the vault unreadable by design.

## Report a vulnerability

**tlcfounder@protonmail.com** — subject line `SECURITY`.

Please include: affected version (`versionName` from *Settings → About*), platform, reproduction
steps, and impact. If you need to send something sensitive, say so first and we will arrange an
encrypted channel. Do not open a public issue.

**Our commitments**

| Stage | Target |
|---|---|
| Acknowledge receipt | 72 hours |
| Initial assessment + severity | 7 days |
| Fix or documented mitigation | 30 days (critical), 90 days (others) |
| Public credit | with your permission |

We will not pursue legal action for good-faith research that stays within the scope below. We will
not ask you to sign an NDA to report a bug.

## Scope

**In scope**

- The Android app (APK from GitHub Releases, F-Droid, Orion Store or Uptodown).
- The `core/domain` and `core/ledger` invariants — especially anything that would let a client mint
  an attestation, forge a receipt, or make a hash chain verify after tampering.
- The `ledger-verify` CLI (exit-code contract: `0` verified, `1` failed, `2` usage/IO).
- Signature, release-pipeline and supply-chain integrity.
- The v0.0.8 shared plane: the relay, account/directory/connection endpoints, presence filtering,
  message envelopes, WebSocket stream, account deletion and the optional peer layer.

**Out of scope**

- Findings that require a rooted device with an unlocked bootloader, unless they survive a factory reset.
- Missing security headers on a site that sets none of its own.
- Rate-limiting or spam on endpoints with no accounts.
- Social engineering of staff, or physical access.
- Denial of service by volume.
- Reports generated solely by an automated scanner with no demonstrated impact.

## Rules of engagement

- Test only **your own** accounts and data. Never another person's dyad, proofs or receipts.
- No exfiltration, no persistence, no lateral movement. Stop at the first proof of impact and report.
- Do not run automated scans against production at a rate that degrades service.
- Do not attempt to obtain, use or publish anyone's bank details.
- If you encounter real user data, stop, delete it, and tell us what you saw — do not read further.

## Design commitments you can hold us to

- **No custody.** No code path holds, pools, escrows or moves money (INV-01). `Effect.requiresCustody`
  is `false` for every member, and `EntryKind` contains no balance/transfer/escrow type.
- **No client minting.** Attestations come from an authorised human, never from `SYSTEM` (INV-08).
- **Tamper-evident history.** Append-only SHA-256 chain; truncation is detected against a published
  Merkle anchor.
- **No secrets in the app.** No account number is compiled into the APK. A destination you choose to
  save is entered by you and kept only in the encrypted vault on that device.
- **No silence.** A breach affecting personal data is assessed and, where required, reported to the
  Nigeria Data Protection Commission within 72 hours of awareness.

## Bank destinations

A bank destination is not a public profile, a listing screenshot, a repository value or a constant
in the APK. It is entered by the person who will use it and kept on that device:

1. **Encrypted at rest.** The full account number lives only in a separate vault file, encrypted with
   AES-256-GCM under a key generated inside the Android Keystore and never exported. The state file,
   the record, a backup bundle, the contact log, a notification and an export see only the bank name,
   the last four digits and a fingerprint.
2. **Revealed on purpose.** The app shows the masked destination until an explicit reveal. A reveal
   is written to the record, hides again when its timer ends, and is never copied into the state or a
   backup.
3. **Right reason.** The only reason to reveal is to make a transfer the person has chosen to make.
   There is no browseable directory and no copy path that survives the reveal window.
4. **No relay exposure.** The vault is never uploaded. v0.0.8 sends only relay account data,
   consented presence and ciphertext messages; a destination can leave the device only if the
   person exports or shares something deliberately, and the vault itself is never part of an export.
5. **Loss is honest.** Uninstalling the app or resetting the device destroys the Keystore key, so the
   saved numbers become unreadable. The reference (bank name, last four, fingerprint) remains; the
   person can enter the destination again deliberately.

If a release compiles an account number, prints one to a log, stores a full number in the ledger or
state, includes one in a backup or export, or shows one before an explicit reveal, treat that as a
security defect and report it under this policy.

## v0.0.8 shared plane — commitments you can hold us to

The shared plane ships in this build. These promises are in scope now, and a regression in any of
them is a security defect:

- **The passphrase never leaves the device.** Registration sends a handle and device public keys.
  The salted PBKDF2 verifier stays local. No passphrase or recovery phrase is transmitted, logged,
  cached or put in a crash report. A future PAKE (OPAQUE or an audited SRP-class protocol) is the
  direction; this build does not claim one.
- **Device keys are explicit and revocable.** Each device has its own P-256 key pair, wrapped by an
  Android Keystore AES-256-GCM key. Deleting the account removes the public key from the relay.
  Revocation of a lost device is a next-milestone item, not a v0.0.8 claim.
- **Messages are end-to-end encrypted (alpha).** The relay holds ciphertext and metadata only.
  Sealing is P-256 ECDH (ephemeral-static) + HKDF-SHA256 + AES-256-GCM with a SHA256withECDSA
  sender signature. This composes audited JCA primitives directly; it is not a formally audited
  ratchet and it is labelled alpha. There is no silent server-side content scanning or recording.
- **Presence is filtered at the edge.** A signal the person did not share is sent as `null` and
  never enters another person's event stream, cache or export. A steward cannot see or override
  presence.
- **TLC holds no money and no card data.** A Transfer Method adapter may instruct; it may never
  debit, hold, net or settle. No PAN/CVV is stored, logged or shown.
- **The peer layer has no authority.** Pear/Holepunch replicates record-shape-only handoff (no
  subjects, amounts, photos, receipts or vault fields) and can never order a shared fact, mint a
  receipt or settle a transfer. With the peer layer disabled, every shared and money-adjacent
  behaviour is unchanged.
- **Deletion and retention are real.** `DELETE /v1/account` removes the account, its connections and
  queued ciphertext. Undelivered ciphertext is pruned at 30 days.
- **Fail closed.** Clock skew above five minutes, an invalid sender signature, a changed pinned
  device key or an unverifiable chain blocks the sensitive action rather than assuming authority.

## Safe harbour

We consider research conducted under this policy authorised. If a third party brings a claim against
you for such research, we will make our authorisation clear.
