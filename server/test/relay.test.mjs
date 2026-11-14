/**
 * Relay tests run against the real Worker handler and an in-memory store.
 * No network, no Cloudflare account, no wrangler required.
 *
 *   npm run test:relay
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { ed25519 } from '@noble/curves/ed25519';
import { x25519 } from '@noble/curves/ed25519';
import { sha256 } from '@noble/hashes/sha256';
import { base64, base64url } from '@scure/base';

import { createApp, appDefaults } from '../src/app.js';
import { memoryStore } from '../src/store-memory.js';

const enc = new TextEncoder();

const b64 = (bytes) => base64.encode(bytes);
const b64u = (bytes) => base64url.encode(bytes);
const hex = (bytes) => [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');

function canonical(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value) ?? 'null';
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`;
  const keys = Object.keys(value).filter((k) => value[k] !== undefined).sort();
  return `{${keys.map((k) => `${JSON.stringify(k)}:${canonical(value[k])}`).join(',')}}`;
}

function makeIdentity(handle) {
  const edPriv = ed25519.utils.randomPrivateKey();
  const xPriv = x25519.utils.randomPrivateKey();
  const ed = ed25519.getPublicKey(edPriv);
  const x = x25519.getPublicKey(xPriv);
  const id = `did:union:${b64u(ed)}`;
  const card = {
    v: 1,
    k: 'union-card',
    id,
    handle,
    name: 'Test Person',
    about: '',
    ed: b64(ed),
    x: b64(x),
    color: '#5b5bd6',
    ts: new Date().toISOString()
  };
  card.sig = b64(ed25519.sign(enc.encode(canonical(card)), edPriv));
  return { id, handle, edPriv, ed, x, card };
}

function setup() {
  const store = memoryStore();
  const app = createApp({ ...appDefaults, store, allowedOrigins: 'https://union.example,https://other.example' });
  const origin = 'https://union.example';
  const call = async (method, path, { body, identity, timestamp, signature } = {}) => {
    const text = body === undefined ? '' : JSON.stringify(body);
    const headers = { origin };
    if (identity) {
      const ts = timestamp ?? Math.floor(Date.now() / 1000).toString();
      const digest = hex(sha256(enc.encode(text)));
      const message = `${method}\n${path}\n${ts}\n${digest}`;
      headers['x-union-id'] = identity.id;
      headers['x-union-ts'] = ts;
      headers['x-union-sig'] = signature ?? b64(ed25519.sign(enc.encode(message), identity.edPriv));
    }
    if (body !== undefined) headers['content-type'] = 'application/json';
    const request = new Request(`https://relay.example${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : text
    });
    const response = await app(request);
    const type = response.headers.get('content-type') || '';
    const payload = type.includes('json') ? await response.json() : await response.text();
    return { status: response.status, headers: response.headers, payload };
  };
  return { call, store };
}

async function register(call, identity) {
  const response = await call('POST', '/v1/identities', { body: { card: identity.card } });
  assert.equal(response.status, 201, JSON.stringify(response.payload));
}

test('health is public and CORS is present', async () => {
  const { call } = setup();
  const response = await call('GET', '/health');
  assert.equal(response.status, 200);
  assert.equal(response.payload.service, 'union-relay');
  assert.equal(response.headers.get('access-control-allow-origin'), 'https://union.example');
});

test('a card must be signed by the key its id names', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  const forged = { ...alice.card, handle: 'mallory' };
  const response = await call('POST', '/v1/identities', { body: { card: forged } });
  assert.equal(response.status, 401);
});

test('identity registration is idempotent and handles are unique', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  await register(call, alice);
  await register(call, alice);

  const lookup = await call('GET', '/v1/identities/ALICE');
  assert.equal(lookup.status, 200);
  assert.equal(lookup.payload.card.id, alice.id);

  const bob = makeIdentity('alice');
  const taken = await call('POST', '/v1/identities', { body: { card: bob.card } });
  assert.equal(taken.status, 409);
});

test('write requests require a fresh valid signature', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  await register(call, alice);

  const unsigned = await call('POST', '/v1/messages', {
    body: { id: 'msg_unsigned_1', to: alice.id, payload: 'x' }
  });
  assert.equal(unsigned.status, 401);

  const badSignature = await call('POST', '/v1/messages', {
    body: { id: 'msg_unsigned_2', to: alice.id, payload: 'x' },
    identity: alice,
    signature: b64(new Uint8Array(64))
  });
  assert.equal(badSignature.status, 401);

  const stale = await call('POST', '/v1/messages', {
    body: { id: 'msg_unsigned_3', to: alice.id, payload: 'x' },
    identity: alice,
    timestamp: '1000000000'
  });
  assert.equal(stale.status, 401);

  const tampered = await call('POST', '/v1/messages', {
    body: { id: 'msg_unsigned_4', to: alice.id, payload: 'changed after signing' },
    identity: alice,
    signature: b64(ed25519.sign(enc.encode('POST\n/v1/messages\n1\nx'), alice.edPriv))
  });
  assert.equal(tampered.status, 401);
});

test('sealed envelopes are delivered once and can be acknowledged', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  const bob = makeIdentity('bob');
  await register(call, alice);
  await register(call, bob);

  const sent = await call('POST', '/v1/messages', {
    body: { id: 'msg_00000001', to: bob.id, payload: 'ciphertext-envelope' },
    identity: alice
  });
  assert.equal(sent.status, 201);

  const duplicate = await call('POST', '/v1/messages', {
    body: { id: 'msg_00000001', to: bob.id, payload: 'ciphertext-envelope' },
    identity: alice
  });
  assert.equal(duplicate.status, 200);
  assert.equal(duplicate.payload.duplicate, true);

  const inbox = await call('GET', '/v1/messages?since=1970-01-01T00%3A00%3A00.000Z', { identity: bob });
  assert.equal(inbox.status, 200);
  assert.equal(inbox.payload.messages.length, 1);
  assert.equal(inbox.payload.messages[0].payload, 'ciphertext-envelope');

  const ack = await call('POST', '/v1/messages/ack', {
    body: { ids: ['msg_00000001'] },
    identity: bob
  });
  assert.equal(ack.status, 200);

  const after = await call('GET', '/v1/messages?since=1970-01-01T00%3A00%3A00.000Z', { identity: bob });
  assert.equal(after.payload.messages.length, 0);
});

test('links resolve publicly, revoke, and reject a second owner', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  const bob = makeIdentity('bob');
  await register(call, alice);
  await register(call, bob);

  const link = {
    id: 'lnk_test_1',
    kind: 'profile',
    title: 'Alice',
    subtitle: 'A calm introduction',
    payload: { handle: 'alice' },
    permissions: ['view'],
    expiresAt: null
  };
  const created = await call('POST', '/v1/links', { body: link, identity: alice });
  assert.equal(created.status, 201);

  const resolved = await call('GET', '/l/lnk_test_1');
  assert.equal(resolved.status, 200);
  assert.equal(resolved.payload.title, 'Alice');
  assert.deepEqual(resolved.payload.permissions, ['view']);

  const stolen = await call('POST', '/v1/links', { body: link, identity: bob });
  assert.equal(stolen.status, 409);

  const foreignRevoke = await call('POST', '/v1/links/lnk_test_1/revoke', { body: {}, identity: bob });
  assert.equal(foreignRevoke.status, 403);

  const revoked = await call('POST', '/v1/links/lnk_test_1/revoke', { body: {}, identity: alice });
  assert.equal(revoked.status, 200);

  const gone = await call('GET', '/l/lnk_test_1');
  assert.equal(gone.status, 410);
});

test('public rooms accept posts; private rooms do not', async () => {
  const { call } = setup();
  const alice = makeIdentity('alice');
  await register(call, alice);

  const publicRoom = {
    id: 'room_public_1',
    name: 'Reading room',
    about: 'Slow reading',
    color: '#0d9488',
    visibility: 'public'
  };
  assert.equal((await call('POST', '/v1/spaces', { body: publicRoom, identity: alice })).status, 201);
  assert.equal(
    (await call('GET', '/v1/spaces/room_public_1')).payload.name,
    'Reading room'
  );

  const post = {
    id: 'post_0001',
    body: 'The first note.',
    authorName: 'Alice',
    createdAt: new Date().toISOString()
  };
  assert.equal(
    (await call('POST', '/v1/spaces/room_public_1/posts', { body: post, identity: alice })).status,
    201
  );
  const posts = await call('GET', '/v1/spaces/room_public_1/posts');
  assert.equal(posts.payload.posts.length, 1);
  assert.equal(posts.payload.posts[0].body, 'The first note.');

  const privateRoom = { id: 'room_private_1', name: 'Quiet', visibility: 'private' };
  assert.equal((await call('POST', '/v1/spaces', { body: privateRoom, identity: alice })).status, 201);
  const refused = await call('POST', '/v1/spaces/room_private_1/posts', {
    body: { id: 'post_0002', body: 'not shared' },
    identity: alice
  });
  assert.equal(refused.status, 403);
});

test('unknown endpoints fail closed', async () => {
  const { call } = setup();
  const response = await call('GET', '/v1/nothing');
  assert.equal(response.status, 404);
  assert.equal(response.payload.error, 'No such endpoint.');
});
