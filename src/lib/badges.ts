import type { BadgeDef, UnionRecord } from './types';

const me = (r: UnionRecord, id: string): boolean =>
  ('identityId' in r && r.identityId === id) ||
  ('createdBy' in r && r.createdBy === id) ||
  ('ownerId' in r && r.ownerId === id) ||
  ('from' in r && r.from === id);

export const BADGES: BadgeDef[] = [
  {
    id: 'badge_first_light',
    name: 'First Light',
    description: 'Created a portable Union identity on your own device.',
    domain: 'exploration',
    icon: 'spark',
    check: (r, id) => r.some((x) => x.type === 'identity' && x.id === id)
  },
  {
    id: 'badge_consent_keeper',
    name: 'Consent Keeper',
    description: 'Accepted a connection with explicit, revocable scopes.',
    domain: 'communication',
    icon: 'shield',
    check: (r, id) =>
      r.some(
        (x) =>
          x.type === 'connection' &&
          x.status === 'accepted' &&
          x.scopes.length > 0 &&
          (x.aId === id || x.bId === id)
      )
  },
  {
    id: 'badge_first_signal',
    name: 'First Signal',
    description: 'Wrote and delivered a message from your own device.',
    domain: 'communication',
    icon: 'send',
    check: (r, id) =>
      r.some((x) => x.type === 'message' && x.from === id && x.status !== 'failed')
  },
  {
    id: 'badge_relay_reach',
    name: 'Global Reach',
    description: 'Connected a relay and exchanged an end-to-end sealed envelope.',
    domain: 'communication',
    icon: 'globe',
    check: (r, id) => r.some((x) => x.type === 'message' && x.from === id && x.status === 'sent')
  },
  {
    id: 'badge_commoner',
    name: 'Commoner',
    description: 'Joined a community and took part in its shared room.',
    domain: 'community',
    icon: 'people',
    check: (r, id) =>
      r.some((x) => x.type === 'membership' && x.identityId === id) &&
      r.some((x) => x.type === 'post' && x.authorId === id)
  },
  {
    id: 'badge_host',
    name: 'Room Maker',
    description: 'Created a community with a clear purpose.',
    domain: 'community',
    icon: 'flag',
    check: (r, id) => r.some((x) => x.type === 'community' && x.createdBy === id)
  },
  {
    id: 'badge_voice',
    name: 'A Useful Voice',
    description: 'Posted something practical in a community.',
    domain: 'community',
    icon: 'message',
    check: (r, id) => r.some((x) => x.type === 'post' && x.authorId === id)
  },
  {
    id: 'badge_learner',
    name: 'Practitioner',
    description: 'Finished a lesson and answered its practice question.',
    domain: 'learning',
    icon: 'book',
    check: (r, id) => r.some((x) => x.type === 'enrollment' && x.identityId === id && x.done.length > 0)
  },
  {
    id: 'badge_graduate',
    name: 'Course Complete',
    description: 'Completed an entire Union course.',
    domain: 'learning',
    icon: 'cap',
    check: (r, id) => r.some((x) => x.type === 'enrollment' && x.identityId === id && Boolean(x.completedAt))
  },
  {
    id: 'badge_explorer',
    name: 'Explorer',
    description: 'Took part in a real experience or event.',
    domain: 'experiences',
    icon: 'compass',
    check: (r, id) => r.some((x) => x.type === 'rsvp' && x.identityId === id && x.status !== 'declined')
  },
  {
    id: 'badge_presence',
    name: 'A Place of Your Own',
    description: 'Published a digital presence page.',
    domain: 'presence',
    icon: 'layout',
    check: (r, id) => r.some((x) => x.type === 'page' && x.ownerId === id && x.published)
  },
  {
    id: 'badge_link_smith',
    name: 'Link Smith',
    description: 'Created a Union link with declared permissions.',
    domain: 'presence',
    icon: 'link',
    check: (r, id) => r.some((x) => x.type === 'link' && x.createdBy === id)
  },
  {
    id: 'badge_intelligence',
    name: 'Authorised Intelligence',
    description: 'Approved a Union AI action and kept it within consent.',
    domain: 'intelligence',
    icon: 'spark',
    check: (r, id) =>
      r.some(
        (x) =>
          x.type === 'aiMessage' &&
          x.action &&
          (x.action.state === 'approved' || x.action.state === 'done') &&
          x.role === 'assistant'
      )
  },
  {
    id: 'badge_giver',
    name: 'Joyful Giver',
    description: 'Recorded a transparent gift, plan or token entry.',
    domain: 'economy',
    icon: 'heart',
    check: (r, id) =>
      r.some(
        (x) =>
          x.type === 'ledger' &&
          (x.kind === 'gift' || x.kind === 'joy' || x.kind === 'activation' || x.kind === 'reactivation') &&
          x.from === id
      )
  },
  {
    id: 'badge_integrity',
    name: 'Chain of Trust',
    description: 'Built a verifiable ledger chain with three or more entries.',
    domain: 'economy',
    icon: 'check',
    check: (r, id) => r.filter((x) => x.type === 'ledger' && x.from === id).length >= 3
  },
  {
    id: 'badge_operator',
    name: 'Operator',
    description: 'Configured a relay, provider or documented integration.',
    domain: 'operations',
    icon: 'sliders',
    check: (r) =>
      r.some((x) => x.type === 'setting' && (x.id === 'relay.url' || x.id === 'ai.baseUrl') && x.value)
  },
  {
    id: 'badge_steward',
    name: 'Steward',
    description: 'Opened a voluntary mentorship or stewardship relationship.',
    domain: 'stewardship',
    icon: 'handshake',
    check: (r, id) =>
      r.some(
        (x) =>
          x.type === 'mentorship' &&
          (x.mentorId === id || x.menteeId === id) &&
          (x.status === 'active' || x.status === 'requested')
      )
  },
  {
    id: 'badge_guidance',
    name: 'Guidance Kept',
    description: 'Recorded a stewardship note that serves the other person.',
    domain: 'stewardship',
    icon: 'notes',
    check: (r, id) =>
      r.some(
        (x) =>
          x.type === 'mentorship' &&
          (x.mentorId === id || x.menteeId === id) &&
          x.notes.length > 0
      )
  }
];

export const DOMAINS = [
  { id: 'communication', name: 'Communication', icon: 'send', blurb: 'Identity, consent, messages and global reach.' },
  { id: 'community', name: 'Community', icon: 'people', blurb: 'Relationships, memberships, rooms and shared work.' },
  { id: 'learning', name: 'Learning', icon: 'book', blurb: 'Courses, practical lessons and demonstrated progress.' },
  { id: 'experiences', name: 'Experiences', icon: 'compass', blurb: 'Events, programmes, practice and exploration.' },
  { id: 'presence', name: 'Digital Presence', icon: 'layout', blurb: 'Pages, publications and links that remain useful.' },
  { id: 'intelligence', name: 'Union AI', icon: 'spark', blurb: 'Context, memory, projects and authorised actions.' },
  { id: 'economy', name: 'Economic Coordination', icon: 'coins', blurb: 'Plans, Digital Giving and transparent records.' },
  { id: 'operations', name: 'Operations', icon: 'sliders', blurb: 'Integrations, APIs, relay and extensibility.' },
  { id: 'exploration', name: 'Exploration & Progress', icon: 'compass', blurb: 'Life, Reality, Discovery, progress and Explorer Badges.' },
  { id: 'stewardship', name: 'Mentorship & Stewardship', icon: 'handshake', blurb: 'Guidance, discipleship and voluntary development.' }
] as const;

export function earnedBadgeIds(records: UnionRecord[], identityId: string): string[] {
  return BADGES.filter((b) => {
    try {
      return b.check(records, identityId);
    } catch {
      return false;
    }
  }).map((b) => b.id);
}

export function domainProgress(records: UnionRecord[], identityId: string, domain: string): { done: number; total: number } {
  const list = BADGES.filter((b) => b.domain === domain);
  const earned = earnedBadgeIds(records, identityId);
  return { done: list.filter((b) => earned.includes(b.id)).length, total: list.length };
}
