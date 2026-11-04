# F-Droid submission

`metadata/culture.thelove.tlc.yml` is the exact file that goes into F-Droid's
[`fdroiddata`](https://gitlab.com/fdroid/fdroiddata) repository at
`metadata/culture.thelove.tlc.yml`.

## Why the signing key is pinned

`AllowedAPKSigningKeys` plus `Binaries` tells F-Droid: build this commit from source, prove the result
is byte-identical to the APK at that URL, and publish *that* APK with *that* signature. Without it,
F-Droid would sign with its own key, and the same application id would exist with two signatures —
so a user who moved between GitHub and F-Droid could not update without uninstalling.

## Before opening the merge request

1. **Reproducibility.** `SOURCE_DATE_EPOCH` is pinned to the commit and two independent runners
   produce identical app content: `apksigcopier compare` reports no difference between build A,
   build B and the published `v0.0.5` asset. Only the APK signature block differs from run to run,
   which is expected and is exactly what `apksigcopier` is designed to ignore — F-Droid's
   buildserver uses the same comparison for developer-signed apps.
   Run [`.github/workflows/reproducible.yml`](../.github/workflows/reproducible.yml) against any tag
   to re-check it.
2. **Not a demo any more.** 0.0.5 keeps a durable, verifiable record in the app's private folder and
   adds the trust and contact surfaces, so the "too thin to review" objection to a demonstration
   build no longer applies. The listing still describes the narrow scope honestly: no account, no
   sync, no multi-device merge.
3. **Description provenance.** The text in the metadata must match the app; if the app changes, the
   metadata changes in the same merge request.

## Opening the merge request

```sh
git clone --depth 1 https://gitlab.com/fdroid/fdroiddata.git
cp fdroid/metadata/culture.thelove.tlc.yml fdroiddata/metadata/
cd fdroiddata
git checkout -b culture.thelove.tlc
git add metadata/culture.thelove.tlc.yml
git commit -m "New app: culture.thelove.tlc"
git push -o merge_request.create -o merge_request.title="New app: TLC" origin culture.thelove.tlc
```

`fdroid checkupdates culture.thelove.tlc` and `fdroid lint culture.thelove.tlc` run locally if the
`fdroidserver` tools are installed.

## Self-hosted alternative

If F-Droid's main repository declines the app, the same APK can be served from a self-hosted
F-Droid repository (one `fdroid update` over the release artifacts, signed with a repo key that is
separate from the app key). That keeps the app key untouched and the download path independent.
