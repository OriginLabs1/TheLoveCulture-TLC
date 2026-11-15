import { BADGES, DOMAINS } from './badges';

export interface Capability {
  id: string;
  name: string;
  blurb: string;
  icon: string;
  route: string;
}

export const CAPABILITIES: Capability[] = [
  { id: 'communication', name: 'Communication', blurb: 'Identity, consent, sealed messages, notes.', icon: 'message', route: '/messages' },
  { id: 'community', name: 'Community', blurb: 'Shared rooms with a stated purpose.', icon: 'people', route: '/communities' },
  { id: 'learning', name: 'Learning', blurb: 'Practical courses, lessons and practice.', icon: 'book', route: '/learning' },
  { id: 'experiences', name: 'Experiences', blurb: 'Events, assemblies and field practice.', icon: 'compass', route: '/experiences' },
  { id: 'presence', name: 'Digital Presence', blurb: 'A page and links that respect visitors.', icon: 'layout', route: '/presence' },
  { id: 'intelligence', name: 'Union AI', blurb: 'Context, memory, projects, approval.', icon: 'spark', route: '/ai' },
  { id: 'economy', name: 'Economic Coordination', blurb: 'Plans, Digital Giving, transparent records.', icon: 'coins', route: '/economy' },
  { id: 'operations', name: 'Operations', blurb: 'Relay, provider, storage, audit, export.', icon: 'sliders', route: '/operations' },
  { id: 'exploration', name: 'Exploration & Progress', blurb: 'Life, Reality, Discovery, progress.', icon: 'star', route: '/explore' },
  { id: 'stewardship', name: 'Mentorship & Stewardship', blurb: 'Guidance that leaves someone stronger.', icon: 'handshake', route: '/mentorship' }
];

export function capability(id: string): Capability {
  return CAPABILITIES.find((c) => c.id === id) ?? CAPABILITIES[0];
}

export const DOMAIN_NAMES = new Map(DOMAINS.map((d) => [d.id, d.name]));

export const TOTAL_BADGES = BADGES.length;
