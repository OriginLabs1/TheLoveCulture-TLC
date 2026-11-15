#!/usr/bin/env node
/** Finish a Union web build: fallback page, build metadata, asset checks and
 *  a deterministic checksum list suitable for a release note. */

import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(new URL('.', import.meta.url)));
const dist = join(root, 'dist');
const pkg = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8'));

if (!existsSync(join(dist, 'index.html'))) {
  console.error('postbuild: dist/index.html is missing — run vite build first.');
  process.exit(1);
}

// Hash routing keeps every route on the same document. A 404 copy still helps
// static hosts that receive a path that is not the root.
copyFileSync(join(dist, 'index.html'), join(dist, '404.html'));

const required = [
  'manifest.webmanifest',
  'sw.js',
  'icons/favicon.svg',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/apple-touch-icon.png'
];
const missing = required.filter((file) => !existsSync(join(dist, file)));
if (missing.length) {
  console.error(`postbuild: missing assets: ${missing.join(', ')}`);
  process.exit(1);
}

const index = readFileSync(join(dist, 'index.html'), 'utf8');
if (index.includes('<!--CSP-->')) {
  console.error('postbuild: CSP marker was not replaced.');
  process.exit(1);
}
if (!index.includes('Content-Security-Policy')) {
  console.warn('postbuild: no Content-Security-Policy found in index.html.');
}

const wasm = join(dist, 'union_core.wasm');
if (!existsSync(wasm)) {
  console.warn('postbuild: union_core.wasm not present. The app will use the JavaScript SHA-256 fallback. Run `npm run wasm`.');
}

const files = [];
function walk(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else if (entry.isFile()) files.push(full);
  }
}
walk(dist);

const sums = files
  .filter((file) => !file.endsWith('SHA256SUMS'))
  .sort()
  .map((file) => {
    const hash = createHash('sha256').update(readFileSync(file)).digest('hex');
    return `${hash}  ${relative(dist, file).replace(/\\/g, '/')}`;
  });

writeFileSync(join(dist, 'SHA256SUMS'), `${sums.join('\n')}\n`);

const info = {
  name: pkg.name,
  version: pkg.version,
  builtAt: new Date().toISOString(),
  files: files.length,
  bytes: files.reduce((n, file) => n + statSync(file).size, 0),
  wasm: existsSync(wasm),
  commit: process.env.GITHUB_SHA || process.env.COMMIT_SHA || '',
  run: process.env.GITHUB_RUN_NUMBER || ''
};
writeFileSync(join(dist, 'build.json'), `${JSON.stringify(info, null, 2)}\n`);

console.log(`postbuild: ${info.files} files · ${(info.bytes / 1024).toFixed(1)} KB · wasm=${info.wasm}`);
console.log('postbuild: wrote 404.html, build.json and SHA256SUMS');
