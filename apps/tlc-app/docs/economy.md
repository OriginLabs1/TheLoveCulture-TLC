# Activation, plans and the economy

Union v0.0.1 has a real economic **record** and a **test-mode** economy. The distinction matters, and the app states it on every economic screen:

> v0.0.1 test mode. No real funds move inside the app, and no withdrawable value is promised. A production economy needs a backed provider, custody and legal support.

Nothing in Union asks for card details, bank credentials or a wallet seed. Payments happen outside the app; Union records the request, the proof and the decision.

## 1 · Activation Token

- **Minimum: $80** (or the Naira equivalent).
- One-time.
- Sent outside the app (bank transfer, mobile money, or another method agreed with the Creator).
- Sending an **Activation Request** in Union records the method, reference and note, and immediately opens a **14-day trial**.
- The request is a local, signed record. It does not withdraw money.

The trial exists so the environment is usable while a proof is reviewed. It is not an entitlement to a paid activation.

## 2 · Activation Proof

After the request, attach a receipt:

- a screenshot or photo (under 1.2 MB), or
- pasted/typed receipt text, or a `.txt` / `.md` file.

Union computes a **SHA-256** of the exact proof bytes and shows the hash. The app then builds an **activation package** containing:

```
UNION ACTIVATION REQUEST
Name, handle, identity id
Experience
Requested at, method, reference, note
Trial until
Proof file name, size, SHA-256
Message to the reviewer
```

You can copy the package, save it, and open the **Union Assistant** on Telegram — [@Oneorigin_bot](https://t.me/Oneorigin_bot) — to send the proof and package. The assistant reviews the submission and reports to Union's Creator.

**The Creator's WhatsApp number is never shown to users.** It is not printed in the app, not placed in the package, not exposed by the assistant, and must not be added to any export. If anyone claims otherwise, treat it as impersonation.

## 3 · Verification

Verification is a human decision, not an automatic payment check. Once verified:

- the activation record shows **Verified**,
- the ledger receives an activation entry,
- **Reactivation Token (Plan)** becomes available.

While a request is pending, union remains in trial. A rejected or expired request can be replaced with a new proof; it never silently charges anything.

For local development and demos, step 3 also contains a disclosure-labelled **“Mark verified in test mode”** control. It changes only local records, is clearly marked as a simulation, and is not a substitute for verification.

## 4 · Reactivation Token (Plan)

- **Minimum: $10 per month** equivalent, after an Activation Request and Proof are **verified**.
- 30 days per period.
- No automatic charge. You decide when to renew; the app records the period and reference.

Reactivation keeps an activated identity current. It does not unlock a different app: the environment stays usable, and the record stays honest.

## 5 · Joy/Giving Plans (optional)

These are optional, exact-named plans for people who want to give beyond activation.

- Minimum **$1** per plan period (Naira equivalent accepted).
- Requested by texting the courtesy words **“Joyful Giver”** to [@CollinsRoyalMedium](https://t.me/CollinsRoyalMedium).
- A **2-week trial** starts in the app immediately when you start or request the plan.
- Nothing is charged automatically; you can end the plan at any time.

The **Digital Giving record** is a local ledger for gifts made outside the app. Each entry is hash-chained to the previous one, so edits are visible. In v0.0.1 it is a record, not a payment rail.

## 6 · Ledger and honesty

The ledger is a local, append-only, hash-chained list:

```
hash = SHA-256(previous hash + canonical entry)
```

The Economy screen lets you verify the chain. A relay cannot read it, and no server holds a copy unless you export it yourself.

What the economy does **not** do in v0.0.1:

- take payments,
- hold balances,
- promise returns, yields or withdrawable value,
- convert currency,
- verify a bank receipt automatically.

A production economy needs a backed payment provider, custody arrangements, refund rules, tax treatment and legal review. None of that is pretended here.

## 7 · Support and disputes

Use the Union Assistant ([@Oneorigin_bot](https://t.me/Oneorigin_bot)) for activation and proof questions, or the Creator's public Telegram ([@CollinsRoyalMedium](https://t.me/CollinsRoyalMedium)) for plan requests. Never send a WhatsApp message to an unverified number claiming to be the Creator.

## 8 · Disclosure summary

| Item | v0.0.1 |
| --- | --- |
| Activation minimum | $80 equivalent |
| Trial after request | 14 days |
| Proof required | Yes — screenshot, photo, text, `.txt` or `.md` |
| Proof integrity | SHA-256 shown in app and package |
| Verification | Human, via Union Assistant → Creator |
| Reactivation | $10/month equivalent, after verification |
| Joy/Giving Plans | Optional, $1 minimum, “Joyful Giver”, 2-week trial |
| Real funds moved in app | No |
| Automatic charges | None |
| Creator WhatsApp shown | Never |
