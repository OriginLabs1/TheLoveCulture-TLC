# Privacy Notice — TLC

**Version:** 0.0.1 · **Applies to:** the TLC Android app and `tlc.app`
**Data regime:** Nigeria Data Protection Act 2023 (NDPA), GDPR-shaped by design (INV-10)
**Contact:** concept-pioneer@theloveculture.app

This notice is short because the app collects little. It is written to be read, not to be survived.

---

## 1. Who is responsible

TLC is operated by **Concept Pioneer** (Collins Christopher), Nigeria — the *data controller* for the
purposes below. There is no separate analytics company, ad network or data broker in the chain.

## 2. What the app handles

| Category | Examples | Why | Stored where |
|---|---|---|---|
| Account | display name, role (Single / I-Partner / Love Model), device-held identifier | Sign-in, dyad membership | On your device; server only if you enable sync |
| Relationship | dyad membership, Receptive Mode | Route support to the right person | On your device |
| Fee evidence | proof image/PDF, bank reference, amount, timestamp | Prove *you* paid the Concept Pioneer directly | Encrypted object storage, 90 days, then hash only |
| Receipts | obligation id, amount, attestation, hash chain | Your permanent record of what was attested | On your device + append-only ledger |
| Technical | crash signature, app version, OS version | Fix defects | 90 days |

**Not collected, ever:** card numbers, bank passwords, full bank account numbers in plaintext,
contacts, location, advertising identifiers, clipboard contents, or the contents of your messages.

`TLC` never holds, pools, escrows or moves money. It records claims about transfers that already
happened between two private bank accounts (INV-01).

## 3. The `foss` build is telemetry-free

The `foss` flavour ships with **no** Google Play Services, Firebase, Crashlytics, ad SDK or
third-party analytics. It makes network requests only when you ask it to (sync, proof upload).
The `full` flavour may add first-party integrations; each one is opt-in and listed in the release
notes for that version.

## 4. Lawful bases

- **Contract / steps before contract** — account, dyad, receipts.
- **Legitimate interest (fraud prevention, dispute resolution)** — fee evidence and the ledger.
- **Consent** — Receptive Mode, optional sync, any optional integration. Withdrawable at any time.

## 5. Retention

| Data | Retention |
|---|---|
| Fee proofs (image/PDF) | 90 days, then deleted; only the SHA-256 hash and the attestation remain |
| Receipts and ledger entries | 7 years (financial-record expectation) |
| Crash/technical logs | 90 days |
| Optional product analytics (never in `foss`) | 13 months |
| Account data | Until you delete your account, then 30 days in backups |

Deleting your account deletes your account data. It does not delete an attestation that a payment
was made — that record belongs to the Concept Pioneer's financial history and to the other member of
the dyad. It contains no proof image after 90 days.

## 6. Who else sees data

- **Hosting / object storage** — one infrastructure provider, under a data-processing agreement.
- **Payment rails** — none. Money moves bank-to-bank between you and the recipient; TLC is not a
  party to that transfer and receives no payment data from your bank.
- **Authorities** — only on a valid legal order, and we will tell you unless legally barred.

We do not sell data. We do not share it for advertising. We do not use it to train third-party models.

## 7. Your rights (NDPA ss. 34–38)

Access, correction, deletion, restriction, portability, objection, and withdrawal of consent.
**Export is always available offline** — your ledger is a file on your device (INV-05).

Make a request to **concept-pioneer@theloveculture.app**. We answer within **30 days**. If you are
not satisfied you may complain to the **Nigeria Data Protection Commission (NDPC)**.

## 8. Security

TLS in transit, encryption at rest, short-lived signed URLs for proofs, hashed bank references,
no secrets in URLs or logs. The Pioneer account number is a runtime secret and is never compiled
into the app (CONTEXT §0). See [`SECURITY.md`](SECURITY.md) to report a vulnerability.

## 9. Children

TLC is for adults (18+). We do not knowingly process data of anyone under 18.

## 10. Changes

Material changes are announced in the release notes and in-app before they take effect. The version
and date at the top of this file always identify the notice you accepted.
