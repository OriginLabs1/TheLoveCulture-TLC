import test from 'node:test';
import assert from 'node:assert/strict';

import {
  canonical,
  newIdentity,
  sign,
  verify,
  publicFromPrivate,
  makeCard,
  encodeCard,
  decodeCard,
  sealMessage,
  openSealed,
  encryptBackup,
  decryptBackup,
  hashValue,
  encodeToken,
  decodeToken
} from './.build/crypto.mjs';
import { sha256Hex, coreBackend } from './.build/core.mjs';
import { readToken } from './.build/links.mjs';

const alice = newIdentity('Alice Example', 'alice_example');
const bob = newIdentity('Bob Example', 'bob_example');

test('canonical JSON is key-order independent and drops undefined', () => {
  const a = canonical({ b: 1, a: { d: [3, 2], c: 'x' } });
  const b = canonical({ a: { c: 'x', d: [3, 2] }, b: 1 });
  assert.equal(a, b);
  assert.equal(canonical({ a: 1, skipped: undefined }), '{"a":1}');
  assert.equal(canonical([null, true, 'x']), '[null,true,"x"]');
});

test('identity keys agree with the published public keys', () => {
  const pub = publicFromPrivate(alice.keyring);
  assert.equal(pub.edPub, alice.identity.edPub);
  assert.equal(pub.xPub, alice.identity.xPub);
  assert.match(alice.identity.id, /^did:union:/);
});

test('signatures verify, and a changed message does not', () => {
  const sig = sign(alice.keyring, 'a message worth trusting');
  assert.equal(verify(alice.identity.edPub, sig, 'a message worth trusting'), true);
  assert.equal(verify(alice.identity.edPub, sig, 'a message worth trusting!'), false);
  assert.equal(verify(bob.identity.edPub, sig, 'a message worth trusting'), false);
});

test('a signed card round-trips and rejects tampering', () => {
  const text = encodeCard(makeCard(alice.identity, alice.keyring));
  assert.ok(text.startsWith('union-card:'));
  const card = decodeCard(text);
  assert.ok(card);
  assert.equal(card.id, alice.identity.id);
  assert.equal(card.handle, 'alice_example');

  const raw = text.replace('union-card:', '');
  const payload = JSON.parse(Buffer.from(raw, 'base64url').toString('utf8'));
  payload.name = 'Alice Impostor';
  const forged = `union-card:${Buffer.from(JSON.stringify(payload)).toString('base64url')}`;
  assert.equal(decodeCard(forged), null);
  assert.equal(decodeCard('union-card:not-base64'), null);
});

test('sealed messages open only for the intended recipient', async () => {
  const sealed = await sealMessage(
    'Meet at the reading room at six.',
    {
      id: alice.identity.id,
      edPriv: alice.keyring.edPriv,
      xPriv: alice.keyring.xPriv,
      xPub: alice.identity.xPub
    },
    { id: bob.identity.id, xPub: bob.identity.xPub }
  );

  assert.equal(
    await openSealed(sealed, bob.identity.id, bob.keyring.xPriv),
    'Meet at the reading room at six.'
  );
  // The sender keeps a readable copy without re-encrypting.
  assert.equal(
    await openSealed(sealed, alice.identity.id, alice.keyring.xPriv),
    'Meet at the reading room at six.'
  );
  // A third party cannot open it.
  const eve = newIdentity('Eve', 'eve_example');
  await assert.rejects(() => openSealed(sealed, eve.identity.id, eve.keyring.xPriv));

  // Ciphertext is authenticated: flipping a byte must fail.
  const broken = { ...sealed, body: { ...sealed.body } };
  const bytes = Buffer.from(broken.body.c, 'base64');
  bytes[5] ^= 0xff;
  broken.body.c = bytes.toString('base64');
  await assert.rejects(() => openSealed(broken, bob.identity.id, bob.keyring.xPriv));
});

test('encrypted backups round-trip with the right passphrase only', async () => {
  const payload = { schema: 'union.backup.v1', identity: alice.identity, note: 'recovery' };
  const text = await encryptBackup(payload, 'correct horse battery staple');
  const envelope = JSON.parse(text);
  assert.equal(envelope.kdf, 'PBKDF2-SHA256');
  assert.equal(envelope.iterations, 310000);
  assert.ok(!text.includes('Alice Example'), 'backup must not leak plaintext');

  const restored = await decryptBackup(text, 'correct horse battery staple');
  assert.deepEqual(restored, payload);
  await assert.rejects(() => decryptBackup(text, 'wrong passphrase'));
  await assert.rejects(() => encryptBackup(payload, 'short'));
});

test('hashValue is stable and distinguishes values', async () => {
  const one = await hashValue({ amount: 80, token: 'ACT' });
  const two = await hashValue({ token: 'ACT', amount: 80 });
  const three = await hashValue({ amount: 81, token: 'ACT' });
  assert.equal(one, two);
  assert.notEqual(one, three);
  assert.match(one, /^[0-9a-f]{64}$/);
});

test('the hash primitive matches the known SHA-256 vector', async () => {
  assert.equal(
    await sha256Hex('abc'),
    'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
  );
  assert.equal(
    await sha256Hex(''),
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
  );
  // No DOM in Node, so the fallback is expected here. The browser reports the
  // Rust core after loading the wasm module.
  assert.equal(coreBackend().backend, 'js');
});

test('link tokens round-trip through the typed reader', () => {
  const token = {
    v: 1,
    kind: 'page',
    page: {
      id: 'page_1',
      title: 'A page',
      tagline: 'Honest',
      about: '',
      sections: [],
      links: [],
      theme: 'calm',
      updatedAt: new Date().toISOString()
    }
  };
  const encoded = encodeToken(token);
  assert.equal(encoded.includes('+'), false);
  assert.equal(encoded.includes('/'), false);
  const decoded = readToken(encoded);
  assert.equal(decoded?.kind, 'page');
  assert.equal(decoded?.page.title, 'A page');
  assert.equal(readToken('not-a-token'), null);
  assert.equal(decodeToken('%%%'), null);
});

test('a hash chain detects a rewritten entry', async () => {
  const entries = [];
  let prev = '0'.repeat(64);
  for (const amount of [80, 10, 5]) {
    const base = { kind: 'test', amount, prevHash: prev };
    const hash = await hashValue(base);
    entries.push({ ...base, hash });
    prev = hash;
  }
  const verifyChain = async (list) => {
    let prior = '0'.repeat(64);
    for (const entry of list) {
      const expected = await hashValue({
        kind: entry.kind,
        amount: entry.amount,
        prevHash: prior
      });
      if (entry.prevHash !== prior || entry.hash !== expected) return false;
      prior = entry.hash;
    }
    return true;
  };
  assert.equal(await verifyChain(entries), true);

  const tampered = entries.map((entry) => ({ ...entry }));
  tampered[1].amount = 1000;
  assert.equal(await verifyChain(tampered), false);
});
