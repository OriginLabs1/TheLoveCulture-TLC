# Distribution — v0.0.6

One tag, one build, one signature. The source of truth is the GitHub Release; every other channel
must carry the same signed APK, never a re-signed copy.

## Live now

| Channel | Status | Where |
|---|---|---|
| GitHub Releases | **Published** | <https://github.com/OriginLabs1/TheLoveCulture-TLC/releases/tag/v0.0.6> |
| GitHub Pages install page | **Live** | <https://originlabs1.github.io/TheLoveCulture-TLC/> |
| Obtainium | **Available** | add `https://github.com/OriginLabs1/TheLoveCulture-TLC` as a source |

Release facts:

| | |
|---|---|
| APK | `tlc-0.0.6-foss.apk` |
| versionName / versionCode | `0.0.6` / `6` |
| Commit | tag `v0.0.6` |
| Signature | APK Signature Scheme v2, RSA 4096 |
| Certificate SHA-256 | `e04cb8aa2cf5fdeec043d917a74a5ba634121f2a0b98df92869a42bb4acb732c` |
| Permissions | no `INTERNET` permission |

The APK SHA-256 and size are in the release's `SHA256SUMS`, published with the file. They are not
copied into this document, because a copy can drift from the artifact while the signed checksum
cannot.

Verify a download:

```sh
gh release download v0.0.6 --repo OriginLabs1/TheLoveCulture-TLC
sha256sum -c SHA256SUMS
apksigner verify --print-certs tlc-0.0.6-foss.apk
```

v0.0.6 is built and published by the GitHub Actions release workflow on x86-64, which also emits the
build-provenance attestation. Verify that attestation for the tag:

```sh
gh attestation verify tlc-0.0.6-foss.apk --repo OriginLabs1/TheLoveCulture-TLC
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
   - `commit: v0.0.6` (the exact tag this release was built from),
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
| `version` / `latestVersion` | `0.0.6` / `Latest` |
| `description` | the short description in [`fastlane/metadata`](../fastlane/metadata/android/en-US/short_description.txt) |

Orion should resolve `tlc-0.0.6-foss.apk` from the GitHub Release above. The listing needs a direct
icon URL and direct screenshot URLs; this repository does not yet carry raster screenshots, so the
owner must supply or approve them before the Orion entry can be complete.

### Uptodown

Uptodown is a manual developer-console upload:

1. Create the app once under `culture.thelove.tlc`.
2. Upload the same `tlc-0.0.6-foss.apk`; versionCode `6`.
3. Use [`changelogs/6.txt`](../fastlane/metadata/android/en-US/changelogs/6.txt) as the changelog and the
   `fastlane/metadata` files as the listing copy.
4. Supply the icon, feature graphic and screenshots from the release commit (still outstanding).
5. Never upload a debug build, an unsigned build or a re-signed build.

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
