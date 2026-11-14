/** Loads the Rust trust core and exposes a SHA-256 primitive.
 *  The JavaScript fallback exists so the app still opens if a WebView refuses
 *  WebAssembly; the UI reports which core is active. */

import { sha256 } from '@noble/hashes/sha256';
import { bytesToHex } from './util';

interface CoreExports {
  memory: WebAssembly.Memory;
  union_alloc(len: number): number;
  union_free(ptr: number, len: number): void;
  union_sha256(input: number, len: number, out: number): void;
  union_version(): number;
  union_self_test(): number;
}

let core: CoreExports | null = null;
let loading: Promise<boolean> | null = null;
let lastError = '';
let backend: 'rust-wasm' | 'js' = 'js';

export async function initCore(): Promise<boolean> {
  if (core) return true;
  if (loading) return loading;
  loading = (async () => {
    try {
      const base = location.href.replace(/#.*$/, '').replace(/[^/]*$/, '');
      const url = new URL('union_core.wasm', base);
      const response = await fetch(url, { cache: 'no-cache' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const bytes = await response.arrayBuffer();
      const { instance } = await WebAssembly.instantiate(bytes, {});
      const exports = instance.exports as unknown as CoreExports;
      if (exports.union_self_test() !== 1) throw new Error('core self-test failed');
      core = exports;
      backend = 'rust-wasm';
      return true;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
      return false;
    }
  })();
  return loading;
}

export function coreBackend(): { backend: 'rust-wasm' | 'js'; error: string; version: number } {
  return { backend, error: lastError, version: core ? core.union_version() : 0 };
}

export async function sha256Bytes(data: Uint8Array): Promise<Uint8Array> {
  if (!core) await initCore();
  if (!core) return sha256(data);
  const ptr = core.union_alloc(data.length);
  const out = core.union_alloc(32);
  try {
    new Uint8Array(core.memory.buffer, ptr, data.length).set(data);
    core.union_sha256(ptr, data.length, out);
    return new Uint8Array(new Uint8Array(core.memory.buffer, out, 32));
  } finally {
    core.union_free(ptr, data.length);
    core.union_free(out, 32);
  }
}

export async function sha256Hex(data: string | Uint8Array): Promise<string> {
  const bytes = typeof data === 'string' ? new TextEncoder().encode(data) : data;
  return bytesToHex(await sha256Bytes(bytes));
}
