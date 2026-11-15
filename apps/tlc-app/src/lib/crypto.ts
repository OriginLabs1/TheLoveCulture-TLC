/** Union cryptography.
 *
 *  - Identity: Ed25519 signatures (portable, verifiable).
 *  - Key agreement: X25519, ephemeral per message.
 *  - Message encryption: HKDF-SHA256 -> AES-256-GCM (WebCrypto).
 *  - Backup encryption: PBKDF2-SHA256 (310k) -> AES-256-GCM.
 *
 *  Established implementations only: @noble/curves, @noble/hashes and the
 *  platform WebCrypto. Nothing in this file invents a protocol.
 */

import { ed25519, x25519 } from '@noble/curves/ed25519';
import { hkdf } from '@noble/hashes/hkdf';
import { sha256 } from '@noble/hashes/sha256';
import { randomBytes } from '@noble/hashes/utils';
import { base64, base64url } from '@scure/base';
import type { Identity, Keyring } from './types';
import { sha256Hex } from './core';
import { now, uid } from './util';

const encoder = new TextEncoder();
const decoder = new TextDecoder();

const toBytes = (text: string): Uint8Array<ArrayBuffer> =>
  encoder.encode(text) as Uint8Array<ArrayBuffer>;
const fromBytes = (bytes: Uint8Array): string => decoder.decode(bytes);

const b64 = (bytes: Uint8Array): string => base64.encode(bytes);
const b64u = (bytes: Uint8Array): string => base64url.encode(bytes);
const fromB64 = (text: string): Uint8Array<ArrayBuffer> =>
  new Uint8Array(base64.decode(text));
const fromB64u = (text: string): Uint8Array<ArrayBuffer> =>
  new Uint8Array(base64url.decode(text));

export const COLORS = [
  '#5b5bd6', '#0d9488', '#c2410c', '#7c3aed', '#0369a1',
  '#b45309', '#be123c', '#15803d', '#4f46e5', '#0e7490'
];

export function colorFor(seed: string): string {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  return COLORS[h % COLORS.length];
}

export function newIdentity(name: string, handle: string, color?: string): { identity: Identity; keyring: Keyring } {
  const edPriv = ed25519.utils.randomPrivateKey();
  const xPriv = x25519.utils.randomPrivateKey();
  const edPub = ed25519.getPublicKey(edPriv);
  const xPub = x25519.getPublicKey(xPriv);
  const id = `did:union:${b64u(edPub)}`;
  const createdAt = now();
  const identity: Identity = {
    type: 'identity',
    id,
    handle,
    name: name.trim() || handle,
    about: '',
    edPub: b64(edPub),
    xPub: b64(xPub),
    color: color || colorFor(id),
    createdAt,
    updatedAt: createdAt,
    experience: 'special',
    activation: { status: 'inactive' }
  };
  const keyring: Keyring = {
    type: 'keyring',
    id: `key_${uid('k').slice(2)}`,
    identityId: id,
    edPriv: b64(edPriv),
    xPriv: b64(xPriv),
    createdAt
  };
  return { identity, keyring };
}

export function sign(keys: Keyring, message: string | Uint8Array): string {
  const bytes = typeof message === 'string' ? toBytes(message) : message;
  return b64(ed25519.sign(bytes, fromB64(keys.edPriv)));
}

export function verify(edPub: string, signature: string, message: string | Uint8Array): boolean {
  try {
    const bytes = typeof message === 'string' ? toBytes(message) : message;
    return ed25519.verify(fromB64(signature), bytes, fromB64(edPub));
  } catch {
    return false;
  }
}

export function publicFromPrivate(keys: Keyring): { edPub: string; xPub: string } {
  return {
    edPub: b64(ed25519.getPublicKey(fromB64(keys.edPriv))),
    xPub: b64(x25519.getPublicKey(fromB64(keys.xPriv)))
  };
}

/** Deterministic JSON with sorted keys: the input to every signature/hash. */
export function canonical(value: unknown): string {
  if (value === null || typeof value !== 'object') return JSON.stringify(value) ?? 'null';
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`;
  const obj = value as Record<string, unknown>;
  const keys = Object.keys(obj).filter((k) => obj[k] !== undefined).sort();
  return `{${keys.map((k) => `${JSON.stringify(k)}:${canonical(obj[k])}`).join(',')}}`;
}

/* ------------------------------------------------------------------ cards */

export interface UnionCard {
  v: 1;
  k: 'union-card';
  id: string;
  handle: string;
  name: string;
  about: string;
  ed: string;
  x: string;
  color: string;
  ts: string;
  sig?: string;
}

export function makeCard(identity: Identity, keys: Keyring): UnionCard {
  const card: UnionCard = {
    v: 1,
    k: 'union-card',
    id: identity.id,
    handle: identity.handle,
    name: identity.name,
    about: identity.about,
    ed: identity.edPub,
    x: identity.xPub,
    color: identity.color,
    ts: now()
  };
  card.sig = sign(keys, canonical({ ...card, sig: undefined }));
  return card;
}

export function encodeCard(card: UnionCard): string {
  return `union-card:${b64u(toBytes(JSON.stringify(card)))}`;
}

export function decodeCard(text: string): UnionCard | null {
  try {
    const raw = text.trim().replace(/^union-card:/, '');
    const card = JSON.parse(fromBytes(fromB64u(raw))) as UnionCard;
    if (card.v !== 1 || card.k !== 'union-card' || !card.id || !card.ed || !card.x) return null;
    if (!card.sig) return null;
    const copy = { ...card };
    delete copy.sig;
    return verify(card.ed, card.sig, canonical(copy)) ? card : null;
  } catch {
    return null;
  }
}

/* --------------------------------------------------------------- messaging */

export interface SealedMessage {
  v: 1;
  alg: 'x25519-hkdf-sha256-aes256gcm';
  epk: string;
  from: string;
  to: string;
  body: { n: string; c: string };
  self: { n: string; c: string };
}

async function aesKey(shared: Uint8Array, info: string): Promise<CryptoKey> {
  const keyBytes = new Uint8Array(hkdf(sha256, shared, undefined, toBytes(info), 32));
  return crypto.subtle.importKey('raw', keyBytes, 'AES-GCM', false, ['encrypt', 'decrypt']);
}

async function aesEncrypt(key: CryptoKey, text: string): Promise<{ n: string; c: string }> {
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const cipher = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: nonce }, key, toBytes(text));
  return { n: b64(nonce), c: b64(new Uint8Array(cipher)) };
}

async function aesDecrypt(key: CryptoKey, box: { n: string; c: string }): Promise<string> {
  const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: fromB64(box.n) }, key, fromB64(box.c));
  return fromBytes(new Uint8Array(plain));
}

export async function sealMessage(
  plaintext: string,
  sender: { id: string; edPriv: string; xPriv: string; xPub: string },
  recipient: { id: string; xPub: string }
): Promise<SealedMessage> {
  const ephPriv = x25519.utils.randomPrivateKey();
  const epk = b64(x25519.getPublicKey(ephPriv));
  const toShared = x25519.getSharedSecret(ephPriv, fromB64(recipient.xPub));
  const selfShared = x25519.getSharedSecret(ephPriv, fromB64(sender.xPub));
  const toKey = await aesKey(toShared, `union/v1/message|${sender.id}|recipient`);
  const selfKey = await aesKey(selfShared, `union/v1/message|${sender.id}|self`);
  return {
    v: 1,
    alg: 'x25519-hkdf-sha256-aes256gcm',
    epk,
    from: sender.id,
    to: recipient.id,
    body: await aesEncrypt(toKey, plaintext),
    self: await aesEncrypt(selfKey, plaintext)
  };
}

export async function openSealed(
  sealed: SealedMessage,
  myId: string,
  myXPriv: string
): Promise<string> {
  // The recipient decrypts `body`; the sender can still read its own copy
  // from `self`. Deciding by recipient id here would make every received
  // message try the sender-only box and fail.
  const isSenderCopy = sealed.from === myId;
  const shared = x25519.getSharedSecret(fromB64(myXPriv), fromB64(sealed.epk));
  const info = isSenderCopy
    ? `union/v1/message|${sealed.from}|self`
    : `union/v1/message|${sealed.from}|recipient`;
  const key = await aesKey(shared, info);
  return aesDecrypt(key, isSenderCopy ? sealed.self : sealed.body);
}

/* ----------------------------------------------------------------- backup */

export interface BackupEnvelope {
  v: 1;
  kdf: 'PBKDF2-SHA256';
  iterations: number;
  salt: string;
  iv: string;
  ct: string;
  createdAt: string;
}

async function passKey(passphrase: string, salt: Uint8Array<ArrayBuffer>, iterations: number): Promise<CryptoKey> {
  const base = await crypto.subtle.importKey('raw', toBytes(passphrase), 'PBKDF2', false, ['deriveKey']);
  return crypto.subtle.deriveKey(
    { name: 'PBKDF2', salt, iterations, hash: 'SHA-256' },
    base,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt']
  );
}

export async function encryptBackup(payload: unknown, passphrase: string): Promise<string> {
  if (passphrase.length < 8) throw new Error('Use a passphrase of at least 8 characters.');
  const salt = new Uint8Array(randomBytes(16));
  const iv = new Uint8Array(randomBytes(12));
  const iterations = 310000;
  const key = await passKey(passphrase, salt, iterations);
  const ct = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    toBytes(JSON.stringify(payload))
  );
  const envelope: BackupEnvelope = {
    v: 1,
    kdf: 'PBKDF2-SHA256',
    iterations,
    salt: b64(salt),
    iv: b64(iv),
    ct: b64(new Uint8Array(ct)),
    createdAt: now()
  };
  return JSON.stringify(envelope, null, 2);
}

export async function decryptBackup<T = unknown>(text: string, passphrase: string): Promise<T> {
  const envelope = JSON.parse(text) as BackupEnvelope;
  if (envelope.v !== 1 || envelope.kdf !== 'PBKDF2-SHA256') throw new Error('Unknown backup format.');
  const key = await passKey(passphrase, fromB64(envelope.salt), envelope.iterations);
  const plain = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: fromB64(envelope.iv) },
    key,
    fromB64(envelope.ct)
  );
  return JSON.parse(fromBytes(new Uint8Array(plain))) as T;
}

/** Hash an arbitrary value for the audit/ledger chain. */
export async function hashValue(value: unknown): Promise<string> {
  return sha256Hex(canonical(value));
}

export function randomToken(bytes = 16): string {
  return b64u(randomBytes(bytes));
}

export function encodeToken(payload: unknown): string {
  return b64u(toBytes(JSON.stringify(payload)));
}

export function decodeToken<T = unknown>(token: string): T | null {
  try {
    return JSON.parse(fromBytes(fromB64u(token))) as T;
  } catch {
    return null;
  }
}
