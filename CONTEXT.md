# TLC — The Love Culture (Community Edition) v0.0.1 — CONTEXT

> Doctrine, identity and non-negotiables. This file answers **why** and **what must never change**.
> Executable detail lives in `SPECS.md`. When the two conflict, `CONTEXT.md` wins.

---

## 0. Credential Hygiene (read first)

No secret ever enters this repository, an APK, a PWA bundle, a screenshot, or a chat log.
Secrets live only in: GitHub Actions secrets, Cloudflare Worker secrets, Android Keystore,
and the operator's password manager. `.env*`, `*.keystore`, `google-services.json` are ignored.
Any token that has been pasted into a document or chat is **considered burned** and must be
rotated before use. Bank account numbers are *data*, not *config*, and are never committed.

---

## 1. Identity & Doctrine (immutable)

TLC is a **Godly, strategic concept powered by love** to reveal and sustain the identity and
impact of love in society. It is **not** a religion, a political movement, a commercial
business, a bank, or an investment scheme.

**Mission.** Move love from emotion to structured, intelligent cultural force:
Identity Restoration · Love as Leadership · Cultural Intelligence · Sustainable Impact.

**Operational doctrine.** Sovereign · local-first · modular · agnostic · non-custodial ·
universal · dignity-preserving.

**Non-negotiables (INV — tested, not aspirational):**

| ID | Invariant |
|---|---|
| INV-01 | TLC never takes custody of funds it does not own. No pooling, no float, no escrow, no wallet balance. |
| INV-02 | Money moves **person → person** through each party's own bank. TLC issues *instructions*, never transfers. |
| INV-03 | A "touchpoint" is not a "token": TLC Tokens are recognition signals, **not** currency, crypto, e-money, securities, or redeemable credit. |
| INV-04 | Every sensitive action needs independent authentication + explicit authorization. A link never grants authority. |
| INV-05 | User data is user-owned. Local-first by default; sync is a service, not a cage. Export is always possible, offline. |
| INV-06 | No generic "admin". Authority is delegated, scoped, signed, expiring and revocable. |
| INV-07 | Deterministic, reversible, reproducible builds. No unreviewed pre-release dependency in a release branch. |
| INV-08 | Money-adjacent facts are **server-attested, append-only**. A client may declare intent; only attestation settles. |
| INV-09 | No dark patterns. Consent, fees and destinations are stated in plain language before the action. |
| INV-10 | Privacy by design (NDPA 2023 / GDPR-shaped): minimisation, purpose limitation, retention limits, no bank PANs stored in plaintext, no secrets in URLs. |

---

## 2. Doctrine Refinements for v0.0.1 (deliberate changes)

These are corrections to the source brief. Each is a decision, not a preference.

1. **Growth Grants are not escrow.** The brief said "escrow" while mandating zero custody —
   a contradiction and a licensing hazard. v0.0.1 model: *Commitment + Witness*. A grant is a
   signed pledge → beneficiary fulfils a milestone → steward verifies evidence → **donor transfers
   directly to the beneficiary's own account** → receipt attested. TLC holds nothing. Optional
   third-party escrow (smart contract / licensed provider) is a *replaceable adapter*, never default.
2. **"Tokens" are signals.** Keep the names (Supportive, Growth, Recognition, Activation,
   Reactivation, Courtesy, Joy) for cultural continuity, but every surface states:
   *no cash value · not transferable · not redeemable · not an investment · not crypto.*
   Amounts are ordinary NGN bank transfers chosen by the sender.
3. **Complementary Stewardship is role-based, not gender-based.** Keep the two invariants —
   (a) structural authority, (b) full visibility + veto — but assign them as **Primary Steward**
   and **Co-Steward**, chosen by the couple. Gender, if declared, is descriptive only. Veto is
   **symmetric** on irreversible acts (bank-detail change, dyad dissolution, delegation grant).
   This removes a discrimination/legibility risk without weakening the model.
4. **"Pioneer fees" are membership dues and voluntary gifts.** Activation ₦1,000 (one-off),
   Reactivation ₦2,000/month, Courtesy (10%, voluntary), Joy (voluntary). Marketing, ToS and UI
   language must avoid: invest, yield, return, profit-share, referral commission, escrow, wallet.
5. **No "admin panel".** There is a **Steward Console**: scoped views, per-case grants,
   every read and write audited, no bulk export of people or bank data.
6. **No card acquiring in v0.0.1.** Bank transfer + attested proof only. This keeps TLC outside
   PCI scope and outside custody entirely. Payment-rail webhooks are adapters, switched off by default.
7. **PWA ≠ second-class.** One core, many shells: Android (primary), PWA (universal), CLI (ops).
   Same domain code; no forked business logic.
8. **Traceability is a product feature, not an afterthought** (see §8 Pioneer Ledger).

---

## 3. TLC APP MANIFEST (mandatory)

```yaml
manifest_version: 1
app:
  name: "TLC — The Love Culture"
  short_name: "TLC"
  package_id: "culture.thelove.tlc"        # stable forever; changing it breaks continuity links
  version: "0.0.1"
  version_scheme: "semver"
  release_channel: "alpha"                  # alpha | beta | stable
  default_locale: "en-NG"
  locales: ["en-NG", "en"]
  min_android_sdk: 26                       # Android 8.0
  target_android_sdk: "latest-stable"
  form_factor: ["phone", "tablet", "foldable", "web-pwa"]
  orientation: "adaptive"
  install: "pwa-installable + play-store + direct-apk"
  offline: "full read; queued writes; no money-adjacent offline settlement"
ownership:
  concept_pioneer: "Collins Christopher ('Sir Collins')"
  github_org: "OriginLabs1"
  repository: "TheLoveCulture-TLC"
  project_path: "TLC App"
  jurisdiction: "Nigeria"
  entity_status: "pending-legal-review"     # see Risk R-1
identity:
  stable_id_scheme: "did:key (Ed25519) + ULID records"
  auth: "passkey-first, OTP fallback, device-bound keys in Android Keystore/StrongBox"
economy:
  currency: "NGN"
  custody: "none"                           # INV-01
  pioneer_fees: { activation: 1000, reactivation_monthly: 2000, courtesy_pct: 10 }
  pioneer_account_source: "runtime secret (never in repo/APK)"
  rails_v001: ["manual-bank-transfer", "proof-attestation"]
  rails_later: ["payment-provider-webhook (adapter)", "third-party-escrow (adapter)"]
tech:
  core: "Kotlin Multiplatform (domain + contracts) · SQLDelight"
  android: "Jetpack Compose (Material 3) · AGP stable · JDK 21"
  pwa: "SvelteKit (stable major) + Vite (stable major) + Tailwind · Svelte 5 runes"
  sync: "PowerSync (adapter) → Postgres (server truth)"
  edge: "Cloudflare Workers · R2 · Queues · Durable Objects · Turnstile"
  semantics: "JSON-LD 1.1 · W3C VC 2.0 · CBOR-LD (size) · Ed25519/ES256"
  native: "C++/NDK only for crypto primitives & canonicalisation (audited libs)"
  ci: "GitHub Actions (mobile-first) + signed releases"
compliance:
  notices: ["TLC Financial Notice", "Token Nature Notice", "Data & Privacy Notice"]
  data_regime: "NDPA 2023 (NG) · GDPR-shaped"
  retention: { proofs: "90d then hash-only", receipts: "7y", analytics: "13m", logs: "90d" }
brand:
  tone: "warm · dignified · precise · unhurried"
  aesthetics: "premium, culturally intelligent, liquid-glass-capable but not dependent on it"
  a11y: "WCAG 2.2 AA baseline; large-type & reduced-motion honoured"
  assets: "tokens in /design; no stock-photo sentimentality"
links:
  scheme: "https://tlc.app/* (universal) + tlc:// (fallback)"
  signing: "PASETO v4.public / JWS detached; ≤15 min TTL; single-use nonce; revocable"
security:
  posture: "zero-trust between shells; server-authoritative for money facts"
  sbom: required
  secrets: "GH Actions + CF Worker secrets only"
```

---

## 4. Roles & Economic Model (SEF)

| Role | Who | Cost | Can do |
|---|---|---|---|
| **Single** | Unmarried individual | Free | Send/receive Supportive & Recognition if **Receptive Mode** ON; give Joy/Courtesy |
| **I-Partner** | Engaged couple, pre-marriage | Free | Dyad formation, Growth Grant pledges, shared stewardship |
| **Love Model** | Married couple | ₦1,000 activation + ₦2,000/mo reactivation | Full dyad, receive Supportive/Recognition, publish verified transfer details |

**Routing — every row is a direct transfer between two private accounts. TLC is not a party.**

| Token | Sender → Receiver | Mechanism | TLC role |
|---|---|---|---|
| Supportive | User → Love Model | Direct to recipient's own account (recipient-supplied, verified) | None |
| Growth Grant | Donor → Beneficiary | Pledge → evidence → steward verification → **donor pays beneficiary directly** | Witness only |
| Recognition | User → Love Model | Direct | None |
| Activation / Reactivation | Love Model → Pioneer | Direct to Pioneer account, shown **only** inside the fee flow | Attestor of receipt |
| Courtesy (10%, voluntary) | Love Model → Pioneer | Direct; always declinable; never auto-added | Attestor |
| Joy | User → Pioneer | Direct, voluntary thanks | Attestor |

**Mandatory UI copy (exact, non-paraphrasable) — "TLC Financial Notice":**

> TLC facilitates **direct peer-to-peer** support. It does not hold, pool, escrow or move money.
> Activation, Reactivation, Courtesy and Joy are sent **directly to the Concept Pioneer's own
> account** and sustain the platform. Supportive Tokens, Growth Grants and Recognition are sent
> **directly to the intended recipient**. TLC Tokens are recognition signals: they have no cash
> value, are not transferable, not redeemable, and are not an investment or cryptocurrency.

Terminology law: *Transfer Method* (not "Payment Method") · *Send Support* (not "Pay") ·
*Attested* (not "Confirmed by bank") unless an adapter actually confirmed it.

---

## 5. Governance & Trust

- **Dyad (I-Partner / Love Model):** two members, one shared space, two signatures for
  irreversible acts. Primary Steward administers structure; Co-Steward holds full visibility and
  a symmetric veto. Either may leave; leaving never destroys the other's records.
- **Delegated Trust (Steward Protocol):** *Account Steward* (personal proxy: verification, tasks)
  and *Community Manager* (regional moderation). Grants are scoped, expiring, signed consents
  (`ConsentGrant` VC). Compensation = direct optional gratuities from users, never a cut of
  transfers, never central pooling. Revocation is instant and offline-propagated.
- **Evidence over opinion:** moderation decisions carry a reason code and an evidence link;
  every decision is appealable in-app.
- **Sunset clause:** any steward power unused for 90 days auto-expires; re-grant is explicit.

---

## 6. Technical Architecture — "one core, many shells"

Hexagonal (ports & adapters). The domain knows nothing about Android, Cloudflare or SvelteKit.

```
apps/     android (Compose) · pwa (SvelteKit) · console (steward) · cli (ops)
core/     domain (roles, dyad, tokens, ledger) · contracts (JSON-LD, VC) · crypto · policy
adapters/ storage(SQLDelight|Room) · sync(PowerSync|none) · rail(manual|provider|escrow)
          evidence(R2) · notify · identity · clock · flags
contracts/ JSON-LD contexts · JSON Schemas · OpenAPI · VC templates
infra/    cloudflare(workers,r2,queues,do) · github-actions · IaC
docs/     CONTEXT.md · SPECS.md · ADRs · threat-model · runbooks
```

**Ports (stable, replaceable):** `IdentityPort · StoragePort · SyncPort · EvidencePort ·
RailPort · NotifyPort · LinkPort · ClockPort · PolicyPort · FlagPort`.
Every port has: a fake (tests), a local adapter (offline), a production adapter. Replaceability is
proven by a conformance test suite per port, not by intention.

**Language allocation (strategic, not decorative):**

| Layer | Choice | Why |
|---|---|---|
| Domain/model/contracts | **Kotlin Multiplatform** | one source of truth across Android/web/CLI; strong typing; mature tooling |
| Android UI | **Kotlin + Compose (M3)** | native feel, accessibility, performance, no WebView tax |
| PWA | **SvelteKit + Vite + Tailwind (stable majors)** | smallest payload, best DX, offline-first ergonomics |
| Crypto / canonicalisation | **C++ via NDK** on audited libs (libsodium/BoringSSL) | constant-time primitives, no hand-rolled crypto |
| Java | **JDK 21 toolchain only** (Gradle/CI) | zero app-level Java; no dual-language drift |
| Edge API | **Cloudflare Workers (TS)** | global latency, cheap, no servers to babysit |
| Native glue (optional) | Rust or C++ behind a port | only where measured need exists |

> Version rule: pin to **latest stable** in a version catalog; upgrade deliberately; a pre-release
> major (e.g. an unreleased SvelteKit major) is allowed only behind a feature branch and never in a
> release tag. "Newest" is not "best" until it is stable.

**Data:** SQLite as the user's own store (OPFS/WASM on web, SQLDelight on Android). PowerSync
carries changes; **Postgres remains the server truth for shared and money-adjacent facts**.
Conflicts: last-writer-wins per column for preferences; **append-only, server-assigned sequence**
for receipts and attestations (clients cannot mint them). JSON-LD/CBOR-LD + W3C VC 2.0 for roles,
consents and receipts so they remain verifiable outside TLC.

---

## 7. Link Doctrine (compressed)

Link meaningfully · share minimally · authenticate independently · authorize explicitly ·
verify outcomes · preserve user control.

- **Identity links** attach a verified credential to a profile, revealing only a proof, not the document.
- **Relationship / Invitation links** are invitations, never authorizations; acceptance is mutual and logged.
- **Consent links** are signed, scoped (`action`, `resource`, `dyad`, `exp`), single-use, revocable;
  possession ≠ authority — the approver must authenticate separately.
- **Evidence / Receipt links** are permission-aware, expiring, watermark-free-of-PII, with an offline fallback page.
- **Continuity links** restore a user's own records to a new device via a user-held key.
- **Service links** are optional, documented, replaceable, and clearly mark "you are leaving TLC".
- Stable IDs (DID/ULID) outlive URLs; every link has a meaningful fallback (web → store → help).
- Never in a URL: bank details, tokens, relationship status, health/pastoral data, full names of third parties.

---

## 8. Observability, Traceability & Accountability (VITAL)

**Objective:** every naira that reaches the Pioneer account is explainable, attributable and
verifiable — without TLC ever holding funds or reading the Pioneer's bank feed by default.

**Pioneer Ledger (append-only, hash-chained):**

```
FeeIntent (device-signed: role, token, amount, period, idempotency-key)
   → Instruction (Pioneer account details released from runtime secret; expires in 15 min)
   → ProofArtifact (user upload → R2 signed PUT → SHA-256 recorded → 90-day retention)
   → Attestation (Pioneer/Steward confirms settlement w/ statement reference; 4-eyes where possible)
   → Receipt (JSON-LD VC 2.0, prev_hash chained, Merkle-leaf)
   → Anchor (daily Merkle root published: signed manifest + optional public timestamp)
```

- **Reconciliation ladder (cheapest first):** manual attestation → bank-statement/CSV import
  (adapter) → provider webhook (adapter, opt-in). Each rung raises confidence; none gives TLC
  debit authority over any account.
- **Three-way match:** `intent amount = proof amount = attestation amount`; mismatch ⇒ exception
  queue with a reason code, never silent correction.
- **Idempotency:** every fee carries a client-generated key; duplicates resolve to one receipt.
- **Non-repudiation:** receipts are signed (Ed25519) and chained; a verifier can validate the whole
  chain offline from an export. Monthly signed statement (PDF + JSON) is one tap.
- **Privacy:** no full PANs stored; counterparty shown as `bank · last4 · hash`. Proofs are
  access-scoped, scanned for malware, and reduced to hashes after 90 days.
- **Operational visibility:** dashboards for obligation → attested → exception → overdue, with
  cohort and period views; alerts on ageing exceptions, chain-verification failures, and
  attestation latency. Nightly CI job re-verifies the chain and the last anchor.
- **Accountability both ways:** an attested receipt is evidence the user paid; the ledger is also
  the Pioneer's auditable record for tax and for a public "platform sustainability" summary
  (aggregates only, never individuals).

---

## 9. Risk Register (v0.0.1)

| ID | Risk | Severity | Mitigation |
|---|---|---|---|
| R-1 | Personal account receiving recurring dues reads as unregistered financial activity | High | CAC/entity registration before public launch; frame as dues/gifts; ToS + receipts; tax filing; no investment language; legal review |
| R-2 | "Token/escrow" vocabulary triggers securities/e-money scrutiny | High | INV-03 notices everywhere; remove "escrow" from product; zero custody; no yield/referral payouts |
| R-3 | Bank-detail substitution fraud (recipient account swapped) | High | Verified-detail badge, change requires dual signature + 24h cool-down + re-notice to both members |
| R-4 | Gender-coded authority challenged | Medium | Refined to Primary/Co-Steward, symmetric veto (§2.3) |
| R-5 | NDPA breach via proof uploads | Medium | Signed URLs, encryption at rest, retention 90d, DSR export/delete, DPIA before beta |
| R-6 | Sync conflicts corrupt money state | High | Server-authoritative append-only attestations; clients cannot mint receipts; conformance tests |
| R-7 | Offline UX lies about payment state | Medium | Explicit state machine + "pending/attested/failed" language; no optimistic "paid" |
| R-8 | Secrets leaked via repo/AI chat | High | Rotation policy, gitleaks in CI, pre-commit hooks, scoped tokens |
| R-9 | Dependency churn (unreleased majors) | Medium | Version catalog + pinned stable + renovate windows |
| R-10 | Founder key-person dependency | Medium | Documented runbooks, signed config, multi-steward attestation option |

---

## 10. Glossary & Decision Log

**Attest** — a human/system assertion that a transfer settled. **Receptive Mode** — opt-in
permission to receive Supportive/Recognition tokens. **Dyad** — two-person shared space.
**Rarity Symbol** — TBD (open question Q-3). **Pioneer** — Concept Pioneer, Collins Christopher.
**Steward** — scoped delegate. **Shell** — a client app over the shared core.

| ADR | Decision | Status |
|---|---|---|
| 001 | No custody anywhere; Attestation replaces escrow | accepted |
| 002 | Kotlin Multiplatform core + Compose Android + SvelteKit PWA | accepted |
| 003 | Manual transfer + proof for v0.0.1; rails behind `RailPort` | accepted |
| 004 | Hash-chained Pioneer Ledger + daily Merkle anchor | accepted |
| 005 | Primary/Co-Steward replaces gender-coded authority | accepted (pending Pioneer confirmation) |
| 006 | PowerSync behind `SyncPort`; Postgres server truth | provisional |
| 007 | Passkey-first auth, device-bound keys | accepted |

**Open questions:** Q-1 CAC entity + fee taxation path. Q-2 Is a licensed escrow partner ever
needed for large Growth Grants? Q-3 Define "Rarity Symbol" (must not imply scarcity economics).
Q-4 iOS timing. Q-5 Moderation appeals SLA.

---

*TLC v0.0.1 · CONTEXT · single source of truth for doctrine. Executable contracts: `SPECS.md`.*
