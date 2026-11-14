# Distribution

Union ships three ways from the same source:

1. **GitHub Pages** — installable PWA at `https://originlabs1.github.io/Union/`
2. **GitHub Release** — signed `Union-0.0.1.apk` plus `SHA256SUMS` and the web bundle
3. **Cloudflare** — the optional relay (Worker + D1); the site itself can also be mirrored on Cloudflare Pages

## GitHub Pages (PWA)

The workflow in [`.github/workflows/pages.yml`](../.github/workflows/pages.yml) runs on every push to `main`:

```sh
npm ci
npm run check
npm test
npm run test:relay
npm run build
```

It uploads `dist/` as the Pages artifact. Vite uses `base: './'`, so the same build works at a domain root, under `/Union/`, and inside the APK's loopback server.

First-time setup:

1. Create `OriginLabs1/Union` on GitHub.
2. Push `main`.
3. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
4. Wait for the workflow; the site appears at `https://originlabs1.github.io/Union/`.

PWA install: open the URL, use **Add to Home screen**. Chrome/Edge will install the service worker and offline shell. The app has no runtime font or script fetches.

## GitHub Release (APK)

```sh
sh scripts/build-apk.sh
cd release
sha256sum -c SHA256SUMS
```

Create a release tagged `v0.0.1` and attach:

- `Union-0.0.1.apk`
- `SHA256SUMS`
- `union-0.0.1-web.zip` (optional, `cd dist && zip -qr ../release/union-0.0.1-web.zip .`)
- release notes from [docs/release-notes-0.0.1.md](release-notes-0.0.1.md)

The APK is signed with the Union release key. Certificate SHA-256:

```
35:58:51:C9:DB:74:76:8F:8C:2B:E7:F3:79:38:0B:7B:F9:68:AB:9C:F5:F3:6A:91:3E:F5:FA:CB:DB:81:F9:42
```

Publish that fingerprint in the release notes. Keep `android/keys/` backed up offline; losing it ends updates for anyone who installed v0.0.1.

### Install honesty

Android will warn about installing an app from outside an app store. That warning is expected. Union asks for `INTERNET` only, and the release page says why. Never ask people to disable Play Protect.

## Cloudflare

### Relay

See [docs/relay.md](relay.md). Short version:

```sh
cd server
npx wrangler@4 d1 create union-relay
npx wrangler@4 d1 execute union-relay --remote --file schema.sql
npx wrangler@4 deploy
```

Set `ALLOWED_ORIGINS` to the Pages origin before the production deploy.

### Static site mirror (optional)

```sh
cd server
npx wrangler@4 pages deploy ../dist --project-name union
```

This is a mirror, not a requirement. The GitHub Pages build is canonical.

## Pre-release checklist

- [ ] `npm run check` — 0 errors, 0 warnings
- [ ] `npm test` — 10/10
- [ ] `npm run test:relay` — 8/8
- [ ] `npm run build` — `dist/` complete, `SHA256SUMS` written
- [ ] Preview the production build on a phone viewport
- [ ] Fresh install: onboarding → backup step → Home
- [ ] Activation request → trial → proof → verified (test mode)
- [ ] APK: signature verifies, one permission, `minSdk 28`, installs on a real device
- [ ] Screens: no horizontal overflow at 320 px
- [ ] Fonts: Google Sans and Google Sans Code load offline; no other family
- [ ] Relay health, handle lookup, message send/ack (if deployed)
- [ ] No secrets, keystores, tokens, phone numbers or proofs in git
- [ ] `release/SHA256SUMS` attached and fingerprint published

## Versioning

`package.json` and `android/app/src/main/AndroidManifest.xml` carry the version. The APK script reads `package.json` by default. For a new release:

```sh
UNION_VERSION_NAME=0.0.2 UNION_VERSION_CODE=2 sh scripts/build-apk.sh
```

Android requires `versionCode` to increase for updates.
