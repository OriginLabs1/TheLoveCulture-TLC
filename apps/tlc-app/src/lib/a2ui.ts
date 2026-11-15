/** A2UI for Union — a small, safe, declarative interface language.
 *
 *  An agent may compose a surface from the catalog below, never from HTML or
 *  JavaScript. Every node is validated before it reaches the renderer, and the
 *  only effects a surface can request are the allowlisted actions here. The same
 *  catalog maps cleanly onto Jetpack Compose + Material 3 later: each node type
 *  is one composable, and each action is one intent handed back to the host.
 */

export const A2UI_VERSION = 'a2ui/1' as const;

export const A2UI_CATALOG = {
  version: A2UI_VERSION,
  components: [
    'heading',
    'text',
    'stats',
    'list',
    'actions',
    'form',
    'note',
    'progress',
    'divider',
    'card'
  ],
  actions: ['navigate', 'ask', 'copy', 'dismiss'],
  limits: { nodes: 80, depth: 4, string: 600, list: 12, fields: 6 }
} as const;

export type A2UIAction =
  | { type: 'navigate'; to: string }
  | { type: 'ask'; text: string }
  | { type: 'copy'; text: string }
  | { type: 'dismiss' };

export interface A2UIStat {
  label: string;
  value: string;
  hint?: string;
}

export interface A2UIListItem {
  title: string;
  text?: string;
  value?: string;
  action?: A2UIAction;
}

export interface A2UIButton {
  label: string;
  action: A2UIAction;
  variant?: 'primary' | 'soft' | 'ghost';
  icon?: string;
}

export interface A2UIField {
  name: string;
  label: string;
  placeholder?: string;
  type?: 'text' | 'number' | 'textarea';
  value?: string;
}

export type A2UINode =
  | { type: 'heading'; title: string; subtitle?: string }
  | { type: 'text'; text: string; style?: 'body' | 'display' | 'caption' | 'label' }
  | { type: 'stats'; items: A2UIStat[] }
  | { type: 'list'; title?: string; items: A2UIListItem[] }
  | { type: 'actions'; items: A2UIButton[] }
  | { type: 'form'; title?: string; fields: A2UIField[]; submit: A2UIButton }
  | { type: 'note'; text: string }
  | { type: 'progress'; value: number; label?: string }
  | { type: 'divider' }
  | { type: 'card'; title: string; text?: string; action?: A2UIAction };

export interface A2UISurface {
  version: typeof A2UI_VERSION;
  id: string;
  nodes: A2UINode[];
}

export interface ValidationResult {
  ok: boolean;
  surface?: A2UISurface;
  errors: string[];
}

const MAX = A2UI_CATALOG.limits;
const actionTypes = new Set<string>(A2UI_CATALOG.actions);

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function text(value: unknown, max: number = MAX.string): string | null {
  if (typeof value !== 'string') return null;
  const clean = value.replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, '').trim();
  if (!clean || clean.length > max) return null;
  return clean;
}

function safePath(value: unknown): string | null {
  const path = text(value, 140);
  if (!path) return null;
  if (!path.startsWith('/') || path.startsWith('//') || path.includes('..')) return null;
  return path;
}

function iconName(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined;
  return /^[a-z0-9-]{1,24}$/.test(value) ? value : undefined;
}

function action(value: unknown): A2UIAction | null {
  if (!isRecord(value) || typeof value.type !== 'string' || !actionTypes.has(value.type)) return null;
  if (value.type === 'navigate') {
    const to = safePath(value.to);
    return to ? { type: 'navigate', to } : null;
  }
  if (value.type === 'ask') {
    const body = text(value.text, 320);
    return body ? { type: 'ask', text: body } : null;
  }
  if (value.type === 'copy') {
    const body = text(value.text, 600);
    return body ? { type: 'copy', text: body } : null;
  }
  return { type: 'dismiss' };
}

function stats(value: unknown): A2UIStat[] | null {
  if (!Array.isArray(value) || !value.length || value.length > 4) return null;
  const out: A2UIStat[] = [];
  for (const item of value) {
    if (!isRecord(item)) return null;
    const label = text(item.label, 40);
    const val = text(item.value, 24);
    if (!label || !val) return null;
    const hint = text(item.hint, 80) ?? undefined;
    out.push({ label, value: val, ...(hint ? { hint } : {}) });
  }
  return out;
}

function listItems(value: unknown): A2UIListItem[] | null {
  if (!Array.isArray(value) || !value.length || value.length > MAX.list) return null;
  const out: A2UIListItem[] = [];
  for (const item of value) {
    if (!isRecord(item)) return null;
    const title = text(item.title, 90);
    if (!title) return null;
    const body = text(item.text, 240) ?? undefined;
    const val = text(item.value, 40) ?? undefined;
    const act = action(item.action) ?? undefined;
    out.push({ title, ...(body ? { text: body } : {}), ...(val ? { value: val } : {}), ...(act ? { action: act } : {}) });
  }
  return out;
}

function buttons(value: unknown): A2UIButton[] | null {
  if (!Array.isArray(value) || !value.length || value.length > 4) return null;
  const out: A2UIButton[] = [];
  for (const item of value) {
    if (!isRecord(item)) return null;
    const label = text(item.label, 34);
    const act = action(item.action);
    if (!label || !act) return null;
    const variant = item.variant === 'primary' || item.variant === 'soft' || item.variant === 'ghost' ? item.variant : undefined;
    const icon = iconName(item.icon);
    out.push({ label, action: act, ...(variant ? { variant } : {}), ...(icon ? { icon } : {}) });
  }
  return out;
}

function fields(value: unknown): A2UIField[] | null {
  if (!Array.isArray(value) || !value.length || value.length > MAX.fields) return null;
  const out: A2UIField[] = [];
  const seen = new Set<string>();
  for (const item of value) {
    if (!isRecord(item)) return null;
    const name = text(item.name, 32);
    const label = text(item.label, 60);
    if (!name || !label || !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(name) || seen.has(name)) return null;
    seen.add(name);
    const placeholder = text(item.placeholder, 120) ?? undefined;
    const type = item.type === 'number' || item.type === 'textarea' || item.type === 'text' ? item.type : 'text';
    const initial = text(item.value, 300) ?? undefined;
    out.push({ name, label, type, ...(placeholder ? { placeholder } : {}), ...(initial ? { value: initial } : {}) });
  }
  return out;
}

function node(value: unknown): A2UINode | null {
  if (!isRecord(value) || typeof value.type !== 'string') return null;
  switch (value.type) {
    case 'heading': {
      const title = text(value.title, 90);
      if (!title) return null;
      const subtitle = text(value.subtitle, 180) ?? undefined;
      return { type: 'heading', title, ...(subtitle ? { subtitle } : {}) };
    }
    case 'text': {
      const body = text(value.text, 460);
      if (!body) return null;
      const style = value.style === 'display' || value.style === 'caption' || value.style === 'label' || value.style === 'body' ? value.style : 'body';
      return { type: 'text', text: body, style };
    }
    case 'stats': {
      const items = stats(value.items);
      return items ? { type: 'stats', items } : null;
    }
    case 'list': {
      const items = listItems(value.items);
      if (!items) return null;
      const title = text(value.title, 70) ?? undefined;
      return { type: 'list', items, ...(title ? { title } : {}) };
    }
    case 'actions': {
      const items = buttons(value.items);
      return items ? { type: 'actions', items } : null;
    }
    case 'form': {
      const f = fields(value.fields);
      const submit = isRecord(value.submit) ? buttons([value.submit]) : null;
      if (!f || !submit) return null;
      const title = text(value.title, 70) ?? undefined;
      return { type: 'form', fields: f, submit: submit[0], ...(title ? { title } : {}) };
    }
    case 'note': {
      const body = text(value.text, 300);
      return body ? { type: 'note', text: body } : null;
    }
    case 'progress': {
      const val = typeof value.value === 'number' && Number.isFinite(value.value) ? Math.max(0, Math.min(100, Math.round(value.value))) : null;
      if (val === null) return null;
      const label = text(value.label, 60) ?? undefined;
      return { type: 'progress', value: val, ...(label ? { label } : {}) };
    }
    case 'divider':
      return { type: 'divider' };
    case 'card': {
      const title = text(value.title, 90);
      if (!title) return null;
      const body = text(value.text, 300) ?? undefined;
      const act = action(value.action) ?? undefined;
      return { type: 'card', title, ...(body ? { text: body } : {}), ...(act ? { action: act } : {}) };
    }
    default:
      return null;
  }
}

/** Validate untrusted data from an agent, a provider or storage. */
export function validateSurface(input: unknown): ValidationResult {
  const errors: string[] = [];
  if (!isRecord(input)) return { ok: false, errors: ['Surface must be an object.'] };
  if (input.version !== A2UI_VERSION) errors.push(`Unsupported surface version.`);
  const id = text(input.id, 64);
  if (!id) errors.push('Surface needs a short id.');
  if (!Array.isArray(input.nodes)) return { ok: false, errors: [...errors, 'Surface needs a nodes array.'] };
  if (input.nodes.length > MAX.nodes) errors.push(`Surface exceeds ${MAX.nodes} nodes.`);
  const nodes: A2UINode[] = [];
  input.nodes.slice(0, MAX.nodes).forEach((raw, index) => {
    const parsed = node(raw);
    if (parsed) nodes.push(parsed);
    else errors.push(`Node ${index + 1} is not in the catalog or malformed.`);
  });
  if (!nodes.length) errors.push('Surface has no renderable nodes.');
  if (errors.length) return { ok: false, errors };
  return { ok: true, errors: [], surface: { version: A2UI_VERSION, id: id!, nodes } };
}

/* ------------------------------------------------------------ composition */

export interface UnionSurfaceContext {
  name: string;
  connections: number;
  badges: number;
  totalBadges: number;
  unread: number;
  pending: number;
  memories: number;
  courses: number;
  nextLesson?: string;
  nextCourse?: string;
  activation: string;
  relay: boolean;
}

export type SurfaceKind = 'briefing' | 'people' | 'consent' | 'learning' | 'messages' | 'economy' | 'presence';

const SURFACE_HINTS: Record<Exclude<SurfaceKind, 'briefing'>, RegExp> = {
  consent: /consent|scope|permission|revoke|boundary|privacy/gi,
  economy: /activation|token|payment|naira|joy|giving|reactivation|price/gi,
  messages: /message|relay|queued|outbox|unread|chat/gi,
  learning: /learn|course|lesson|study|badge|progress/gi,
  people: /people|person|connection|request/gi,
  presence: /page|presence|publish|link/gi
};

/** Pick the companion surface by counting intent words in the exchange.
 *  The person's own prompt counts twice, so a reply that merely mentions
 *  "revoke" while drafting a page does not turn into a consent lesson. */
export function surfaceKindFor(reply: string, prompt = ''): SurfaceKind {
  const text = `${prompt} ${prompt} ${reply}`;
  let best: SurfaceKind = 'briefing';
  let bestScore = 0;
  for (const [kind, pattern] of Object.entries(SURFACE_HINTS) as [Exclude<SurfaceKind, 'briefing'>, RegExp][]) {
    const score = (text.match(pattern) ?? []).length;
    if (score > bestScore) {
      best = kind;
      bestScore = score;
    }
  }
  return bestScore > 0 ? best : 'briefing';
}

/** Compose a surface from local, verified counts. No invented activity. */
export function buildUnionSurface(kind: SurfaceKind, ctx: UnionSurfaceContext): A2UISurface {
  const pct = ctx.totalBadges ? Math.round((ctx.badges / ctx.totalBadges) * 100) : 0;
  const hello = ctx.name.split(/\s+/)[0] || 'friend';
  const next = ctx.nextLesson
    ? `Continue “${ctx.nextLesson}”${ctx.nextCourse ? ` in ${ctx.nextCourse}` : ''}.`
    : 'Choose one lesson and finish its practice.';

  const base: Record<SurfaceKind, A2UINode[]> = {
    briefing: [
      { type: 'heading', title: `A reading for ${hello}`, subtitle: 'Composed from records on this device. Nothing moves without you.' },
      { type: 'stats', items: [
        { label: 'Connections', value: String(ctx.connections), hint: 'scoped' },
        { label: 'Badges', value: `${ctx.badges}/${ctx.totalBadges}`, hint: 'earned' },
        { label: 'Waiting', value: String(ctx.pending + ctx.unread), hint: 'needs you' }
      ] },
      { type: 'progress', value: pct, label: 'Explorer path' },
      { type: 'list', title: 'Three useful moves', items: [
        { title: ctx.nextLesson ? `Continue ${ctx.nextLesson}` : 'Start Union Foundations', text: next, value: 'Learn', action: { type: 'navigate', to: '/learning' } },
        { title: ctx.pending ? `Review ${ctx.pending} consent request${ctx.pending === 1 ? '' : 's'}` : 'Open one honest conversation', text: ctx.pending ? 'Read the scopes before accepting.' : 'A short note is enough.', value: 'People', action: { type: 'navigate', to: '/people' } },
        { title: ctx.memories ? 'Inspect what I remember' : 'Teach me one constraint', text: ctx.memories ? `${ctx.memories} explicit memories, all deletable.` : 'Memory is explicit here, never a hidden profile.', value: 'Memory', action: { type: 'ask', text: ctx.memories ? 'What do you remember about me, and where did it come from?' : 'Ask me one question that would help you support my work.' } }
      ] },
      { type: 'actions', items: [
        { label: 'Plan my week', action: { type: 'ask', text: 'What should I do next this week?' }, variant: 'primary', icon: 'spark' },
        { label: 'Open People', action: { type: 'navigate', to: '/people' }, variant: 'soft', icon: 'people' }
      ] }
    ],
    people: [
      { type: 'heading', title: 'People, with boundaries', subtitle: 'Discovery is not endorsement. Contact is not consent.' },
      { type: 'stats', items: [
        { label: 'Accepted', value: String(ctx.connections) },
        { label: 'Pending', value: String(ctx.pending) },
        { label: 'Unread', value: String(ctx.unread) }
      ] },
      { type: 'list', title: ctx.pending ? 'Waiting for your decision' : 'A calm next step', items: [
        { title: ctx.pending ? `Review ${ctx.pending} request${ctx.pending === 1 ? '' : 's'}` : 'Add one real card', text: 'Grant only the scopes the relationship needs.', action: { type: 'navigate', to: '/people' } },
        { title: 'Rehearse consent with Echo', text: 'Accept, scope, message, then revoke one permission.', action: { type: 'navigate', to: '/people' } }
      ] },
      { type: 'actions', items: [
        { label: 'Open People', action: { type: 'navigate', to: '/people' }, variant: 'primary', icon: 'people' },
        { label: 'Explain scopes', action: { type: 'ask', text: 'Explain consent scopes with a concrete example.' }, variant: 'soft', icon: 'shield' }
      ] }
    ],
    consent: [
      { type: 'heading', title: 'Consent is scoped and revocable', subtitle: 'Four permissions. Grant the smallest one that works.' },
      { type: 'list', items: [
        { title: 'Message', text: 'Exchange sealed notes with this person.' },
        { title: 'Profile', text: 'See the card they chose to publish.' },
        { title: 'Presence', text: 'See a coarse sign of availability, never location history.' },
        { title: 'Mentorship', text: 'Open a time-bounded stewardship agreement.' }
      ] },
      { type: 'note', text: 'Revocation stops future use. It does not rewrite history, and it is never punished.' },
      { type: 'actions', items: [
        { label: 'Review requests', action: { type: 'navigate', to: '/people' }, variant: 'primary', icon: 'shield' },
        { label: 'Ask a harder question', action: { type: 'ask', text: 'What should I do if someone keeps pushing a boundary?' }, variant: 'soft', icon: 'spark' }
      ] }
    ],
    learning: [
      { type: 'heading', title: 'Progress that can be checked', subtitle: 'Badges mark demonstrated participation. They cannot be bought.' },
      { type: 'stats', items: [
        { label: 'Badges', value: `${ctx.badges}/${ctx.totalBadges}` },
        { label: 'Courses', value: String(ctx.courses) },
        { label: 'Path', value: `${pct}%` }
      ] },
      { type: 'progress', value: pct, label: 'Explorer progress' },
      { type: 'list', title: 'Next', items: [
        { title: ctx.nextLesson ?? 'Union Foundations', text: next, action: { type: 'navigate', to: '/learning' } }
      ] },
      { type: 'actions', items: [
        { label: 'Continue learning', action: { type: 'navigate', to: '/learning' }, variant: 'primary', icon: 'book' },
        { label: 'How badges work', action: { type: 'ask', text: 'How are Explorer Badges awarded?' }, variant: 'soft', icon: 'badge' }
      ] }
    ],
    messages: [
      { type: 'heading', title: 'Messages that wait honestly', subtitle: 'Local first. Sealed end-to-end. Queued is a real state.' },
      { type: 'stats', items: [
        { label: 'Unread', value: String(ctx.unread) },
        { label: 'Relay', value: ctx.relay ? 'On' : 'Off', hint: ctx.relay ? 'configured' : 'local only' }
      ] },
      { type: 'actions', items: [
        { label: 'Open Messages', action: { type: 'navigate', to: '/messages' }, variant: 'primary', icon: 'message' },
        { label: 'Test offline', action: { type: 'ask', text: 'Show me how to test a queued message offline.' }, variant: 'soft', icon: 'wifi-off' }
      ] }
    ],
    economy: [
      { type: 'heading', title: 'Transparent by construction', subtitle: 'v0.0.1 records economic intent in test mode. No funds move in the app.' },
      { type: 'list', items: [
        { title: 'Activation Token · $80 minimum', text: 'One-time. Naira equivalent accepted. 14-day trial while proof is reviewed.' },
        { title: 'Reactivation Plan · $10/month minimum', text: 'After a verified Special or Super experience.' },
        { title: 'Joy / Giving plans', text: 'Requested with the courtesy signal “Joyful Giver”. 2-week trial.' }
      ] },
      { type: 'note', text: 'Every ledger entry is hash-chained and visible. Test mode is labelled on every record.' },
      { type: 'actions', items: [
        { label: 'Open Economy', action: { type: 'navigate', to: '/economy' }, variant: 'primary', icon: 'coins' },
        { label: 'Explain the plans', action: { type: 'ask', text: 'Explain the activation, reactivation and Joy plans.' }, variant: 'soft', icon: 'info' }
      ] }
    ],
    presence: [
      { type: 'heading', title: 'A page, not a funnel', subtitle: 'One honest place with links that declare their permissions.' },
      { type: 'list', items: [
        { title: 'Write the sentence', text: 'Title, one honest sentence, two sections. That is enough.' },
        { title: 'Publish deliberately', text: 'A page becomes public only when you publish it.' },
        { title: 'Make links expire', text: 'Read-only links can carry an expiry and be revoked later.' }
      ] },
      { type: 'form', title: 'Draft a page', fields: [
        { name: 'subject', label: 'What is it about?', placeholder: 'The work, the practice, the question…', type: 'text' }
      ], submit: { label: 'Draft with Union', action: { type: 'ask', text: 'Help me draft a short Presence page about {subject}.' }, variant: 'primary', icon: 'spark' } },
      { type: 'actions', items: [
        { label: 'Open Presence', action: { type: 'navigate', to: '/presence' }, variant: 'soft', icon: 'globe' }
      ] }
    ]
  };

  return { version: A2UI_VERSION, id: `union.${kind}`, nodes: base[kind] };
}
