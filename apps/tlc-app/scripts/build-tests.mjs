#!/usr/bin/env node
/** Bundle the pure library modules for Node's test runner.
 *
 *  The browser build is a Vite app; the test runner needs plain ESM without
 *  TypeScript syntax. Rolldown (already part of Vite) does that without adding
 *  a dependency. Only modules that do not touch IndexedDB or the DOM are
 *  bundled here. */

import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { rolldown } from 'rolldown';

const root = dirname(fileURLToPath(new URL('.', import.meta.url)));
const outDir = join(root, 'tests/.build');
mkdirSync(outDir, { recursive: true });

const entries = {
  crypto: 'src/lib/crypto.ts',
  core: 'src/lib/core.ts',
  links: 'src/lib/links.ts'
};

for (const [name, input] of Object.entries(entries)) {
  const bundle = await rolldown({ input: join(root, input), logLevel: 'warn' });
  await bundle.write({ format: 'esm', file: join(outDir, `${name}.mjs`) });
  await bundle.close();
  console.log(`tests: bundled ${input} -> tests/.build/${name}.mjs`);
}
