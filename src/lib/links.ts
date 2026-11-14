/** Union link payloads. A link is a first-class object: kind, creator,
 *  permissions, expiry and revocation. Self-contained links carry a compact
 *  token so they remain useful outside the app. */

import { decodeToken } from './crypto';
import type { LinkKind, Page, UnionLink } from './types';

export interface PersonToken {
  v: 1;
  kind: 'identity';
  card: unknown;
}

export interface PageToken {
  v: 1;
  kind: 'page';
  page: {
    id: string;
    title: string;
    tagline: string;
    about: string;
    sections: Page['sections'];
    links: Page['links'];
    theme: Page['theme'];
    author: string;
    handle: string;
    updatedAt: string;
  };
}

export interface CommunityToken {
  v: 1;
  kind: 'community';
  community: {
    id: string;
    name: string;
    about: string;
    visibility: string;
    joinPolicy: string;
    color: string;
    createdBy: string;
  };
}

export interface EventToken {
  v: 1;
  kind: 'event';
  event: {
    id: string;
    title: string;
    about: string;
    start: string;
    end: string;
    mode: string;
    place: string;
    hostName: string;
  };
}

export interface CourseToken {
  v: 1;
  kind: 'course';
  course: { slug: string; title: string; subtitle: string };
}

export interface LinkToken {
  v: 1;
  kind: 'link';
  link: UnionLink;
}

export interface GivingToken {
  v: 1;
  kind: 'giving';
  title: string;
  memo: string;
  suggestedUsd: number;
}

export interface InviteToken {
  v: 1;
  kind: 'invite';
  handle: string;
  name: string;
  note: string;
}

export type AnyToken =
  | PersonToken
  | PageToken
  | CommunityToken
  | EventToken
  | CourseToken
  | LinkToken
  | GivingToken
  | InviteToken;

export function readToken<T extends AnyToken = AnyToken>(token: string): T | null {
  const data = decodeToken<T>(token);
  if (!data || typeof data !== 'object') return null;
  if (!('v' in data) || (data as { v: unknown }).v !== 1) return null;
  if (!('kind' in data)) return null;
  return data;
}

export const LINK_KIND_META: Record<LinkKind, { label: string; icon: string; blurb: string }> = {
  identity: { label: 'Identity card', icon: 'fingerprint', blurb: 'A signed public card. Adds a verifiable person.' },
  invite: { label: 'Invitation', icon: 'mail', blurb: 'A courteous invitation to connect.' },
  connection: { label: 'Connection', icon: 'handshake', blurb: 'A request to open a scoped connection.' },
  message: { label: 'Message', icon: 'send', blurb: 'A sealed note addressed to one identity.' },
  community: { label: 'Community', icon: 'people', blurb: 'A shared room with a stated purpose.' },
  course: { label: 'Course', icon: 'book', blurb: 'A practical course with lessons.' },
  lesson: { label: 'Lesson', icon: 'book', blurb: 'One lesson and its practice.' },
  event: { label: 'Experience', icon: 'compass', blurb: 'An event or practice session.' },
  page: { label: 'Presence page', icon: 'layout', blurb: 'A small, read-only destination.' },
  giving: { label: 'Giving', icon: 'heart', blurb: 'A transparent Digital Giving record.' },
  agent: { label: 'AI action', icon: 'spark', blurb: 'A proposed action that needs approval.' },
  external: { label: 'External link', icon: 'external', blurb: 'A link outside Union, opened in your browser.' }
};
