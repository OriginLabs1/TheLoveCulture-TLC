# TLC Pear / Holepunch companion

A real peer node for TLC record handoffs, built on [Hyperswarm](https://github.com/holepunchto/hyperswarm)
and [Hypercore](https://github.com/holepunchto/hypercore). It is a companion to the Android app,
not a replacement for it and not a second account system.

## What it does

- The Android app exports a **handoff** (JSON): handle, display name, device model, export time,
  head hash and one row per record with only `seq`, `kind`, `hash` and `occurredAt`.
- `tlc-peer` validates that handoff, appends it to a local append-only Hypercore and announces
  itself on a Hyperswarm topic derived from a **circle code** — a shared secret both people type
  into their own devices. The code itself is never sent or stored.
- A second node with the same circle code finds the first over the public DHT, connects directly,
  exchanges records and appends validated copies to its own core. No server, no relay account.
- Records are deduplicated by a SHA-256 id over canonical content. Re-importing or re-meeting the
  same peer does not grow the log twice.

The relay (`../relay/`) is still the way to get live 1:1 messages and presence. The Pear node
carries the *record shape* — proofs that a circled record exists — and works without the relay.
Either can be used alone; together they are independent paths.

## Honest limits

- This is a **companion node**, not an Android runtime. Pear does not ship an APK runtime here.
  The Android side is an export/handoff adapter; the node runs wherever Node 20+ runs (desktop,
  server, or a Pear/Bare runtime).
- Handoffs carry no message text, no subjects, no photos, no receipts and no vault references.
- The circle code is the only admission control to the swarm. Treat it like a door key and share
  it out of band. Anyone who has it can join the topic and see handoff shapes.
- Hypercore is single-writer per device. Each node keeps its own core; peers copy records, they
  do not merge into one writable shared core.
- The public DHT is discoverable metadata, not a private network. Use the relay when you want the
  consent record and connection acceptance checks.

## Run

```sh
cd peer
npm install
npm test

# 1. Export a handoff from the app (Live screen → Export Pear handoff).
# 2. Import it and host. Both people use the same circle code.
node src/cli.js import ./tlc-peer-2026-11-10.json \
  --circle "a shared phrase that only we know" \
  --handle ada --seconds 120

# On the other device / machine:
node src/cli.js host \
  --circle "a shared phrase that only we know" \
  --handle grace

# Inspect what this node holds:
node src/cli.js list

# Derive the swarm topic without joining:
node src/cli.js topic --circle "a shared phrase that only we know"
```

`TLC_PEER_DIR` (default `~/.tlc-peer`) holds the Hypercore. `TLC_PEER_CIRCLE` can replace
`--circle`. Nothing is written outside that directory except the one line of status on stdout.

## Wire format

Newline-delimited JSON over the Hyperswarm duplex stream:

| Message | Meaning |
|---|---|
| `hello` | version, handle, display, core key |
| `sync-request` | send me every record you hold |
| `record` | one validated handoff record |
| `sync-done` | how many records were sent |
| `ping` / `pong` | keepalive |
| `bye` | close |

A record is rejected unless its id matches a SHA-256 over its canonical content, its entries are
well shaped and every entry hash is a 64-character hex digest.
