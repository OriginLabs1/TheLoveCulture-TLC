/** IndexedDB persistence with a localStorage fallback.
 *  One record store keeps the schema portable: every record is JSON and every
 *  upgrade is a migration over records, not a new proprietary format. */

import type { UnionRecord } from './types';

const DB_NAME = 'union';
const DB_VERSION = 1;
const STORE = 'records';

let dbPromise: Promise<IDBDatabase> | null = null;
let fallback = false;

function open(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      fallback = true;
      reject(new Error('IndexedDB unavailable'));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: 'id' });
        store.createIndex('type', 'type', { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error ?? new Error('IndexedDB open failed'));
    req.onblocked = () => reject(new Error('IndexedDB blocked'));
  });
  return dbPromise;
}

function lsKey(): string {
  return 'union.records.v1';
}

function readFallback(): UnionRecord[] {
  try {
    const raw = localStorage.getItem(lsKey());
    return raw ? (JSON.parse(raw) as UnionRecord[]) : [];
  } catch {
    return [];
  }
}

function writeFallback(records: UnionRecord[]): void {
  localStorage.setItem(lsKey(), JSON.stringify(records));
}

export async function loadAll(): Promise<UnionRecord[]> {
  try {
    const db = await open();
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(STORE, 'readonly');
      const req = tx.objectStore(STORE).getAll();
      req.onsuccess = () => resolve((req.result ?? []) as UnionRecord[]);
      req.onerror = () => reject(req.error);
    });
  } catch {
    fallback = true;
    return readFallback();
  }
}

export async function put(record: UnionRecord): Promise<void> {
  if (fallback) {
    const rows = readFallback().filter((r) => r.id !== record.id);
    rows.push(record);
    writeFallback(rows);
    return;
  }
  try {
    const db = await open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE, 'readwrite');
      tx.objectStore(STORE).put(record);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch {
    fallback = true;
    const rows = readFallback().filter((r) => r.id !== record.id);
    rows.push(record);
    writeFallback(rows);
  }
}

export async function putMany(records: UnionRecord[]): Promise<void> {
  if (!records.length) return;
  if (fallback) {
    const map = new Map(readFallback().map((r) => [r.id, r]));
    for (const r of records) map.set(r.id, r);
    writeFallback([...map.values()]);
    return;
  }
  try {
    const db = await open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE, 'readwrite');
      const store = tx.objectStore(STORE);
      for (const r of records) store.put(r);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch {
    fallback = true;
    const map = new Map(readFallback().map((r) => [r.id, r]));
    for (const r of records) map.set(r.id, r);
    writeFallback([...map.values()]);
  }
}

export async function remove(id: string): Promise<void> {
  if (fallback) {
    writeFallback(readFallback().filter((r) => r.id !== id));
    return;
  }
  try {
    const db = await open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE, 'readwrite');
      tx.objectStore(STORE).delete(id);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch {
    fallback = true;
    writeFallback(readFallback().filter((r) => r.id !== id));
  }
}

export async function clearAll(): Promise<void> {
  try {
    const db = await open();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(STORE, 'readwrite');
      tx.objectStore(STORE).clear();
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch { /* ignore */ }
  try { localStorage.removeItem(lsKey()); } catch { /* ignore */ }
}

export async function replaceAll(records: UnionRecord[]): Promise<void> {
  await clearAll();
  await putMany(records);
}

export function usingFallback(): boolean {
  return fallback;
}
