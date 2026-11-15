/** Client for the optional Union relay (Cloudflare Worker + D1).
 *  Every write is signed by the identity key. The relay never sees plaintext
 *  messages: it receives sealed envelopes addressed to a public identity. */

import { sign } from './crypto';
import type { UnionCard } from './crypto';
import { sha256Hex } from './core';
import type { Identity, Keyring, UnionLink, Community, Post } from './types';

export interface RelayAuth {
  baseUrl: string;
  identity: Identity;
  keys: Keyring;
}

export interface RelayEnvelope {
  id: string;
  from: string;
  to: string;
  payload: string;
  createdAt: string;
}

export interface RelayLink {
  id: string;
  kind: string;
  title: string;
  subtitle: string;
  payload: unknown;
  permissions: string[];
  createdAt: string;
  expiresAt?: string;
  revokedAt?: string;
}

export interface SpacePost {
  id: string;
  communityId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
}

function normalise(url: string): string {
  return url.trim().replace(/\/+$/, '');
}

async function signedFetch(
  auth: RelayAuth,
  path: string,
  method: 'GET' | 'POST',
  body?: unknown
): Promise<Response> {
  const base = normalise(auth.baseUrl);
  if (!/^https?:\/\//.test(base)) throw new Error('Relay URL must start with https://');
  const url = `${base}${path}`;
  const text = body === undefined ? '' : JSON.stringify(body);
  const ts = Math.floor(Date.now() / 1000).toString();
  const digest = await sha256Hex(text);
  const canonical = `${method}\n${path}\n${ts}\n${digest}`;
  const signature = sign(auth.keys, canonical);
  const response = await fetch(url, {
    method,
    headers: {
      'content-type': 'application/json',
      'x-union-id': auth.identity.id,
      'x-union-ts': ts,
      'x-union-sig': signature
    },
    body: body === undefined ? undefined : text
  });
  return response;
}

async function expectJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  let data: unknown = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* ignore */ }
  if (!response.ok) {
    const message =
      data && typeof data === 'object' && 'error' in data
        ? String((data as { error: unknown }).error)
        : `Relay error ${response.status}`;
    throw new Error(message);
  }
  return data as T;
}

export async function health(baseUrl: string): Promise<{ ok: boolean; service?: string; version?: number }> {
  const response = await fetch(`${normalise(baseUrl)}/health`, { cache: 'no-store' });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return (await response.json()) as { ok: boolean; service?: string; version?: number };
}

export async function register(auth: RelayAuth, card: UnionCard): Promise<void> {
  const response = await signedFetch(auth, '/v1/identities', 'POST', { card });
  await expectJson(response);
}

export async function lookup(baseUrl: string, handle: string): Promise<UnionCard | null> {
  const response = await fetch(`${normalise(baseUrl)}/v1/identities/${encodeURIComponent(handle)}`, {
    cache: 'no-store'
  });
  if (response.status === 404) return null;
  const data = await expectJson<{ card: UnionCard }>(response);
  return data.card ?? null;
}

export async function sendEnvelope(
  auth: RelayAuth,
  to: string,
  payload: string,
  id: string
): Promise<{ id: string; createdAt: string }> {
  const response = await signedFetch(auth, '/v1/messages', 'POST', { id, to, payload });
  return expectJson(response);
}

export async function fetchEnvelopes(
  auth: RelayAuth,
  since: string
): Promise<RelayEnvelope[]> {
  const path = `/v1/messages?since=${encodeURIComponent(since)}`;
  const response = await signedFetch(auth, path, 'GET');
  const data = await expectJson<{ messages: RelayEnvelope[] }>(response);
  return data.messages ?? [];
}

export async function ackEnvelopes(auth: RelayAuth, ids: string[]): Promise<void> {
  if (!ids.length) return;
  const response = await signedFetch(auth, '/v1/messages/ack', 'POST', { ids });
  await expectJson(response);
}

export async function publishLink(auth: RelayAuth, link: UnionLink): Promise<{ id: string }> {
  const response = await signedFetch(auth, '/v1/links', 'POST', {
    kind: link.kind,
    title: link.title,
    subtitle: link.subtitle,
    payload: link.payload,
    permissions: link.permissions,
    expiresAt: link.expiresAt,
    id: link.id
  });
  return expectJson(response);
}

export async function resolveLink(baseUrl: string, id: string): Promise<RelayLink> {
  const response = await fetch(`${normalise(baseUrl)}/l/${encodeURIComponent(id)}`, { cache: 'no-store' });
  return expectJson<RelayLink>(response);
}

export async function revokeLink(auth: RelayAuth, id: string): Promise<void> {
  const response = await signedFetch(auth, `/v1/links/${encodeURIComponent(id)}/revoke`, 'POST', {});
  await expectJson(response);
}

export async function registerSpace(auth: RelayAuth, community: Community): Promise<{ id: string }> {
  const response = await signedFetch(auth, '/v1/spaces', 'POST', {
    id: community.id,
    name: community.name,
    about: community.about,
    color: community.color,
    visibility: community.visibility
  });
  // A space id owned by someone else still exists, which is all a poster
  // needs. Ownership only governs later edits.
  if (response.status === 409) return { id: community.id };
  return expectJson(response);
}

export async function fetchSpace(
  baseUrl: string,
  id: string
): Promise<{ id: string; name: string; about: string; color: string; createdBy: string } | null> {
  const response = await fetch(`${normalise(baseUrl)}/v1/spaces/${encodeURIComponent(id)}`, {
    cache: 'no-store'
  });
  if (response.status === 404) return null;
  return expectJson(response);
}

export async function publishPost(auth: RelayAuth, post: Post): Promise<void> {
  const response = await signedFetch(auth, `/v1/spaces/${encodeURIComponent(post.communityId)}/posts`, 'POST', {
    id: post.id,
    body: post.body,
    authorName: post.authorName,
    createdAt: post.createdAt
  });
  await expectJson(response);
}

export async function fetchPosts(
  baseUrl: string,
  communityId: string,
  since = '1970-01-01T00:00:00.000Z'
): Promise<SpacePost[]> {
  const response = await fetch(
    `${normalise(baseUrl)}/v1/spaces/${encodeURIComponent(communityId)}/posts?since=${encodeURIComponent(since)}`,
    { cache: 'no-store' }
  );
  if (response.status === 404) return [];
  const data = await expectJson<{ posts: SpacePost[] }>(response);
  return data.posts ?? [];
}
