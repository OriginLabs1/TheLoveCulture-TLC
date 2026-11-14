/** Union AI.
 *
 *  Two modes, both honest:
 *  1. On-device assistant (default): deterministic, grounded in the app's own
 *     records and the Constitution's vocabulary. No network, no invented facts.
 *  2. Optional OpenAI-compatible provider: only used when you configure a base
 *     URL and key in Operations. The request contains your message, a short
 *     Union briefing, your memories and record counts — never your private keys
 *     or message bodies.
 *
 *  Proposals are never executed automatically. They return as an AiAction that
 *  the person must approve in the interface.
 */

import type { AiAction, AiThread, Identity, UnionRecord } from './types';
import { plural } from './format';

export interface AiRequest {
  text: string;
  thread: AiThread;
  identity: Identity;
  records: UnionRecord[];
  baseUrl: string;
  apiKey: string;
  model: string;
  memories: string[];
}

export interface AiReply {
  text: string;
  provider: string;
  action?: Omit<AiAction, 'state'>;
}

const BRIEF = `You are Union's assistant inside the Union mobile app, version 0.0.1.
Voice: calm, concise, practical. Never use engagement-farming language.
Union is one connected environment with these capability domains: communication,
community, learning, experiences, digital presence, intelligence, economic
coordination, operations, exploration & progress, mentorship & stewardship.
Non-negotiables: identity is verifiable; discovery is not endorsement; contact
is not consent; consent is scoped and revocable; records are transparent;
Union does not invent activity or pretend money moved. v0.0.1 economic records
are in test mode.
Answer in at most 160 words. Prefer a concrete next step. If asked to create
something, propose an action instead of claiming it is done.`;

const KB: { match: RegExp; answer: string }[] = [
  {
    match: /(message|dm|chat|send|relay|offline|queued)/i,
    answer:
      'Messages are written to this device first, sealed end-to-end, then handed to a relay if one is configured. Without a relay they wait in the outbox and show as **queued**. Open **Messages → Notes to self** to test composing offline. A direct message requires an accepted connection with the **message** scope.'
  },
  {
    match: /(consent|scope|permission|revoke|block|mute|privacy)/i,
    answer:
      'Consent is scoped per connection: message, profile, presence, mentorship. Grant only what the relationship needs. Revocation stops future use but does not rewrite history. Mute is a volume control, block is a boundary, report is a safety signal — use the smallest tool that solves the problem.'
  },
  {
    match: /(activation|reactivation|joy|giving|token|plan|\$80|\$10|naira|price|payment)/i,
    answer:
      '**Activation Token**: minimum $80 equivalent (Naira accepted), one-time. **Reactivation Token (Plan)**: minimum $10/month equivalent after either a Special or Super experience. **Joy/Giving Plans**: optional, requested with the courtesy signal “Joyful Giver” to t.me/CollinsRoyalMedium, with a 2-week trial. In v0.0.1 every entry is recorded in test mode; no funds move in the app.'
  },
  {
    match: /(mentor|steward|guidance|discipleship)/i,
    answer:
      'Mentorship is optional and strategic. Agree on a focus, a cadence and an ending condition. Open **Mentorship**, choose a connection, and write the ending condition in the note. Notes are local to the relationship and exportable when it ends.'
  },
  {
    match: /(learn|course|lesson|study|quiz|badge|progress)/i,
    answer:
      'Start with **Union Foundations** in Learning — five short lessons and a practice for each. Explorer Badges are awarded for demonstrated participation, never for time spent or money paid. Your next unfinished lesson is always on Home.'
  },
  {
    match: /(community|post|commons|space|group)/i,
    answer:
      'Union Commons is the official shared room. Join it, read the welcome post, and share one practical discovery. Public communities can also register with the relay so posts sync; otherwise they remain local and honestly labelled.'
  },
  {
    match: /(event|experience|rsvp|calendar)/i,
    answer:
      'Experiences live in **Experiences**. RSVP to the Union Assembly or the Explorer Field Practice, or host your own. Every event can be exported as an .ics file and shared as a Union link with declared permissions.'
  },
  {
    match: /(page|presence|link|publish|site)/i,
    answer:
      'A page is a small place, not a funnel. In **Presence**, write a title, one honest sentence and two sections, then publish. Create links with explicit permissions and an optional expiry; revoke a link when it should stop working. A self-contained link keeps working outside the app for read-only content.'
  },
  {
    match: /(backup|restore|export|import|key|seed|recover|lost)/i,
    answer:
      'Your Ed25519 and X25519 private keys never leave this device except inside an encrypted backup. Use **Settings → Backup** with a passphrase of at least 8 characters. Store the file offline. Without the keyring or a backup, the identity cannot be recovered — that is the cost of real ownership.'
  },
  {
    match: /(ai|model|assistant|provider|memory)/i,
    answer:
      'I run on-device by default: no network and no invented facts. In **Operations → Union AI** you can connect an OpenAI-compatible endpoint. Requests include your message, this briefing, your saved memories and record counts — never keys or message bodies. Proposed actions require your approval before anything changes.'
  },
  {
    match: /(install|apk|android|pwa|github|cloudflare|deploy)/i,
    answer:
      'Union installs as a PWA from the browser menu, or as the signed Android APK from the GitHub release. The optional relay runs on Cloudflare Workers + D1; **Operations → Relay** is where you connect it. The app is fully usable offline without the relay.'
  },
  {
    match: /(special|super|experience mode)/i,
    answer:
      '**Special** is the focused experience: communication, community, learning, presence and the core Explorer path. **Super** adds the broader intelligence and organisational surface. Both are valid; Reactivation follows either. You can change this in Settings.'
  }
];

function count(records: UnionRecord[], type: string): number {
  return records.filter((r) => r.type === type).length;
}

function localAnswer(req: AiRequest): string {
  const text = req.text.trim();
  const found = KB.find((entry) => entry.match.test(text));
  if (found) return found.answer;
  const openThreads = count(req.records, 'thread');
  const unread = req.records.filter(
    (r) => r.type === 'message' && r.to === req.identity.id && r.status === 'delivered'
  ).length;
  const badges = count(req.records, 'badgeAward');
  return [
    `I can help with Union's capabilities, your next useful step, or a specific task. Right now this environment holds **${plural(count(req.records, 'person'), 'person', 'people')}**, **${plural(openThreads, 'conversation')}** (${unread} awaiting you), **${plural(count(req.records, 'community'), 'community', 'communities')}**, **${plural(count(req.records, 'enrollment'), 'course enrolment')}** and **${plural(badges, 'Explorer Badge')}**.`,
    '',
    'Try:',
    '- “What should I do next?”',
    '- “Explain how consent works here.”',
    '- “Draft a page for my work.”',
    '- “Create a community for my study circle.”'
  ].join('\n');
}

function nextStep(req: AiRequest): string {
  const r = req.records;
  const me = req.identity.id;
  const accepted = r.filter((x) => x.type === 'connection' && x.status === 'accepted' && (x.aId === me || x.bId === me));
  const sent = r.some((x) => x.type === 'message' && x.from === me);
  const lessons = r.filter((x) => x.type === 'enrollment' && x.identityId === me && x.done.length > 0);
  const page = r.some((x) => x.type === 'page' && x.ownerId === me && x.published);
  const ledger = r.some((x) => x.type === 'ledger');
  if (!accepted.length) return 'Open **People**, accept the practice connection from Echo (or add a real card), and choose one consent scope. That unlocks messaging, mentorship and Joy giving.';
  if (!sent) return 'Send a short note to an accepted connection. If no relay is configured, the message stays sealed and queued on this device — that is honest, not broken.';
  if (!lessons.length) return 'Start **Union Foundations** in Learning. Finish one lesson and do its practice before moving on.';
  if (!page) return 'Draft and publish a small **Presence** page: title, one honest sentence, two sections. Then make a read-only link with an expiry.';
  if (!ledger) return 'Record one transparent entry in **Economy** — a test gift or a plan — and verify the hash chain. Read the test-mode disclosure first.';
  return 'Choose one real experience this week: RSVP to an event, host a small practice, or open a mentorship agreement with a clear ending condition. Then rest. Progress is not a streak.';
}

function propose(req: AiRequest): Omit<AiAction, 'state'> | undefined {
  const text = req.text.trim();
  let m = text.match(/create (?:a )?community (?:called |named )?["“']?([^"”'\n.]{3,60})["”']?/i);
  if (m) {
    return {
      tool: 'create_community',
      args: { name: m[1].trim(), about: `A community started from a Union AI conversation: ${text.slice(0, 140)}` },
      description: `Create the community “${m[1].trim()}”`
    };
  }
  m = text.match(/(?:schedule|create|host) (?:an? )?(?:event|experience) (?:called |titled )?["“']?([^"”'\n.]{3,70})["”']?/i);
  if (m) {
    return {
      tool: 'schedule_event',
      args: {
        title: m[1].trim(),
        about: `Proposed from a Union AI conversation: ${text.slice(0, 140)}`,
        mode: 'online',
        place: 'To be confirmed',
        capacity: 20
      },
      description: `Schedule “${m[1].trim()}” one week from now`
    };
  }
  if (/(remember|save this|note that)/i.test(text)) {
    return {
      tool: 'add_memory',
      args: { text: text.replace(/^(please\s+)?(remember|save this|note that)[:,]?\s*/i, '').slice(0, 400) },
      description: 'Save this as an AI memory you can delete later'
    };
  }
  m = text.match(/draft (?:a )?page(?: (?:called|titled|about))? ["“']?([^"”'\n.]{3,60})["”']?/i);
  if (m) {
    return {
      tool: 'draft_page',
      args: { title: m[1].trim(), tagline: 'A small, useful place.', about: '' },
      description: `Draft a Presence page called “${m[1].trim()}”`
    };
  }
  return undefined;
}

async function providerReply(req: AiRequest): Promise<AiReply | null> {
  const base = req.baseUrl.trim().replace(/\/+$/, '');
  if (!base) return null;
  const url = /\/chat\/completions$/.test(base) ? base : `${base}/chat/completions`;
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        ...(req.apiKey ? { authorization: `Bearer ${req.apiKey}` } : {})
      },
      body: JSON.stringify({
        model: req.model || 'qwen3.5-0.8b',
        temperature: 0.4,
        max_tokens: 420,
        messages: [
          { role: 'system', content: BRIEF },
          {
            role: 'system',
            content: `Context counts: ${JSON.stringify({
              people: count(req.records, 'person'),
              communities: count(req.records, 'community'),
              messages: count(req.records, 'message'),
              courses: count(req.records, 'course'),
              ledger: count(req.records, 'ledger')
            })}. Saved memories: ${req.memories.length ? req.memories.join(' | ') : 'none'}.`
          },
          { role: 'user', content: req.text }
        ]
      })
    });
    if (!response.ok) throw new Error(`Provider HTTP ${response.status}`);
    const data = (await response.json()) as {
      choices?: { message?: { content?: string } }[];
    };
    const content = data.choices?.[0]?.message?.content?.trim();
    if (!content) throw new Error('The provider returned an empty reply.');
    return { text: content, provider: `provider · ${req.model}` };
  } catch {
    return null;
  }
}

export async function askUnionAi(req: AiRequest): Promise<AiReply> {
  const action = propose(req);
  const wantsStep = /(next|what should i do|where do i start|help me start)/i.test(req.text);
  const local = wantsStep ? nextStep(req) : localAnswer(req);
  const provider = await providerReply(req);
  if (provider) {
    return {
      text: provider.text,
      provider: provider.provider,
      action
    };
  }
  return {
    text: local,
    provider: req.baseUrl ? 'on-device (provider unreachable)' : 'on-device · no network',
    action
  };
}
