# Security Policy — TLC

## Supported versions

| Version | Supported |
|---|---|
| 0.0.x (pre-release) | ✅ latest patch only |

Pre-release builds are for testing. Do not use them with real bank details until v1.0.

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
- **No secrets in the app.** The Pioneer account number is fetched at runtime, never compiled in.
- **No silence.** A breach affecting personal data is assessed and, where required, reported to the
  Nigeria Data Protection Commission within 72 hours of awareness.

## Pioneer bank details

The Pioneer's bank details are not a public profile, a listing screenshot, a repository value or a
constant in the APK. They are released only through the fee flow, to the person the due belongs to,
after that due exists:

1. **Right people.** One payer and the Pioneer. A due is bound to two distinct member ids, and the
   instruction is issued to that payer alone.
2. **Right time.** Details are released only after the due is recorded and the instruction is issued,
   and the instruction expires after `INSTRUCTION_TTL_MILLIS` (15 minutes). They are never cached,
   never written to the state file or ledger, never included in an export or backup bundle, and
   never logged.
3. **Right reason.** The only reason is to let the payer make the transfer the due describes. There
   is no browseable directory, no "show account" button outside an active due, and no copy path that
   survives the flow.
4. **Never in this build.** v0.0.4 has no networking code, so it has no secure runtime channel for
   those details. It records the due and the proof and says plainly that the details are released
   only inside the fee flow. Do not paste real account numbers into a preview build, a screenshot, an
   issue or a chat.

If a release ever compiles an account number, prints it to a log, stores it in the ledger, or shows
it before a due exists, treat that as a security defect and report it under this policy.

## Safe harbour

We consider research conducted under this policy authorised. If a third party brings a claim against
you for such research, we will make our authorisation clear.
