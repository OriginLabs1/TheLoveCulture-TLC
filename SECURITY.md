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
4. **No network.** v0.0.5 has no networking code, so a destination cannot leave the device through
   the app. It can leave only if the person exports or shares something deliberately, and the vault
   itself is never part of an export.
5. **Loss is honest.** Uninstalling the app or resetting the device destroys the Keystore key, so the
   saved numbers become unreadable. The reference (bank name, last four, fingerprint) remains; the
   person can enter the destination again deliberately.

If a release compiles an account number, prints one to a log, stores a full number in the ledger or
state, includes one in a backup or export, or shows one before an explicit reveal, treat that as a
security defect and report it under this policy.

## Safe harbour

We consider research conducted under this policy authorised. If a third party brings a claim against
you for such research, we will make our authorisation clear.
