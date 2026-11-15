# Distribution

Union ships three ways from the same source:

1. **Cloudflare Pages** — the installable PWA, project `union-app`
2. **Signed APK** — `Union-0.0.1.apk` plus `SHA256SUMS`, served from the same site
3. **Cloudflare Worker + D1** — the optional relay

## Cloudflare Pages (PWA)

The site is a static `dist/` build. Vite uses `base: './'`, so the same output works
at a domain root, under a path, and inside the APK's loopback server.

```sh
npm ci
npm run check
npm test
npm run test:relay
npm run build

CLOUDFLARE_API_TOKEN=… CLOUDFLARE_ACCOUNT_ID=… npm run deploy
```

`npm run deploy` runs `scripts/deploy-cloudflare.mjs`, which:

- stages `release/Union-<version>.apk` and appends its SHA-256 to `dist/SHA256SUMS`,
- uploads `dist/` to the `union-app` Pages project on the `main` branch.

Override the target with `PAGES_PROJECT` and `PAGES_BRANCH`. Without an APK the
script deploys the web app only.

Live at **https://union-app-9ni.pages.dev**. The project has two custom hostnames:

- **https://union.cculture.net** — the canonical brand address
- **https://union-app.cculture.net** — an alternate address

Both are proxied CNAMEs to `union-app-9ni.pages.dev`, attached in
**Pages → union-app → Custom domains**. If a certificate is still initialising,
the `pages.dev` address serves the same deployment.

PWA install: open the site, use **Add to Home screen**. The service worker caches
the shell, icons, fonts and WASM core, so it opens offline after the first load.
There are no runtime font or script fetches.

## Signed APK

```sh
sh scripts/build-apk.sh
cd release
sha256sum -c SHA256SUMS
```

The deploy script places the APK and checksum on the Pages site, so the download
URL is stable:

```
https://union-app-9ni.pages.dev/Union-0.0.1.apk
https://union-app-9ni.pages.dev/SHA256SUMS
```

When a GitHub release is available, attach the same files there instead of
re-signing: one build, one signature, one checksum. Tag it `v0.0.1` and copy the
notes from [docs/release-notes-0.0.1.md](release-notes-0.0.1.md).

The APK is signed with the Union release key. Certificate SHA-256:

```
35:58:51:C9:DB:74:76:8F:8C:2B:E7:F3:79:38:0B:7B:F9:68:AB:9C:F5:F3:6A:91:3E:F5:FA:CB:DB:81:F9:42
```

Publish that fingerprint with any release. Keep `android/keys/` backed up
offline; losing it ends updates for anyone who installed v0.0.1.

### Install honesty

Android warns about installing an app from outside an app store. That warning is
expected. Union asks for `INTERNET` only, and the download page says why. Never
ask people to disable Play Protect.

## Cloudflare relay

See [docs/relay.md](relay.md). Short version:

```sh
cd server
npx wrangler@4 d1 create union-relay
npx wrangler@4 d1 execute union-relay --remote --file schema.sql
npx wrangler@4 deploy
```

`server/wrangler.toml` holds the D1 binding and the `union-relay.cculture.net`
custom domain. Set `ALLOWED_ORIGINS` to the Pages origins before a production
deploy; the live values are listed in the relay doc.

## Pre-release checklist

- [ ] `npm run check` — 0 errors, 0 warnings
- [ ] `npm test` — 10/10
- [ ] `npm run test:relay` — 9/9
- [ ] `npm run build` — `dist/` complete, `SHA256SUMS` written
- [ ] `npm run deploy` — Pages upload succeeds
- [ ] Preview the production build on a phone viewport
- [ ] Fresh install: onboarding → backup step → Home
- [ ] Activation request → trial → proof → verified (test mode)
- [ ] APK: signature verifies, one permission, `minSdk 28`, installs on a real device
- [ ] Screens: no horizontal overflow at 320 px
- [ ] Fonts: Google Sans and Google Sans Code load offline; no other family
- [ ] Relay health, handle lookup, message send/ack (if deployed)
- [ ] No secrets, keystores, tokens, phone numbers or proofs in git
- [ ] `release/SHA256SUMS` published and fingerprint stated

## Versioning

`package.json` and `android/app/src/main/AndroidManifest.xml` carry the version.
The APK script reads `package.json` by default. For a new release:

```sh
UNION_VERSION_NAME=0.0.2 UNION_VERSION_CODE=2 sh scripts/build-apk.sh
```

Android requires `versionCode` to increase for updates.
