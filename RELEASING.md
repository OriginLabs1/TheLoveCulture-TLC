# Releasing TLC

One tag, one build, one signature. The same APK goes to GitHub Releases, F-Droid and Orion Store.

## The identity

| | |
|---|---|
| Application ID | `culture.thelove.tlc` (frozen forever) |
| Signing certificate SHA-256 | `e04cb8aa2cf5fdeec043d917a74a5ba634121f2a0b98df92869a42bb4acb732c` |
| Key algorithm | RSA 4096, SHA256withRSA, validity 10000 days |
| Certificate subject | `CN=Collins Christopher, OU=TLC, O=OriginLabs1, L=Lagos, C=NG` |
| Key alias | `tlc-release` |
| Pinned in | [`release-key.properties`](release-key.properties) |

The fingerprint is public on purpose. It is the thing users check and the value F-Droid pins in
`AllowedAPKSigningKeys`. The release workflow reads it from `release-key.properties` and refuses to
publish an APK signed by anything else, so a wrong keystore fails the build instead of quietly
creating a second identity that cannot update the first.

## Where the private key lives

Not in this repository, not in CI logs, not in this document. Only two places:

1. An offline backup the project owner controls (the `.jks` plus its passwords).
2. GitHub Actions repository secrets:

| Secret | Contents |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the `.jks`, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | `tlc-release` |
| `ANDROID_KEY_PASSWORD` | key password |

Losing the key means no further updates to installed copies — a new package ID would be the only
recovery. Back up the keystore *and* the passwords separately, and test the restore by building a
throwaway signed APK from the backup.

## Cutting a release

```bash
./scripts/bump-version.sh patch        # 0.0.3 -> 0.0.4, versionCode +1
# write release-notes/v0.0.4.md        # required: the workflow fails without it
git add version.properties release-notes/v0.0.4.md
git commit -m "release: v0.0.4"
git tag v0.0.4
git push origin main v0.0.4
```

The tag must equal `v<versionName>`, and `versionCode` must be greater than the previous tag's.
`.github/workflows/release.yml` then:

1. checks the tag against `version.properties` and the previous tag's `versionCode`;
2. fails if any signing secret is missing — an unsigned APK is never a release;
3. runs the core invariants (`./gradlew verify`) and the FOSS guard;
4. builds and signs `assembleFossRelease`;
5. verifies the signature, pins the certificate, and proves the APK asks for no `INTERNET`
   permission and carries no Play Services classes in its dex;
6. publishes an immutable release with the APK, `SHA256SUMS`, the dependency manifest, the
   signature block and a build-provenance attestation.

Published assets are never replaced. If a release is wrong, withdraw it in the store listing, fix
the cause, bump the version and publish again.

## Rotating the key

Rotation is allowed, silently drifting is not. A rotation is one commit that changes
`release-key.properties`, new secrets, a note in the release notes saying why, and a F-Droid
`AllowedAPKSigningKeys` update. Android will keep updating installed apps only if the new key is
introduced through APK Signature Scheme v3 key rotation, which requires building from the *old* key.
Plan rotation as a two-step process, never as a hard cut.

## F-Droid

The `foss` flavour is the F-Droid build: GPL-3.0-or-later, no Play Services, no Firebase, no
tracking, no proprietary SDK, no prebuilt binaries other than the Gradle wrapper jar (which Gradle,
not the project, publishes). The build guard fails if a proprietary dependency reaches the `foss`
configuration.

## Orion Store

Orion Store (`https://www.orionstore.net/`) is GitHub-driven and serverless: it reads a repository and
publishes releases from it, so there is no binary upload desk and no separate store account holding a
second copy. Point Orion at `OriginLabs1/TheLoveCulture-TLC` and let it consume the same immutable
GitHub Release and `tlc-<version>-foss.apk` that F-Droid and GitHub serve.

Before submission, confirm that:

1. the release is public, immutable and tagged exactly `v<versionName>`;
2. the APK is the signed `foss` build, not a rebuilt or re-signed copy;
3. `SHA256SUMS`, the signature block and the dependency manifest are attached to the release;
4. the listing text matches [`fastlane/metadata`](fastlane/metadata/android/en-US/) and
   [`PRIVACY.md`](PRIVACY.md).

If Orion cannot serve the APK directly and asks for an upload, upload the same file unchanged. A
store that re-signs the APK breaks update continuity with GitHub and F-Droid, so that is a blocker
to resolve before submission, not a setting to accept.

## Uptodown

Uptodown is a manual developer-console upload: create the app once, then upload the same signed
`tlc-<version>-foss.apk` for each release, with the version name, version code, changelog and listing
copy. It does not build from source. Use the Android listing at
`https://uptodown-android.uptodown.com/android` as the reference for the store's format and category
conventions.

The upload checklist is the same as Orion's, plus:

1. versionCode must be strictly greater than the previous Uptodown upload;
2. the changelog is the text in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`;
3. the icon, feature graphic and screenshots come from the same release commit;
4. never upload a debug APK, an unsigned APK or an APK built from an unmerged branch.

Uptodown does not replace GitHub Releases as the source of truth. If an upload is wrong, correct the
listing and publish the next version; a published APK is not silently swapped.

## One identity, four channels

| Channel | How it receives the build | Re-signs? |
|---|---|---|
| GitHub Releases | CI builds, signs and publishes the immutable release | No |
| F-Droid | Buildserver rebuilds from the tag and compares to the pinned APK | No (key pinned) |
| Orion Store | Reads the GitHub repository/release | No |
| Uptodown | Manual developer-console upload of the same APK | No |

Every channel must end up with `culture.thelove.tlc` signed by the fingerprint in
[`release-key.properties`](release-key.properties). If any channel insists on a different signature,
stop and resolve it before publishing: two signatures for one package id is the failure mode this
document exists to prevent.
