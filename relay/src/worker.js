// TLC Relay — a thin, open, replaceable shared plane.
//
// It carries account handles, public keys, consent records, connection state and opaque encrypted
// envelopes. It never receives a passphrase, a private key, plaintext message text, a bank detail
// or a proof image. It does not move, hold or settle money.
//
// Deploy: `npx wrangler deploy` from relay/. The Durable Object owns one SQLite database.

import { canShareSignal, normalizeConsent, projectPresence } from './presence.js';

const VERSION = '0.0.8';
const MAX_BODY = 64 * 1024;
const MAX_MESSAGE = 32 * 1024;
const MESSAGE_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const RESERVED = new Set([
  'tlc', 'admin', 'steward', 'support', 'official', 'pioneer', 'root', 'system', 'help', 'about',
]);
const HANDLE = /^[a-z0-9](?:[a-z0-9_-]{1,22})[a-z0-9]$/;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/health') {
      return json({ ok: true, service: 'tlc-relay', version: VERSION, time: Date.now() });
    }
    if (url.pathname === '/' && request.method === 'GET') {
      return json({
        service: 'tlc-relay',
        version: VERSION,
        doctrine: 'thin · open · replaceable · no funds · no plaintext',
        endpoints: [
          'POST /v1/accounts', 'GET /v1/me', 'GET /v1/directory/{handle}',
          'GET/PUT /v1/presence', 'GET /v1/presence/{handle}', 'POST /v1/heartbeat',
          'GET/POST /v1/connections', 'POST /v1/connections/respond', 'POST /v1/connections/block',
          'GET/POST /v1/messages', 'POST /v1/messages/{id}/delivered', 'POST /v1/messages/{id}/read',
          'POST /v1/typing', 'GET /v1/stream', 'GET /v1/config', 'DELETE /v1/account',
        ],
      });
    }
    const id = env.RELAY.idFromName('hub');
    return env.RELAY.get(id).fetch(request);
  },
};

export class RelayHub {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.sql = ctx.storage.sql;
    this.sessions = new Map();
    this.rate = new Map();
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS accounts (
        handle TEXT PRIMARY KEY,
        account_id TEXT NOT NULL UNIQUE,
        token_hash TEXT NOT NULL,
        signing_key TEXT NOT NULL,
        messaging_key TEXT NOT NULL,
        display TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        last_seen INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS consent (
        handle TEXT PRIMARY KEY,
        json TEXT NOT NULL,
        version INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS connections (
        owner TEXT NOT NULL,
        peer TEXT NOT NULL,
        state TEXT NOT NULL,
        purpose TEXT,
        updated_at INTEGER NOT NULL,
        PRIMARY KEY (owner, peer)
      );
      CREATE TABLE IF NOT EXISTS messages (
        id TEXT PRIMARY KEY,
        seq INTEGER NOT NULL,
        sender TEXT NOT NULL,
        recipient TEXT NOT NULL,
        conversation TEXT NOT NULL,
        ephemeral_key TEXT NOT NULL,
        nonce TEXT NOT NULL,
        ciphertext TEXT NOT NULL,
        aad TEXT NOT NULL,
        signature TEXT NOT NULL,
        client_id TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        delivered_at INTEGER,
        read_at INTEGER
      );
      CREATE UNIQUE INDEX IF NOT EXISTS messages_sender_client
        ON messages (sender, client_id);
      CREATE INDEX IF NOT EXISTS messages_recipient_seq
        ON messages (recipient, seq);
    `);
  }

  async fetch(request) {
    try {
      const url = new URL(request.url);
      if (url.pathname === '/v1/accounts' && request.method === 'POST') return await this.register(request);
      if (url.pathname === '/v1/stream') return await this.stream(request);
      if (url.pathname === '/v1/config' && request.method === 'GET') {
        return json({ version: VERSION, maxMessageBytes: MAX_MESSAGE, features: ['e2ee-alpha', 'presence', 'connections', 'typing', 'receipts'] });
      }

      const account = await this.account(request);
      if (!account) return json({ error: 'unauthorised' }, 401);

      if (url.pathname === '/v1/me' && request.method === 'GET') {
        return json({ handle: account.handle, display: account.display, createdAt: account.created_at });
      }
      if (url.pathname === '/v1/account' && request.method === 'DELETE') {
        return this.deleteAccount(account);
      }
      if (url.pathname === '/v1/heartbeat' && request.method === 'POST') {
        return await this.heartbeat(account);
      }
      if (url.pathname === '/v1/directory' && request.method === 'GET') {
        return this.directory(account, url);
      }
      if (url.pathname.startsWith('/v1/directory/') && request.method === 'GET') {
        return this.directory(account, url);
      }
      if (url.pathname === '/v1/presence' && request.method === 'GET') {
        return this.presence(account, url);
      }
      if (url.pathname.startsWith('/v1/presence/') && request.method === 'GET') {
        return this.presence(account, url);
      }
      if (url.pathname === '/v1/presence' && request.method === 'PUT') {
        return await this.putPresence(account, request);
      }
      if (url.pathname === '/v1/connections' && request.method === 'GET') {
        return this.listConnections(account);
      }
      if (url.pathname === '/v1/connections' && request.method === 'POST') {
        return await this.requestConnection(account, request);
      }
      if (url.pathname === '/v1/connections/respond' && request.method === 'POST') {
        return await this.respondConnection(account, request);
      }
      if (url.pathname === '/v1/connections/block' && request.method === 'POST') {
        return await this.blockConnection(account, request);
      }
      if (url.pathname === '/v1/messages' && request.method === 'POST') {
        return await this.postMessage(account, request);
      }
      if (url.pathname === '/v1/messages' && request.method === 'GET') {
        return this.getMessages(account, url);
      }
      const ack = url.pathname.match(/^\/v1\/messages\/([^/]+)\/(delivered|read)$/);
      if (ack && request.method === 'POST') {
        return await this.ackMessage(account, decodeURIComponent(ack[1]), ack[2]);
      }
      if (url.pathname === '/v1/typing' && request.method === 'POST') {
        return await this.typing(account, request);
      }
      return json({ error: 'not_found' }, 404);
    } catch (error) {
      console.error('relay error', error?.stack || error);
      return json({ error: 'server_error' }, 500);
    }
  }

  // ------------------------------------------------------------------ auth and registration

  async register(request) {
    const body = await readJson(request);
    if (!body) return json({ error: 'invalid_json' }, 400);
    const handle = normalizeHandle(body.handle);
    if (!handle) return json({ error: 'invalid_handle' }, 400);
    const token = String(body.token || '');
    const signingKey = String(body.signingKey || '');
    const messagingKey = String(body.messagingKey || '');
    if (token.length < 32 || token.length > 256) return json({ error: 'invalid_token' }, 400);
    if (!looksLikePublicKey(signingKey) || !looksLikePublicKey(messagingKey)) {
      return json({ error: 'invalid_keys' }, 400);
    }
    const tokenHash = await sha256(token);
    const existing = this.row('SELECT * FROM accounts WHERE handle=?', handle);
    if (existing) {
      if (existing.token_hash !== tokenHash) return json({ error: 'handle_taken' }, 409);
      if (existing.signing_key !== signingKey || existing.messaging_key !== messagingKey) {
        return json({ error: 'keys_already_registered' }, 409);
      }
      return json({ handle, accountId: existing.account_id, createdAt: existing.created_at });
    }
    const accountId = crypto.randomUUID();
    const now = Date.now();
    const display = cleanDisplay(body.display, handle);
    this.sql.exec(
      'INSERT INTO accounts (handle, account_id, token_hash, signing_key, messaging_key, display, created_at, last_seen) VALUES (?,?,?,?,?,?,?,?)',
      handle, accountId, tokenHash, signingKey, messagingKey, display, now, now,
    );
    this.sql.exec(
      'INSERT INTO consent (handle, json, version, updated_at) VALUES (?,?,?,?)',
      handle, JSON.stringify(normalizeConsent({})), 1, now,
    );
    return json({ handle, accountId, createdAt: now });
  }

  async account(request) {
    const header = request.headers.get('Authorization') || '';
    if (!header.startsWith('Bearer ')) return null;
    const token = header.slice(7).trim();
    if (!token) return null;
    const hash = await sha256(token);
    return this.row('SELECT * FROM accounts WHERE token_hash=?', hash);
  }

  /**
   * Delete every row this relay holds for the account: keys, consent, connections, ciphertext and
   * the account row itself. Live sockets for the handle are closed. Nothing about the person
   * remains in the relay database after this returns.
   */
  deleteAccount(account) {
    const handle = account.handle;
    this.sql.exec('DELETE FROM messages WHERE sender=? OR recipient=?', handle, handle);
    this.sql.exec('DELETE FROM connections WHERE owner=? OR peer=?', handle, handle);
    this.sql.exec('DELETE FROM consent WHERE handle=?', handle);
    this.sql.exec('DELETE FROM accounts WHERE handle=?', handle);
    const sockets = this.sessions.get(handle);
    if (sockets) {
      for (const socket of sockets) {
        try {
          socket.send(JSON.stringify({ type: 'account_deleted', at: Date.now() }));
          socket.close(1000, 'account deleted');
        } catch {
          // the socket was already gone
        }
      }
      this.sessions.delete(handle);
    }
    return json({ ok: true, deleted: handle });
  }

  // ------------------------------------------------------------------ presence and connections

  async putPresence(account, request) {
    const body = await readJson(request);
    if (!body) return json({ error: 'invalid_json' }, 400);
    const consent = normalizeConsent(body.consent || body);
    const current = this.row('SELECT version FROM consent WHERE handle=?', account.handle);
    const version = Number(current?.version || 0) + 1;
    const now = Date.now();
    this.sql.exec(
      'INSERT INTO consent (handle, json, version, updated_at) VALUES (?,?,?,?) ON CONFLICT(handle) DO UPDATE SET json=excluded.json, version=excluded.version, updated_at=excluded.updated_at',
      account.handle, JSON.stringify(consent), version, now,
    );
    this.broadcastPresence(account.handle);
    return json({ version, consent });
  }

  presence(account, url) {
    const target = url.pathname.startsWith('/v1/presence/')
      ? normalizeHandle(decodeURIComponent(url.pathname.slice('/v1/presence/'.length)))
      : normalizeHandle(url.searchParams.get('handle') || account.handle);
    if (!target) return json({ error: 'invalid_handle' }, 400);
    const owner = this.row('SELECT * FROM accounts WHERE handle=?', target);
    if (!owner) return json({ error: 'not_found' }, 404);
    return json(this.projectionFor(owner, account.handle));
  }

  projectionFor(owner, viewer) {
    const consent = this.consentFor(owner.handle);
    const circle = this.circleFor(owner.handle);
    const now = Date.now();
    const online = this.isOnline(owner.handle);
    const projection = projectPresence({
      consent,
      viewer,
      circle,
      online,
      lastSeen: owner.last_seen,
      now,
    });
    const connection = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', owner.handle, viewer);
    return {
      handle: owner.handle,
      display: owner.display,
      state: connection?.state || 'NONE',
      projection,
      at: now,
    };
  }

  async heartbeat(account) {
    const now = Date.now();
    this.sql.exec('UPDATE accounts SET last_seen=? WHERE handle=?', now, account.handle);
    this.broadcastPresence(account.handle);
    return json({ ok: true, at: now });
  }

  listConnections(account) {
    const rows = this.sql.exec(
      'SELECT c.peer, c.state, c.purpose, c.updated_at, a.display, a.signing_key, a.messaging_key, a.last_seen FROM connections c JOIN accounts a ON a.handle=c.peer WHERE c.owner=? ORDER BY c.updated_at DESC',
      account.handle,
    ).toArray();
    return json({
      connections: rows.map((row) => ({
        handle: row.peer,
        display: row.display,
        state: row.state,
        purpose: row.purpose,
        updatedAt: row.updated_at,
        lastSeen: row.last_seen,
        signingKey: row.state === 'ACCEPTED' ? row.signing_key : null,
        messagingKey: row.state === 'ACCEPTED' ? row.messaging_key : null,
        online: this.isOnline(row.peer),
      })),
    });
  }

  async requestConnection(account, request) {
    const body = await readJson(request);
    const peer = normalizeHandle(body?.handle || body?.to);
    if (!peer) return json({ error: 'invalid_handle' }, 400);
    if (peer === account.handle) return json({ error: 'cannot_connect_to_self' }, 400);
    const target = this.row('SELECT handle FROM accounts WHERE handle=?', peer);
    if (!target) return json({ error: 'not_found' }, 404);
    const existing = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', account.handle, peer);
    if (existing && ['ACCEPTED', 'BLOCKED', 'DECLINED'].includes(existing.state)) {
      return json({ error: 'connection_not_open', state: existing.state }, 409);
    }
    const now = Date.now();
    const purpose = String(body?.purpose || 'conversation').slice(0, 80);
    this.sql.exec(
      'INSERT INTO connections (owner, peer, state, purpose, updated_at) VALUES (?,?,?,?,?) ON CONFLICT(owner,peer) DO UPDATE SET state=excluded.state, purpose=excluded.purpose, updated_at=excluded.updated_at',
      account.handle, peer, 'REQUESTED', purpose, now,
    );
    this.sql.exec(
      'INSERT INTO connections (owner, peer, state, purpose, updated_at) VALUES (?,?,?,?,?) ON CONFLICT(owner,peer) DO UPDATE SET state=excluded.state, purpose=excluded.purpose, updated_at=excluded.updated_at',
      peer, account.handle, 'PENDING', purpose, now,
    );
    this.sendTo(peer, { type: 'connection', handle: account.handle, display: account.display, state: 'PENDING', at: now });
    return json({ ok: true, state: 'REQUESTED' });
  }

  async respondConnection(account, request) {
    const body = await readJson(request);
    const peer = normalizeHandle(body?.handle || body?.from);
    if (!peer) return json({ error: 'invalid_handle' }, 400);
    const incoming = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', account.handle, peer);
    if (!incoming || incoming.state !== 'PENDING') return json({ error: 'no_pending_request' }, 409);
    const state = body?.accept === true ? 'ACCEPTED' : 'DECLINED';
    const now = Date.now();
    this.sql.exec('UPDATE connections SET state=?, updated_at=? WHERE owner=? AND peer=?', state, now, account.handle, peer);
    this.sql.exec('UPDATE connections SET state=?, updated_at=? WHERE owner=? AND peer=?', state, now, peer, account.handle);
    this.sendTo(peer, { type: 'connection', handle: account.handle, display: account.display, state, at: now });
    this.broadcastPresence(account.handle);
    this.broadcastPresence(peer);
    return json({ ok: true, state });
  }

  async blockConnection(account, request) {
    const body = await readJson(request);
    const peer = normalizeHandle(body?.handle);
    if (!peer) return json({ error: 'invalid_handle' }, 400);
    const now = Date.now();
    this.sql.exec(
      'INSERT INTO connections (owner, peer, state, purpose, updated_at) VALUES (?,?,?,?,?) ON CONFLICT(owner,peer) DO UPDATE SET state=excluded.state, updated_at=excluded.updated_at',
      account.handle, peer, 'BLOCKED', null, now,
    );
    this.sql.exec(
      'INSERT INTO connections (owner, peer, state, purpose, updated_at) VALUES (?,?,?,?,?) ON CONFLICT(owner,peer) DO UPDATE SET state=excluded.state, updated_at=excluded.updated_at',
      peer, account.handle, 'BLOCKED', null, now,
    );
    return json({ ok: true, state: 'BLOCKED' });
  }

  // ------------------------------------------------------------------ messages

  async postMessage(account, request) {
    if (!this.allowed(account.handle, 'message', 90, 60_000)) {
      return json({ error: 'rate_limited' }, 429);
    }
    const body = await readJson(request);
    if (!body) return json({ error: 'invalid_json' }, 400);
    const recipient = normalizeHandle(body.to || body.handle);
    if (!recipient) return json({ error: 'invalid_handle' }, 400);
    if (recipient === account.handle) return json({ error: 'cannot_message_self' }, 400);
    const state = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', account.handle, recipient);
    if (state?.state !== 'ACCEPTED') return json({ error: 'not_connected', state: state?.state || 'NONE' }, 403);

    const clientId = String(body.clientId || '').slice(0, 80);
    if (!clientId) return json({ error: 'missing_client_id' }, 400);
    const existing = this.row('SELECT * FROM messages WHERE sender=? AND client_id=?', account.handle, clientId);
    if (existing) return json(this.wireMessage(existing), 200);

    const envelope = {
      ephemeralPublicKey: String(body.ephemeralPublicKey || ''),
      nonce: String(body.nonce || ''),
      ciphertext: String(body.ciphertext || ''),
    };
    if (
      !envelope.ephemeralPublicKey || !envelope.nonce || !envelope.ciphertext ||
      envelope.ciphertext.length > MAX_MESSAGE ||
      Math.max(envelope.ephemeralPublicKey.length, envelope.nonce.length, envelope.ciphertext.length) > MAX_MESSAGE
    ) {
      return json({ error: 'invalid_envelope' }, 400);
    }
    const signature = String(body.signature || '');
    if (!signature || signature.length > 512) return json({ error: 'invalid_signature' }, 400);

    const conversation = conversationFor(account.handle, recipient);
    const expectedAad = `${conversation}|${account.handle}|${recipient}|${clientId}`;
    if (String(body.aad || '') !== expectedAad) return json({ error: 'invalid_aad' }, 400);

    const seq = Number(this.row('SELECT COALESCE(MAX(seq),0) AS seq FROM messages')?.seq || 0) + 1;
    const id = crypto.randomUUID();
    const now = Date.now();
    this.sql.exec(
      'INSERT INTO messages (id, seq, sender, recipient, conversation, ephemeral_key, nonce, ciphertext, aad, signature, client_id, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
      id, seq, account.handle, recipient, conversation, envelope.ephemeralPublicKey, envelope.nonce,
      envelope.ciphertext, expectedAad, signature, clientId, now,
    );
    const message = {
      id, seq, from: account.handle, to: recipient, conversation, ...envelope, signature, aad: expectedAad, createdAt: now,
    };
    this.sendTo(recipient, { type: 'message', message });
    this.sendTo(account.handle, { type: 'sent', id, seq, clientId, at: now });
    return json(message, 201);
  }

  getMessages(account, url) {
    this.sql.exec('DELETE FROM messages WHERE created_at < ?', Date.now() - MESSAGE_TTL_MS);
    const since = Math.max(0, Number(url.searchParams.get('since') || 0));
    const limit = Math.min(200, Math.max(1, Number(url.searchParams.get('limit') || 100)));
    const rows = this.sql.exec(
      'SELECT * FROM messages WHERE recipient=? AND seq>? ORDER BY seq ASC LIMIT ?',
      account.handle, since, limit,
    ).toArray();
    return json({
      messages: rows.map((row) => this.wireMessage(row)),
      cursor: rows.length ? rows[rows.length - 1].seq : since,
    });
  }

  async ackMessage(account, id, kind) {
    const row = this.row('SELECT * FROM messages WHERE id=? AND recipient=?', id, account.handle);
    if (!row) return json({ error: 'not_found' }, 404);
    const now = Date.now();
    if (kind === 'delivered' && !row.delivered_at) {
      this.sql.exec('UPDATE messages SET delivered_at=? WHERE id=?', now, id);
      this.sendTo(row.sender, { type: 'delivered', id, at: now });
    }
    if (kind === 'read' && !row.read_at) {
      this.sql.exec('UPDATE messages SET read_at=? WHERE id=?', now, id);
      const consent = this.consentFor(account.handle);
      const circle = this.circleFor(account.handle);
      if (canShareSignal(consent, 'read_receipts', row.sender, circle)) {
        this.sendTo(row.sender, { type: 'read', id, at: now });
      }
    }
    return json({ ok: true });
  }

  async typing(account, request) {
    const body = await readJson(request);
    const recipient = normalizeHandle(body?.to || body?.handle);
    if (!recipient) return json({ error: 'invalid_handle' }, 400);
    const state = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', account.handle, recipient);
    if (state?.state !== 'ACCEPTED') return json({ error: 'not_connected' }, 403);
    const consent = this.consentFor(account.handle);
    const circle = this.circleFor(account.handle);
    if (canShareSignal(consent, 'typing', recipient, circle)) {
      this.sendTo(recipient, { type: 'typing', handle: account.handle, typing: body?.typing !== false, at: Date.now() });
    }
    return json({ ok: true });
  }

  wireMessage(row) {
    return {
      id: row.id,
      seq: row.seq,
      from: row.sender,
      to: row.recipient,
      conversation: row.conversation,
      ephemeralPublicKey: row.ephemeral_key,
      nonce: row.nonce,
      ciphertext: row.ciphertext,
      signature: row.signature,
      aad: row.aad,
      createdAt: row.created_at,
      deliveredAt: row.delivered_at,
      readAt: row.read_at,
    };
  }

  directory(account, url) {
    const target = url.pathname.startsWith('/v1/directory/')
      ? normalizeHandle(decodeURIComponent(url.pathname.slice('/v1/directory/'.length)))
      : normalizeHandle(url.searchParams.get('handle'));
    if (!target) return json({ error: 'invalid_handle' }, 400);
    const row = this.row('SELECT handle, display, signing_key, messaging_key, created_at FROM accounts WHERE handle=?', target);
    if (!row) return json({ error: 'not_found' }, 404);
    const connection = this.row('SELECT state FROM connections WHERE owner=? AND peer=?', account.handle, target);
    return json({
      handle: row.handle,
      display: row.display,
      signingKey: row.signing_key,
      messagingKey: row.messaging_key,
      createdAt: row.created_at,
      state: connection?.state || 'NONE',
    });
  }

  // ------------------------------------------------------------------ realtime stream

  async stream(request) {
    if ((request.headers.get('Upgrade') || '').toLowerCase() !== 'websocket') {
      return json({ error: 'websocket_required' }, 426);
    }
    const account = await this.account(request);
    if (!account) return json({ error: 'unauthorised' }, 401);
    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    server.accept();
    const handle = account.handle;
    if (!this.sessions.has(handle)) this.sessions.set(handle, new Set());
    this.sessions.get(handle).add(server);
    this.sql.exec('UPDATE accounts SET last_seen=? WHERE handle=?', Date.now(), handle);

    server.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(String(event.data || '{}'));
        if (data.type === 'ping') {
          this.sql.exec('UPDATE accounts SET last_seen=? WHERE handle=?', Date.now(), handle);
          server.send(JSON.stringify({ type: 'pong', at: Date.now() }));
        }
      } catch {
        // A malformed client frame is ignored; it never reaches another person.
      }
    });
    const close = () => {
      const set = this.sessions.get(handle);
      if (set) {
        set.delete(server);
        if (!set.size) this.sessions.delete(handle);
      }
      this.broadcastPresence(handle);
    };
    server.addEventListener('close', close);
    server.addEventListener('error', close);

    server.send(JSON.stringify({ type: 'ready', handle, at: Date.now(), version: VERSION }));
    for (const peer of this.circleFor(handle)) this.sendPresenceTo(handle, peer);
    this.broadcastPresence(handle);
    return new Response(null, { status: 101, webSocket: client });
  }

  // ------------------------------------------------------------------ helpers

  consentFor(handle) {
    const row = this.row('SELECT json FROM consent WHERE handle=?', handle);
    try {
      return normalizeConsent(JSON.parse(row?.json || '{}'));
    } catch {
      return normalizeConsent({});
    }
  }

  circleFor(handle) {
    const rows = this.sql.exec(
      "SELECT peer FROM connections WHERE owner=? AND state='ACCEPTED'",
      handle,
    ).toArray();
    return new Set(rows.map((row) => row.peer));
  }

  isOnline(handle) {
    return (this.sessions.get(handle)?.size || 0) > 0;
  }

  broadcastPresence(owner) {
    for (const peer of this.circleFor(owner)) this.sendPresenceTo(owner, peer);
  }

  sendPresenceTo(owner, viewer) {
    if (!this.isOnline(viewer)) return;
    const row = this.row('SELECT * FROM accounts WHERE handle=?', owner);
    if (!row) return;
    this.sendTo(viewer, {
      type: 'presence',
      ...this.projectionFor(row, viewer),
    });
  }

  sendTo(handle, payload) {
    const set = this.sessions.get(handle);
    if (!set) return;
    const text = JSON.stringify(payload);
    for (const socket of set) {
      try {
        socket.send(text);
      } catch {
        set.delete(socket);
      }
    }
  }

  allowed(handle, bucket, limit, windowMs) {
    const key = `${handle}:${bucket}`;
    const now = Date.now();
    const hits = (this.rate.get(key) || []).filter((time) => now - time < windowMs);
    if (hits.length >= limit) {
      this.rate.set(key, hits);
      return false;
    }
    hits.push(now);
    this.rate.set(key, hits);
    return true;
  }

  row(query, ...values) {
    return this.sql.exec(query, ...values).toArray()[0];
  }
}

function normalizeHandle(value) {
  const handle = String(value || '').trim().toLowerCase().replace(/^@/, '');
  if (!HANDLE.test(handle) || RESERVED.has(handle)) return null;
  return handle;
}

function cleanDisplay(value, fallback) {
  const display = String(value || '').replace(/[\u0000-\u001f\u007f]/g, ' ').trim();
  return (display || fallback).slice(0, 48);
}

function looksLikePublicKey(value) {
  return typeof value === 'string' && value.length >= 80 && value.length <= 512;
}

function conversationFor(a, b) {
  return [a, b].sort().join('~');
}

async function readJson(request) {
  const text = await request.text();
  if (!text || text.length > MAX_BODY) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function sha256(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest('SHA-256', bytes);
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('');
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': 'no-store',
      'access-control-allow-origin': '*',
    },
  });
}
