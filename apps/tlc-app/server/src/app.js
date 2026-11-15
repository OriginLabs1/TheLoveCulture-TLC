/**
 * Union relay — a small, honest store-and-forward service.
 *
 * What it can see: routing metadata (who, when, how large).
 * What it cannot see: message bodies, link payloads' meaning, keys, backups.
 * Every write is an Ed25519 signature by the identity that owns it.
 *
 * Deliberately no accounts, no passwords, no cookies, no analytics.
 */

const MAX_BODY = 96 * 1024;
const MAX_CARD = 8 * 1024;
const MAX_PAYLOAD = 64 * 1024;
const MAX_LINK = 32 * 1024;
const MAX_POST = 8 * 1024;
const SKEW_SECONDS = 300;
const HANDLE = /^[a-z0-9_]{2,32}$/;
const ID = /^did:union:[A-Za-z0-9_-]{20,64}={0,2}$/;

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const enc = new TextEncoder();
const dec = new TextDecoder();

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': 'no-store',
      ...headers
    }
  });
}

function fail(status, message, headers = {}) {
  return json({ error: message }, status, headers);
}

/** Deterministic JSON with sorted keys — identical to the client's canonical(). */
function canonical(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value) ?? 'null';
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`;
  const obj = value;
  const keys = Object.keys(obj).filter((k) => obj[k] !== undefined).sort();
  return `{${keys.map((k) => `${JSON.stringify(k)}:${canonical(obj[k])}`).join(',')}}`;
}

function b64ToBytes(text) {
  const raw = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) out[i] = raw.charCodeAt(i);
  return out;
}

function b64url(bytes) {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_');
}

async function sha256Hex(text) {
  const digest = await crypto.subtle.digest('SHA-256', enc.encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

async function verifyEd25519(edBase64, signatureBase64, message) {
  try {
    const key = await crypto.subtle.importKey(
      'raw',
      b64ToBytes(edBase64),
      { name: 'Ed25519' },
      false,
      ['verify']
    );
    return await crypto.subtle.verify(
      { name: 'Ed25519' },
      key,
      b64ToBytes(signatureBase64),
      typeof message === 'string' ? enc.encode(message) : message
    );
  } catch {
    return false;
  }
}

/** A card is valid only if its signature holds and its id really is its key. */
async function readCard(card) {
  if (!card || typeof card !== 'object' || card.k !== 'union-card' || card.v !== 1) {
    throw new HttpError(400, 'Not a signed Union card.');
  }
  if (typeof card.id !== 'string' || !ID.test(card.id)) throw new HttpError(400, 'Card id is invalid.');
  if (typeof card.handle !== 'string' || !HANDLE.test(card.handle)) {
    throw new HttpError(400, 'Handle must be 2–32 lowercase letters, digits or _.');
  }
  if (typeof card.ed !== 'string' || typeof card.sig !== 'string') {
    throw new HttpError(400, 'Card is missing its key or signature.');
  }
  if (JSON.stringify(card).length > MAX_CARD) throw new HttpError(413, 'Card is too large.');

  const copy = { ...card };
  delete copy.sig;
  if (!(await verifyEd25519(card.ed, card.sig, canonical(copy)))) {
    throw new HttpError(401, 'Card signature does not verify.');
  }

  const derived = `did:union:${b64url(b64ToBytes(card.ed))}`;
  if (derived !== card.id) throw new HttpError(401, 'Card id does not match its key.');

  return card;
}

const ORIGIN_DEFAULT = '*';

function corsHeaders(origin, allowed) {
  const list = (allowed || ORIGIN_DEFAULT)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
  const headers = {
    'access-control-allow-methods': 'GET, POST, OPTIONS',
    'access-control-allow-headers': 'content-type, x-union-id, x-union-ts, x-union-sig',
    'access-control-max-age': '86400',
    vary: 'origin'
  };
  // Never echo an origin that is not on the list: a disallowed caller gets a
  // response without the allow-origin header and the browser blocks it.
  if (list.includes('*')) headers['access-control-allow-origin'] = '*';
  else if (origin && list.includes(origin)) headers['access-control-allow-origin'] = origin;
  return headers;
}

/** Coarse per-isolate limiter: bounded, cheap, and honest about being best-effort. */
function limiter(limit, windowMs) {
  const buckets = new Map();
  return (key) => {
    const now = Date.now();
    const bucket = buckets.get(key);
    if (!bucket || now - bucket.start > windowMs) {
      buckets.set(key, { start: now, count: 1 });
      if (buckets.size > 5000) {
        for (const [k, v] of buckets) if (now - v.start > windowMs) buckets.delete(k);
      }
      return true;
    }
    bucket.count += 1;
    return bucket.count <= limit;
  };
}

/**
 * Verify a signed request. The client signs:
 *   METHOD \n path-with-query \n unix-seconds \n sha256(body)
 */
async function authenticate(store, request, rawBody, method, path) {
  const id = request.headers.get('x-union-id') || '';
  const ts = request.headers.get('x-union-ts') || '';
  const sig = request.headers.get('x-union-sig') || '';
  if (!ID.test(id) || !ts || !sig) throw new HttpError(401, 'This request is not signed.');

  const seconds = Number(ts);
  if (!Number.isFinite(seconds) || Math.abs(Date.now() / 1000 - seconds) > SKEW_SECONDS) {
    throw new HttpError(401, 'Signed request is outside the allowed time window.');
  }

  const identity = await store.identityGetById(id);
  if (!identity) throw new HttpError(401, 'Register this identity first.');

  const digest = await sha256Hex(rawBody);
  const canonicalRequest = `${method}\n${path}\n${ts}\n${digest}`;
  if (!(await verifyEd25519(identity.card.ed, sig, canonicalRequest))) {
    throw new HttpError(401, 'Request signature does not verify.');
  }
  return identity;
}

export function createApp(options) {
  const store = options.store;
  const allowedOrigins = options.allowedOrigins || ORIGIN_DEFAULT;
  const writeLimit = limiter(120, 60_000);
  const readLimit = limiter(600, 60_000);
  const health = { ok: true, service: 'union-relay', version: 1 };

  async function route(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const ip = request.headers.get('cf-connecting-ip') || 'local';
    const method = request.method;

    if (method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders(request.headers.get('origin') || '', allowedOrigins) });
    }

    if (path === '/health' && method === 'GET') return json(health);

    const isWrite = method !== 'GET';
    if (!(isWrite ? writeLimit(ip) : readLimit(ip))) {
      throw new HttpError(429, 'Too many requests. Try again shortly.');
    }

    if (path === '/v1/identities' && method === 'POST') {
      const envelope = options.readBody(await options.body(request));
      const card = await readCard(envelope.card);
      const existingHandle = await store.identityGetByHandle(card.handle);
      if (existingHandle && existingHandle.card.id !== card.id) {
        throw new HttpError(409, 'That handle already belongs to another identity.');
      }
      const now = new Date().toISOString();
      await store.identityPut(card, now);
      return json({ ok: true, id: card.id }, 201);
    }

    if (path.startsWith('/v1/identities/') && method === 'GET') {
      const handle = decodeURIComponent(path.slice('/v1/identities/'.length)).toLowerCase();
      if (!HANDLE.test(handle)) throw new HttpError(400, 'Invalid handle.');
      const found = await store.identityGetByHandle(handle);
      if (!found) return fail(404, 'No such identity.');
      return json({ card: found.card });
    }

    if (path === '/v1/messages' && method === 'POST') {
      const raw = await options.body(request);
      const identity = await authenticate(store, request, raw, 'POST', path + url.search);
      const body = options.readBody(raw);
      if (typeof body.id !== 'string' || body.id.length < 8 || body.id.length > 80) {
        throw new HttpError(400, 'Message id is invalid.');
      }
      if (typeof body.to !== 'string' || !ID.test(body.to)) throw new HttpError(400, 'Recipient is invalid.');
      if (typeof body.payload !== 'string' || body.payload.length > MAX_PAYLOAD) {
        throw new HttpError(413, 'Payload is empty or too large.');
      }
      if (body.to === identity.card.id) throw new HttpError(400, 'You cannot relay to yourself.');
      if (await store.messageGet(body.id)) return json({ ok: true, duplicate: true });
      const createdAt = new Date().toISOString();
      await store.messagePut({
        id: body.id,
        from: identity.card.id,
        to: body.to,
        payload: body.payload,
        createdAt
      });
      return json({ id: body.id, createdAt }, 201);
    }

    if (path === '/v1/messages' && method === 'GET') {
      const raw = '';
      const identity = await authenticate(store, request, raw, 'GET', path + url.search);
      const since = url.searchParams.get('since') || '1970-01-01T00:00:00.000Z';
      const messages = await store.messagesFor(identity.card.id, since);
      return json({ messages });
    }

    if (path === '/v1/messages/ack' && method === 'POST') {
      const raw = await options.body(request);
      const identity = await authenticate(store, request, raw, 'POST', path);
      const body = options.readBody(raw);
      const ids = Array.isArray(body.ids) ? body.ids.filter((id) => typeof id === 'string').slice(0, 200) : [];
      await store.messagesAck(identity.card.id, ids);
      return json({ ok: true, acked: ids.length });
    }

    if (path === '/v1/links' && method === 'POST') {
      const raw = await options.body(request);
      const identity = await authenticate(store, request, raw, 'POST', path);
      const body = options.readBody(raw);
      if (typeof body.id !== 'string' || body.id.length < 4 || body.id.length > 80) {
        throw new HttpError(400, 'Link id is invalid.');
      }
      if (typeof body.kind !== 'string' || typeof body.title !== 'string') {
        throw new HttpError(400, 'A link needs a kind and a title.');
      }
      if (body.title.length > 140 || (body.subtitle || '').length > 240) {
        throw new HttpError(400, 'Link title or subtitle is too long.');
      }
      if (JSON.stringify(body.payload ?? null).length > MAX_LINK) throw new HttpError(413, 'Link payload is too large.');
      const existing = await store.linkGet(body.id);
      if (existing && existing.ownerId !== identity.card.id) {
        throw new HttpError(409, 'That link id belongs to another identity.');
      }
      const permissions = Array.isArray(body.permissions)
        ? body.permissions.filter((p) => typeof p === 'string').slice(0, 12)
        : [];
      const createdAt = new Date().toISOString();
      await store.linkPut({
        id: body.id,
        ownerId: identity.card.id,
        kind: body.kind,
        title: body.title,
        subtitle: typeof body.subtitle === 'string' ? body.subtitle : '',
        payload: body.payload ?? null,
        permissions,
        createdAt,
        expiresAt: typeof body.expiresAt === 'string' ? body.expiresAt : null
      });
      return json({ id: body.id }, 201);
    }

    if (path.startsWith('/l/') && method === 'GET') {
      const id = decodeURIComponent(path.slice(3));
      const link = await store.linkGet(id);
      if (!link) return fail(404, 'That link is not on this relay.');
      if (link.revokedAt) return fail(410, 'The owner revoked this link.');
      if (link.expiresAt && Date.parse(link.expiresAt) < Date.now()) {
        return fail(410, 'This link has expired.');
      }
      return json({
        id: link.id,
        kind: link.kind,
        title: link.title,
        subtitle: link.subtitle,
        payload: link.payload,
        permissions: link.permissions,
        createdAt: link.createdAt,
        expiresAt: link.expiresAt || undefined,
        revokedAt: link.revokedAt || undefined
      });
    }

    if (path.startsWith('/v1/links/') && path.endsWith('/revoke') && method === 'POST') {
      const raw = await options.body(request);
      const identity = await authenticate(store, request, raw, 'POST', path);
      const id = decodeURIComponent(path.slice('/v1/links/'.length, -'/revoke'.length));
      const link = await store.linkGet(id);
      if (!link) return fail(404, 'That link is not on this relay.');
      if (link.ownerId !== identity.card.id) return fail(403, 'Only the owner can revoke a link.');
      await store.linkRevoke(id, identity.card.id, new Date().toISOString());
      return json({ ok: true });
    }

    if (path === '/v1/spaces' && method === 'POST') {
      const raw = await options.body(request);
      const identity = await authenticate(store, request, raw, 'POST', path);
      const body = options.readBody(raw);
      if (typeof body.id !== 'string' || body.id.length < 4 || body.id.length > 80) {
        throw new HttpError(400, 'Space id is invalid.');
      }
      if (typeof body.name !== 'string' || !body.name.trim() || body.name.length > 80) {
        throw new HttpError(400, 'A space needs a name.');
      }
      const existing = await store.spaceGet(body.id);
      if (existing && existing.ownerId !== identity.card.id) {
        throw new HttpError(409, 'That space id belongs to another identity.');
      }
      const visibility = body.visibility === 'public' ? 'public' : 'private';
      await store.spacePut({
        id: body.id,
        ownerId: identity.card.id,
        name: body.name.trim(),
        about: typeof body.about === 'string' ? body.about.slice(0, 600) : '',
        color: typeof body.color === 'string' ? body.color.slice(0, 32) : '',
        visibility,
        createdAt: new Date().toISOString()
      });
      return json({ id: body.id }, 201);
    }

    if (path.startsWith('/v1/spaces/') && method === 'GET' && !path.endsWith('/posts')) {
      const id = decodeURIComponent(path.slice('/v1/spaces/'.length));
      const space = await store.spaceGet(id);
      if (!space) return fail(404, 'No such space.');
      return json({
        id: space.id,
        name: space.name,
        about: space.about,
        color: space.color,
        visibility: space.visibility,
        createdBy: space.ownerId
      });
    }

    if (path.startsWith('/v1/spaces/') && path.endsWith('/posts')) {
      const id = decodeURIComponent(path.slice('/v1/spaces/'.length, -'/posts'.length));
      if (method === 'POST') {
        const raw = await options.body(request);
        const identity = await authenticate(store, request, raw, 'POST', path);
        const body = options.readBody(raw);
        const space = await store.spaceGet(id);
        if (!space) return fail(404, 'No such space.');
        if (space.visibility !== 'public') return fail(403, 'Only public rooms accept relay posts.');
        if (typeof body.id !== 'string' || body.id.length < 4) throw new HttpError(400, 'Post id is invalid.');
        if (typeof body.body !== 'string' || !body.body.trim() || body.body.length > MAX_POST) {
          throw new HttpError(400, 'A post needs text, up to 8 KB.');
        }
        await store.postPut({
          id: body.id,
          communityId: id,
          authorId: identity.card.id,
          authorName: typeof body.authorName === 'string' ? body.authorName.slice(0, 80) : '',
          body: body.body,
          createdAt: typeof body.createdAt === 'string' ? body.createdAt : new Date().toISOString()
        });
        return json({ ok: true }, 201);
      }
      if (method === 'GET') {
        const space = await store.spaceGet(id);
        if (!space) return fail(404, 'No such space.');
        const since = url.searchParams.get('since') || '1970-01-01T00:00:00.000Z';
        const posts = await store.postsSince(id, since);
        return json({ posts });
      }
    }

    return fail(404, 'No such endpoint.');
  }

  return async function fetch(request) {
    const origin = request.headers.get('origin') || '';
    const cors = corsHeaders(origin, allowedOrigins);
    try {
      const response = await route(request);
      for (const [key, value] of Object.entries(cors)) {
        if (!response.headers.has(key)) response.headers.set(key, value);
      }
      return response;
    } catch (error) {
      if (error instanceof HttpError) return fail(error.status, error.message, cors);
      console.error('relay error', error);
      return fail(500, 'The relay could not complete that request.', cors);
    }
  };
}

export const appDefaults = {
  /** Read a raw request body with a hard ceiling. */
  async body(request) {
    const declared = Number(request.headers.get('content-length') || 0);
    if (declared > MAX_BODY) throw new HttpError(413, 'Request is too large.');
    const text = await request.text();
    if (text.length > MAX_BODY) throw new HttpError(413, 'Request is too large.');
    return text;
  },
  readBody(text) {
    try {
      return text ? JSON.parse(text) : {};
    } catch {
      throw new HttpError(400, 'Request body is not JSON.');
    }
  }
};
