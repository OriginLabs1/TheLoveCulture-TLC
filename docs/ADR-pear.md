# ADR: Pear / Holepunch in TLC v0.0.8

**Status:** accepted · **Date:** 2026-11-10 · **Release:** v0.0.8

## Context

TLC needs two different things that are easy to confuse:

1. **A shared fact in real time.** Two people on different phones want encrypted messages and
   consented presence *now*. This needs an always-on rendezvous, ordered delivery and a place to
   leave a sealed envelope when the recipient is offline.
2. **A durable handoff without an operator.** A person wants to carry the shape of their record to
   another device or another peer without asking a server to hold it.

Pear/Holepunch (Hyperswarm discovery, Hypercore append-only logs) is a strong fit for the second.
It is a poor fit for the first: if both people are offline, nobody is there to receive; a Hypercore
is single-writer per device; and peer discovery is not an ordered mailbox.

## Options considered

| Option | For | Against | Verdict |
|---|---|---|---|
| Relay only | Simple, works while one side is offline, presence is server-filtered | One operator sees connection metadata; no serverless path | Shipped |
| Pear only | No operator, direct encrypted streams, append-only logs | No offline mailbox; discovery requires both online; no stable presence | Rejected for live messaging |
| Relay + Pear handoff | Each path does what it is good at; relay can be self-hosted; Pear stays independent | Two things to explain and test | **Chosen** |
| Embedded Pear runtime in the APK | One download | No supported Android runtime path in this build; would be a large, unproven native dependency | Deferred |

## Decision

Ship the shared plane in this order:

1. **Relay + E2EE 1:1 messaging + live presence** as the real-time path.
2. **Pear/Holepunch companion node** plus an Android **handoff adapter** as the independent path.

The Android app does not embed a Pear runtime. It exports a handoff file. The companion node in
`peer/` runs on Node 20+, validates the file, appends it to a Hypercore and replicates it over
Hyperswarm to peers that know the same circle code.

### Why the handoff is narrow

The export contains only `seq`, `kind`, `hash`, `occurredAt`, the head hash, the handle, display
name and device model. It excludes subjects, amounts, notes, photos, receipt files, bank references,
the contact log and every vault field. A peer can prove that a record of a given shape exists and
that its hashes match; it cannot reconstruct the private record from the handoff. That is the
minimum needed for the handoff to be useful and the maximum that is safe to replicate.

### Why the relay stays

Presence is not a broadcast of local state. It is a server-filtered projection of a consent record:
a hidden signal is `null` (“not shared”), never `false` (“offline”). Offline delivery, connection
acceptance and read receipts need a mailbox that outlives a socket. A Hypercore cannot provide that
without becoming a server. The relay source is self-hostable, so the path is replaceable even though
it is centralised in this build.

## Proven

- `peer/test/records.test.js`: 5 tests for topic derivation, id canonicalisation, shape validation,
  framing and store deduplication/persistence.
- A two-peer run on the public DHT: peer A imported a handoff and hosted; peer B, using the same
  circle code and a different storage directory, received the record, logged `record-added`, and
  listed it after reopening. `received` stayed at 0 on A and the record count on B became 1.
- The relay end-to-end test: two accounts, connection request and acceptance, live presence,
  invisible returning `null`, an encrypted-envelope-shaped message delivered over WebSocket,
  sender `sent`, recipient `delivered`, typing and read events, and mailbox retrieval.

## Consequences and limits

- **No Android Pear runtime.** The companion node runs outside the app. A future Bare/Pear runtime
  would be a separate ADR with a real device matrix.
- **Single writer per core.** Each node keeps its own core and copies records; cores are not merged
  into one writable shared log.
- **Circle code is the only admission control.** It is a shared secret, never stored or sent by the
  node. Anyone who has it can join the topic and see handoff shapes.
- **No offline delivery on the Pear path.** Both peers must be online at the same time to exchange.
- **The public DHT leaks metadata.** Discovery is observable, like any DHT. The relay path has a
  different, smaller metadata surface.
- **Handoffs are snapshots.** A later export is a new record, not a delta. Deduplication is by
  content id, so unchanged exports collapse and changed exports add a row.

## Revisit when

- A supported Pear/Bare Android runtime can be embedded without a proprietary dependency.
- Multi-writer merge is needed (would require a CRDT or a different log design).
- The relay's operator model changes; the source is deliberately small enough to replace.
