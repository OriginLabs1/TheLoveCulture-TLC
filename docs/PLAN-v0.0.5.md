# TLC v0.0.5 — Trust & Contact

**Status:** proposal updated after the founder's review. Not a commitment, not a tag.
**Working title:** *Trust & Contact* · **Release headline:** *Names, numbers, and a way to reach the Pioneer.*
**Baseline:** v0.0.4 (`10bfa88`), 112 tests, local-only, no `INTERNET` permission.
**Companion docs:** [`CONTEXT.md`](../CONTEXT.md) · [`SPECS.md`](../SPECS.md) · [`docs/SEF.md`](SEF.md) · [`PRIVACY.md`](../PRIVACY.md) · [`SECURITY.md`](../SECURITY.md)

---

## 0. The answer in one paragraph

v0.0.4 proved the record. **v0.0.5 must prove the relationship.** The app is currently a
beautiful, honest, local journal that cannot complete a single real-world TLC interaction: there
is no person to address, no bank account to use, no peer-support flow, and no way to reach the
Concept Pioneer that is better than opening About. v0.0.5 should be the **usability release**: a
new visual language with no pink wash, distinct names for every person and role, a bank vault that
saves an account once and reveals it only on purpose, the missing Send Support flow, the
**Pioneer's Token** (renamed from "Pioneer dues"), and a **Pioneer Desk** that makes contacting the
founder one tap. It stays **local-only**: the complete experience ships without hosting, without
accounts and without a network permission. It also adopts the **Contact & Consent** laws —
intentionality, visible consent, reversible permission — for the one contact surface it has, and
stages the full social layer for v0.0.6.

**Do not put remote experience, the full SEF economy, accounts, multi-device sync or the
attestation console in v0.0.5.** Those are v0.0.6 or later. A release that tries all of it will
ship none of it well.

---

## 1. Why v0.0.4 is not usable yet (evidence from the code)

| # | Gap | Where it is visible |
|---|---|---|
| 1 | **No person to address.** `Profile` is a display name and a circle name. Grants and recognition are free text. There is no address book, no handle, no way to say "this is the same Ada" twice. | `core/experience/.../Model.kt` `Profile`; `JournalState.addRecognition(who, what)` |
| 2 | **No bank details at all.** The app says so in the Dues screen: "Pioneer bank details are not stored in this build." The money loop is a dead end. | `Workspace.PIONEER_DETAILS_NOTE`; `DuesScreen` |
| 3 | **No peer-support flow.** `SUPPORTIVE` and `RECOGNITION` exist in the domain and have no screen. The product's central promise — direct support between two people — is not implemented. | `TokenKind` vs `TlcApp.Dest` |
| 4 | **The dyad is theatre.** A Co-Steward is a local persona on one device. The app admits it: "local personas, not accounts." Two-key decisions cannot span two phones. | `circle_identity_note`; `Workspace.addCoSteward` |
| 5 | **No remote.** No `INTERNET`, no account, no sync, no attestation desk. Correct for v0.0.4, but it means the app cannot be used between two people who are not in the same room. | `AndroidManifest.xml`; `release.yml` |
| 6 | **Contact is buried.** Three rows at the bottom of About. No purpose, no template, no channel chooser, no follow-up, no record that you reached out. | `AboutScreen.ContactRow` |
| 7 | **The UI is a demo surface.** Pink-tinted gradient panels, five tabs with Settings as a tab, long doctrine paragraphs, no clear next action. | `Theme.kt`; `Components.GlassPanel`; `TlcApp` |
| 8 | **The naming is wrong for the founder.** "Pioneer dues" reads like a debt; the founder wants **Pioneer's Token**. | `strings.xml`; `docs/SEF.md` |

The result is a record with no relationships. v0.0.5 closes that gap.

---

## 2. The three principles for this release

1. **Local is the product; remote is deferred.** Every core flow works offline. v0.0.5 ships no
   network experience at all: no Worker, no remote ticket, no account. A later release may add
   reach; it must never be required for a local action.
2. **Save once, reveal on purpose.** The app remembers a bank account so the user never retypes
   it. It auto-*selects* the account; it never auto-*discloses* the full number. Disclosure needs
   a concrete context, a stated reason, a re-authentication, and a short expiry.
3. **Every role has a name; every name is distinct.** People get `@handles`; roles and characters
   get reserved handles and badges. Nobody can impersonate the Pioneer or a steward, and the
   record can tell a person from a role at a glance.
4. **Consent before contact.** Contact is intentional, visible and reversible. v0.0.5 applies this
   to the one contact surface it has — the Pioneer Desk — and adopts the full Contact & Consent
   Framework as the doctrine for the social layer in v0.0.6 (section 7A).

---

## 3. Scope: must / should / will not

### Must ship (the release is not v0.0.5 without these)

1. **Design system v2** — no pink surfaces, logo integrated, 4-tab IA + Create sheet, contrast and
   a11y tests, every screen polished.
2. **Names** — `@handle` on the profile, a local People address book, reserved handles and badges
   for every role/character, handles used in the record.
3. **Bank Vault** — save an account once, NUBAN validation, encrypted at rest, reveal sheet with
   re-auth and countdown, never in the ledger/export/logs.
4. **Send Support** — the missing peer flow (Supportive / Recognition) using the vault and the
   address book, with Receptive Mode enforced and no optimistic "paid".
5. **Pioneer's Token** — rename, new notice with the "Wording under legal review" label, the full
   flow from token to proof to contact.
6. **Pioneer Desk** — self-serve FAQ, one-tap contact with purpose/intent, templates and channels,
   preview-before-send, local contact log and follow-up. **No remote ticket.**
7. **Contact & Consent (local slice)** — the three laws, a guided request instead of a DM, and
   Receptive Mode on Send Support. The full social layer is v0.0.6 (section 7A).
8. **Honest release** — migration from v0.0.4, updated docs/store copy/release notes, signed
   artifacts, all gates green.

### Should ship (cut first if time is short)

- Multiple bank accounts per person.
- vCard share / "save the Pioneer as a contact".
- Reactivation reminder (local notification, no server).
- A "Share a seal" image card (not just text).
- Tablet/foldable two-pane layout for Record and People.

### Will not ship in v0.0.5 (deliberately deferred)

- **Remote experience of any kind:** Worker, remote contact ticket, Pioneer bank-detail release,
  `full` connected build, "when you connect" privacy section.
- **Contact & Consent social layer:** profiles/discovery, follow, connection requests, DMs,
  inbound consent dashboard, verification badges, contact-frequency limits, block/report,
  community spaces, AI moderation/assistance.
- **WhatsApp channel:** deferred because Telegram already uses the same number.
- Full SEF valuation, curation, transfer and distribution record.
- Accounts, passkeys, handle registry and remote uniqueness.
- Sync, multi-device merge, cross-device dyad invitations.
- Remote attestation console and signed attestation import.
- PWA, iOS, card acquiring, crypto, escrow, wallet, balance.
- Any analytics or crash reporting.

---

## 4. Pillar 1 — Design system v2 (the end of the pink)

### 4.1 What is actually wrong

The complaint is not one hex value. It is the **combination**:

- `Rosewood` `#8A2F45` and `RosewoodMist` `#F7DEE2` are the primary pair.
- `DarkRose` `#F0A8B6` is the dark-mode primary.
- `GlassPanel` paints a gradient from `primaryContainer` → `surface` → `tertiaryContainer` on
  almost every card, so the pink is not an accent; it is the **surface**.
- The result reads as a rose-tinted fintech demo rather than paper, ink and a seal.

### 4.2 The rule that fixes it

> **Neutral paper. Deep ink. One accent, used like a seal — never as a wash.**

- No tinted surface gradients. `GlassPanel` becomes a flat `Panel`: `surface`, 1dp `outlineVariant`,
  optional 1dp top highlight. Remove the blur-like depth; it costs frames and reads as decoration.
- The logo's dominant colours become the **seal accent**: royal blue for primary actions, azure
  for the active state and focus, silver-grey for hairlines. None of them becomes the background.
- Money and hashes get their own treatment: tabular numerals for money, monospace for hashes.
- The palette below is derived from the founder's logo description (royal-blue, azure, white,
  silver-grey). It stays a **candidate** until the SVG/PNG master is in the repo and the exact
  pixels can be measured. It is cool, calm and paper-like — nowhere near pink.

### 4.3 Candidate palette (light)

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#F4F7FB` | app background (cool paper, silver-white) |
| `surface` | `#FFFFFF` | cards, sheets |
| `surfaceRaised` | `#FFFFFF` | dialogs, focused fields |
| `ink` | `#0B1B33` | primary text (deep navy) |
| `inkSoft` | `#4A5B73` | secondary text (slate) |
| `line` | `#D9E2EE` | hairlines (silver-grey) |
| `primary` | `#1B3A8C` | primary actions, active tab (royal blue) |
| `onPrimary` | `#FFFFFF` | text on primary |
| `primaryContainer` | `#DEE8FB` | quiet selected states (pale azure) |
| `onPrimaryContainer` | `#10275F` | text on selected states |
| `seal` | `#1466C7` | the logo/azure accent, used sparingly |
| `onSeal` | `#FFFFFF` | text on the seal accent |
| `sealContainer` | `#E3F0FF` | token/recognition badges |
| `onSealContainer` | `#0E3F7E` | badge text |
| `success` | `#1F6B4A` | verified, acknowledged |
| `warning` | `#8A5A12` | pending, expiring |
| `error` | `#B3261E` | damaged record, exception |

### 4.4 Candidate palette (dark)

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#0A1220` | background (midnight navy) |
| `surface` | `#111C2E` | cards |
| `surfaceRaised` | `#16243A` | dialogs, focused fields |
| `ink` | `#EAF1FB` | primary text |
| `inkSoft` | `#A9B8CC` | secondary text |
| `line` | `#24344C` | hairlines |
| `primary` | `#7FB2FF` | primary actions (light azure) |
| `onPrimary` | `#08131F` | text on primary |
| `primaryContainer` | `#17325C` | quiet selected states |
| `onPrimaryContainer` | `#D6E6FF` | text on selected states |
| `seal` | `#8CC4FF` | accent |
| `onSeal` | `#08131F` | text on the seal accent |
| `sealContainer` | `#123A66` | token/recognition badges |
| `onSealContainer` | `#D6ECFF` | badge text |
| `success` | `#6FD3A3` | verified |
| `warning` | `#F0BE6A` | pending |
| `error` | `#FFB4AB` | error |

Contrast: every text/background pair must reach WCAG 2.2 AA (4.5:1 body, 3:1 large and UI). The
candidate pairs were checked: the lowest light-mode ratio is `seal` on `canvas` at 5.20:1 and the
lowest dark-mode ratio is `primary` on `canvas` at 8.67:1. Add a JVM test that computes the ratios
from the token table, so a future palette edit cannot quietly fail.

### 4.5 Logo: first master created from the description

The founder supplied this description:

> "A minimalist geometric vector emblem with a crowned circular figure and a split-tone heart
> incorporating botanical and celestial motifs. Refined flat-design aesthetics, bold negative
> space, balanced symmetry, crisp silhouettes, and a royal-blue, azure, white, and silver-grey
> palette. Presented as a standalone emblem and mobile app icon."

A first **true vector master** is now in the repo:

- `design/logo/tlc-logo.svg` — the emblem, transparent background, 512×512 viewBox.
- `design/logo/tlc-logo-icon.svg` — the emblem on a rounded midnight-navy app-icon tile.
- `design/logo/preview.html` — light/dark and 48px review page.
- `design/logo/README.md` — intake notes and production direction.

What the master contains:

- a silver outer ring with an azure inner rule;
- a crowned circular figure (crown, head, minimal face) in royal blue and azure;
- a split-tone heart (royal blue / azure) with a white centre gap;
- a white botanical sprout inside the heart;
- silver laurel leaves flanking the heart;
- celestial four-point stars and dots around the ring.

The geometry is deliberately bold so it survives 24–48px. The SVG is the master; PNG exports are
generated from it, never traced. The founder still needs to review the shapes — especially the
crown contours and the heart's internal symbols — before the icon is locked.

Next asset work, once the shape is approved:

- adaptive-icon foreground/background layers (66dp safe zone inside a 108dp canvas);
- the monochrome/themed icon for Android 13+;
- the Android 12 splash mark;
- the About mark and the store listing icon;
- optimised PNG/WebP exports at 48/72/96/144/192/512px.

The design system keeps the accent in one token, so the palette can still be tuned without
touching screens.

### 4.6 Information architecture (approved)

**Approved: 4 tabs + one Create action.**

| Tab | Contains |
|---|---|
| **Today** | greeting with `@handle` and role badge; seal card; one primary next action; open loops; Pioneer card; recent entries |
| **Record** | the chain, search, filters (Recognition, Support, Grants, Pioneer's Token, Governance), entry detail with proof |
| **People** | your circle (Primary/Co-Steward), address book, person detail (handle, role, saved account ref, history) |
| **Settings** | appearance, motion, haptics, Receptive Mode, **Bank Vault**, **Remote**, privacy, backup, verify, about |

The Create FAB opens a sheet with four verbs: **Recognise**, **Send Support**, **Pledge a
Grant**, **Pioneer's Token**. Grants and Pioneer's Token stop being top-level tabs; they become
create verbs and Record filters. This removes the "Settings is a tab" waste and gives the app one
obvious action.

Contingency only (not the approved plan): if the navigation refactor proves too large inside the
release, keep five tabs, add the Create sheet, and move Settings to the top bar. Do not ship the
current five-tab layout unchanged.

### 4.7 Component changes

| v0.0.4 | v0.0.5 | Why |
|---|---|---|
| `GlassPanel` (gradient) | `Panel` (flat, hairline, optional top highlight) | remove the pink wash and the fake glass |
| `RecordHero` | `SealCard` | one clear statement of the record's health |
| `ActionTile` | `VerbTile` | one primary verb per screen; secondary verbs are quieter |
| `Pill` | `Badge` | role/status badges are consistent |
| `ModuleRow` | `PersonRow` / `LinkRow` | the app now has people, not just modules |
| — | `DestinationCard` | bank · last4 · holder · "Show full details" |
| — | `RevealSheet` | re-auth, 60-second countdown, copy, clear |
| — | `ContactSheet` | reason → channel → preview → send |
| — | `EmptyState` with a next action | no dead ends |

### 4.8 Typography, shape, motion

- Keep the system serif for display and the system sans for body; no bundled fonts (download size,
  licence surface). Add **tabular numerals** for money and **monospace** for hashes.
- Shape: 12dp small, 18dp medium, 24dp large, 32dp sheets. Fewer radii, used consistently.
- Motion: 180–240ms, one shared easing; respect `prefers-reduced-motion` / the existing Motion
  preference; no entrance animation on money surfaces.
- Touch targets: 48dp minimum; TalkBack labels on every icon; large-type pass at 200%.
- Every screen has designed empty, loading, error, offline and blocked states.

### 4.9 What "better UI" means concretely

- **One primary action per screen.** Today has one; the Create sheet has four equal verbs.
- **Less doctrine, better placement.** The financial notice stays exact, but it lives in a
  collapsible "The short version" card and in About, not in the middle of every flow.
- **Money surfaces are calm.** No gradients, no confetti, no animation; amount, destination,
  reason, notice, one button.
- **The record is legible.** Entry rows show `@handle`, kind, time, amount (when present), and a
  one-line subject. Tap for proof.
- **The app answers "what next?"** Every empty state offers the next action; every pending state
  says who is waiting and for what.

---

## 5. Pillar 2 — Names: people, roles, characters

### 5.1 The model

Add a `Persona` to the domain:

```kotlin
enum class PersonaKind { PERSON, ROLE, CHARACTER, SYSTEM }

data class Handle(val value: String) {
    // 3–24 chars, lowercase a-z 0-9 _, starts with a letter, no leading/trailing underscore,
    // no double underscore, not reserved.
}

data class Persona(
    val primaryHandle: Handle,
    val aliases: Set<Handle> = emptySet(),  // e.g. @pioneer and @sircollins
    val displayName: String,
    val kind: PersonaKind,
    val role: Role?,          // for a person
    val badge: String?,       // "PRIMARY STEWARD", "CO-STEWARD", "PIONEER"
    val reserved: Boolean,    // cannot be claimed by a person
) {
    // primaryHandle + aliases must all be unique across every persona.
}
```

`Profile` gains `handle`. `Steward` gains `handle`. `AppSnapshot` gains `people: List<Person>`.
`GrowthGrant` gains `beneficiaryHandle: String?`. `DueRecord` keeps its ids; the UI resolves them
to handles. Every ledger subject that names a person uses `@handle` (e.g.
`Recognised @ada · covered the night shift`), so the record is unambiguous.

The Concept Pioneer is one `Persona` with `primaryHandle = @pioneer` and `aliases = {@sircollins}`.
Lookups, mentions and reserved-handle checks must match **either** address and resolve to the same
person; a message may be signed `@sircollins` while the card still shows `@pioneer`.

### 5.2 Reserved handles and badges

**Approved:** both Pioneer handles exist and are linked to the same person.

| Character | Handle | Badge | Rule |
|---|---|---|---|
| Concept Pioneer (role) | `@pioneer` | `PIONEER` | reserved; the official role handle shown on the Pioneer card |
| Concept Pioneer (person) | `@sircollins` | `PIONEER` | reserved; the founder's person handle, shown in person detail and message signatures |
| Primary Steward | person's `@handle` | `PRIMARY STEWARD` | one per circle |
| Co-Steward | person's `@handle` | `CO-STEWARD` | one per circle |
| Account Steward | `@steward` | `STEWARD` | reserved; delegated, scoped, expiring |
| Community Manager | `@community` | `COMMUNITY` | reserved; regional moderation |
| TLC system | `@tlc` | `SYSTEM` | reserved; never a person |
| Single / I-Partner / Love Model | person's `@handle` | role badge | role is descriptive, not a handle |

`@pioneer` and `@sircollins` resolve to one `Persona` with two addresses: the role address and the
person address. The Today card and contact sheet show `@pioneer`; the person detail, vCard and
message signature show `@sircollins` as well. Neither can be claimed by another user, now or in the
v0.0.6 registry.

Reserved list (cannot be claimed): `tlc`, `pioneer`, `sircollins`, `steward`, `co-steward`,
`community`, `support`, `admin`, `system`, `help`, `official`, `verified`, `team`, `tlc-support`.

### 5.3 Onboarding

- Ask for the handle after the name, with a suggested value (`@ada`, `@ada-o`, `@ada-2` if taken
  locally). Show the rule and a live preview: `@ada · Single · Primary Steward`.
- State plainly: "This handle stays on your device until you connect. It is how your record
  addresses you; it is not a public profile yet."
- Do not ask for bank details in onboarding. Ask contextually, the first time a flow needs them.

### 5.4 Why distinct names matter

- **No impersonation.** A person cannot be `@pioneer`, `@steward` or `@tlc`.
- **No ambiguity.** Two people called "Ada" are `@ada` and `@ada-o`, and the record shows which.
- **Roles are visible, not inferred.** The badge says Primary/Co-Steward; the handle says who.
- **Remote-ready.** When the handle registry arrives in v0.0.6, the same handle is the anchor; no
  migration of the record's subjects.

---

## 6. Pillar 3 — Numbers: the Bank Vault

### 6.1 The promise

> **Save once. Auto-select. Reveal on purpose. Never leak.**

### 6.2 What is stored where

| Data | Where | Export? | Ledger? | Logs? |
|---|---|---|---|---|
| Holder name, bank name, bank code, account number | `BankVault` (Android Keystore AES-GCM, private file) | **No** | **No** | **No** |
| `BankAccountRef` (id, bank, last4, fingerprint, verified, changedAt) | `AppSnapshot` | optional, last4 only | last4 only if a reveal is audited | no |
| Reveal event (time, account ref, context, reason) | `AppSnapshot` / ledger `BANK_DETAILS_REVEALED` | yes, no number | yes, no number | no |

The full number must never enter `StateCodec`, `BundleCodec`, `LedgerCodec`, a log, a crash
report, a notification, a URL, a screenshot or an analytics event. Add a property test that
serialises the whole state and bundle and asserts the number is absent.

### 6.3 Validation (local, no network)

- **Bank picker:** a bundled JSON list of CBN bank names and 3-digit codes (static data, no
  network). Searchable.
- **NUBAN:** 10 digits; first 3 must equal the selected bank code; validate the check digit with
  the CBN weights `3,7,3,3,7,3,3,7,3` (mod 10). This catches typos before the user sends money.
- **Holder name:** required, 2–80 characters, normalised whitespace.
- **Verification badge:** **no "Verified" badge in v0.0.5.** Every saved account shows "Saved by
  you · unverified". A future name-enquiry adapter can change that only when it actually returns
  a match. Never claim a verification that did not happen.

### 6.4 Reveal policy

A `RevealSheet` is allowed only when all of these are true:

1. There is a **concrete context**: a Send Support intent, a Growth Grant payment, a Pioneer's
   Token, or the user's own "share my details" action.
2. The sheet states the **reason**: "Shown because you are sending Support to `@ada`." or "Shown
   because your Activation Token is due."
3. The user taps **Show full account number** (never automatic).
4. Device credential confirmation (PIN/pattern/password/biometric) succeeds, if the device has a
   lock screen. If not, show a warning and require a second tap.
5. The number is visible for **60 seconds** with a countdown; `FLAG_SECURE` is set while it is
   visible; copying clears the clipboard after 60 seconds.
6. The reveal is audited with `bank · last4 · fingerprint`, never the number.

### 6.5 Disclosure policy (the important nuance)

- **Auto-select, never auto-share.** The app preselects the saved account in a flow. It does not
  publish it.
- **Your own details:** share only when you tap "Share my details" in a specific request. The app
  shows a copyable card; the full number is behind the reveal. TLC does not transmit it in
  v0.0.5.
- **Recipient details:** saved by the sender after receiving them; marked "Saved by you" until a
  name enquiry verifies them.
- **Dyad-shared account:** a change is a `SensitiveAction.CHANGE_BANK_DETAILS` proposal, two
  signatures, a 24-hour cool-down, and a notice to both members (R-3).
- **Recent change:** any account changed in the last 24 hours shows a warning in the destination
  card and requires an extra confirmation before a new outbound flow.

### 6.6 The "right time, right reason" matrix

| Flow | Account shown | Reason line | Expiry |
|---|---|---|---|
| Send Support to `@ada` | Ada's saved ref | "Shown because you are sending Support to `@ada`." | 60s reveal |
| Growth Grant payment | Beneficiary's saved ref | "Shown because you are paying the grant to `@ada`." | 60s reveal |
| Pioneer's Token (local) | Pioneer's ref, if saved | "Shown because your Activation Token is due." | 60s reveal |
| Share my details | Your own account | "Shown because you chose to share your details with `@ada`." | 60s reveal |

### 6.7 What we will not do

- No full account number in a URL, notification, analytics event, screenshot or export.
- No "verified" badge without an actual verification.
- No auto-publish of a user's bank details to a circle.
- No bank details in the ledger subject; last4 and fingerprint only.
- No "wallet", "balance", "deposit", "withdraw", "credit" language (the existing `BANNED_TERMS`).

---

## 7. Pillar 4 — The missing flow: Send Support

This is the flow that makes TLC real. It uses the People list and the Bank Vault.

```
Person → Token kind (Supportive / Recognition) → Amount (optional for Recognition)
      → DestinationCard (bank · last4 · holder)
      → RevealSheet (re-auth, 60s) → transfer in the user's banking app
      → Record intent (ledger: SUPPORT_INTENT)
      → Optional proof (reference, hash) → recipient acknowledges
      → Ledger: SUPPORT_ACKNOWLEDGED
```

Rules:

- **Receptive Mode is enforced** by the existing `SupportPolicy.gate`. A Single recipient with
  Receptive Mode off cannot receive Supportive/Recognition; the UI explains why.
- **No optimistic "paid".** States are `intent → claimed → acknowledged`; the app never says
  "paid" or "confirmed by bank" unless a rail adapter actually confirmed it.
- **Recognition carries no amount.** Supportive and Growth Grant may carry an amount; the amount
  is chosen by the sender, never auto-charged.
- **The money moves in the user's bank.** The app opens the banking app or shows the instruction;
  it never initiates a transfer.
- **Cross-device reality in v0.0.5:** the recipient shares their details manually (copy/share a
  Support Card; no bank details in a URL). The remote relay is v0.0.6. Say this in the UI so the
  flow is honest.
- **Growth Grant reuses the same DestinationCard** for the final direct payment, so there is one
  bank-detail experience, not two.

New ledger kinds: `SUPPORT_INTENT`, `SUPPORT_ACKNOWLEDGED`, `BANK_DETAILS_REVEALED`. Keep the
existing `EntryKind` names; add, never rename.

---

## 7A. Contact & Consent Framework — review and staging

### 7A.1 Verdict

The framework is strong doctrine and should become the **constitution of the social layer**. It is
not a v0.0.5 feature set. The current app has no accounts, no discovery, no inbox, no server and no
inbound messages, so most of the framework has nothing to attach to yet. Building it now would
turn a focused usability release into an unfinished social network.

**Adopt the laws now. Ship the local slice now. Build the social layer in v0.0.6+.**

### 7A.2 What ships in v0.0.5 (local slice)

| Framework element | v0.0.5 implementation |
|---|---|
| **Intentionality** | Pioneer contact requires a **purpose** before a message exists: Activation help, Reactivation help, Pioneer's Token question, Support/transfer help, Mentorship, Collaboration, Partnership/media, Report a problem, Something else. |
| **Visible consent** | Send Support is gated by the existing `SupportPolicy.gate` and Receptive Mode. The sender sees the recipient's state before choosing an amount. The Pioneer contact sheet says plainly: "This is a request, not a direct message." |
| **Reversible permission** | The local contact log can be cleared; follow-up reminders can be turned off; Receptive Mode can be changed at any time. There is no punishment or hidden state. |
| **Role-aware contact** | The Pioneer Desk adapts the form by the user's role (`SINGLE`, `I_PARTNER`, `LOVE_MODEL`). A Love Model gets the mentorship request shape; a Single gets the activation/guidance shape. |
| **Request structure** | Before contacting the Pioneer the user provides: reason, category of discussion, expected guidance (optional), and the message. The template is prefilled and editable. |
| **Language** | Use **"Respectful Contact"** / **"Intentional Connection"**, never "Messaging" or "DM", until a real messaging system exists. |
| **No cold contact** | There is no discovery feed, no follow, no unsolicited message, and no contact-frequency system in v0.0.5 because there is no inbound channel to protect. |

Add a small `ContactIntent` value object used by the Pioneer Desk:

```kotlin
data class ContactIntent(
    val purpose: ContactPurpose,      // activation, reactivation, token, support, mentorship, ...
    val category: String?,            // "guidance", "collaboration", "media", ...
    val expectedGuidance: String?,    // optional, Love Model mentorship requests
)
```

It is local, appears in the prefilled message, and is recorded in the contact log without the
message body.

### 7A.3 What waits for v0.0.6 or later

| Framework element | Why it waits |
|---|---|
| Profiles, discovery, follow, connection requests | Needs accounts, a directory and remote uniqueness. |
| Direct messages and mutual-approval chat | Needs an inbox, delivery, blocking and abuse handling. |
| Universal consent dashboard (inbound permissions, who-can-contact-me) | Only meaningful when someone can contact you through TLC. |
| Verification badges (identity, Love Culture, Love Model) | Needs an identity provider and a trust authority; cannot be claimed locally. If ever built, a badge may signal trust but **must not buy reach** — no higher visibility or "more profiles" for verified members, or it contradicts "impact over popularity". |
| Contact-frequency limits, cooldowns, anonymous reports | Needs identity and a server-side record. |
| AI moderation / respectful-message suggestions | Explicitly deferred by the founder; needs a model, a policy and an appeal path. |
| Community spaces and collective impact | A separate product surface. |

### 7A.4 Role mapping

| TLC role | Framework behaviour | v0.0.5 | v0.0.6+ |
|---|---|---|---|
| **Single** | Follow, request connection, respectful message request; messaging only after approval | Activation/guidance contact to the Pioneer | Discovery, follow, connection requests, consent dashboard |
| **Intentional Partner** | Intent tag (friendship, courtship, collaboration, guidance); chat only when both approve | Intent tag on Pioneer contact | Mutual-approval connection and chat |
| **Married / Love Model** | Mentorship request, guided contact form, invite to conversation; accept/decline/redirect | Guided mentorship request to the Pioneer | Mentorship queue, accept/decline/redirect, resources |

Roles describe experience, not worth. Access may be role-specific; dignity and consent are
universal. This matches the existing `Role` enum and the doctrine.

### 7A.5 Design laws carried forward

- Consent is explicit, informed, purpose-specific and revocable.
- Privacy by default: a Single starts at **Limited Contact**, not Open; a new profile is not
  discoverable until the person chooses it.
- Verification signals trust, never reach: no higher visibility, faster approval or "more
  profiles" for verified members.
- No engagement manipulation, forced visibility or coercive monetization.
- Non-custodial: TLC never holds user funds.
- Portable identity and data where feasible; no vendor lock-in.
- Impact over popularity: no status hierarchy based on followers, payment or engagement.
- Accessible on low-end Android and unreliable connections.

These are already consistent with `CONTEXT.md` and `SPECS.md`. Add them to the doctrine index when
the social layer is scheduled, not as a v0.0.5 code change.

### 7A.6 What we will not pretend

v0.0.5 must not show a "Consent Dashboard" that controls nothing, a "Verified" badge that verified
nothing, or a "Message" button that cannot deliver. The local slice is honest: a guided request
through the user's own Telegram, email, phone or website. The social layer arrives when there is a
real inbound channel to govern.

### 7A.7 Framework changes to make before v0.0.6

The framework is sound, but four points need tightening before it becomes a build spec:

1. **Verification must not buy reach.** "Higher request visibility, faster approval probability,
   access to more profiles" is a status hierarchy and contradicts "impact over popularity". A
   badge may say "identity checked"; it must not change who sees whom.
2. **Privacy default for Singles.** Start at **Limited Contact**, not Open. Discovery should be
   opt-in, not opt-out.
3. **Define the consent record.** A real consent engine needs `scope`, `purpose`, `grantedAt`,
   `expiresAt?`, `revokedAt?` and an audit trail. "Revocable" is not a button; it is a data model.
4. **AI moderation needs a human path.** If it ever ships: no automatic punishment, a visible
   reason, an appeal, and a bias review. Until then, defer it completely (the founder already
   chose this).

Two smaller notes: contact-frequency limits must be controlled by the receiver, not the platform;
and anonymous reports need evidence handling and protection from weaponised reporting.

---

## 8. Pillar 5 — Pioneer's Token + Pioneer Desk

### 8.1 Rename table (approved, user-facing)

| v0.0.4 | v0.0.5 |
|---|---|
| Pioneer dues | **Pioneer's Token** |
| Record a due | Record a Pioneer's Token |
| Due | Token |
| Activation | Activation Token |
| Reactivation | Reactivation Token |
| Courtesy | Courtesy Token |
| Joy | Joy Token |
| Awaiting attestation by the Concept Pioneer | Awaiting the Pioneer's confirmation |
| Fee | Token (in the user-facing flow; "fee" may remain in legal copy) |
| Dues (nav/filter) | Pioneer's Token |

**Do not rename persisted identifiers** (`DueRecord`, `dues`, `EntryKind.DUE`, `due:*`
idempotency keys, `StateCodec` field names) in v0.0.5. Add `@SerialName` aliases if a Kotlin name
must change. The ledger is append-only and the state file must migrate; a cosmetic rename is not
worth a data loss.

### 8.2 The notice (draft — legal review required before public launch)

Replace `DoctrineCopy.FINANCIAL_NOTICE` with:

> TLC facilitates direct peer-to-peer support. It does not hold, pool, escrow or move money.
> **Pioneer's Tokens** — Activation, Reactivation, Courtesy and Joy — are monetary support sent
> directly to the Concept Pioneer's own account; they sustain the platform. **Community Tokens** —
> Supportive, Recognition and Growth Grants — are records of recognition and support sent directly
> to the intended recipient. A Token is a record of value or recognition, never a balance: it is
> not transferable, not redeemable, not an investment and not cryptocurrency. TLC is not a party
> to any transfer.

Add a separate `PIONEER_TOKEN_NOTICE`:

> A Pioneer's Token is the name TLC gives to the support allocated to the Concept Pioneer:
> Activation, Reactivation, Courtesy and Joy. It is a record of a direct transfer to the Pioneer's
> own account. It is not a balance, not transferable, not redeemable, not an investment and not
> cryptocurrency. TLC never holds, pools, escrows or moves the money.

This is a **doctrine change**: the old `InvariantsTest` asserts "every token is a recognition
signal" and "no cash value". The new test must assert the new position: one token nature
(`RECORD_OF_VALUE`), a `monetary` flag on the kinds that settle money, no custody, no balance,
no transferability, no redemption, no investment, no crypto. Update `CONTEXT.md` INV-03 and add
ADR-008 in the same change. Do not let the copy and the test disagree.

#### What "legal review" means here (plain language)

The rename is easy. The **claim** is the risk. If the app says a Pioneer's Token is "monetary
support sent directly to the Concept Pioneer's own account", a regulator, a bank or a payment
provider can read that in one of three ways:

1. **Gifts / dues to a person** — the current model: money moves directly between two people, TLC
   never touches it. Lowest regulatory burden, but recurring payments to a personal account can
   still look like unregistered financial activity (this is **R-1**).
2. **A product sold by an entity** — if TLC (rather than the Pioneer personally) is seen as
   receiving the money, the founder may need a registered company, a business account, receipts,
   tax treatment and consumer terms.
3. **A regulated financial product** — if "Token" plus "monetary" is read as a deposit, e-money,
   security or investment, a licence or a licensed payment processor is required (**R-2**).

The review is a lawyer answering three concrete questions:

1. On these words and this flow, is a Pioneer's Token a **gift/dues payment to an individual**, or
   is TLC **selling a product**?
2. If it is a product, should the money go to a **registered entity/business account**, or through
   a **licensed payment processor** — and what changes in the app?
3. What **tax, receipt, refund and disclaimer** wording is required, and is the draft notice in
   this section safe to publish as written?

Until that review is done, the safe default is:

- Ship the **rename** (it is only a name) and the flow.
- Keep the non-custody, no-balance, no-transfer, no-redemption, no-investment, no-crypto lines.
- Mark the monetary framing in-app as **"Wording under legal review"** in the About/legal screen.
- Do **not** market it as an investment, savings, yield or deposit product, and do not add referral
  commission or profit share.
- Do not publish the public launch/store listing with the final wording until the lawyer signs off.

This does not block v0.0.5 engineering. It blocks the final public wording, not the code.

### 8.3 The flow

```
Kind (Activation / Reactivation / Courtesy / Joy)
  → Amount (prefilled: 1,000 / 2,000 / 10% voluntary / voluntary)
  → Token exists (ledger: PIONEER_TOKEN_INTENT)
  → Release instruction:
       local: reference + "contact the Pioneer for the account" (foss)
  → Transfer in the user's bank
  → Proof: amount + bank reference → hash (ledger: PROOF_RECORDED)
  → "Send this to the Pioneer" (Pioneer Desk, prefilled)
  → Awaiting the Pioneer's confirmation
```

The app still does not attest a Pioneer's Token locally. Only the Pioneer can do that. The
contact handoff is the bridge until the remote attestation console ships in v0.0.6.

### 8.4 Pioneer Desk — the contact automation

**Self-serve first (deflect):**

- **Pioneer Answers** — a bundled FAQ with 10 questions:
  1. What is TLC?
  2. What is a Pioneer's Token?
  3. How do I activate?
  4. How do I reactivate?
  5. Where do I send it?
  6. How do I know the Pioneer received it?
  7. What is Courtesy? Is it required?
  8. What is Joy?
  9. How do I change my bank details?
  10. How do I reach a human?
- **Status first** — the token's state, the reference, and "what happens next" before the contact
  button.
- **Copyable answers** — every FAQ answer can be copied or shared, so the Pioneer can reuse it.

**One-tap escalation (the useful part):**

- A **Pioneer card** on Today: `Concept Pioneer · @pioneer` + "Contact".
- A **Contact sheet**: purpose → category/guidance (role-aware) → channel → preview → send.
- **Purposes:** Activation, Reactivation, Pioneer's Token question, Support/transfer help,
  Mentorship, Collaboration, Partnership/media, Report a problem, Something else.
- **Channels:** **Telegram first** (the founder's Telegram already uses the WhatsApp number),
  Email, Phone, Website. These are Android `Intent`s; they work in `foss` with no `INTERNET`
  permission in our app.
- **WhatsApp is deferred.** The founder's Telegram already uses the same number, so adding a
  second channel would duplicate the inbox without adding reach. Do not build a WhatsApp config,
  do not invent a number, and do not show the channel. If a separate WhatsApp identity appears
  later, it can be added as a configuration value without a schema change.
- **It is a request, not a DM.** The sheet says so, and there is no TLC inbox in v0.0.5. The reply
  comes through the founder's own channel.
- **Prefilled message** includes only what the user can see and edit: name, `@handle`, role,
  circle, token reference, app version, and the reason. **No bank details, no amounts unless the
  user leaves them in, no message history.**
- **Preview before send.** The user sees the exact text and can edit it.
- **Local contact log** — `ContactEvent(channel, reason, at, tokenRef?)` in a separate local log
  (or a ledger entry `CONTACT_PIONEER` with no message body). Today shows "You contacted the
  Pioneer 2 days ago about Activation. Follow up?" after 3 days.
- **Copy message** for any channel, so the user is never trapped if the app is not installed.
- **Save the Pioneer as a contact** via a vCard share (no contacts permission).

**No remote ticket in v0.0.5:**

- There is no "Send through TLC" option, no Worker, no ticket id and no status polling.
- If the user has no Telegram/email/phone app, the sheet offers **Copy message** and **Save the
  Pioneer as a contact**; the user can send from any other app.
- The founder replies from his normal channels. A structured inbox, if ever needed, is a v0.0.6
  decision and requires the remote review that v0.0.5 deliberately avoids.

**Why this is "moderately and efficiently useful":** it answers the common questions without a
human, makes the remaining contact one tap, gives the Pioneer a consistent message with context,
and records the follow-up so the user is not left wondering.

---

## 9. Pillar 6 — Local-only posture (remote deferred)

### 9.1 v0.0.5 ships local-only

The founder's decision: **the local build is complete without hosting, so v0.0.5 has no remote
experience.**

- `foss` is the only shipped artifact: no `INTERNET` permission, no HTTP client on the classpath,
  no analytics.
- No Worker, no `full` connected build, no remote ticket, no 15-minute bank-detail release.
- Contact uses Android `Intent`s (Telegram, email, phone, website). The app itself makes no
  network call.
- The `full` flavour stays dormant in the build files. Do not ship it and do not add
  `TlcDeskRemote`.
- `PRIVACY.md` stays the local-only policy. There is no "when you connect" section in v0.0.5.
- The future `RemotePort` and Worker design are kept in Appendix D for v0.0.6+. They are not part
  of this release.

### 9.2 Why this is the right call

- The local release has no accounts, no server and no inbound channel. Remote would add risk
  without completing a user task.
- Hosting adds secrets, retention, legal review, a DPIA and operational work. The founder has not
  justified it as vital or necessary.
- The privacy claim stays strong and simple: **no network permission**.
- The Contact & Consent social layer (section 7A) needs accounts and an inbox before remote makes
  sense. Build the local slice first; earn the right to connect later.

### 9.3 Release gates

- `foss`: assert **no** `INTERNET` permission, **no** HTTP client and **no** analytics dependency.
- One signed artifact, one SBOM, one checksum set, one privacy notice that matches the build.
- `PRIVACY.md` and `SECURITY.md` must describe the local-only build exactly.
- If a future release adds remote, it must add consent, retention, a kill switch, a DPIA and the
  privacy delta in the same commit. Not now.

### 9.4 What would justify remote later

Only if at least one is true:

1. Users genuinely cannot reach the Pioneer through Telegram, email, phone or website.
2. The Pioneer wants the app to release bank details rather than doing it by hand, and accepts the
   legal and operational responsibility of holding them as Worker secrets.
3. Contact volume is high enough that a structured ticket and status saves real time.

None of these is established for v0.0.5. Revisit in v0.0.6 with evidence, not enthusiasm.

---

## 10. Data model, schema and migration

### 10.1 New domain types

```kotlin
Handle, Persona(primaryHandle, aliases, displayName, kind, role, badge, reserved), PersonaKind
Person(id, handle, displayName, role, notes, createdAt)
BankAccount(id, holderName, bankName, bankCode, accountNumber)   // vault only
BankAccountRef(id, bankName, last4, fingerprint, verified, changedAt)
ContactEvent(id, channel, purpose, category?, at, tokenRef?)
ContactIntent(purpose, category?, expectedGuidance?)
SupportIntent(id, toHandle, kind, amountKobo?, at, state)
```

### 10.2 Schema changes (backward compatible)

- `Profile.handle: String?` (new; default derived on read).
- `Steward.handle: String?`.
- `AppSnapshot.people: List<Person> = emptyList()`.
- `AppSnapshot.bankAccounts: List<BankAccountRef> = emptyList()` (refs only).
- `Preferences.revealSeconds: Int = 60`, `followUpReminders: Boolean = true`.
- `ContactEvent.purpose` and optional `category`; no message body stored.
- `GrowthGrant.beneficiaryHandle: String?`.
- `DueRecord.displayName: String?` (optional; UI falls back to `kind`).
- `StateCodec.FORMAT = "tlc-state-v2"`; **read v1 and v2**, write v2. A v1 file gets defaults;
  a v2 file read by v0.0.4 is refused (downgrade not supported).
- `BundleCodec` version 2; read v1 and v2. The bank vault is **not** in the bundle by default;
  a later "portable vault" can add an encrypted, passphrase-protected section.
- `LedgerCodec.HEADER` stays `#tlc-ledger-v1`; add `EntryKind` values; never rename persisted
  enum names or `due:*` keys.

### 10.3 Migration tests (must exist before the release)

- A real v0.0.4 state fixture loads into v0.0.5 with the same profile, preferences, stewards,
  grants, dues and proposals.
- A v0.0.4 bundle restores into v0.0.5 and re-verifies.
- A v0.0.5 state file is refused by a v0.0.4 reader with a clear message (no silent downgrade).
- A hand-edited state with an invalid handle, NUBAN or account ref is rejected, not guessed.
- A state/bundle export contains no full account number (property test).

---

## 11. Security and privacy delta

### 11.1 New threats

| Threat | Control |
|---|---|
| Bank-detail substitution (R-3) | last4 + fingerprint, re-auth, 24h cool-down for dyad changes, recent-change warning, no auto-publish |
| Shoulder surfing / screenshots | `FLAG_SECURE` on the reveal sheet, 60s countdown, no full number in notifications |
| Clipboard leakage | copy button + clear after 60s + a visible "Copied" toast |
| Malicious contact message | no attachments, 1,000-char limit, 10-digit-number warning; the user sends through their own Telegram/email app |
| Handle impersonation | reserved handles, badges, `@pioneer` reserved; a registry is v0.0.6 |
| Secrets in the APK | no bank details compiled in; gitleaks; release gate; the vault key stays in Android Keystore |

### 11.2 Privacy notice changes

v0.0.5 stays local-only, so the privacy notice does **not** gain a "when you connect" section.

- What leaves the device: **nothing through TLC**. Contact messages are handed to Telegram, email,
  phone or the browser by an Android `Intent`; the user sees the message before it is sent.
- What is stored locally: the bank vault (encrypted, never exported), the contact log (channel,
  purpose, time, token reference — no message body), and the record.
- What is not collected: analytics, crash reports, advertising identifiers, contact lists,
  location.
- Control: the user can clear the contact log, disable follow-up reminders, change Receptive Mode,
  and export the record without the bank vault.
- A remote privacy section is added only when a remote feature ships, in the same commit.

### 11.3 Legal flags (do not ignore)

- **R-1 — entity question:** a personal account receiving recurring Pioneer's Tokens can read as
  unregistered financial activity. A lawyer must decide whether this is gifts/dues to an
  individual, a product sold by a registered entity, or a regulated service. The answer changes
  the account, receipts, tax and terms. Until then: keep the non-custody framing and mark the
  wording "under legal review".
- **R-2 — instrument question:** "Token" + "monetary" can attract securities/e-money scrutiny. The
  notice must keep "not a balance, not transferable, not redeemable, not an investment, not
  crypto"; no yield, no referral commission, no profit share.
- **Status:** legal review is **pending**. Engineering may ship the rename and the flow behind a
  "wording under legal review" label; the final public/store wording waits for sign-off. See
  section 8.2 for the three questions the lawyer should answer.
- **NDPA:** a DPIA delta for the contact message and the bank vault; retention jobs; DSR paths.
- **PCI:** still out of scope; no card data, no acquiring.

---

## 12. Test plan

### 12.1 New unit and property tests

- Handle rules: length, charset, reserved list, normalisation, local uniqueness.
- Alias rules: `@pioneer` and `@sircollins` resolve to one persona; no alias collides with another
  primary handle or alias; lookups match either address.
- Persona badges: one Primary, one Co-Steward, no duplicate handles.
- NUBAN: valid examples, wrong bank prefix, wrong check digit, non-digits.
- Bank vault: encrypt/decrypt round-trip; wrong key fails; no plaintext on disk.
- Reveal policy: no context → refused; no re-auth → refused; expired → refused; audit has last4
  only.
- Disclosure policy: no auto-share; dyad change needs two signatures + cool-down.
- Contact templates: purpose → subject/body; role-aware mentorship shape; no bank details;
  1,000-char limit; preview matches sent text.
- Contact log: records channel, purpose, time and token reference; never the message body;
  clearable; follow-up reminder can be disabled.
- State migration v1 → v2; bundle migration v1 → v2; downgrade refused.
- Copy lint: no "Pioneer dues"; no "WhatsApp" in the v0.0.5 build; no banned terms in
  `strings.xml`; the new notices contain the required phrases and the "Wording under legal review"
  label.
- No full account number in state, bundle, ledger, logs (property test).
- `foss` has no `INTERNET` permission, no HTTP client and no analytics dependency.

### 12.2 UI and a11y

- Compose screenshot tests at phone, tablet and foldable widths for Today, Record, People,
  Settings, Send Support, Pioneer's Token, Contact, Reveal.
- Dark mode and 200% large type.
- TalkBack labels on every icon; 48dp targets; focus order.
- Contrast test for every token pair (AA).
- Empty, loading, error, offline and blocked states for each screen.

### 12.3 End-to-end

- Onboard with a handle → save a bank account → send Support to a person → record intent →
  acknowledge.
- Record a Pioneer's Token → release instruction → proof → contact the Pioneer (Telegram/email/
  phone/website) → awaiting confirmation.
- Contact the Pioneer from Today in one tap; the local log records purpose and channel; the
  follow-up appears and can be disabled.
- Restore a v0.0.4 bundle; the record verifies; the vault is empty and asks to re-enter once.
- Put the device in airplane mode: every local flow still works, and no screen offers a remote
  action that cannot happen.

### 12.4 Release gates

`make verify` · `make shell-check` · `make check-foss` · `make android-build` ·
`make ledger-verify` · `make security` · `make a11y` · `make contrast` · `make e2e` ·
signed `foss` APK · signature pinned · checksums · SBOM · release notes.

Target: ~165 tests (112 existing + ~55 new); coverage ≥80% on the new core code.

---

## 13. Release engineering

- `version.properties`: `versionName=0.0.5`, `versionCode=5`.
- **One artifact:** `tlc-0.0.5-foss.apk`. No `full` build, no connected variant.
- `release.yml`: build `foss` only; assert no `INTERNET`, no HTTP client, no analytics; sign;
  verify; publish; attach `SHA256SUMS`, dependency manifests, signature block, SBOM, provenance.
- F-Droid metadata: `foss` only, pinned to the release commit.
- Install page: new palette and logo, one download button, the verify block, and the local-only
  privacy statement ("no network permission, no analytics, no account").
- Store copy: title, short description, full description, changelog. Use "Pioneer's Token"; state
  the non-custody line; no banned terms; no "dues"; no "WhatsApp"; no remote promise.
- Docs: update `README.md`, `CONTEXT.md` (INV-03), `SPECS.md` (SK-03, SK-04, new SK-13
  bank-vault, SK-14 contact-desk), `docs/SEF.md` (v0.0.5 status), `PRIVACY.md`, `SECURITY.md`,
  `docs/DISTRIBUTION.md`, `release-notes/v0.0.5.md`.
- ADRs: 008 token nature, 009 bank vault, 010 handles, 011 contact & consent staging.
- Rollback: keep v0.0.4; migration is one-way; export before upgrade; a failed migration opens
  read-only with a restore path.

---

## 14. Timeline (6 focused weeks)

| Week | Work | Exit criterion |
|---|---|---|
| 1 | Logo master + icons, palette, tokens, IA, copy deck, ADRs, schema design | design tokens compile; logo reviewed; ADRs approved |
| 2 | Handles, people, bank refs, state v2, migration, tests | v0.0.4 fixture loads; new tests green |
| 3 | Bank Vault, NUBAN, reveal sheet, re-auth, Send Support | end-to-end support flow works offline |
| 4 | Pioneer's Token rename + flow, FAQ, contact sheet, local log, consent slice | one-tap contact works in `foss` |
| 5 | UI polish, empty/error/offline states, local-only privacy/release gates | every screen finished; `foss` clean |
| 6 | a11y/contrast, e2e, store copy, release notes, build, tag | signed artifact published; all gates green |

If the work is part-time, plan 8–10 weeks. Cut the "should ship" list before cutting tests,
migration or the privacy gate.

---

## 15. Risks and mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Legal challenge to "Pioneer's Token" monetary framing | Medium | High | legal review; "Wording under legal review" label; exact non-custody notice; no yield/investment language; keep R-1/R-2 open |
| Bank-detail substitution fraud | Medium | High | last4/fingerprint, re-auth, cool-down, recent-change warning, no auto-publish |
| Scope creep from the Contact & Consent social layer | High | High | section 7A staging: local slice only; profiles, DMs, verification and AI are v0.0.6+ |
| Scope creep (sync, SEF, accounts, remote) | High | High | the must/won't table; v0.0.6 for the rest |
| Logo is a first draft from a text description | Medium | Medium | founder reviews the SVG; refine geometry before locking the icon; palette in one token |
| Handle squatting / impersonation | Medium | Medium | reserved list, badges, remote registry in v0.0.6 |
| Migration loses data | Low | High | read v1/v2; fixture tests; export before upgrade; read-only on failure |
| UI refresh breaks accessibility | Medium | Medium | contrast test, TalkBack, 48dp, large type, screenshot tests |

---

## 16. Decisions: confirmed and still open

### Confirmed by the founder

| # | Decision | Result |
|---|---|---|
| 1 | Scope cuts | **Approved.** Full sync, accounts, SEF economy and the attestation console move to v0.0.6. |
| 2 | Information architecture | **Approved.** 4 tabs (Today, Record, People, Settings) + Create sheet. |
| 3 | Pioneer handles | **Both exist.** `@pioneer` is the official role handle; `@sircollins` is the person handle; both reserved and linked. |
| 4 | Logo direction | **Approved.** Royal-blue, azure, white, silver-grey; crowned circular figure with split-tone heart and botanical/celestial motifs. A first SVG master is created from the description; founder review still needed. |
| 5 | WhatsApp | **Deferred.** The founder's Telegram already uses the WhatsApp number, so v0.0.5 uses Telegram first and does not add WhatsApp. |
| 6 | Remote experience | **Deferred.** No Worker, no remote ticket, no `full` connected build. The local build is complete without hosting. |
| 7 | Legal wording | **Ship the rename with the "Wording under legal review" label.** Hold the final public wording until the lawyer replies. |
| 8 | Contact & Consent Framework | **Adopted as doctrine; staged.** Local slice in v0.0.5 (section 7A); profiles, DMs, consent dashboard, verification, anti-abuse and AI in v0.0.6+. |

### Still open (blocks final polish or public launch, not the first code)

1. **Logo review.** The SVG at `design/logo/tlc-logo.svg` is a first master built from the text
   description. Review the shapes, crown and heart; then lock the icon geometry. PNG exports are
   generated from the SVG, not traced.
2. **Legal review** of the "Pioneer's Token" monetary framing and the R-1 entity question (see
   8.2 and 11.3). Engineering ships with the label; the final public/store wording waits.
3. **Contact & Consent schedule.** Confirm that the social layer is v0.0.6+ and that v0.0.5 only
   carries the local slice.
4. **Privacy posture.** v0.0.5 is local-only: no `INTERNET`, no analytics, no account. Approve
   that the privacy notice stays local-only.

---

## 17. Definition of done

A new user can, in under five minutes and without reading a manual:

1. Onboard with a distinct `@handle` and see their role badge.
2. Save a bank account once; it is validated locally and never appears in an export or log.
3. Send Support to a named person, reveal the destination on purpose, and record the intent.
4. Record a Pioneer's Token, get the instruction, submit proof, and contact the Pioneer in one tap
   with a purpose and a preview.
5. See exactly what the app sent, to whom, and clear the local contact log.

And the build proves:

- `foss` has no `INTERNET`, no HTTP client and no analytics.
- The full account number never enters the state, bundle, ledger, logs, notifications or
  screenshots.
- v0.0.4 data and bundles migrate and verify.
- No pink surface wash; AA contrast; TalkBack; 48dp; dark mode; 200% type.
- All tests green; one signed APK; store copy and privacy notice match the local-only build.
- No screen promises a remote action, a DM, a verification badge or an AI feature that v0.0.5 does
  not ship.

---

## Appendix A — Contact message templates

**Activation**
> Hello Pioneer. I am {name} ({handle}), a {role} in {circle}. I want to activate my Love Model
> circle. My reference is {ref}. Please tell me the next step. (TLC {version})

**Reactivation**
> Hello Pioneer. I am {name} ({handle}). I am reactivating my Love Model circle for {period}. My
> reference is {ref}. Please confirm when you receive it. (TLC {version})

**Pioneer's Token question**
> Hello Pioneer. I am {name} ({handle}). I have a question about my {kind} Token, reference {ref}.
> {question} (TLC {version})

**Support / transfer help**
> Hello Pioneer. I am {name} ({handle}). I need help with a direct support transfer. The reference
> is {ref}. {question} (TLC {version})

**Partnership / media**
> Hello Pioneer. I am {name} ({handle}) from {organisation}. I would like to discuss {topic}.
> (TLC {version})

**Report a problem**
> Hello Pioneer. I am {name} ({handle}). I want to report: {problem}. App version {version},
> Android {android}. (TLC {version})

**Something else**
> Hello Pioneer. I am {name} ({handle}). {message} (TLC {version})

The user edits every template before sending. No template includes a bank number or a full
account number.

---

## Appendix B — Files that change (map)

| Area | Files |
|---|---|
| Theme / components | `ui/theme/Theme.kt`, `ui/Components.kt`, new `ui/theme/Tokens.kt`, new `ui/components/*` |
| Navigation | `ui/TlcApp.kt`, `MainActivity.kt` |
| Screens | `TodayScreen.kt`, `JournalScreen.kt`, `GrantsScreen.kt`, `DuesScreen.kt` → `PioneerTokenScreen.kt`, `CircleScreen.kt` → `PeopleScreen.kt`, `SettingsScreen.kt`, `AboutScreen.kt`, new `SendSupportScreen.kt`, `ContactScreen.kt`, `BankVaultScreen.kt` |
| State | `data/AppState.kt`, new `data/BankVault.kt`, new `data/ContactLog.kt` |
| Experience | `core/experience/Model.kt`, `StateCodec.kt`, `BundleCodec.kt`, `Workspace.kt`, new `People.kt`, `Support.kt`, `PioneerToken.kt` |
| Domain | `core/domain/Model.kt` (TokenNature/monetary), `Governance.kt` (DoctrineCopy), new `Persona.kt`, `BankAccount.kt`, `ContactIntent.kt` |
| Ledger | `core/ledger/Entry.kt` (new kinds only) |
| Remote | **None in v0.0.5.** No `RemotePort`, no Worker, no `full` adapter. See Appendix D for the deferred design. |
| Resources | `res/values/strings.xml`, `colors.xml`, `themes.xml`, `design/logo/*` |
| Build / release | `apps/android/build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/release.yml`, `version.properties`, `fdroid/metadata/culture.thelove.tlc.yml` |
| Docs | `README.md`, `CONTEXT.md`, `SPECS.md`, `docs/SEF.md`, `PRIVACY.md`, `SECURITY.md`, `docs/DISTRIBUTION.md`, `release-notes/v0.0.5.md`, `docs/adr/008..011` |

---

## Appendix C — The one-sentence test for every screen

> **Can a real person finish the task on this screen without asking the founder what to do next?**

If the answer is no, the screen is not done — no matter how good it looks.

---

## Appendix D — Deferred remote design (not in v0.0.5)

Kept here so the knowledge is not lost. **Do not implement any of it in v0.0.5.**

```kotlin
interface RemotePort {
    val available: Boolean
    suspend fun contact(request: ContactRequest): ContactResult
    suspend fun releasePioneerDetails(request: PioneerDetailsRequest): PioneerDetails
    suspend fun ticketStatus(id: String): TicketStatus
}
```

Future Worker endpoints (v0.0.6+, only if section 9.4 is satisfied):

| Endpoint | Purpose | Retention |
|---|---|---|
| `POST /v1/desk/contact` | forward a contact request to the Pioneer | 30 days |
| `POST /v1/pioneer/details` | release the Pioneer's account for a token, 15-min TTL | not stored |
| `GET /v1/desk/ticket/{id}` | status only | 30 days |

A future remote release must ship, in the same commit: consent, retention, a kill switch, a
network log, a DPIA delta, a "when you connect" privacy section, Turnstile, rate limits, TLS-only,
no message body in logs, and no analytics. None of that is v0.0.5 work.

---

*TLC v0.0.5 · Trust & Contact · proposal · 2026-11-03*
