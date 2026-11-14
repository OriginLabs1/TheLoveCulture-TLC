/** Small formatting helpers that are not worth pulling into util.ts. */

export function plural(n: number, one: string, many = `${one}s`): string {
  return `${n} ${n === 1 ? one : many}`;
}

export function domainLabel(domain: string): string {
  const labels: Record<string, string> = {
    communication: 'Communication',
    community: 'Community',
    learning: 'Learning',
    experiences: 'Experiences',
    presence: 'Digital Presence',
    intelligence: 'Union AI',
    economy: 'Economic Coordination',
    operations: 'Operations',
    exploration: 'Exploration & Progress',
    stewardship: 'Mentorship & Stewardship'
  };
  return labels[domain] ?? domain;
}

export function listJoin(items: string[], fallback = ''): string {
  if (!items.length) return fallback;
  if (items.length === 1) return items[0];
  if (items.length === 2) return `${items[0]} and ${items[1]}`;
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}

export function truncate(text: string, n = 120): string {
  const clean = text.replace(/\s+/g, ' ').trim();
  return clean.length > n ? `${clean.slice(0, n - 1)}…` : clean;
}
