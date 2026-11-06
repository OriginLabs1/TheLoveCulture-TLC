# The Standard Economic Framework (SEF) — Token Direction

**Status:** direction recorded before v0.0.5 and **partly** implemented. v0.0.5 shipped Pioneer's
Token and Send Support; v0.0.6 added the shared Tokens surface with circulation counters, receipts
and an honest timeline. The domain still models `RECOGNITION_SIGNAL` only; `TokenValuation` and
record-transfer provenance are not implemented yet (CONTEXT Appendix D). This document records the
founder's correction so the product does not carry two contradictory doctrines. The non-custody line
has not moved: a Token is a record of value, not a balance.

## The unit

The unit of the SEF is the **Token**.

A Token represents a meaningful and valuable resource, item, gift or monetary value. Money is
probably the least important Token, but it is usable for distributive convenience.

All meaningful and valuable resources, items and gifts may be valued in monetary terms for
distributive convenience. That valuation is a declared conversion, not a market price and not a
promise of return.

Tokens may be:

- **curated** — a steward records why a resource, gift or contribution is recognised and what it
  represents;
- **transferred** — the record moves a claim from one person to another with provenance intact;
- **distributed in monetary form** — the claim is settled bank-to-bank, directly between the people
  involved, for convenience.

Their purpose must align with the SEF of TLC: making love, care, contribution and material support
visible, structured and distributable without turning TLC into a bank, an escrow or an investment
scheme.

## Non-custody is still the line

A Token is a record of value, not a balance held by TLC. The following remain true in every SEF
version:

1. **TLC never holds, pools, escrows or moves money.** Settlement is bank-to-bank between two
   private accounts. TLC is not a party to the transfer.
2. **A Token is not a wallet balance.** There is no account balance, no float and no credit.
3. **Valuation is a declaration, not a market.** A conversion to monetary value exists so a
   distribution can be described and checked, not so a Token can be traded speculatively.
4. **No yield, no investment promise, no redeemable instrument.** A Token is not a security,
   a deposit, a loan or crypto.
5. **Curation has a reason and an author.** A steward who values, transfers or distributes a Token
   leaves a signed record, and either party can contest it.
6. **Two keys for irreversible acts.** A transfer or monetary distribution that changes another
   person's record needs the authority the dyad rules require.
7. **Bank details stay inside the fee flow.** The Pioneer's bank details are released only to the
   payer, only after the Token exists, and expire. They are never compiled in, logged, stored in the
   ledger, exported or shown to a third party. See [`SECURITY.md`](../SECURITY.md).

## What still has to change

| Area | v0.0.4 today | v0.0.5 direction |
|---|---|---|
| Token nature | `RECOGNITION_SIGNAL` only | a valued unit that can represent a resource, gift or money |
| Valuation | none | `TokenValuation` (amount, method, curator, time, reason) |
| Transfer | not transferable | recorded claim transfer with provenance and authority |
| Distribution | direct bank transfer recorded as evidence | distribution in monetary form, settled bank-to-bank |
| Curation | not modelled | steward curation with a reason and an audit trail |
| UI copy | "no cash value, not transferable, not redeemable" | SEF language that still states non-custody plainly |
| Store copy | recognition-signal-only | SEF token description with the same non-custody line |
| Tests | token nature is single-valued | conservation, valuation, transfer authority, no-custody invariants |

## The doctrine test

Any SEF change must pass all of these before it ships:

- delete the Token nature and the build must fail, not silently fall back to "not money";
- no code path can create a balance, escrow or float;
- a transfer cannot be recorded without the authority the dyad rules require;
- a valuation cannot be negative or unstated;
- a distribution cannot be marked settled without the evidence the rules require;
- the Pioneer's bank details cannot enter the record, an export, a log or a screenshot.

Until those tests exist, the current build's copy is the honest description of the current build. The
next version changes the copy and the code together.
