# TLC — The Love Culture (Community Edition) — CONTEXT v0.0.2

> Doctrine, identity and non-negotiables. This file answers **why** and **what must never change**.
> Executable detail lives in `SPECS.md`. When the two conflict, `CONTEXT.md` wins.

> **Doctrine version 0.0.2 — the online/offline baseline.**
> This file governs both the local-only shell that ships today and the shared plane that follows.
> It is not a claim that every capability below is already shipped. Release identity lives in
> `version.properties`; doctrine identity lives here. The shipped v0.0.6 shell is deliberately
> local-only: no account, no sync, no `INTERNET` permission. The next shell (the v0.0.7 direction)
> adds the shared plane. Both obey this document.
>
> **v0.0.2 makes seven things mandatory.** They are not options, later ideas or marketing copy:
> (1) a distinct **username + word/phrase passphrase** identity that feels seamless; (2) **live,
> instant communication and connection** as a real user experience, not a badge; (3) **presence
> privacy** — the person decides who may see their online/last-seen state, if anyone; (4) **live,
> flexible Transfer Methods** in every token transfer; (5) **TLC as an instrument, never a bank** —
> people move real money between their own accounts and feel the reality of it; (6) **Pioneer's
> Token** language everywhere, with "dues" banned from every user-facing surface; and (7) a
> deliberate, maximised **Pear/Holepunch** peer layer where it adds privacy and resilience without
> becoming the source of shared truth.

> **Nothing here is legal advice.** Multi-currency support, hosted card acquiring, cross-border
> sync and entity/tax structure require jurisdiction-specific legal and regulatory review before
> public launch.

---

## In one page

- TLC is a **Godly, strategic concept powered by love**; it is not a religion, a political
  movement, a commercial business, a bank or an investment scheme.
- **TLC is an instrument, not a bank.** It gives people a clear, trusted way to direct, record and
  prove real transfers between their own accounts. It never becomes the account, the wallet, the
  counterparty or the custodian. Using TLC should feel like *money really moved* — because it did,
  in the person's own bank — while TLC itself holds nothing.
- **TLC never holds money.** People pay people directly; TLC records intent, instruction, proof,
  attestation and receipt. No pooling, float, escrow or wallet balance.
- **Identity is a username and a word/phrase passphrase**, not a phone number and not a password
  database. Sign-in is seamless on a trusted device and recoverable with a printed phrase.
- **Two planes:** the private plane is device-authoritative and works offline forever; the shared
  plane is server-authoritative because two people need one agreed truth.
- **Offline is a designed state.** Private read/write/export always work; shared writes queue
  visibly; money-adjacent settlement never happens offline.
- **"Live" means connected and acknowledged.** Cached, queued, sent, delivered, acknowledged,
  attested and verified are different words and must never be blurred. Live communication is a
  real experience — presence, messaging, calls and shared state — not a pulsing decoration.
- **Presence is a consent setting, not a default.** Who can see you online, when you were last
  seen, and whether you are typing is chosen by you, per circle or per person, and can be
  withdrawn at any time.
- **Transfer Methods are live and flexible.** Bank transfer, hosted card (Pioneer's Token only),
  licensed provider rails and peer-to-peer methods appear where they are genuinely available, with
  their provider, fee, currency, timing and failure states stated before the person commits.
- **Tokens are records of value and recognition**, not currency, crypto, e-money or a balance.
  Declared value · transferable as a record · not redeemable · not an investment.
- **Stewardship is role-based:** Primary/Co-Steward, symmetric veto, scoped, signed, expiring and
  revocable. No "admin panel", no standing superuser, no bulk export of people or bank data.
- **Pioneer's Tokens and gifts go directly to the Pioneer.** No auto-charge, no card-on-file, no
  debt, no access gate; a hardship waiver is always available. **"Dues" is never shown to a user.**
- **Any currency both banks support** may be used; **NGN is always supported** and anchors the fee
  schedule. TLC does not convert, quote a rate or take an FX position.
- **Pear/Holepunch is an optional peer substrate.** Hypercore, Hyperswarm and Pear extend backup,
  device-to-device replication and nearby discovery. They never replace the server's ordering of
  shared facts and never become a requirement for using TLC.
- **One core, many shells:** Android, PWA and CLI share the same domain logic. Export and
  verification work offline. The build tells the truth about connectivity, money state, presence
  and authority; when it cannot, it says so.

---

## 0. Credential Hygiene (read first)

No secret ever enters this repository, an APK, a PWA bundle, a screenshot, a log, or a chat log.
Secrets live only in: GitHub Actions secrets, Cloudflare Worker secrets, the Android Keystore,
and the operator's password manager.

- `.env*`, `*.keystore`, `google-services.json`, signing keys and recovery kits are ignored.
- Any token pasted into a document, issue, screenshot or chat is **burned** and must be rotated
  before use.
- Bank account numbers are *data*, not *config*. They are never committed, compiled in, logged,
  placed in a URL, shown in a screenshot, or included in an export unless the person deliberately
  exports their own vault.
- AI assistants and external tools are untrusted channels. Never paste a secret, a recovery kit,
  a bank number, a private key, or a user's proof image into a prompt.
- Builds are reproducible, signed, checksummed and accompanied by an SBOM. A release without a
  verifiable provenance record is not a release.
- The `foss` channel carries no Play Services, no Firebase, no analytics and no proprietary SDK.
  `verifyFossOnly` fails the build if that changes.

---

## 1. Identity & Doctrine (immutable)

TLC is a **Godly, strategic concept powered by love** to reveal and sustain the identity and impact
of love in society. It is **not** a religion, a political movement, a commercial business, a bank,
or an investment scheme.

"Godly" describes motive and accountability, not a religious institution or a creed test. TLC does
not require a religion, a gender role, a phone number, an email address or a bank account to belong.
It does not proselytise, discriminate or rank people by wealth, status or contribution.

**Mission.** Move love from emotion to a structured, intelligent cultural force:

**Identity Restoration · Love as Leadership · Cultural Intelligence · Sustainable Impact.**

**Operational doctrine.**
Sovereign · non-custodial · modular · agnostic · universal · dignity-preserving ·
**local-first for private facts · remote-first for shared facts · truthful in real time.**

Sovereignty is not isolation. It is the ability to hold your own keys, read your own record, export
it without permission, and leave without losing it. Modularity is not decoration. Every provider is
an adapter behind a port, and every adapter can be replaced without rewriting the doctrine.

### 1.1 Non-negotiables (INV — tested, not aspirational)

| ID | Invariant | Proof in the build |
|---|---|---|
| **INV-01** | TLC never takes custody of funds it does not own. No pooling, no float, no wallet balance, no TLC-operated escrow. | Every `Effect` has `requiresCustody = false`; `EntryKind` has no `BALANCE`, `TRANSFER`, `ESCROW` or `WALLET` member; custody review in every money PR. |
| **INV-02** | Money moves **person → person** through each party's own bank. TLC issues *instructions*, never transfers. | Routing-table tests; no rail adapter can debit, hold or net value; a client cannot mint a settlement. |
| **INV-03** | A Token is a **record of value and recognition**, not money. It may carry a declared value and may be transferred as a record with provenance, but it is never a balance, currency, crypto, e-money, security, deposit, loan or redeemable credit. TLC never buys, sells, redeems or makes a market in it. | Token-nature and valuation tests; conservation tests; vocabulary lint; no `TokenNature` member that implies money or custody. |
| **INV-04** | Every sensitive action needs **independent authentication + explicit authorization**. Possession of a link never grants authority. An offline device may *prepare* an irreversible shared or money-adjacent act but may not *complete* it. A local data wipe is always available offline. | Consent-link possession test; offline-irreversible test; single-use nonce and replay tests; local-wipe test. |
| **INV-05** | User data is user-owned. The **private plane** is local-first and works offline without an account. The **shared plane** is remote-first because two people need one agreed truth. Sync is a service, not a cage. Export is always possible offline. | Offline export test; sync opt-out test; deletion and tombstone tests; no private field required by the server. |
| **INV-06** | No generic "admin". Authority is delegated, scoped, signed, expiring and revocable. | Grant scope/expiry/revocation tests; console route tests; no wildcard scope; no bulk export. |
| **INV-07** | Deterministic, reversible, reproducible builds. No unreviewed pre-release dependency in a release branch. | Pinned version catalog; SBOM; checksums; signature check; tag = version; rollback path. |
| **INV-08** | Money-adjacent facts are **server-attested, append-only**. A client may declare intent; only attestation settles. Offline never settles. | Hash-chain and Merkle tests; client-cannot-mint test; reorder/truncation/torn-write tests; reconciliation tests. |
| **INV-09** | No dark patterns. Consent, fees, currency, destination and consequence are stated in plain language before the action. | Copy review against §5.3 and §5.6; no pre-checked consent; no hidden fee; cancellation as easy as signup. |
| **INV-10** | Privacy by design (NDPA 2023 / GDPR-shaped): minimisation, purpose limitation, retention limits, no bank PANs in plaintext, no secrets in URLs. | DPIA per feature; retention jobs; no-PAN test; no-secret-in-URL test; cache audit. |
| **INV-11** | **Truthful connectivity.** "Live" means connected and acknowledged. `Cached`, `queued`, `sent`, `delivered`, `acknowledged`, `attested` and `verified` are distinct states, always visible in words, never implied by animation or colour alone. | Connectivity-label tests; no "Live" while offline; no "Paid" before attestation; screen-reader live-region tests. |
| **INV-12** | **Offline dignity.** A network failure never blocks reading or exporting your own record, and never converts an intention into a settlement. Shared actions queue visibly; money-adjacent settlement never happens offline. | Airplane-mode end-to-end suite; kill-mid-write test; queue-durability test; no false "sent" test. |
| **INV-13** | **Presence is consented.** Online state, last seen, typing and read state are private by default. The person chooses who may see each one — nobody, selected people, a circle, or everyone — and can change or withdraw it at any time. Presence is never a condition of access, never sold and never inferred from hidden activity. | Presence-permission matrix tests; default-private test; per-contact/circle override tests; reciprocity test; withdrawal-propagation test; no last-seen in API responses without consent. |
| **INV-14** | **Transfer Methods are honest and live.** Every method shown is genuinely available to this person, for this currency, at this moment. Its provider, fee, timing, limits and failure states are stated before commitment. TLC never quietly substitutes a method, never hides a fee and never becomes a party to the transfer. | Method-availability tests; fee-disclosure snapshots; currency/method matrix; stale-quote rejection; provider-outage fallback; no "Payment Method" string. |
| **INV-15** | **Identity is a username and a word/phrase passphrase.** Usernames are unique, human-readable and changeable without losing history. The passphrase is never stored in plaintext, never sent to the server and never logged; the server sees only a verifier. A device key and a printable recovery phrase make sign-in seamless and recoverable. | Unique-handle tests; Argon2id/OPAQUE verifier tests; no-plaintext-passphrase test; offline-unlock test; recovery-drill test; rate-limit/lockout tests. |
| **INV-16** | **The peer layer has no authority.** Pear/Holepunch replication may carry encrypted private data and discovery, but it can never order, settle or authorise a shared or money-adjacent fact. Disabling the peer layer degrades convenience, never correctness. | Peer-off test; peer-forgery test; wrong-key rejection; server-truth test; battery/storage budget; no peer-only settlement. |

A change is conformant only if no INV is violated, port conformance tests pass, the ledger chain
verifies, the accessibility audit passes, and every user-visible money string matches §5.3.

---

## 2. Doctrine Refinements for v0.0.2 (deliberate changes)

These are corrections to the source brief. Each is a decision, not a preference.

**What changed from v0.0.1:** the two-plane model; SEF Token nature; multi-currency with NGN as
anchor; optional hosted card acquiring for Pioneer's Tokens only; truthful-connectivity and
offline-dignity invariants (INV-11, INV-12); **username + word/phrase passphrase identity**
(INV-15); **consented presence and last seen** (INV-13); **live communication as a product
pillar**; **live Transfer Methods** (INV-14); the explicit **"instrument, not a bank"** position;
**Pear/Holepunch as an optional peer substrate** (INV-16); PWA parity; and a language that
separates *cached*, *queued*, *sent*, *attested* and *verified*.

### 2.1 Two planes replace the local-versus-remote argument

The false choice is "local-first or remote-first". The answer is **two planes, one experience**:

- **Private plane — device-authoritative.** Your record, bank vault, drafts, private notes,
  preferences, local receipts and export. It works offline forever and needs no account.
- **Shared plane — server-authoritative.** Account and handle, consent, dyad, token circulation,
  receipts, attestations, steward grants and (where offered) encrypted message envelopes. Two
  people need one agreed truth, so the server orders shared facts.

No shared action is shown as complete until the server acknowledges it. No private action waits for
the network. Promotion from private to shared is an explicit act by the person, never a background
default.

### 2.2 Growth Grants are not escrow

The brief said "escrow" while mandating zero custody — a contradiction and a licensing hazard.
The model is **Commitment + Witness**:

> signed pledge → beneficiary fulfils a milestone → steward verifies evidence → donor transfers
> **directly to the beneficiary's own account** → receipt attested.

TLC holds nothing. The default is direct payment. A licensed third-party escrow may exist only as
an explicitly opted-in, clearly disclosed adapter where the **third party** is custodian; TLC is
never the custodian, never controls the funds and never guarantees the provider. The word "escrow"
may appear only in that provider's own terms, never as a TLC feature.

### 2.3 Tokens are records of value, not signals-only

Keep the names — Supportive Tokens, Growth Tokens, Recognition, Activation Tokens, Reactivation
Tokens, Courtesy Tokens and Joy Tokens — for cultural continuity. The
[Standard Economic Framework (SEF)](docs/SEF.md) refines what a Token *is*:

- A Token is a **record of a meaningful resource, gift, contribution or monetary value**, with an
  author, a reason and a history.
- It may carry a **declared value** in a stated currency for distribution convenience. That value
  is a declaration at a time, not a market price, not a promise of return and not a balance.
- It may be **transferred as a record** with provenance and the authority the dyad rules require.
- It may be **distributed in monetary form** bank-to-bank, directly between the people involved.
- It is **not redeemable by TLC**, not a security, not a deposit, not e-money, not crypto, not a
  loan and not a promise of profit. TLC operates no market, order book, exchange, redemption
  window or secondary trading venue.

Every surface must state the nature plainly. The short label is:

> **Declared value · Transferable as a record · Not redeemable · Not an investment · Not crypto ·
> TLC holds nothing.**

"Transferable" means the record of a claim can move to another person with its history and the
required authority. It does not mean a tradable instrument, a cash-out right or a financial claim
against TLC.

### 2.4 Complementary Stewardship is role-based, not gender-based

Keep the two invariants — (a) structural authority, (b) full visibility + veto — but assign them as
**Primary Steward** and **Co-Steward**, chosen by the couple. Gender, if declared, is descriptive
only. Veto is **symmetric** on irreversible acts (bank-detail change, dyad dissolution, delegation
grant, account deletion). Either steward may leave; leaving never destroys the other's records.
No role is earned by gender, seniority, wealth or contribution.

### 2.5 Pioneer's Tokens are membership contributions and voluntary gifts

"Pioneer's Token" is the umbrella for membership contributions and voluntary gifts, not a purchased
financial instrument. The schedule is:

| Token | Amount | Nature |
|---|---|---|
| **Activation Token** | ₦1,000, one-off | Membership contribution |
| **Reactivation Token** | ₦2,000 / month | Membership contribution |
| **Courtesy Token** | 10% of a Pioneer's Token transfer, optional | Voluntary gratuity; never auto-added; always declinable |
| **Joy Token** | Any amount, optional | Voluntary gift |

Pioneer's Tokens and gifts are sent **directly to the Concept Pioneer's own account**. They sustain
the platform. They are not a condition for reading, exporting or deleting your own record. A
hardship waiver is always available and never creates debt, penalty or a public mark. Reactivation
is never auto-charged: the person initiates each month's transfer. There is no stored mandate, no
silent renewal and no card-on-file. Marketing, ToS and UI language must avoid: *invest, yield,
return, profit-share, referral commission, escrow, wallet, balance, cash-out, guaranteed*. The word
**"dues" is banned from every user-facing surface**; say **Pioneer's Token**, **membership
contribution** or **gift** instead.

### 2.6 No "admin panel"

There is a **Steward Console**: scoped views, per-case grants, every read and write audited, no
bulk export of people or bank data, no "view as", no impersonation, no standing superuser. Console
writes are online-only. Cached reads, where offered, carry a visible watermark and an audit record.
A scoped grant expires automatically after 90 days of non-use.

### 2.7 Optional card Transfer Method in v0.0.2

Bank transfer + attested proof + optional hosted card only.

- Card acquiring is **off by default** and exists only as a hosted checkout **Transfer Method**.
- Card data never touches TLC: no PAN, no CVV, no card token stored, logged, exported or shown.
- Card is used **only for Pioneer's Tokens and gifts paid to the Concept Pioneer's own merchant
  account**, never to collect or route money for another member. Peer support remains direct bank
  transfer.
- Provider and card-webhooks are adapters, switched off by default, verified by signature and
  idempotency, and never used to mark a peer transfer as settled.
- Enabling card acquiring requires PCI scope review (SAQ A or equivalent), acquirer terms,
  chargeback policy, tax treatment and a jurisdiction-specific legal review.

### 2.8 PWA is not second-class

One core, many shells: **Android (primary on mobile), PWA (universal), CLI (ops)**. Same domain
code; no forked business logic. The PWA must work offline after first load, install, export and
verify at parity with Android for every flow in Appendix A. "PWA later" is not an excuse for a
weaker offline story.

### 2.9 Multi-currency, with Naira as the anchor

Community transfers may be made in **any ISO 4217 currency both parties' banks support**, chosen by
the sender. **NGN is always supported** and is the default for the Pioneer schedule.

- Money is stored as integer minor units plus an ISO 4217 currency code. Never `Double`, never a
  bare number, never an implicit currency.
- TLC does not convert currency, quote a rate, hold a float or take an FX position. Any rate is
  the bank's rate, shown by the bank, at the time of the transfer.
- Pioneer's Tokens are denominated in NGN. If the operator accepts another currency, the instruction
  states the accepted amount, the Transfer Method and the FX reference in writing; the receipt
  records both. No silent conversion.
- A receipt records the currency and amount actually stated. Reconciliation matches like currency
  and amount; a mismatch is an exception with a reason code, never a silent correction.
- No cryptocurrency, no stablecoin, no parallel-market rate, no "USDT equivalent".

### 2.10 Traceability is a product feature, not an afterthought

See §9. Every obligation has an intent, an instruction, a proof, an attestation and a receipt. The
whole chain is verifiable **offline** from an export and **online** against the daily anchor. A
person can answer *what happened, when, and what is next* without asking a steward.

### 2.11 Real time is acknowledged truth, not animation

Real time means the shared plane converges quickly and both sides see the same truth. It does not
mean optimistic UI, fake live badges or instant settlement. Online, the target is p95 ≤ 5 seconds
for a shared state change. Offline, the app responds immediately, queues durably, and catches up
without duplicates when the network returns.

### 2.12 No "zero-server" and no "metadata-free" absolutes

TLC is **server-minimal** and **metadata-minimised**, not server-free or metadata-free. The relay
sees what any relay must see: account identifiers, timing, sizes and IP. It never sees private
record contents, bank credentials, plaintext messages or proof images it does not need. The privacy
notice says so plainly.

### 2.13 TLC is an instrument, not a bank

This is the sentence every surface must make true:

> **TLC is an instrument for directing and proving real transfers between people's own accounts.
> It is not a bank, not a wallet, not a payment institution and not a custodian.**

An instrument is a means: a clear instruction, a shared reference, a proof, an attestation and a
receipt. The **bank is where the money lives**; the **person is the payer**; the **recipient is the
payee**; TLC is the trusted instrument panel and the record of what happened. A person should feel
that money genuinely moved, because it moved in their bank and both sides can see the evidence —
not because TLC displayed a number that looked like a balance.

Therefore:

- TLC never has an account that can receive, hold, delay, net, redirect or release a user's money.
- TLC never shows a "balance", "wallet", "top up", "withdraw funds", "deposit funds" or "available
  funds". (Withdrawing a *pledge* is an ordinary, honest verb and is allowed.)
- TLC never says "we will send it", "we received your money" or "your payment is safe with us".
- TLC never becomes the counterparty, the merchant of record for peer transfers, or the guarantor
  of a bank's action. Hosted card acquiring, where enabled, is a Pioneer's Token method provided by
  a licensed acquirer to the Pioneer's own merchant account — never a TLC wallet.
- A transfer is real when the person's bank says so. TLC shows **Instructed → Proof added →
  Attested → Receipt issued → Verified** and never upgrades a claim into a bank confirmation.
- The person can always leave with their record and verify it without TLC's servers.

The plain-language notice is in §5.3. The distinction is also a design constraint: if a proposed
feature only makes sense when TLC holds or controls money, the feature is out of scope.

### 2.14 Identity: a distinct username and a word/phrase passphrase

The front door is deliberately simple and human:

- **Username.** A unique, human-readable handle such as `@ada`. It is not an email address, not a
  phone number and not a public display name. Uniqueness is server-enforced; reserved and
  impersonation-prone handles are blocked. A changed handle leaves a signed forwarding tombstone
  for 30 days so people are not silently impersonated.
- **Word/phrase passphrase.** The person chooses a memorable phrase of several words (minimum four
  words or equivalent entropy; the UI explains strength without shaming). It is called a
  **passphrase**, never a "password" in user-facing copy. It is never stored in plaintext, never
  sent to the server and never written to a log. Authentication uses a password-authenticated key
  exchange (OPAQUE or an audited SRP-class protocol) so the server holds only a verifier; an
  Argon2id verifier is the minimum fallback where PAKE is unavailable.
- **Seamless after the first time.** A device-bound key in Android Keystore/StrongBox, released only
  after the word/phrase passphrase is verified, keeps the shared session signed in. The passphrase
  is requested on a new device, after a long absence, after a lock, for high-risk actions, or when
  the person chooses to re-authenticate. Sign-in must feel like opening the app, not filing a form.
- **Recovery phrase.** A high-entropy recovery phrase (12–24 words) plus a second trusted device is
  the baseline. It is shown once, printable and never uploaded. SMS or email alone is never
  sufficient to recover an account. Social recovery is out of scope until a reviewed design
  exists.
- **No biometrics, no passkeys, ever.** TLC never shows a fingerprint prompt, a face prompt, a
  passkey sheet or a device-credential shortcut. The passphrase is the only door, in every build
  and on every platform. This is enforced as a product rule, not a user setting.
- **No security questions, no knowledge-based recovery, no secret in a URL.** Rate limits,
  progressive delays and device notices protect the handle. A failed sign-in never reveals whether
  a username exists beyond the minimum needed to avoid enumeration.
- **Offline.** The private plane remains usable without an account. An existing device can unlock
  and work offline; a new device needs a connection for the first shared-plane sign-in. The app
  says which is happening.

The username is how people find and address each other; the display name is how they are seen; the
passphrase is how the account is protected. Keeping these three separate prevents the common
mistake of treating a public handle as a secret.

### 2.15 Presence is a consent setting, not a default

People must be able to choose **who can see them online, when they were last seen, and whether
they are typing or have read a message**. This is a first-class product surface, not a hidden
privacy toggle.

- **Private by default.** A new account shows no online state, no last seen, no typing and no read
  receipts to anyone until the person opts in.
- **Granular audience.** For each signal (online now, last seen, typing, read receipts) the person
  chooses: **Nobody · Selected people · A circle · Everyone using TLC**. A per-person exception can
  widen or narrow the circle. The UI shows a plain-language preview: "Ada will see: online now;
  not last seen."
- **Time and mode.** Optional schedules ("share while I am in the app", "hide after 22:00"),
  temporary invisibility, and a one-tap **Go invisible** that takes effect immediately on every
  device.
- **Reciprocity, not coercion.** A person may require reciprocity ("you can see mine only if I can
  see yours"), but no one is ever forced to share to use TLC, message, transfer or receive. A
  blocked or declined person sees nothing.
- **Minimum precision.** Last seen is coarse by default ("today", "this week"), with an optional
  exact time. Presence history is not stored beyond what the chosen feature needs.
- **Truthful offline state.** If the app is closed and the person shares last seen, the label is
  "last seen <when>"; if they share online state, it reads "offline" — never a fake green dot. If
  the person has hidden presence, the other side sees a neutral "presence not shared" — never an
  inferred guess.
- **Withdrawal is immediate.** Turning a signal off removes it from the shared projection within
  the real-time target, clears cached presence on other clients, and is auditable by the person.
- **No sale, no inference, no shadow profile.** Presence is never used for ads, ranking,
  recommendations, streak pressure or "your friend is waiting". No third party receives it.

Presence applies to the shared plane only; the private plane never reports activity anywhere.

### 2.16 Live communication is a real user experience

TLC must feel alive between people, not like a form that occasionally syncs. Live communication
means:

- **Instant 1:1 messaging** with end-to-end encryption (MLS or an audited equivalent), delivery
  states that never pretend "read", offline queueing, and push that carries no content.
- **Voice and video calls** where the platform supports them: consent-gated, E2EE media
  (DTLS-SRTP), direct peer-to-peer first, TURN only when the network demands it. A call request is
  a request; it is never auto-answered.
- **Live shared state.** When one person records support, a Pioneer's Token, a grant milestone or a
  receipt, the other side sees the same state change within the real-time budget, with the same
  truth label. This is the "both sides of the token" promise made visible.
- **Live connection and networking with consent.** People can discover, request, accept, decline,
  block and report. Discovery is off by default; a request carries a purpose; acceptance is mutual;
  revocation stops new contact immediately. There is no public "who viewed you", no contact-graph
  export and no unsolicited mass messaging.
- **Nearby and peer-to-peer where it helps.** When two people are on the same network, an optional
  peer path may carry encrypted messages or a transfer handoff faster than the relay. The relay
  remains the durable mailbox and the server remains the order of shared facts.
- **Truthful states.** `Sending · Sent · Delivered · Read (only if shared) · Failed (reason)`.
  "Live" describes the connection, not a person's attention. A typing indicator appears only if
  the sender has opted in.
- **Offline dignity.** Messages compose and queue; calls are unavailable and say so; a transfer
  can be prepared but never settled. When the network returns, the outbox flushes in order with
  idempotency keys and no duplicate messages.
- **Safety.** Rate limits, per-person message requests, instant block/report, and local abuse
  signals. Because content is E2EE, moderation acts on reports and voluntary excerpts, never on
  silent server-side scanning.

### 2.17 Transfer Methods are live, flexible and named honestly

**"Payment Method" is banned. The term is "Transfer Method".** A Transfer Method is the real-world
route by which value moves between two accounts. It is chosen inside the transfer flow, never
buried in settings, and it must be live.

| Method | Where it applies | Who moves the money | Notes |
|---|---|---|---|
| **Bank transfer (manual)** | All peer support, grants, Pioneer's Tokens | The payer, in their own bank | Default; NGN always; any currency both banks support |
| **Hosted card checkout** | Pioneer's Tokens and gifts only | Licensed acquirer → Pioneer's merchant account | Off by default; no PAN/CVV in TLC; never peer-to-peer |
| **Licensed provider rail** | Where a reviewed provider supports the corridor | The provider, as the regulated entity | Adapter behind `TransferMethodPort`; user sees provider terms |
| **Mobile money / USSD / wallet provider** | Only where a licensed provider supports it and both parties can receive | The provider | Optional; never a TLC balance; fees and limits shown |
| **Peer handoff (Pear/Holepunch)** | Optional device-to-device delivery of an encrypted instruction or receipt | The payer's bank still moves the money | A convenience channel, never a settlement rail |

Every transfer screen MUST show, before commitment:

1. **Method name and provider** — "Bank transfer · your bank", not a generic "Pay".
2. **Amount and currency** — integer minor units, ISO 4217, no bare number.
3. **Fee** — amount, who pays it, and whether it is charged by TLC, the bank or the provider.
4. **Timing** — "usually instant", "within 1 business day", or the provider's stated window.
5. **Availability** — live status; a method that is down is disabled with a reason and an
   alternative, never a dead end.
6. **Destination** — recipient-supplied and verified, or the Pioneer's signed runtime destination
   inside the Pioneer's Token flow only.
7. **What happens next** — instruction, proof, attestation, receipt; and what TLC does *not* do.
8. **Failure path** — cancel, retry safely, or record an exception. TLC never debits, reverses or
   retries a bank transfer on its own.

Flexibility rules:

- The person can **switch method** before commitment without losing the amount, purpose or note.
- The last-used method may be suggested but is **never silently pre-selected** for a money action.
- A **stale quote or stale availability** is rejected. Method availability and fees have a short
  TTL (≤ 2 minutes for a live quote; ≤ 24 hours for a cached destination, and never for a new
  instruction).
- If a provider fails, the app says which provider failed, preserves the draft and offers another
  available method. It never reports "sent" when the provider did not accept.
- A method that requires a licensed provider shows that provider's terms and the fact that the
  provider, not TLC, is the regulated entity.
- No method may create a TLC balance, a stored value account, a line of credit or a promise to pay
  later.

### 2.18 Pear/Holepunch is an optional peer substrate, deliberately maximised

TLC takes **Pear/Holepunch** seriously without making it a single point of failure. The useful
pieces are:

- **Hypercore** — signed, append-only logs for encrypted private-record replication, receipts and
  audit trails between a person's own devices.
- **Hyperdrive / Hyperblobs** — content-addressed storage for encrypted evidence and media when a
  peer path is better than a central upload.
- **Hyperswarm / HyperDHT** — peer discovery for a person's devices, a nearby friend, or a
  community node, with rotating keys and no public contact graph.
- **Autobase** — multi-writer private logs where a person's devices legitimately write to the same
  private record.
- **Bare / Pear** ([holepunch.to](https://holepunch.to)) — the Holepunch application runtime and its
  small JavaScript runtime, a candidate for running the peer stack on mobile and desktop with a
  small footprint.

Where it is maximised:

1. **Private-plane backup and restore** without a server, end-to-end encrypted, resumable and
   verifiable offline.
2. **Device-to-device continuity** — a new phone can receive the encrypted private plane directly
   from an old phone on the same network.
3. **Community resilience** — a local node can replicate encrypted receipts and public anchors so
   a community can verify records during a relay outage.
4. **Nearby handoff** — an encrypted transfer instruction or receipt can move directly between two
   people, then reconcile with the server when online.
5. **Evidence availability** — a proof or receipt can be fetched from a peer when the central store
   is slow, with the same hash and signature check.

Where it must never be used:

- It never orders shared facts, mints receipts, attests a transfer or resolves a money conflict.
- It never stores plaintext private data on a peer without the owner's explicit, per-device opt-in.
- It never becomes mandatory for sign-up, sign-in, messaging, transfers or export.
- It never weakens the server's append-only record or the "server-authoritative shared plane" rule.

Android viability is a **time-boxed go/no-go**, not an assumption. The preferred order is: (a) a
native Kotlin peer adapter behind `PeerPort` if the protocol can be implemented safely; (b) an
embedded Bare runtime if startup, battery, APK size and store policy pass; (c) a companion desktop
node if neither is acceptable. Until the gate passes, the feature is marked **experimental**, off
by default, and absent from the critical path. Success is measured by restore time, battery per
hour, APK delta, crash-free sessions and a peer-off equivalence test.

---

## 3. Online · Offline · Real-time Doctrine

> **The rule:** the network is an enhancement, never a gate to your own record. Shared truth is
> server-acknowledged, never guessed. Offline is a designed state, not an error page.

### 3.1 The promise

A person can open TLC in a tunnel and still read, write, search, export and verify everything that
belongs to them. A person on a good connection sees the other side of a shared fact within seconds.
A person moving between the two never loses a draft, never sees a false "sent", and never has to
guess whether the screen is telling the truth.

### 3.2 The two planes

| | **Private plane** | **Shared plane** |
|---|---|---|
| **Truth owner** | This device | The server |
| **Examples** | record, bank vault, drafts, private notes, preferences, local receipts, export | account/handle, consent, dyad, token circulation, attestations, receipts, steward grants, message envelopes |
| **Offline** | Full read/write/export, forever | Cached read; writes queue visibly; no settlement |
| **Ordering** | Device sequence | Per-aggregate server sequence |
| **Conflict rule** | Last-writer-wins per field; keep a conflict copy | Append-only; both-signatures for relationship acts; no client merge of money |
| **Account needed** | No | Yes, except local guest |
| **Promotion** | Explicit: the person chooses to share | Becomes a new shared fact; never rewrites the private original |

**Boundary rule.** A private fact is not "synced" by default. A shared fact is not editable by
rewriting history. Correcting a shared fact appends a correction event; it never deletes the past.

### 3.3 Connectivity states

Every shell exposes one state, in words, at all times. Colour is never the only signal.

| State | Meaning | Reads | Writes | Money-adjacent | Visible copy |
|---|---|---|---|---|---|
| `LOCAL_ONLY` | No account, or signed-out guest. Private plane only. | Local | Local private only | Prepare an intent; no instruction, no settlement | "On this device" |
| `LIVE` | Authenticated, stream healthy, last pull fresh | Local + shared cache | Private local + shared push | Allowed by policy, still server-acknowledged | "Live · updated just now" |
| `SYNCING` | Connected, catching up | Local; shared may be stale | Private; shared queues | No new money until pull completes | "Syncing…" |
| `DEGRADED` | Online but slow, partial or push failing | Local + cached | Private; shared may queue | No new settlement | "Slow connection · updated 4m ago" |
| `OFFLINE` | No usable network | Local | Private; shared queues | Prepare only; no instruction, no settlement | "Offline — your record still works" |
| `CONFLICT` | Divergence needs a person | Local + both versions | Blocked for the affected item | Blocked | "Needs your decision" |
| `READ_ONLY` | Chain tamper, storage failure, schema too new | Local | None | None | "Read-only: <reason>" |
| `LOCKED` | App lock or passphrase required | None beyond lock screen | None | None | "Unlock to continue" |

Rules:

- `LIVE` is never shown from a cached page. If the stream drops, the label changes at once.
- A state change is announced through an accessibility live region, not only by colour or motion.
- `CONFLICT` and `READ_ONLY` are actionable. They name the item, the reason and the next step.
- The app may be `OFFLINE` and still fully usable for the private plane. That is success, not a
  degraded mode.
- No modal blocks the private record on connectivity loss. A persistent, dismissible banner is
  enough; irreversible actions may open an explanatory sheet.

### 3.4 Truth ladder (shared facts)

```
DRAFT → QUEUED → SENT → DELIVERED → ACKNOWLEDGED → ATTESTED → RECEIPT_ISSUED → VERIFIED
```

| State | What it means | What it does **not** mean |
|---|---|---|
| `DRAFT` | On this device only | That anyone else can see it |
| `QUEUED` | Accepted locally, waiting for network | That the server has it |
| `SENT` | Server accepted and assigned a sequence | That the other person received it |
| `DELIVERED` | Recipient relay accepted the envelope | That the person opened or read it |
| `ACKNOWLEDGED` | A person says they received it | That a bank confirmed anything |
| `ATTESTED` | An authorised human/system matched evidence | "Confirmed by bank" unless a rail adapter actually did |
| `RECEIPT_ISSUED` | A signed receipt exists | That the chain has been re-verified now |
| `VERIFIED` | Cryptographic chain and anchor check passed | That the underlying bank statement is infallible |

**Hard rules.** No "Paid" badge. Use `ATTESTED` + `RECEIPT_ISSUED`. No "Delivered" rendered as
"Read". No "Live" while offline. No animation that implies a state the server has not acknowledged.

Private-plane facts use a shorter ladder: `DRAFT → RECORDED → EXPORTED`. `RECORDED` means durably
written to this device; `EXPORTED` means the person chose to take it out.

### 3.5 Real-time contract

- **Transport.** WSS/SSE primary through the relay; push notifications are best-effort and never
  required for correctness; foreground polling is the fallback. A dropped stream resumes from the
  last event cursor, not from "now".
- **Events.** Every shared event carries `event_id`, `aggregate_id`, `aggregate_seq`, `server_time`,
  `schema_version`, `causation_id` and `correlation_id`. Clients are idempotent and buffer
  out-of-order events by `aggregate_seq`; a gap triggers a pull.
- **Targets.** Shared state visible to both sides: p50 ≤ 1.5 s, p95 ≤ 5 s on a normal 4G
  connection. Reconnect catch-up: p95 ≤ 3 s for a 7-day delta. Offline local read: ≤ 300 ms.
  Local interaction feedback: ≤ 100 ms.
- **Presence.** Presence is a per-person consent setting, not a default (§2.15). Online state, last
  seen, typing and read receipts are private until the person chooses an audience; every signal is
  independently controllable; withdrawal takes effect across devices within the real-time budget.
  "Live" describes the connection, never surveillance, and a hidden presence is shown as "not
  shared", never guessed.
- **Live communication.** Text, voice/video and live shared token state are part of the real-time
  contract (§2.16). A message may be direct peer-to-peer when possible, but the relay mailbox is
  the durable path and the server orders shared facts. Delivery, read and typing states obey the
  presence settings above.
- **Push.** Opaque wake payload only: no amount, no name, no bank detail, no message text. FOSS uses
  UnifiedPush/ntfy where available; FCM is optional in `full`; neither is required to use TLC.
- **No fake real time.** A pulsing dot, counter animation or "Live" chip must be driven by an actual
  state change. When the socket is down, the animation stops and the label says so.
- **Correctness over immediacy.** If the stream and poll both fail, the app shows `OFFLINE` and
  keeps the queue. It does not pretend to be live.

### 3.6 Offline contract

- **Durable outbox.** Every shared write is written to the local database and the outbox in one
  transaction before the UI says "Queued". It survives process death, reboot and low storage.
- **Visible queue.** The person can see what is queued, why, when it was last attempted, and what
  will happen next. A queued item can be cancelled while it is still a draft.
- **Per-aggregate order.** Outbox items for the same aggregate are sent in order. A failed item
  blocks only that aggregate, not the whole queue.
- **Idempotency.** Every shared write carries a client-generated idempotency key. A retry after a
  timeout resolves to one server fact, not two. Duplicate delivery is harmless.
- **Media.** Capture locally, strip metadata, downscale, hash, encrypt, then queue the upload.
  Uploads are resumable. A partial upload is never shown as a complete proof.
- **Private first.** Reading and writing the private record never waits for a network call. The
  app opens from local storage and paints before any sync starts.
- **Irreversible acts.** An offline device may prepare a proposal, draft, veto or shared-account
  deletion request, but may not complete it. Completion requires an online challenge, independent
  authentication and a server nonce. A local data wipe is always available offline. The UI says
  exactly that.
- **Veto.** An offline veto is queued and clearly labelled "not yet in force". It takes effect when
  the server receives it. The app never implies a veto is active before acknowledgement.
- **Money.** No offline instruction, no offline attestation, no offline receipt minting, no offline
  bank-detail change, no offline delegation grant, no offline method quote, no offline settlement.
  An intent may be drafted; a proof may be captured; that is all.
- **Links.** A link may explain itself offline, but authority is never exercised offline. Consent
  redemption, dyad acceptance and bank-detail change require a connection. The fallback page says
  why and preserves the link.
- **No silent retry of a transfer.** TLC never initiates a bank transfer. It also never retries an
  instruction or proof in a way that could duplicate a real-world payment.

### 3.7 Sync and conflict rules

| Field class | Examples | Rule |
|---|---|---|
| Preference | theme, motion, data saver | Last-writer-wins per field, `updated_at` + device tiebreak |
| Private text | notes, drafts, local contact log | Last-writer-wins; keep a conflict copy on different edits |
| Shared profile | display name, photo, bio | Server sequence; last accepted wins; user can resubmit |
| Relationship | dyad, consent, veto | Both signatures; no LWW; revocation wins |
| Money-adjacent | intent, proof, attestation, receipt | Append-only server sequence; client may declare intent only |
| Media | profile photo, receipt image | Content hash dedupes identical bytes; a different hash is a new version |
| Consent/grant | steward grant, delegation | Signed, scoped, expiring; revocation wins; no offline extension |
| Deletion | account, receipt, message | Tombstone wins; retention exceptions for attested receipts remain |

**Reconnect order** (do not improvise):

1. Verify the device key and refresh the short-lived session.
2. Fetch signed config; verify signature and TTL; keep the last known good config if fetch fails.
3. Pull shared deltas from the last cursor.
4. Verify the ledger chain and the latest anchor for any money-adjacent facts.
5. Push outbox items in per-aggregate order, with idempotency keys.
6. Resolve or flag conflicts; never merge money client-side.
7. Update freshness labels and the connectivity state.
8. Resume the stream from the last event cursor.

**Clock rules.** Server time orders shared and money-adjacent facts. Device time is for display
only. The client maintains a server-time offset; if skew exceeds 5 minutes, it shows a warning and
blocks new money-adjacent actions until corrected. Hybrid logical clocks may hint at causality but
never order money.

### 3.8 Freshness and cache honesty

Every shared fact carries `server_time` and a local `received_at`. The UI shows freshness in words:

| Label | Meaning |
|---|---|
| `Live` | Connected, stream healthy, last event ≤ 10 s ago |
| `Updated just now` | Fetched or evented ≤ 60 s ago |
| `Updated 12m ago` | Cached; name the age |
| `Stale` | Older than 15 minutes; actions that depend on it may be limited |
| `Expired` | Older than the allowed window; re-fetch before use |

- Bank destination instructions expire after **15 minutes**. A cached destination older than
  **24 hours** is never used. If offline, the app cannot issue a new instruction.
- Signed config has a TTL. If it expires, the app keeps the last known good values for display but
  blocks new money-adjacent actions until a fresh signed config arrives.
- Service-worker caches never store bank details, attestation responses or private API payloads.
  Sensitive data lives in memory or an encrypted local store with a TTL.
- "Updated … ago" is computed from server time plus offset, not from a device clock the user can
  change.
- A cache is cleared on sign-out, device revoke and "clear local data". A shared device never shows
  the previous person's cached amounts.

### 3.9 Low bandwidth and device reality

TLC is built for Nigeria and for everywhere that is not a fibre office.

- **Payloads.** Compact JSON/CBOR, gzip/brotli, delta patches, field-level updates, batched events.
- **Media.** Thumbnails first; no autoplay; resumable uploads; Wi-Fi-only media option; data-saver
  mode that suppresses non-essential images and sync.
- **Battery.** WorkManager with network and battery constraints; coalesced sync; no busy loop; no
  background location; foreground sync only when the person is in the app or explicitly waiting.
- **Storage.** Warn before the device is full; stop writes cleanly; keep the record readable; offer
  export. A full disk never produces a half-written fact.
- **PWA.** Service worker: cache-first for versioned assets, network-first with stale-while-
  revalidate for shared data; offline route; OPFS/SQLite WASM for local data; background sync where
  the platform supports it, foreground queue otherwise; update prompt with a versioned cache;
  install prompt only after meaningful engagement, never on first load.
- **Android.** SQLDelight is the local store; WorkManager handles retries; no `INTERNET` permission
  in the current FOSS build. The online build adds `INTERNET`, `ACCESS_NETWORK_STATE` and
  `POST_NOTIFICATIONS`, plus `RECORD_AUDIO` and `CAMERA` **only** when the person enables voice or
  video calls; each is requested at the moment of use, explained in plain language, and recorded in
  the privacy notice. A network-call audit and a permission-purpose test run in CI.
- **CLI.** Ops can verify a ledger export and inspect a queue offline. No bulk export of people or
  bank data.

### 3.10 Multi-device and continuity

- One identity, many devices. Each device has its own key in the Android Keystore/StrongBox or the
  platform secure store. A device can be listed and revoked.
- The **private plane** does not sync by default. Optional encrypted backup or peer replication is
  opt-in, end-to-end encrypted, and restorable without the server.
- The **shared plane** syncs across devices through the server sequence. No device is a master.
- A continuity link restores a person's own records to a new device using a user-held key. It is
  not a server-side authority and it never grants access to another person's data.
- A lost device: revoke its key, keep the shared record, restore the private plane from backup if
  one exists. The UI states honestly what is recoverable and what is not.
- Conflicting edits on two devices follow §3.7. Money facts never merge; the server sequence wins
  and a correction is a new event.

### 3.11 Security in both states

- TLS 1.3 in transit; certificate pinning with a rotation plan; signed, pinned remote config.
- Short-lived session tokens (≤ 15 min), single-use nonces, replay protection, device-bound keys.
- Local database encrypted at rest; bank vault AES-256-GCM under a Keystore key; no plaintext PAN
  in state, record, backup, log, notification or export.
- Sensitive screens block screenshots where the platform allows it; clipboard entries clear after
  a short window; no bank detail in a notification.
- The offline cache is encrypted and scoped to the signed-in device. Sign-out and device revoke
  clear it. A rooted device is a known risk, stated in the security policy.
- Revocation lists have a TTL. If the list is too stale for a sensitive action, the action fails
  closed rather than assuming authority.
- No analytics on amounts, names, bank details or message content. Crash reports are opt-in and
  PII-scrubbed. `foss` has no telemetry at all.

### 3.12 Failure modes (designed, not discovered)

| Situation | Behaviour |
|---|---|
| Airplane mode | Full private read/write/export; shared drafts queue; no false "sent"; no settlement |
| Captive portal | Detect and say "Sign in to the network"; do not treat as `LIVE`; back off retries |
| Flaky 3G | `DEGRADED`; exponential backoff with jitter; resumable uploads; queue preserved |
| Clock skew > 5 min | Warn; use server offset; block new money-adjacent actions until corrected |
| Duplicate event | Dedupe by `event_id` / idempotency key; one receipt, not two |
| Out-of-order event | Buffer by `aggregate_seq`; gap triggers a pull; no guessed order |
| Server 5xx | Queue; retry with backoff; circuit breaker; never lose the draft |
| Expired session | Re-authenticate; keep the draft; no silent logout that loses work |
| Revoked grant while offline | Fail closed on next check; local read remains; explain the revocation |
| Account deletion while offline | Queue the request; confirm online; explain the 30-day backup window |
| Interrupted media upload | Resumable; hash intact; partial bytes never shown as a proof |
| Storage full | Stop writes cleanly; read-only; offer export; never a torn fact |
| Chain tamper | `READ_ONLY`; preserve evidence; recovery path; no silent repair |
| App killed mid-write | Durable write before acknowledgement; drop only the incomplete line |
| Same shared action on two devices | Conflict; show both; server sequence wins; correction is a new event |
| Link opened offline | Explain; no authority; preserve the link; retry when online |
| Relay outage | `DEGRADED`/`OFFLINE`; private plane fully usable; public status page; no data loss |
| Push unavailable | Poll on foreground; correctness never depends on push |
| Certificate/config rotation | Signed rotation history; last known good; fail closed for money if expired |
| Transfer Method unavailable | Disable it with the provider's reason; offer another live method; keep the draft; never substitute silently |
| Transfer Method quote expired | Re-quote before commitment; if offline, explain and keep the draft; never use a stale fee |
| Presence withdrawn | Remove from other clients within the real-time target; show "not shared"; do not infer |
| Call fails | Name the stage (request, connect, media); no auto-redial; no fake "missed" if the person declined |
| Passphrase forgotten | Recovery phrase + second device; rate-limited; no security questions; preserve the private record |
| Username disputed | Freeze the handle on a credible report; reason code; appeal; account record untouched |

### 3.13 Surfaces and copy

The following states must exist on every screen that can be shared, and must be reachable by
screen reader and large type:

- **Connectivity banner:** one line, persistent while not `LIVE`, dismissible only when it is not
  an actionable conflict.
- **Per-item badge:** `Draft`, `Queued`, `Sent`, `Delivered`, `Acknowledged`, `Attested`,
  `Receipt issued`, `Verified`, `Needs attention`.
- **Freshness line:** "Updated 12m ago" or "Live · updated just now".
- **Next honest action:** one sentence, never a dead end. "Queued — will send when you're online."
- **Money notice:** the exact §5.3 notice before any transfer instruction.
- **Transfer Method line:** "Bank transfer · your bank · fee ₦0 · usually instant" or the
  provider's real terms. The method, provider, fee, timing and availability are never hidden.
- **Presence line:** "Presence not shared", "Online now · visible to your Circle", or "Last seen
  today · visible to Ada". The person can open the control from the line itself.
- **Passphrase line:** "Unlock with your face" on a trusted device; "Enter your passphrase" only
  when the risk or device genuinely requires it. Never a password meter, never a security question.
- **Live communication line:** "Sending…", "Sent — not yet delivered", "Delivered — not read",
  "Read — they share read receipts", "Call failed — network". Never a fake "online" or "typing".
- **Offline promise:** "Your record still works. Nothing has been sent."
- **Instrument promise:** "The transfer happens in your bank. TLC holds nothing; it records the
  instruction and the receipt."
- **Irreversible explanation:** "This changes something shared, so it needs a connection and a
  fresh sign-in."
- **Veto state:** "Veto queued — not yet in force" until the server acknowledges.
- **No fake urgency:** no countdown that pressures a transfer, no "last chance", no pre-checked
  consent, no hidden fee, no "are you sure you want to leave?" maze.

### 3.14 Definition of done for any online/offline feature

A feature is not done until all of these are true:

1. It has an entry in the Appendix A capability matrix.
2. Airplane mode: the private path works; the shared path queues; no false success.
3. Reconnect: the queue flushes in order; duplicates resolve to one fact; no lost draft.
4. Kill mid-write: the app reopens with a consistent state and a clear next step.
5. Clock skew: the feature behaves correctly with the device clock ±1 hour.
6. Low bandwidth: it works on a throttled 2G/3G profile without a blocking spinner.
7. Truth labels: every state is visible in words; no "Live" or "Paid" is faked.
8. A11y: state changes are announced; contrast and 48 dp targets pass; no colour-only meaning.
9. Security: no secret, bank detail or proof image in a cache, log, URL or notification.
10. Privacy: the data map and retention row are updated before merge.
11. Tests: offline, reconnect, duplicate, conflict and revocation tests exist in CI.
12. Metrics: the feature has a latency/freshness budget, not just a hope.
13. Presence: default-private is proven; each signal can be hidden; withdrawal propagates; no
    last-seen leaks through an API, push payload, cache or screenshot.
14. Transfer Method: availability and fees are live (or explicitly stale); switching methods
    preserves the draft; a provider failure never becomes a false "sent".
15. Identity: the passphrase never leaves the device; a new device can recover with the phrase;
    handle uniqueness, change and impersonation protection pass.
16. Peer layer: with Pear/Holepunch fully disabled, every shared and money-adjacent behaviour is
    unchanged.

---

## 4. TLC APP MANIFEST (mandatory)

```yaml
manifest_version: 2
doctrine_version: "0.0.2"
release_identity_source: "version.properties"   # current shell: 0.0.6; doctrine is independent
legal_position: "instrument for direct person-to-person transfers; not a bank, wallet, payment institution, custodian or investment scheme"
app:
  name: "TLC — The Love Culture"
  short_name: "TLC"
  package_id: "culture.thelove.tlc"        # stable forever; changing it breaks continuity links
  version_scheme: "semver"
  release_channel: "alpha"                 # alpha | beta | stable
  default_locale: "en-NG"
  locales: ["en-NG", "en"]
  min_android_sdk: 26                      # Android 8.0
  target_android_sdk: "latest-stable"
  form_factor: ["phone", "tablet", "foldable", "web-pwa"]
  orientation: "adaptive"
  install: "pwa-installable + play-store + direct-apk + f-droid"
  offline: "private plane: full read/write/export; shared plane: cached read + visible queue; no money-adjacent settlement offline"
connectivity:
  model: "two planes, one experience"
  private_plane: "device-authoritative; offline-forever; optional encrypted backup"
  shared_plane: "server-authoritative; real-time when online; queued when offline"
  states: ["LOCAL_ONLY", "LIVE", "SYNCING", "DEGRADED", "OFFLINE", "CONFLICT", "READ_ONLY", "LOCKED"]
  realtime_transport: "WSS/SSE for shared facts; WebRTC DTLS-SRTP for 1:1 calls; push optional; foreground poll fallback; cursor resume"
  peer_transport: "optional Pear/Holepunch (Hypercore/Hyperswarm) for encrypted private replication and nearby handoff; never authoritative"
  sync: "durable outbox/inbox; per-aggregate server sequence; idempotency key; at-least-once + dedupe; resumable"
  conflict: "preference LWW; relationship both-signature; money append-only server"
  settlement_offline: "never"
  latency_budget:
    local_feedback_ms: 100
    presence_p95_ms: 2000
    message_p95_ms: 3000
    shared_p50_ms: 1500
    shared_p95_ms: 5000
    reconnect_p95_ms: 3000
    offline_read_ms: 300
  freshness:
    live_s: 10
    fresh_s: 60
    stale_m: 15
    bank_details_max_h: 24
    instruction_ttl_m: 15
    transfer_method_quote_s: 120
  clock: "server time for shared/money; device offset for display; fail closed above 5m skew"
presence:
  default: "private: no online, last-seen, typing or read state shared"
  signals: ["online_now", "last_seen", "typing", "read_receipts"]
  audiences: ["nobody", "selected_people", "circle", "everyone"]
  overrides: "per-person widen/narrow; reciprocal option; temporary invisibility; schedule"
  precision: "coarse last-seen by default; exact time optional"
  retention: "no presence history beyond feature need; clear on withdrawal"
  coercion: "never a condition of access, messaging, transfer or receipt"
ownership:
  concept_pioneer: "Collins Christopher ('Sir Collins')"
  github_org: "OriginLabs1"
  repository: "TheLoveCulture-TLC"
  project_path: "TLC App"
  jurisdiction: "Nigeria"
  entity_status: "pending-legal-review"     # see Risk R-1
communication:
  model: "live 1:1 text, voice/video where supported, and live shared token state; consent-gated"
  text: "MLS (RFC 9420) or audited equivalent; relay mailbox holds ciphertext only"
  calls: "WebRTC DTLS-SRTP; direct first; TURN fallback sees encrypted media only"
  nearby: "optional peer path via Hyperswarm; relay remains durable mailbox"
  discovery: "opt-in; purpose-tagged requests; mutual acceptance; instant block/report; no public contact graph"
  states: ["sending", "sent", "delivered", "read (if shared)", "failed (reason)"]
  push: "opaque wake only; UnifiedPush/ntfy in foss; FCM optional in full; never required"
identity:
  stable_id_scheme: "did:key (Ed25519) + ULID records"
  username: "unique human handle; reserved list; 30-day signed forwarding tombstone on change"
  auth: "username + word/phrase passphrase; OPAQUE/SRP verifier (Argon2id fallback); device key bound to the passphrase; recovery phrase baseline; no biometrics, no passkeys; Keystore/StrongBox"
  seamless: "device key released only after passphrase verification; passphrase requested on new device, after lock, long absence, high-risk action or explicit choice"
  recovery: "12–24 word recovery phrase + second device; no SMS-only or knowledge-based recovery; social recovery out of scope"
  offline: "existing device unlocks offline; new device needs a connection for first shared sign-in"
economy:
  currency: ["NGN", "ISO-4217 (subject to both banks)"]
  currency_anchor: "NGN"
  custody: "none"                           # INV-01
  instrument: "instruction + proof + attestation + receipt; the bank moves the money"
  pioneer_tokens: { activation_ngn: 1000, reactivation_monthly_ngn: 2000, courtesy_pct: 10, courtesy_voluntary: true }
  pioneer_account_source: "signed runtime config; never repo/APK/log/screenshot"
  banned_user_term: "dues"                  # say Pioneer's Token, membership contribution or gift
  transfer_methods:
    default: "manual-bank-transfer (payer's own bank)"
    pioneer_only: ["hosted-card-checkout (licensed acquirer, off by default)"]
    licensed_adapters: ["provider rail", "mobile money / USSD / wallet provider where reviewed"]
    peer_channel: "Pear/Holepunch handoff of encrypted instruction/receipt; never settlement"
    requirements: ["live availability", "fee + payer", "timing", "limits", "currency", "provider terms", "failure path"]
    quote_ttl_s: 120
    silent_substitution: false
    payment_method_wording: forbidden
  tokens:
    nature: "record of value and recognition; not money"
    valuation: "declared at a time; never a market price or promise of return"
    transferable: "as a record with provenance and authority; not a tradable instrument"
    redeemable: false
    investment: false
    crypto: false
    balance: false
tech:
  core: "Kotlin Multiplatform (domain + contracts) · SQLDelight"
  android: "Jetpack Compose (Material 3) · AGP stable · JDK 21"
  pwa: "SvelteKit (stable major) + Vite (stable major) + Tailwind · Svelte 5 runes"
  sync: "SyncPort adapter → Postgres (server truth); PowerSync or equivalent behind the port"
  realtime: "Cloudflare Durable Objects + WSS/SSE; push optional; poll fallback"
  calls: "org.webrtc + coturn; DTLS-SRTP; no recording, no server mixing"
  messaging: "MLS (RFC 9420) via OpenMLS or audited equivalent; never hand-rolled"
  auth: "OPAQUE/SRP-class PAKE + Argon2id fallback; no biometrics, no passkeys; Android Keystore/StrongBox"
  peer: "Pear/Holepunch behind PeerPort: Hypercore/Hyperdrive/Hyperswarm/Autobase; Bare spike, native Kotlin adapter preferred; off by default"
  edge: "Cloudflare Workers · R2 · Queues · Durable Objects · Turnstile"
  semantics: "JSON-LD 1.1 · W3C VC 2.0 · CBOR-LD (size) · Ed25519/ES256"
  native: "C++/NDK only for crypto primitives & canonicalisation (audited libs)"
  ci: "GitHub Actions (mobile-first) + signed releases + network audit + presence/privacy tests + peer-off equivalence"
compliance:
  notices: ["TLC Financial Notice", "Token Nature Notice", "Connectivity Notice", "Presence & Communication Notice", "Transfer Method Notice", "Data & Privacy Notice"]
  data_regime: "NDPA 2023 (NG) · GDPR-shaped"
  retention: { proofs: "90d then hash-only", receipts: "7y", analytics: "13m", logs: "90d", messages: "until delivered + mailbox expiry; user copy until deletion", presence: "not retained beyond feature need", config: "TTL + rotation history" }
brand:
  tone: "warm · dignified · precise · unhurried"
  aesthetics: "premium, culturally intelligent, liquid-glass-capable but not dependent on it"
  a11y: "WCAG 2.2 AA baseline; large-type, reduced-motion and live-region honoured"
  assets: "tokens in /design; no stock-photo sentimentality"
links:
  scheme: "https://tlc.app/* (universal) + tlc:// (fallback)"
  signing: "PASETO v4.public / JWS detached; ≤15 min TTL; single-use nonce; revocable"
  offline: "explain, never exercise authority; redemption requires a connection"
security:
  posture: "zero-trust between shells; server-authoritative for shared and money facts"
  sbom: required
  secrets: "GH Actions + CF Worker secrets only"
```

---

## 5. Roles & Economic Model (SEF)

### 5.1 Roles

| Role | Who | Cost | Can do |
|---|---|---|---|
| **Single** | Unmarried individual | Free | Send/receive Supportive & Recognition if **Receptive Mode** is ON; give Joy/Courtesy |
| **Intentional Partner (I-Partner)** | Engaged couple, pre-marriage | Free | Dyad formation, Growth Grant pledges, shared stewardship |
| **Love Model** | Married couple | ₦1,000 activation + ₦2,000/month reactivation | Full dyad, receive Supportive/Recognition, publish verified transfer details |

Role is self-declared and descriptive. TLC does not demand a marriage certificate, a gender or a
proof of relationship. A person can read, export and delete their record without paying any
membership contribution. Membership never buys authority, visibility or a better place in a queue.

### 5.2 Tokens and routes

**Every monetary row is a direct transfer between two private accounts. TLC is not a party.** The
**Transfer Method** is chosen live in the flow (§2.17); the table names the default, not a limit.

| Token | Sender → Receiver | Default Transfer Method | Mechanism | TLC role |
|---|---|---|---|---|
| **Supportive** | User → Love Model | Bank transfer to recipient's verified account | Direct; recipient-supplied details; optional provider rail where available | None |
| **Growth Grant** | Donor → Beneficiary | Bank transfer to beneficiary's verified account | Pledge → evidence → steward verification → **donor pays beneficiary directly** | Witness only |
| **Recognition** | User → Love Model | None (record) or bank transfer if a value is stated | Recognition record; no amount by default | None |
| **Activation / Reactivation** | Love Model → Pioneer | Bank transfer; hosted card where enabled | Direct to Pioneer account, shown **only** inside the Pioneer's Token flow | Attestor of receipt |
| **Courtesy (10%, voluntary)** | Love Model → Pioneer | Same method as the transfer it accompanies | Direct; always declinable; never auto-added | Attestor |
| **Joy** | User → Pioneer | Bank transfer; hosted card where enabled | Direct, voluntary thanks | Attestor |
| **Pioneer's Token (umbrella)** | Love Model / user → Pioneer | Chosen live from available methods | The name for the membership contributions and gifts above; not a separate movement | Attestor |

**An unfulfilled pledge expires cleanly.** A Growth Grant pledge creates no debt, no penalty and
no claim by TLC. It is a statement of intent witnessed by the community, not a contract enforced by
the platform.

**One movement, one truth.** Every movement has one `transferId`. Both sides see the same amount,
currency, counterparty, reference, state and receipt. A correction is a new event, never an edit of
history. This is what "both sides of the token" means.

**Counters are not a balance.** Circulation, transferred and received counters describe movement
over a period. They are never a balance, a net position, a pooled total, a ranking or a score. No
screen may add them up into a number that looks like a wallet.

### 5.3 Exact notices (non-paraphrasable)

**TLC Financial Notice**

> TLC is an **instrument for direct person-to-person transfers**, not a bank. TLC does not hold,
> pool, escrow, route or move money, and it is not a wallet. Activation, Reactivation, Courtesy and
> Joy are **Pioneer's Tokens**: membership contributions and gifts sent **directly to the Concept
> Pioneer's own account**. Supportive Tokens, Growth Grants and any valued Recognition are sent
> **directly to the intended recipient**. Recognition without a stated value is a record of
> recognition, not a transfer. TLC Tokens are records of value: they may carry a declared value and
> may be transferred as a record, but they are **not redeemable by TLC**, not a balance, not an
> investment and not cryptocurrency. The real money moves in the person's own bank. TLC holds
> nothing.

**Instrument Notice**

> **TLC is an instrument, not a bank.** It helps you direct, prove and remember a transfer between
> your own account and another person's. Your bank moves the money. TLC never receives it, never
> stores it, never lends it and never promises a return. If TLC disappeared tomorrow, your bank
> record and your exported receipt would still be true.

**Token Nature Notice**

> **Declared value · Transferable as a record · Not redeemable · Not an investment · Not crypto ·
> TLC holds nothing.**
>
> "Transferable" means the record of a claim can move to another person with its history and the
> authority the dyad requires. It does not mean a market, a tradable instrument, or a right to cash.

**Connectivity Notice**

> TLC works offline for your own record. Shared actions need a connection to become true. A queued
> action is not a settled action. "Live" means connected and acknowledged, not merely animated.

**Presence & Communication Notice**

> You decide who can see you online, when you were last seen, whether you are typing and whether
> your messages have been read. Nothing is shared by default. You can change or withdraw each one
> at any time, and no one is ever required to share presence to use TLC, message or transfer.
> Messages and calls are end-to-end encrypted; the relay sees an envelope, not the content.

**Transfer Method Notice**

> A **Transfer Method** is the real route your money takes — usually your own bank, sometimes a
> licensed provider or a hosted card for a Pioneer's Token. The method, provider, fee, timing,
> limits and failure path are shown before you commit. TLC never chooses a method for you, never
> hides a fee and never becomes the account that holds your money.

### 5.4 Money and multi-currency

- `Money` is `currency` (ISO 4217) + `amount_minor` (integer). No `Double`, no bare number, no
  implicit currency. NGN uses kobo; other currencies use their ISO minor unit.
- NGN is always supported. Other currencies are supported only when both parties' banks support
  them and the sender chooses them. TLC does not promise every currency in every country.
- Pioneer's Tokens are denominated in NGN. A different-currency payment is accepted only if the
  instruction states the accepted amount, the Transfer Method and the FX reference; the receipt
  records all three.
- The receipt records what was actually stated. Reconciliation is per currency. A mismatch is an
  exception with a reason code; no silent conversion, no netting, no float.
- No cryptocurrency, no stablecoin, no "tokenized naira", no parallel-market rate.
- Amounts are never public by default. No leaderboards, no "top supporter", no ranking by amount.

### 5.5 Regulatory perimeter

TLC is designed to stay outside banking, custody, deposit-taking, remittance, collective
investment, e-money and cryptoasset exchange. It is an **instrument and a record**, not a payment
system that holds or routes user funds. It issues instructions; the banks and licensed providers
move money. Where a licensed partner provides a regulated service (card acquiring, a provider rail,
third-party escrow), the partner is the regulated entity and the user sees the partner's terms
before opting in.

This perimeter is a design intention, not a legal conclusion. Entity structure, tax, consumer
credit, FX, cross-border data and card acquiring require counsel before launch (R-1, R-11, R-12).
The product must never drift into holding a balance, promising a return, or describing itself as a
bank, wallet or payment institution.

### 5.6 Prohibited language

Marketing, ToS and UI must avoid: **dues, invest, yield, return, profit-share, referral commission,
escrow, wallet, balance, cash-out, guaranteed, risk-free, passive income, token sale, presale,
listing, exchange, price, market, APY, ROI, bank account with us, we hold, we received your money,
payment method**.

Preferred words: **support, gift, membership contribution, Pioneer's Token, declared value, record,
transfer, Transfer Method, receipt, attestation, steward, recognition, circulation, movement**.

Terminology law:

- **Pioneer's Token**, **membership contribution** or **gift** — never "dues".
- **Transfer Method**, not "Payment Method".
- **TLC is an instrument, not a bank** — never "we hold", "your balance", "top up", "withdraw
  funds" or "deposit funds" (withdrawing a *pledge* is a different, honest verb and is allowed).
- **Send Support**, not "Pay".
- **Attested**, not "Confirmed by bank", unless a rail adapter actually confirmed it.
- **Claimed**, not "Sent", until the server acknowledges it.
- **Queued**, not "Pending payment".
- **On this device**, not "Live", when offline.
- **Presence not shared**, not "Offline" as a guess about a person who hid it.
- **Username** for the handle, **display name** for the public name, **passphrase** for the secret.
- **Steward**, not "admin" or "moderator".
- **Shared plane**, not "the cloud"; **private plane**, not "local cache".
- **Token**, not "coin" or "asset".

---

## 6. Governance & Trust

- **Dyad (I-Partner / Love Model):** two members, one shared space, two signatures for irreversible
  acts. Primary Steward administers structure; Co-Steward holds full visibility and a symmetric
  veto. Either may leave; leaving never destroys the other's records. Gender is descriptive only.
- **Delegated Trust (Steward Protocol):** *Account Steward* (personal proxy: verification, tasks)
  and *Community Manager* (regional moderation). Grants are scoped, expiring, signed consents
  (`ConsentGrant` VC). Compensation is direct optional gratuities from users, never a cut of
  transfers, never central pooling. Revocation is instant online and propagates on the next sync;
  the UI never implies it is in force before acknowledgement.
- **Offline authority:** a cached grant is checked against its signature, scope, `not_before`,
  `exp` and the freshest revocation list. An expired grant is denied. An expired revocation list
  fails closed for sensitive acts. No one can widen a grant offline.
- **Veto:** symmetric on bank-detail change, dyad dissolution, delegation grant and account
  deletion. An offline veto queues; it is not in force until the server acknowledges it.
- **Evidence over opinion:** moderation decisions carry a reason code and an evidence link; every
  decision is appealable in-app. No decision is based on a private message the reporter did not
  choose to attach.
- **Sunset clause:** any steward power unused for 90 days auto-expires; re-grant is explicit.
- **Four eyes:** amounts ≥ ₦50,000 (or the equivalent in another currency, stated in the
  instruction) require two independent attestors. Bank-detail changes require both dyad signatures
  and a 24-hour cool-down with re-notice to both members.
- **No bulk export:** the console cannot export people, bank data or message content in bulk. Every
  read of a proof is logged. Audit logs are append-only and never deleted to hide a mistake.
- **Presence is personal authority.** No steward, community manager or support agent can see a
  person's presence settings, override them or impersonate presence. Only the person can change
  their audience, and every change is visible to them in a short audit trail.
- **Communication authority.** A steward can act on a report, not on a suspicion. Because messages
  are end-to-end encrypted, moderation uses the report, any excerpt the reporter chooses to attach,
  and metadata limits — never silent content scanning. Block and report take effect immediately.
- **Username stewardship.** Reserved and impersonation-prone handles are blocked at sign-up.
  A credible impersonation report can freeze a handle pending review without deleting the
  account's record; the decision carries a reason code and is appealable.

---

## 7. Technical Architecture — "one core, many shells"

Hexagonal (ports & adapters). The domain knows nothing about Android, Cloudflare or SvelteKit.

### 7.1 Layout

```
apps/     android (Compose) · pwa (SvelteKit) · console (steward) · cli (ops)
core/     domain (roles, dyad, tokens, ledger, presence, transfer methods) · contracts (JSON-LD, VC) · crypto · policy
adapters/ storage(SQLDelight|Room) · sync(PowerSync|none) · rail(manual|provider|card)
          transfer-method · evidence(R2) · notify · identity(passphrase) · presence
          comms(MLS|WebRTC) · peer(Pear/Holepunch) · clock · flags · realtime
contracts/ JSON-LD contexts · JSON Schemas · OpenAPI · VC templates
infra/    cloudflare(workers,r2,queues,do) · turn · github-actions · IaC
docs/     CONTEXT.md · SPECS.md · ADRs · threat-model · runbooks
```

```
┌────────────────────── Android · PWA · CLI ──────────────────────┐
│  one core: KMP domain + contracts + policy                      │
│  private plane (device truth)      shared plane (server truth)  │
│  SQLite/SQLDelight/OPFS            cached projection + outbox   │
│  optional Pear/Holepunch peer ◄──── encrypted replication ────┐ │
└──────────────────────────────┬────────────────────────────────┼─┘
                               │ HTTPS / WSS / WebRTC (pinned)  │
                     ┌─────────▼─────────┐                      │
                     │  TLC Relay (thin)  │  account · consent · mailbox
                     │  no funds          │  receipts · events · config
                     │  no plaintext      │  no bank credentials
                     └───────┬───────┬────┘
                         Postgres   R2 evidence
                         (RLS)      (encrypted, hash-addressed)
                               │
                     ┌─────────▼─────────┐
                     │  Peer node (opt)   │  Hypercore · Hyperswarm · TURN
                     │  no authority      │  encrypted private replication only
                     └────────────────────┘
```

### 7.2 Ports (stable, replaceable)

`IdentityPort · StoragePort · SyncPort · EvidencePort · RailPort · TransferMethodPort ·
PresencePort · CommunicationPort · PeerPort · NotifyPort · LinkPort · ClockPort · PolicyPort ·
FlagPort · RealtimePort`.

- `SyncPort.status/push/pull/subscribe` exposes connectivity state, cursors, queue depth and
  conflict metadata.
- `RealtimePort` carries events; it is an optimisation, not a source of truth. If it fails, the
  app falls back to `SyncPort` polling.
- `TransferMethodPort.list/quote/prepare/confirm/subscribe` returns only methods that are genuinely
  available for this person, currency and recipient; a quote expires; no adapter may hold funds.
- `PresencePort.get/set/audience/subscribe` enforces default-private, per-signal and per-person
  consent. It never exposes a hidden signal to another user or to a steward.
- `CommunicationPort.send/receive/call/request` carries ciphertext and call signalling; the relay
  mailbox is durable, the peer path is optional, and neither grants authority.
- `PeerPort.status/join/replicate/fetch` wraps Pear/Holepunch. It is allowed to replicate encrypted
  private data and public anchors; it is forbidden to author shared or money-adjacent facts.
- `ClockPort` supplies server offset and monotonic time. Domain code never calls `now()` directly.
- Every port has: a fake (tests), a local adapter (offline), a production adapter, and a
  conformance suite all three pass. Replaceability is proven by tests, not intention.

### 7.3 Language allocation (strategic, not decorative)

| Layer | Choice | Why |
|---|---|---|
| Domain/model/contracts | **Kotlin Multiplatform** | one source of truth across Android/web/CLI; strong typing; mature tooling |
| Android UI | **Kotlin + Compose (M3)** | native feel, accessibility, performance, no WebView tax |
| PWA | **SvelteKit + Vite + Tailwind (stable majors)** | smallest payload, best DX, offline-first ergonomics |
| Crypto / canonicalisation | **C++ via NDK** on audited libs (libsodium/BoringSSL) | constant-time primitives, no hand-rolled crypto |
| Java | **JDK 21 toolchain only** | zero app-level Java; no dual-language drift |
| Edge API | **Cloudflare Workers (TS)** | global latency, cheap, no servers to babysit |
| Native glue (optional) | Rust or C++ behind a port | only where measured need exists |

> Version rule: pin to **latest stable** in a version catalog; upgrade deliberately; a pre-release
> major is allowed only behind a feature branch and never in a release tag. "Newest" is not "best"
> until it is stable.

### 7.4 Data and sync

- **Private plane:** SQLite/SQLDelight on Android, OPFS + SQLite WASM on web. Encrypted where it
  holds sensitive data. Export is a first-class file, not a server API.
- **Shared plane:** Postgres with row-level security. Append-only tables for attestations and
  receipts. Per-aggregate server sequence. The relay is thin and replaceable behind OpenAPI.
- **Conflict classes:** §3.7. A money conflict is never resolved client-side.
- **Contracts:** JSON-LD 1.1 + W3C VC 2.0 + CBOR-LD for size. Receipts and grants remain verifiable
  outside TLC. The public verifier is a read-only projection with no PII.
- **Media:** encrypted at rest, hash-addressed, access-scoped, EXIF-stripped before upload.
  Thumbnails are derived; originals are never served directly. Proof images are reduced to hashes
  after 90 days.

### 7.5 Real-time transport

- Durable Object per account or per high-traffic aggregate; coalesced fan-out; cursor-based resume.
- WSS/SSE for shared facts; WebRTC DTLS-SRTP for 1:1 audio/video; signalling travels inside the
  encrypted conversation. TURN is a fallback and sees encrypted media only.
- Push (UnifiedPush/ntfy in FOSS, FCM optional in full) is a wake hint only, with an opaque payload.
- Poll fallback with exponential backoff and jitter; foreground only unless the person is waiting
  on a specific action.
- Event log is append-only and replayable. Clients are idempotent. A gap triggers a pull; a
  duplicate is ignored; an unknown schema version is held, not guessed.
- Presence events are coalesced and permission-filtered at the edge: a hidden signal never enters
  another person's event stream, a cache or a push payload.
- Message envelopes are ordered by server sequence but carry ciphertext only; delivery state is
  separate from read state, and read state exists only when both sides opted in.
- No plaintext in the relay, no amounts in push payloads, no contact-graph export.
- Cost control: coalesce counters, batch deltas, avoid per-keystroke sync, cap fan-out per account.

### 7.6 Config and flags

- Pioneer destination, fee schedule, supported currencies, rail availability and feature flags are
  **signed remote config**, pinned to a key, with a TTL and a rotation history.
- Never compiled in, never logged, never in a screenshot, never in a link.
- If config fetch fails: use the last known good signed config for display; if it is expired, block
  new money-adjacent actions. Never fall back to an unsigned default for a destination.
- Flags default off. A flag can disable a risky rail or a broken sync path without a release. Flags
  never execute remote code and never widen authority.

### 7.7 Performance & UX budgets

Cold start ≤ 2.0 s on a mid-range Android; PWA TTI ≤ 1.8 s on 4G; APK ≤ 30 MB with optional
media/WebRTC modules (current local-only APK is far smaller); initial PWA JS ≤ 180 KB gzip; 60 fps
scrolling; offline read ≤ 300 ms; presence update ≤ 2 s p95; message delivery ≤ 3 s p95 on 4G;
zero layout shift on money surfaces; every async action shows a determinate state; no blocking
spinner without a label.

### 7.8 Pear/Holepunch integration (optional, behind `PeerPort`)

**Adopt now (design + adapter seam):**

- `PeerPort` with a no-op fake, a local test node, and a production adapter. The app must behave
  identically when the adapter is absent.
- **Hypercore** for encrypted, append-only private-record segments and receipt bundles. Each
  device signs its own blocks; keys never leave the device unencrypted.
- **Hyperswarm/HyperDHT** for discovery of a person's own devices and of explicitly paired peers.
  Swarm keys rotate; discovery is not a public directory and is never used for marketing.
- **Hyperdrive/Hyperblobs** for encrypted evidence blobs where a direct transfer is cheaper than
  the relay, with R2 remaining the default evidence store.

**Spike, then decide:**

- **Autobase** for multi-writer private logs across a person's devices.
- **Bare/Pear on Android** behind a feature flag: measure cold start, memory, battery per hour,
  APK delta, crash-free rate and F-Droid/Play policy fit. The preferred order is native Kotlin
  adapter → embedded Bare → companion desktop node.

**Never:**

- A peer is never trusted to order shared facts, author an attestation, mint a receipt or settle a
  transfer.
- No peer-to-peer path may expose plaintext, a bank detail, a contact graph or a presence signal
  the person did not choose to share.
- No critical path depends on a DHT, a relay, a bootstrap node or a specific runtime.

**Acceptance:** with `PeerPort` disabled, all capability-matrix rows behave exactly as specified;
with it enabled, a private-record restore completes over LAN without the relay, and the restored
record verifies against the same hash chain.

---

## 8. Link Doctrine (compressed)

**Link meaningfully · share minimally · authenticate independently · authorize explicitly ·
verify outcomes · preserve user control.**

- **Identity links** attach a verified credential to a profile, revealing only a proof, not the
  document.
- **Relationship / Invitation links** are invitations, never authorizations; acceptance is mutual
  and logged.
- **Consent links** are signed, scoped (`action`, `resource`, `dyad`, `exp`, `jti`), single-use,
  revocable. **Possession ≠ authority**: the approver must authenticate separately. A link may
  carry a request; only independent authentication and explicit authorization grant authority.
- **Evidence / Receipt links** are permission-aware, expiring, `noindex`, PII-minimised, with an
  offline fallback page that explains what it is and why it needs a connection to verify.
- **Continuity links** restore a user's own records to a new device via a user-held key; they are
  not server-side authority.
- **Service links** are optional, documented, replaceable, and clearly mark "you are leaving TLC".
- **Conversation links** open a request, not a channel. They carry no authority to message until a
  request is accepted, and they never reveal presence or read state.
- **Presence links** do not exist. Presence is a setting, never a URL, a QR code or a shareable
  token. A screenshot of a profile must never prove that someone is online.
- Stable IDs (DID/ULID) outlive URLs; every link has a meaningful fallback (web → store → help).
- **Never in a URL:** bank details, tokens, relationship status, health/pastoral data, full names
  of third parties, message content or proof bytes.
- **Offline:** a link may render an explanatory page, but it cannot be redeemed or used to change
  authority without a connection. No cached nonce is reusable.
- **Revocation:** a revoked link fails closed. A link preview must not leak sensitive metadata.

---

## 9. Observability, Traceability & Accountability (VITAL)

**Objective:** every naira that reaches the Pioneer account is explainable, attributable and
verifiable — without TLC ever holding funds or reading the Pioneer's bank feed by default.

**Pioneer Ledger (append-only, hash-chained):**

```
FeeIntent (device-signed: role, token, amount, currency, period, transfer-method, idempotency-key)
   → Instruction (Pioneer account details released from signed runtime config; method, fee and TTL stated; expires in 15 min)
   → ProofArtifact (user upload → R2 signed PUT → SHA-256 recorded → 90-day retention)
   → Attestation (Pioneer/Steward confirms settlement w/ statement reference; 4-eyes where possible)
   → Receipt (JSON-LD VC 2.0, prev_hash chained, Merkle-leaf; method + fee recorded)
   → Anchor (daily Merkle root published: signed manifest + optional public timestamp)
```

- **Reconciliation ladder (cheapest first):** manual attestation → bank-statement/CSV import
  (adapter) → provider webhook (adapter, opt-in). Each rung raises confidence; none gives TLC
  debit authority over any account.
- **Three-way match:** `intent amount = proof amount = attestation amount`, per currency; mismatch
  ⇒ exception queue with a reason code, never silent correction.
- **Idempotency:** every fee carries a client-generated key; duplicates resolve to one receipt.
- **Transfer Method is part of the fact.** The intent records the chosen method and provider; the
  instruction records the quoted fee, payer and timing; the receipt records what was actually
  stated. A method change before commitment is a new intent version, not an edit of history.
- **Method honesty:** a provider webhook may strengthen confidence, but a receipt is never rendered
  as "confirmed by bank" unless the bank or a licensed adapter actually said so.
- **Proof is not a receipt.** A **Proof** is the image/PDF a person uploads as a claim. A
  **Receipt** is the signed record TLC issues after attestation. They are never used as synonyms,
  and a proof never settles anything by itself.
- **Non-repudiation:** receipts are signed (Ed25519) and chained; a verifier can validate the whole
  chain **offline** from an export. Monthly signed statement (PDF + JSON) is one tap.
- **Privacy:** no full PANs stored; counterparty shown as `bank · last4 · hash`. Proofs are
  access-scoped, scanned for malware, and reduced to hashes after 90 days.
- **Operational visibility:** dashboards for obligation → attested → exception → overdue, with
  cohort and period views; alerts on ageing exceptions, chain-verification failures, and
  attestation latency. Nightly CI job re-verifies the chain and the last anchor.
- **Real-time, not surveillance:** the console sees the minimum needed for a case; every read is
  audited; no bulk export; no individual ranking; public transparency is aggregate only.
- **Accountability both ways:** an attested receipt is evidence the user paid; the ledger is also
  the Pioneer's auditable record for tax and for a public "platform sustainability" summary
  (aggregates only, never individuals).
- **Reconnect:** verify the chain before showing new money facts; never replace a verified local
  fact with an unverified remote claim.

---

## 10. Privacy, Safety & Dignity

- **Data map before code.** Every new field answers: purpose, lawful basis, location, retention,
  access, export path, delete path. No field without a row.
- **Minimisation.** The private plane is the default home for personal data. The shared plane gets
  only what two people need to agree on.
- **Retention.** Proofs 90 days then hash-only; receipts 7 years; analytics 13 months (never in
  `foss`); logs 90 days; account data until deletion + 30 days in backups; config rotation history
  as required for audit.
- **Export and deletion.** Export is always available offline (JSON + human-readable PDF). Delete
  uses crypto-shredding where possible and hash-only residue for receipts required by tax. Never
  delete another person's evidence.
- **No sale, no ads, no broker.** No analytics on amounts, names, bank details or message content.
  No contact-graph export. No advertising identifier.
- **Presence is not surveillance.** Online state, last seen, typing and read receipts are private
  by default and controlled per signal, per person and per circle (§2.15). Nothing is inferred,
  sold, used for ranking or shared with a steward. Withdrawal clears caches and propagates across
  devices. Location is never collected.
- **Communication is private by design.** Messages and calls are end-to-end encrypted; the relay
  holds ciphertext, a recipient inbox ID, size and time. No plaintext scanning, no ad profiling, no
  contact-graph mining. A reporter may attach an excerpt; nothing else is read.
- **Identity data is minimal.** A username, a display name, a passphrase verifier (never the
  passphrase), a device public key and an optional recovery public key. No phone or email is
  required for a shared account. The username is public; the passphrase never is.
- **Transfer Method data is limited.** Store the method kind, provider, fee and reference needed
  for reconciliation; never a card PAN/CVV, never a bank password, never a full bank credential.
  Provider terms are shown before the person opts in.
- **No "who viewed you", no location history, no shadow profile.** Discovery is opt-in and
  purpose-tagged. Block and report are immediate and do not notify the blocked person with
  unnecessary detail.
- **Public receipts are redacted.** Receipt visibility and amount visibility are separate grants.
  Making a receipt public never makes the amount public, and a public receipt never carries a bank
  number, a counterparty's identity or a free-text note by default.
- **Consent-first social.** Discovery is private by default. A request carries a purpose. Declining
  is never punished. Block and report are immediate.
- **Safety.** 18+. No public amounts by default. No leaderboards. No dark patterns. A person can
  leave with their record. A person can delete without a maze.
- **AI.** On-device and optional. No server-side scanning of end-to-end encrypted content. A local
  model may suggest respectful wording; it never sends the draft anywhere.
- **Children.** TLC is for adults; it does not knowingly process data of anyone under 18.
- **Breach.** Assess under NDPA; notify the NDPC within 72 hours where required; tell affected
  people plainly; publish a ledger integrity statement if attestations were affected.
- **Cross-border.** Data residency and processor terms are documented before enabling a region.
  The privacy notice names the infrastructure provider and the categories it sees.

---

## 11. Risk Register (v0.0.2)

| ID | Risk | Severity | Mitigation |
|---|---|---|---|
| R-1 | Personal account receiving recurring membership contributions reads as unregistered financial activity | High | CAC/entity registration before public launch; frame as Pioneer's Tokens and gifts; ToS + receipts; tax filing; no investment language; legal review |
| R-2 | "Token/escrow" vocabulary triggers securities/e-money scrutiny | High | INV-03 notices everywhere; no TLC escrow; declared-value-not-market language; no yield/referral payouts; legal review |
| R-3 | Bank-detail substitution fraud | High | Verified-detail badge; dual signature + 24h cool-down + re-notice; no stale destination; vault encryption |
| R-4 | Gender-coded authority challenged | Medium | Primary/Co-Steward; symmetric veto; gender descriptive only |
| R-5 | NDPA breach via proof uploads | Medium | Signed URLs, encryption at rest, 90-day retention, DSR export/delete, DPIA before beta |
| R-6 | Sync conflicts corrupt money state | High | Server-authoritative append-only attestations; clients cannot mint receipts; conflict classes; chaos tests |
| R-7 | Offline UX lies about payment state | High | Truth ladder; explicit labels; no optimistic "paid"; airplane-mode e2e |
| R-8 | Secrets leaked via repo/AI chat | High | Rotation policy, gitleaks in CI, pre-commit hooks, scoped tokens, AI-channel ban |
| R-9 | Dependency churn (unreleased majors) | Medium | Version catalog + pinned stable + renovate windows |
| R-10 | Founder key-person dependency | Medium | Runbooks, signed config, multi-steward attestation option |
| R-11 | Multi-currency/FX ambiguity or implicit conversion | High | ISO 4217 + minor units; NGN anchor; no TLC conversion; per-currency reconciliation; FX reference in receipt |
| R-12 | Card acquiring pulls TLC into PCI/custody/chargeback scope | High | Hosted checkout only; no PAN/CVV; Pioneer-only payee; off by default; SAQ review; acquirer terms |
| R-13 | Real-time fan-out cost, battery drain or relay outage | Medium | Coalesced events, cursor resume, poll fallback, private plane usable offline, cost caps |
| R-14 | Offline queue replay creates duplicates | High | Idempotency keys, per-aggregate order, dedupe, at-least-once + exactly-once effect |
| R-15 | Clock skew or out-of-order events misorder money facts | High | Server time and sequence; skew warning; gap pull; no client settlement |
| R-16 | Stale bank destination used after rotation | High | 15-min instruction TTL; 24h max cache; signed rotation history; re-fetch before use |
| R-17 | Push notification leaks metadata | Medium | Opaque wake payload; no amount/name/bank detail; opt-in; FOSS UnifiedPush |
| R-18 | PWA/service-worker cache leaks on a shared device | High | No sensitive API caching; encrypted store; clear on sign-out/revoke; TTL; cache audit |
| R-19 | Token semantics drift back into investment language | High | Vocabulary lint; SEF tests; no market/valuation display by default; legal review |
| R-20 | Relay/vendor lock-in or provider outage | Medium | OpenAPI contract; replaceable adapters; export; Docker fallback; status page |
| R-21 | Account recovery failure locks a person out | High | Recovery kit + second device; tested restore drill; no phone-only recovery; support runbook |
| R-22 | Public receipt link or preview leaks PII | Medium | Default-closed visibility; redacted derivative; `noindex`; expiry; revocation; no PII in URL |
| R-23 | Offline veto or revocation appears active before it is | High | "Not yet in force" label; server acknowledgement; fail closed on stale revocation list |
| R-24 | Cross-border data transfer without a lawful basis | High | DPIA; region choice; processor terms; NDPA/GDPR review before enabling |
| R-25 | Presence becomes a stalking or coercion tool | High | Default private; independent per-signal consent; coarse last seen; per-person block; no inference; quick "go invisible"; safety review of every presence surface |
| R-26 | Username impersonation or handle squatting | High | Reserved list; uniqueness; verified badge for public roles; 30-day signed forwarding tombstone; report-and-freeze review |
| R-27 | Passphrase loss or phishing locks a person out | High | PAKE/Argon2id; printable recovery phrase + second device; rate limits; no security questions; tested restore drill; sign-in notices |
| R-28 | Transfer Method is misquoted, unavailable or silently substituted | High | Live availability + 120 s quote TTL; explicit provider/fee/timing; no silent substitution; draft preserved; provider outage fallback; status page |
| R-29 | Live communication enables harassment or spam | High | Purpose-tagged requests; mutual acceptance; per-day limits; instant block/report; E2EE; local abuse signals; no public contact graph |
| R-30 | Pear/Holepunch drains battery, bloats the APK or adds a vulnerability | Medium | Off by default; `PeerPort` seam; time-boxed Android go/no-go; crypto review; peer-off equivalence test; companion-node fallback |
| R-31 | "Instrument, not a bank" positioning drifts into wallet or deposit language | High | Copy lint; banned-term list; legal review; no balance/top-up/withdraw; notices in every money flow; release gate |
| R-32 | A presence or read-receipt state is leaked through push, cache or API | High | Permission-filtered events at the edge; opaque push; no presence in cache without consent; leak tests; withdrawal propagation test |

---

## 12. Glossary & Decision Log

**Attest** — a human/system assertion that a transfer settled. **Acknowledge** — a person says they
received something; it is not a bank confirmation. **Receptive Mode** — opt-in permission to receive
Supportive/Recognition tokens. **Dyad** — two-person shared space with two signatures for
irreversible acts. **Primary Steward / Co-Steward** — role-based structural authority and symmetric
veto. **Pioneer** — Concept Pioneer, Collins Christopher. **Steward** — scoped delegate.
**Shell** — a client app over the shared core. **Instrument** — a means of directing, proving and
recording a transfer; never a bank, wallet or custodian. **Token** — a record of value and
recognition, never a balance. **Valuation** — a declared value at a time, never a market price.
**Transfer** — moving a record of a claim with provenance and authority. **Distribution** — settling
a Token in monetary form bank-to-bank. **Private plane** — device-authoritative personal data.
**Shared plane** — server-authoritative relational data. **Live** — connected, stream healthy and
acknowledged. **Cached** — last known, with age. **Queued** — accepted locally, waiting for network.
**Freshness** — how old a cached shared fact is. **Outbox** — durable local queue of shared writes.
**Idempotency key** — a client key that makes a retry resolve to one fact. **Server sequence** —
the server's per-aggregate order for shared facts. **Username** — the unique public handle used to
find a person; not an email, phone or display name. **Passphrase** — the word/phrase secret that
authenticates a person; never stored or sent in plaintext. **Recovery phrase** — the offline
12–24-word secret that restores an account on a new device. **Presence** — the consensual sharing of
online state, last seen, typing or read state; private by default. **Transfer Method** — the real
route money takes (bank, licensed provider, hosted card), with provider, fee, timing and limits.
**Peer layer** — optional Pear/Holepunch replication and discovery that never holds authority.
**Hypercore** — a signed append-only log. **Hyperswarm** — peer discovery over a DHT. **Pear/Bare**
— the Holepunch application runtime and its small JavaScript runtime. **Rarity Symbol** — TBD (Q-3).

| ADR | Decision | Status |
|---|---|---|
| 001 | No custody anywhere; attestation replaces escrow | accepted |
| 002 | Kotlin Multiplatform core + Compose Android + SvelteKit PWA | accepted |
| 003 | Manual transfer + proof for v0.0.2; methods behind `TransferMethodPort` | accepted |
| 004 | Hash-chained Pioneer Ledger + daily Merkle anchor | accepted |
| 005 | Primary/Co-Steward replaces gender-coded authority | accepted |
| 006 | Two-plane sync: Postgres server truth; PowerSync or equivalent behind `SyncPort` | accepted |
| 007 | Username + word/phrase passphrase only; PAKE verifier; device key bound to the passphrase; recovery phrase; no biometrics or passkeys | accepted |
| 008 | SEF Token nature: record of value, declared valuation, transferable as record, no market | accepted |
| 009 | Real-time transport WSS/SSE for shared facts + WebRTC for calls; push optional; poll fallback | accepted |
| 010 | NGN anchor + multi-currency, integer minor units, no TLC conversion | accepted |
| 011 | Card acquiring hosted, Pioneer-only, off by default | accepted (pending legal/PCI review) |
| 012 | Offline may prepare irreversible acts but never complete them | accepted |
| 013 | PWA parity; one core, many shells | accepted |
| 014 | Server-minimal and metadata-minimised, not "zero-server" or "metadata-free" | accepted |
| 015 | Truthful connectivity labels; INV-11 and INV-12 | accepted |
| 016 | TLC is an instrument, not a bank; no balance/wallet/deposit surfaces | accepted |
| 017 | Presence is per-signal, per-person, private by default, revocable | accepted |
| 018 | Live communication (text, calls, shared state) is a first-class pillar | accepted |
| 019 | Transfer Methods are live, disclosed and never silently substituted | accepted |
| 020 | Pear/Holepunch is optional, behind `PeerPort`, never authoritative | accepted (Android go/no-go) |
| 021 | "Dues" and "Payment Method" are banned user-facing terms | accepted |

**Open questions:** Q-1 CAC entity + contribution taxation path. Q-2 Is a licensed escrow partner
ever needed for large Growth Grants? Q-3 Define "Rarity Symbol" (must not imply scarcity
economics). Q-4 iOS timing. Q-5 Moderation appeals SLA. Q-6 Which currencies beyond NGN, and what
FX evidence is enough? Q-7 Card acquirer and merchant structure. Q-8 PWA background-sync support
and fallback matrix. Q-9 Data residency (Nigeria/EU) and cross-border mechanism. Q-10 Which live
communication surfaces ship first (text, calls, live token state) and in what order? Q-11 How are
offline receipt verification keys distributed? Q-12 Scope of the public transparency page. Q-13
Should a local guest be able to create a shared account later without losing the private record?
(Proposed: yes.) Q-14 Final umbrella naming: "Pioneer's Token" vs "Pioneer Tokens". Q-15 Is Token
valuation ever visible by default? (Proposed: no.) Q-16 OPAQUE vs SRP-class PAKE for v0.0.7. Q-17
Which presence default best protects safety without making the product feel dead? Q-18 Does the
Android peer path use native Kotlin or embedded Bare? Q-19 Which licensed Transfer Method providers
are approved for which corridors? Q-20 Username policy: length, reserved words, rename cooldown.

---

## Appendix A — Capability Matrix (online / offline)

| Capability | Offline | Online | Notes |
|---|---|---|---|
| Onboarding / local guest | Yes | Yes | Private record works without an account; shared account needs a connection |
| Username + passphrase sign-in | Existing device: yes | Yes | Passphrase is the only door; new device needs a connection |
| Passphrase / account recovery | Recovery phrase only | Yes | 12–24 words + second device; no SMS-only recovery |
| Presence controls (online, last seen, typing, read) | Edit locally; queue | Yes | Private by default; per signal, per person, per circle; withdrawal propagates |
| Private record read/write | Yes | Yes | Device-authoritative; durable before acknowledgement |
| Search / timeline | Yes | Yes | Local index; shared cache searchable |
| Export / backup | Yes | Yes | JSON + human PDF; chain + anchor; never needs the server |
| Verify ledger / receipt | Yes | Yes | Offline chain verification; anchor checked when available |
| Bank vault (save/reveal) | Yes | Yes | Encrypted on device; never synced; re-auth on reveal |
| Support / Recognition intent | Yes (draft/queue) | Yes | `SENT` only after server ack; no balance |
| Receipt image attach/view | Yes | Yes | Local capture, hash; upload queues |
| Receipt share link | No | Yes | Needs server for expiry/ACL; offline page explains |
| Dyad invitation / acceptance | No (draft) | Yes | Two signatures; link is invitation, not authority |
| Dyad veto | Queue only | Yes | "Not yet in force" until acknowledged |
| Bank-detail change | No (draft) | Yes | Dual signature + 24h cool-down + re-notice |
| Transfer Method list / live quote | Last known, labelled stale | Yes | Provider, fee, timing, limits; quote TTL ≤ 120 s |
| Pioneer's Token instruction | No | Yes | Destination TTL 15 min; never stale > 24h |
| Pioneer proof capture | Yes | Yes | Capture/hash locally; upload queues |
| Attestation / receipt mint | No | Yes | Server-attested; client cannot mint |
| Growth Grant pledge / evidence | Draft/queue | Yes | Witness verifies online; donor pays beneficiary directly |
| Steward Console write | No | Yes | Scoped, audited, online-only; no bulk export |
| Steward Console read | Cache (watermarked) | Yes | Minimum necessary; every read audited |
| 1:1 encrypted messaging | Compose/queue | Yes | Delivery ≠ read; relay sees envelope metadata only |
| 1:1 voice/video call | No | Yes (if supported) | Consent-gated; DTLS-SRTP; TURN fallback; no recording |
| Live shared token state | Cached, labelled | Yes | Both sides see the same state within the real-time budget |
| Discovery / connection requests | Draft/queue | Yes | Opt-in, purpose-tagged, mutual acceptance, block/report |
| Public receipt verification | Export only | Yes | Verifier page is read-only, no PII |
| Config / fee schedule update | Last known good | Yes | Signed, pinned, TTL; fail closed for money |
| Account deletion | Queue | Yes | Confirm online; 30-day backup window explained |
| Multi-device shared state | Cache | Yes | Server sequence; no device master |
| Multi-device private backup | Optional | Optional | End-to-end encrypted; user-held key |
| Peer backup / restore (Pear/Holepunch) | Yes (LAN/paired peer) | Yes | Encrypted; optional; never authoritative; peer-off equivalence |
| PWA install / offline shell | Yes after first load | Yes | Service worker; OPFS; update prompt |
| Hosted card Transfer Method (Pioneer only) | No | Yes (if enabled) | Licensed acquirer; no card data in TLC; off by default |

---

## Appendix B — Event Catalogue & State Ladders

**Event envelope (minimum):**

```json
{
  "event_id": "ulid",
  "aggregate_id": "ulid",
  "aggregate_seq": 42,
  "server_time": "2026-11-05T12:00:00Z",
  "schema_version": 1,
  "type": "token.acknowledged",
  "causation_id": "ulid",
  "correlation_id": "ulid",
  "payload": {}
}
```

**Event families:** `account.*` · `identity.*` · `presence.*` · `consent.*` · `dyad.*` ·
`message.*` · `call.*` · `token.*` · `transfer_method.*` · `grant.*` · `receipt.*` ·
`steward.*` · `device.*` · `peer.*` · `config.*`. Payloads carry the minimum needed; no bank
details, no message plaintext, no proof bytes, and no presence signal the recipient is not
permitted to see.

**Community token:** `DRAFT → QUEUED → INSTRUCTED → PROOF_ADDED → ACKNOWLEDGED → CLOSED`.
**Pioneer token:** `DRAFT → QUEUED → INSTRUCTED → PROOF_ADDED → ATTESTED → RECEIPT_ISSUED`.
**Exception:** any state → `NEEDS_ATTENTION` with a reason code; never a silent correction.
**Grant:** `PLEDGED → ACCEPTED → EVIDENCE → VERIFIED → PAID → RECEIPT`.
**Consent/delegation:** `REQUESTED → GRANTED → ACTIVE → EXPIRED | REVOKED`.
**Transfer Method:** `LISTED → QUOTED → SELECTED → INSTRUCTED → PROVIDER_PENDING → ATTESTED | FAILED(reason)`.
**Presence:** `PRIVATE → CONSENTED(nobody|selected|circle|everyone) → WITHDRAWN`; never a public
history, never inferred for a person who withdrew.
**Message:** `DRAFT → QUEUED → SENT → DELIVERED → READ (only if shared) | FAILED(reason)`.
**Call:** `REQUESTED → ACCEPTED → CONNECTING → LIVE → ENDED | DECLINED | MISSED | FAILED(reason)`.

---

## Appendix C — Copy Deck (connectivity and truth)

Use these exact ideas; translate carefully, never weaken them.

- **Offline:** "Offline — your record still works. Nothing has been sent."
- **Queued:** "Queued — will send when you're online."
- **Draft:** "Saved on this device. Not shared."
- **Sent:** "Sent — the other side has not opened it yet." (Only after server ack.)
- **Delivered:** "Delivered — not read." (If delivery receipts exist; otherwise omit.)
- **Acknowledged:** "Acknowledged — they say they received it. A bank has not confirmed it."
- **Attested:** "Attested — the steward matched your proof. This is not a bank confirmation."
- **Receipt:** "Receipt issued. You can verify it offline."
- **Live:** "Live · updated just now." (Only when connected, stream healthy, ≤10 s.)
- **Stale:** "Updated 12m ago. Some actions may need a refresh."
- **Irreversible:** "This changes something shared, so it needs a connection and a fresh sign-in."
- **Veto:** "Veto queued — not yet in force."
- **Link offline:** "This link needs a connection to verify and use. It has not granted anything."
- **No fake scarcity:** never "last chance", "expires in 60 seconds", "everyone is supporting".
- **No fake social proof:** never "3 people are viewing", never a leaderboard.
- **No custody implication:** "TLC holds nothing. The transfer happens in your bank."
- **Instrument:** "TLC is an instrument, not a bank. It helps you direct and prove the transfer;
  your bank moves the money."
- **Passphrase:** "Your passphrase never leaves this device. We only keep a verifier that cannot
  be turned back into your words." Never "password", never a security question.
- **Recovery:** "Your recovery phrase is the only way back into this account if you lose every
  device. Write it down, keep it offline, never share it."
- **Presence (private):** "Presence not shared. You decide who sees you online or last seen."
- **Presence (shared):** "Online now · visible to your Circle." / "Last seen today · visible to
  Ada." / "Typing · you chose to share this."
- **Withdrawal:** "Presence hidden. People who could see it will see 'not shared' from now on."
- **Transfer Method:** "Bank transfer · your bank · fee ₦0 · usually instant." / "Card · licensed
  provider · fee 1.5% paid by you · Pioneer's Token only."
- **Method switch:** "You can change the Transfer Method before you send. Nothing has moved yet."
- **Method failure:** "The provider could not complete this. Nothing was sent. Try another method
  or come back later."
- **Live communication:** "Sent — not yet delivered." / "Delivered — not read." / "Read — they
  share read receipts." / "Call failed — the network did not connect it."
- **No fake presence:** never "online" from a cache, never "typing" without consent, never "last
  seen 2 minutes ago" for someone who hid it.
- **No investment implication:** "This is a record, not an investment. There is no yield or return."
- **No false bank confirmation:** "Attested" unless a rail adapter confirmed it.
- **Banned words:** never "dues", never "Payment Method", never "balance", "wallet" or "top up",
  and never "withdraw funds" or "deposit funds" in a user-facing string.

---

## Appendix D — Doctrine-to-code status (honest)

This table exists so doctrine never silently outruns the build.

| Doctrine | Shipped v0.0.6 | Next change |
|---|---|---|
| Local-only private plane | Yes: no account, no sync, no `INTERNET` | Keep as `LOCAL_ONLY` / local guest when the shared plane ships |
| Username + word/phrase passphrase | No account at all | Add handle uniqueness, PAKE verifier, device key, recovery phrase, seamless unlock |
| Presence controls | No shared plane; no presence | Add `PresencePort`, per-signal consent, last-seen precision, withdrawal propagation |
| Live communication | No | Add MLS messaging, WebRTC calls, live token state, request/accept gates |
| Two-plane model | No shared plane yet | Add relay, `SyncPort`, outbox, server sequence |
| Real-time shared truth | No | WSS/SSE + cursor resume + poll fallback; WebRTC for calls |
| Truth ladder | Partial: local states exist; the Tokens screen now says **"On this device"** instead of a pulsing "Live" pill | Add server-acknowledged states and `SENT` only after ack when the shared plane ships |
| Transfer Methods | Local instruction only; no live method list or quote | Add `TransferMethodPort`, availability, fee/timing disclosure, method switching, failure states |
| Instrument, not a bank | Copy is close; the grant stage now reads **"Transfer recorded"** instead of "Paid" | Add the banned-term lint and make it fail the build |
| "Dues" user-facing | No shipped string value contains it (resource names only) | Keep the lint; rename internal identifiers only when safe |
| SEF Token valuation | Domain still models `RECOGNITION_SIGNAL` only | Add `TokenValuation`, transfer provenance and conservation tests |
| Multi-currency | `Money` is NGN kobo only | ISO 4217 + minor units + per-currency reconciliation |
| Hosted card acquiring | No | Adapter, off by default, Pioneer-only, PCI/legal review |
| Pear/Holepunch peer layer | No | `PeerPort`, Hypercore encrypted backup, Hyperswarm spike, Bare Android go/no-go |
| PWA shell | No | SvelteKit shell with offline parity |
| Steward Console | Local personas only | Scoped grants, audit, online-only writes |
| Offline settlement guard | Yes: no network, no settlement | Keep and test explicitly when network arrives |
| Bank vault | Yes: Keystore encryption, no network | Keep; never sync the vault |
| Ledger verification | Yes: offline chain + anchor | Add server sequence and receipt verification |
| Multi-device | No | Device list, revoke, optional encrypted private backup |

**Known code gaps to close first (honest):** the local-only Tokens screen used to render
`tokens_live` ("Live") through a pulsing `LivePill`, and a Growth Grant stage rendered
`tokens_status_paid` ("Paid"). Both violated INV-11/INV-13 and §5.6. They now read **"On this
device"** and **"Transfer recorded"**, the pulse is gone, and a locally recorded claim reads
**"Claimed"**, not "Sent". The copy lint that must make either return a build failure is still to be
wired into CI (Part E, T-28).

**SPECS delta.** `SPECS.md` is now aligned to this doctrine: it carries the identity, presence,
live-communication, Transfer Method and Pear/Holepunch skills, workflows, ports and release gates.
Where a capability is not shipped, the doctrine still says so here rather than implying it exists.

---

*TLC CONTEXT v0.0.2 · doctrine is the source of truth. Executable contracts: `SPECS.md`.
When they conflict, this file wins. When a capability is not yet shipped, this file says so.*
