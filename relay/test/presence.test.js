import assert from 'node:assert/strict';
import test from 'node:test';
import { canShareSignal, normalizeConsent, projectPresence } from '../src/presence.js';

const now = 1_700_000_000_000;
const circle = new Set(['grace']);

test('default consent reveals nothing', () => {
  const projection = projectPresence({
    consent: normalizeConsent({}),
    viewer: 'grace',
    circle,
    online: true,
    lastSeen: now,
    now,
  });
  assert.equal(projection.onlineNow, null);
  assert.equal(projection.lastSeen, null);
  assert.equal(projection.typing, null);
  assert.equal(projection.readReceipts, null);
});

test('invisible overrides everyone', () => {
  const consent = normalizeConsent({
    onlineNow: 'EVERYONE',
    lastSeen: 'EVERYONE',
    typing: 'EVERYONE',
    readReceipts: 'EVERYONE',
    invisible: true,
  });
  const projection = projectPresence({ consent, viewer: 'grace', circle, online: true, lastSeen: now, now });
  assert.equal(projection.onlineNow, null);
  assert.equal(projection.lastSeen, null);
});

test('selected is exact and hidden wins', () => {
  const selected = normalizeConsent({ onlineNow: 'SELECTED', selected: ['GRACE'] });
  assert.equal(
    projectPresence({ consent: selected, viewer: 'grace', circle, online: true, lastSeen: now, now }).onlineNow,
    true,
  );
  assert.equal(
    projectPresence({ consent: selected, viewer: 'mallory', circle, online: true, lastSeen: now, now }).onlineNow,
    null,
  );
  const hidden = normalizeConsent({ onlineNow: 'EVERYONE', hidden: ['grace'] });
  assert.equal(
    projectPresence({ consent: hidden, viewer: 'grace', circle, online: true, lastSeen: now, now }).onlineNow,
    null,
  );
});

test('circle requires an accepted connection', () => {
  const consent = normalizeConsent({ onlineNow: 'CIRCLE' });
  assert.equal(
    projectPresence({ consent, viewer: 'grace', circle, online: true, lastSeen: now, now }).onlineNow,
    true,
  );
  assert.equal(
    projectPresence({ consent, viewer: 'grace', circle: new Set(), online: true, lastSeen: now, now }).onlineNow,
    null,
  );
});

test('last seen is coarse unless exact is chosen', () => {
  const consent = normalizeConsent({ lastSeen: 'EVERYONE' });
  const coarse = projectPresence({ consent, viewer: 'grace', circle, online: false, lastSeen: now - 1000, now });
  assert.equal(coarse.lastSeenCoarse, true);
  assert.notEqual(coarse.lastSeen, now - 1000);

  const exact = normalizeConsent({ lastSeen: 'EVERYONE', exactLastSeen: true });
  const precise = projectPresence({ consent: exact, viewer: 'grace', circle, online: false, lastSeen: now - 1000, now });
  assert.equal(precise.lastSeenCoarse, false);
  assert.equal(precise.lastSeen, now - 1000);
});

test('signal checks used by typing and receipts obey the same policy', () => {
  const consent = normalizeConsent({ typing: 'CIRCLE', readReceipts: 'NOBODY' });
  assert.equal(canShareSignal(consent, 'typing', 'grace', circle), true);
  assert.equal(canShareSignal(consent, 'typing', 'mallory', circle), false);
  assert.equal(canShareSignal(consent, 'read_receipts', 'grace', circle), false);
});
