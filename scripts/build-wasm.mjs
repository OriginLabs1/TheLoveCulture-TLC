#!/usr/bin/env node
/** Build the Union trust core to WebAssembly. */
import { execFileSync } from 'node:child_process';
import { mkdirSync, copyFileSync, existsSync, statSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(new URL('.', import.meta.url)));

// npm strips shell profile PATH entries; make the usual Rust install discoverable.
const cargoBin = join(homedir(), '.cargo', 'bin');
if (existsSync(cargoBin) && !process.env.PATH.split(':').includes(cargoBin)) {
  process.env.PATH = `${cargoBin}:${process.env.PATH}`;
}
const crate = join(root, 'rust/union-core');
const out = join(root, 'public/union_core.wasm');
const target = 'wasm32-unknown-unknown';

function run(cmd, args, opts = {}) {
  return execFileSync(cmd, args, { stdio: 'inherit', cwd: crate, ...opts });
}

try {
  const installed = execFileSync('rustup', ['target', 'list', '--installed'], { encoding: 'utf8' });
  if (!installed.split(/\s+/).includes(target)) {
    console.log(`==> adding Rust target ${target}`);
    run('rustup', ['target', 'add', target], { cwd: root });
  }
} catch (error) {
  console.warn(`rustup not available or target install failed: ${error.message}`);
}

console.log('==> cargo test (host)');
try {
  run('cargo', ['test', '--quiet']);
} catch {
  console.warn('host tests failed; continuing to wasm build');
}

console.log('==> cargo build --release --target wasm32-unknown-unknown');
run('cargo', ['build', '--release', '--target', target]);

const wasm = join(crate, `target/${target}/release/union_core.wasm`);
mkdirSync(dirname(out), { recursive: true });
copyFileSync(wasm, out);
console.log(`==> ${out} (${statSync(out).size} bytes)`);
