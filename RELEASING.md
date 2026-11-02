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
./scripts/bump-version.sh patch        # 0.0.2 -> 0.0.3, versionCode +1
# write release-notes/v0.0.3.md        # required: the workflow fails without it
git add version.properties release-notes/v0.0.3.md
git commit -m "release: v0.0.3"
git tag v0.0.3
git push origin main v0.0.3
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

The same signed `tlc-<version>-foss.apk` is submitted unchanged. No re-signing: a store that
re-signs the APK breaks update continuity with GitHub and F-Droid, so that is a blocker to resolve
before submission, not a setting to accept.
