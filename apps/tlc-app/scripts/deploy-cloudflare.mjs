#!/usr/bin/env node
/**
 * Deploy the built site to Cloudflare Pages.
 *
 *   CLOUDFLARE_API_TOKEN=… CLOUDFLARE_ACCOUNT_ID=… npm run deploy
 *
 * The optional signed APK and its checksum are staged into dist/ first, so the
 * same host serves the installable PWA and the Android download. Nothing here
 * reads or writes a secret: credentials come from the environment only.
 */

import { execFileSync } from 'node:child_process';
import { copyFileSync, existsSync, readFileSync, appendFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(new URL('.', import.meta.url)));
const dist = join(root, 'dist');
const pkg = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'));
const project = process.env.PAGES_PROJECT || 'union-app';
const branch = process.env.PAGES_BRANCH || 'main';

if (!existsSync(join(dist, 'index.html'))) {
  console.error('deploy: dist/index.html is missing — run `npm run build` first.');
  process.exit(1);
}
if (!process.env.CLOUDFLARE_API_TOKEN && !process.env.CLOUDFLARE_API_KEY) {
  console.error('deploy: set CLOUDFLARE_API_TOKEN (and CLOUDFLARE_ACCOUNT_ID) first.');
  process.exit(1);
}

// The APK is optional: a web-only deploy is still valid.
const apkName = `Union-${pkg.version}.apk`;
const apk = join(root, 'release', apkName);
if (existsSync(apk)) {
  copyFileSync(apk, join(dist, apkName));
  const sum = createHash('sha256').update(readFileSync(apk)).digest('hex');
  const sums = join(dist, 'SHA256SUMS');
  const kept = existsSync(sums)
    ? readFileSync(sums, 'utf8')
        .split('\n')
        .filter((line) => line && !line.endsWith(`  ${apkName}`))
    : [];
  appendFileSync(sums, `${[...kept, `${sum}  ${apkName}`].join('\n')}\n`, { flag: 'w' });
  console.log(`deploy: staged ${apkName} (${sum.slice(0, 16)}…)`);
} else {
  console.log('deploy: no release APK to stage; deploying the web app only.');
}

execFileSync(
  'npx',
  ['--yes', 'wrangler@4', 'pages', 'deploy', 'dist', '--project-name', project, '--branch', branch, '--commit-dirty=true'],
  { cwd: root, stdio: 'inherit' }
);
