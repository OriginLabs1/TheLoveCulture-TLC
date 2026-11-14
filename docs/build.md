# Building Union

## Web / PWA

Requirements: **Node 20+** (developed on Node 22).

```sh
npm install
npm run dev       # Vite dev server on http://127.0.0.1:5173
npm run build     # production bundle in dist/
npm run preview   # serve dist/ on http://127.0.0.1:4173
```

`npm run build` runs `vite build`, then [`scripts/postbuild.mjs`](../scripts/postbuild.mjs), which copies the WASM core and icons, writes `404.html`, `build.json` and `SHA256SUMS`, and stamps the service worker manifest.

### Checks

```sh
npm run check       # svelte-check: 0 errors, 0 warnings
npm test            # 10 app tests
npm run test:relay  # 8 relay tests
```

The app tests are plain `node:test` against a Rolldown bundle of the pure library modules. No extra test dependency is added.

### Rust core

`rust/` contains the small Union core compiled to WebAssembly. To rebuild it:

```sh
npm run wasm
```

The checked-in `public/union_core.wasm` is what the app ships. The TypeScript layer treats it as an optional accelerator: if the module is missing, the app still works.

## Android APK (no Gradle)

Union ships a small, auditable Java WebView shell instead of a Gradle project.

### Requirements

- JDK 17+ (`javac`, `keytool`)
- Android build-tools: `aapt2`, `zipalign`, `apksigner`
- `android-34.jar` and `r8.jar`

On a machine with the Android SDK:

```sh
sdkmanager "platforms;android-34" "build-tools;34.0.0"
mkdir -p android/toolchain
cp "$ANDROID_HOME/platforms/android-34/android.jar" android/toolchain/
cp "$ANDROID_HOME/build-tools/34.0.0/lib/r8.jar" android/toolchain/
```

Both jars are gitignored because of their size. `scripts/build-apk.sh` also looks in `/root/projects/Coordinator/mobile/toolchain/` as a local fallback.

### Signing key

```sh
sh android/scripts/gen-keys.sh
```

This writes `android/keys/union.p12` and `android/keys/keystore.properties` (both gitignored). **Back this up offline.** Android will not update an installed app signed with a different key. The script prints the SHA-256 certificate fingerprint; publish it with the release so people can verify.

Set the password explicitly if you prefer:

```sh
UNION_STORE_PASSWORD='…' UNION_KEY_PASSWORD='…' sh android/scripts/gen-keys.sh
```

### Build

```sh
sh scripts/build-apk.sh
```

The script:

1. builds the web surface (`npm run build`),
2. copies `dist/` into `android/app/src/main/assets/web/`,
3. compiles resources with `aapt2`,
4. compiles the Java shell against `android-34.jar`,
5. dexes with D8 (`--min-api 28`),
6. zipaligns, signs with `apksigner` (v1/v2/v3),
7. verifies the signature, permission set and `minSdkVersion`,
8. writes `release/Union-<version>.apk` and `release/SHA256SUMS`.

Useful environment variables:

| Variable | Default |
| --- | --- |
| `UNION_VERSION_NAME` | `package.json` version |
| `UNION_VERSION_CODE` | `1` |
| `UNION_SKIP_WEB` | `0` (set `1` to reuse `dist/`) |
| `UNION_UNSIGNED` | `0` (set `1` for CI/F-Droid signing) |
| `UNION_KEYSTORE` | `android/keys/union.p12` |
| `UNION_STORE_PASSWORD` | from `keystore.properties` |
| `ANDROID_JAR`, `R8_JAR` | `android/toolchain/*` |

### Inspect

```sh
aapt2 dump badging release/Union-0.0.1.apk
apksigner verify --verbose --print-certs release/Union-0.0.1.apk
sha256sum release/Union-0.0.1.apk
unzip -l release/Union-0.0.1.apk | head
```

Expected: package `com.oneorigin.union`, version `0.0.1 (1)`, `sdkVersion 28`, `targetSdkVersion 34`, one permission `android.permission.INTERNET`.

### Install

```sh
adb install -r release/Union-0.0.1.apk
```

Or copy the APK to the phone and open it, allowing install from that source once.

### Why a loopback asset server?

Browsers treat `file://` as an opaque origin: no IndexedDB, no service worker, no WebCrypto in the way Union needs. The shell serves the same packaged bytes from `http://127.0.0.1:<ephemeral port>` and binds only to loopback. Assets never touch the network.

### Shell security choices

- `allowFileAccess=false`, no universal file URL access, no mixed content.
- JavaScript bridge exposes only secret storage, share/copy, `https` external links, theme colour, exit and save-to-Downloads.
- `allowBackup=false`; backups are Union's own encrypted files.
- File chooser accepts images and text only, through the system document picker.
- Back gesture closes a sheet before navigating; otherwise the activity backgrounds.

## Troubleshooting

**`Missing aapt2`** — install `android-sdk-build-tools` or add the build-tools directory to `PATH`.

**`Missing android-34.jar`** — see Requirements above, or set `ANDROID_JAR` to an existing platform jar.

**`java.lang.UnsupportedClassVersionError`** — the JDK is too old. Use JDK 17+.

**Blank screen in the APK** — check `adb logcat | grep -i union`. The shell finishes with a toast if the loopback server cannot start.

**Fonts look like a fallback** — the two Google Sans files must be present in `src/assets/fonts/` before building. They are part of the repository.

**Service worker keeps an old build** — `npm run build` changes the service worker version; a hard reload clears it. In development the service worker is not registered.
