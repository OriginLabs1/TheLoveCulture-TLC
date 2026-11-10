# Distribution — v0.0.8

One tag, one build, one signature. The source of truth is the GitHub Release; every other channel
must carry the same signed APK, never a re-signed copy.

## Live now

| Channel | Status | Where |
|---|---|---|
| GitHub Releases | **Published** | <https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/tag/v0.0.8> |
| GitHub Pages install page | **Live** | <https://originlabs1.github.io/TheLoveCulture-TLC/> |
| Obtainium | **Available** | add `https://github.com/OriginLabs1/TheLoveCulture-TLC` as a source |
| TLC relay | **Deployed** | <https://tlc-relay.access1tutamailcom.workers.dev> — source in [`relay/`](../relay/), self-hostable |
| Pear companion node | **In release** | source in [`peer/`](../peer/); install with `npm ci` and run `tlc-peer` |

Release facts:

| | |
|---|---|
| APK | `tlc-0.0.8-foss.apk` |
| versionName / versionCode | `0.0.8` / `8` |
| Commit | tag `v0.0.8` |
| Signature | APK Signature Scheme v2, RSA 4096 |
| Certificate SHA-256 | `e04cb8aa2cf5fdeec043d917a74a5ba634121f2a0b98df92869a42bb4acb732c` |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE` (relay only; no storage, contacts, location, camera or microphone) |

The APK SHA-256 and size are in the release's `SHA256SUMS`, published with the file. They are not
copied into this document, because a copy can drift from the artifact while the signed checksum
cannot.

Verify a download:

```sh
gh release download v0.0.8 --repo OriginLabs1/TheLoveCulture-TLC
sha256sum -c SHA256SUMS
apksigner verify --print-certs tlc-0.0.8-foss.apk
```

v0.0.8 is built and published by the GitHub Actions release workflow on x86-64, which also emits the
build-provenance attestation. Verify that attestation for the tag:

```sh
gh attestation verify tlc-0.0.8-foss.apk --repo OriginLabs1/TheLoveCulture-TLC
```

A local build is a verification step, not the published artifact.

## Ready, waiting on an owner account

These channels need a human login that is not stored in this repository or in CI. The artifact and
copy are ready; only the account action is missing.

### F-Droid

1. Fork <https://gitlab.com/fdroid/fdroiddata> with the project owner's GitLab account.
2. Copy [`fdroid/metadata/culture.thelove.tlc.yml`](../fdroid/metadata/culture.thelove.tlc.yml) to
   `metadata/culture.thelove.tlc.yml` unchanged.
3. Open a merge request. The metadata pins:
   - `commit: v0.0.8` (the exact tag this release was built from),
   - `Binaries:` to the GitHub APK,
   - `AllowedAPKSigningKeys:` to the project certificate above.
4. F-Droid's buildserver rebuilds from source and compares the result. If it differs, the build
   fails; do not switch to an F-Droid signature, because that would create a second identity for
   `culture.thelove.tlc`.

No screenshots are required for the merge request. If the linter asks for an icon, take it from the
APK's adaptive launcher icon; screenshots can be added later without changing the build.

### Orion Store

Orion reads public GitHub releases. Add the catalog entry through Orion's own submission flow or a
pull request to <https://github.com/RookieEnough/Orion-Data>:

| Field | Value |
|---|---|
| `id` | `tlc` |
| `name` | `TLC` |
| `packageName` | `culture.thelove.tlc` |
| `githubRepo` | `OriginLabs1/TheLoveCulture-TLC` |
| `repoUrl` | `https://github.com/OriginLabs1/TheLoveCulture-TLC` |
| `releaseKeyword` | `foss.apk` |
| `category` | `Social` |
| `platform` | `Android` |
| `author` | `Collins Christopher` |
| `version` / `latestVersion` | `0.0.8` / `Latest` |
| `description` | the short description in [`fastlane/metadata`](../fastlane/metadata/android/en-US/short_description.txt) |

Orion should resolve `tlc-0.0.8-foss.apk` from the GitHub Release above. The listing needs a direct
icon URL and direct screenshot URLs; this repository does not yet carry raster screenshots, so the
owner must supply or approve them before the Orion entry can be complete.

### Uptodown

Uptodown is a manual developer-console upload:

1. Create the app once under `culture.thelove.tlc`.
2. Upload the same `tlc-0.0.8-foss.apk`; versionCode `8`.
3. Use [`changelogs/8.txt`](../fastlane/metadata/android/en-US/changelogs/8.txt) as the changelog and the
   `fastlane/metadata` files as the listing copy.
4. Supply the icon, feature graphic and screenshots from the release commit (still outstanding).
5. Never upload a debug build, an unsigned build or a re-signed build.

## Self-hosted pieces

The APK is not the whole release. Two server-side components ship alongside it, and neither is
required to use the private record.

### Relay

The relay is a Cloudflare Worker with one SQLite Durable Object. It runs at
`https://tlc-relay.access1tutamailcom.workers.dev` and its source is in [`relay/`](../relay/).
Anyone can deploy the same code under their own account and build the app against it:

```sh
cd relay
npm ci
npx wrangler deploy          # creates the Durable Object and database binding
npm test
```

The relay is a courier. It stores account records, consented presence, connections and ciphertext,
never plaintext, the vault or the ledger. See [`relay/README.md`](../relay/README.md) for endpoints
and the 30-day prune.

### Pear companion node

[`peer/`](../peer/) is an optional Holepunch/Hyperswarm node. The app exports a record-shape-only
handoff JSON from the Live tab; the node imports it, publishes it on a topic derived from the
circle code, and syncs with other peers over the public DHT.

```sh
cd peer
npm ci
node src/cli.js import handoff.json
node src/cli.js host
node src/cli.js list
npm test
```

The node never receives subjects, amounts, photos, receipts or vault fields, and it cannot order,
mint or settle anything. The go/no-go and threat notes are in
[`docs/ADR-pear.md`](../docs/ADR-pear.md).

## Guardrails

- **No bank details.** Nothing in the release, its assets, its metadata or this repository contains
  a bank account number. The full number lives only inside the payer's encrypted vault; only the
  bank name, the last four digits and a fingerprint enter the state. See [`SECURITY.md`](../SECURITY.md).
- **No image bytes in the record.** Profile and receipt images are re-encoded, stripped of metadata
  and kept in app-private storage; the state and the record carry file names and fingerprints only.
- **No re-signing.** One package id, one certificate. A store that insists on its own key is a
  blocker, not a setting.
- **No silent swaps.** Published assets are immutable. A wrong release is withdrawn in the listing,
  fixed, and published under a new version.
