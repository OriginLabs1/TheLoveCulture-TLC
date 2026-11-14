import { clearAll, loadAll, put, putMany, remove, replaceAll } from './db';
import { coreBackend, initCore, sha256Hex } from './core';
import {
  canonical,
  colorFor,
  decryptBackup,
  encodeCard,
  encodeToken,
  encryptBackup,
  hashValue,
  makeCard,
  newIdentity,
  publicFromPrivate,
  sealMessage,
  sign,
  verify
} from './crypto';
import type { SealedMessage, UnionCard } from './crypto';
import { isNative, secretDelete, secretGet, secretSet } from './native';
import { officialRecords, UNION_COMMONS_ID } from './seed';
import { BADGES, earnedBadgeIds } from './badges';
import {
  ackEnvelopes,
  fetchEnvelopes,
  fetchPosts,
  health,
  lookup,
  publishLink,
  publishPost,
  register,
  registerSpace,
  resolveLink,
  revokeLink as relayRevokeLink,
  sendEnvelope
} from './relay';
import type { RelayAuth, RelayEnvelope } from './relay';
import { askUnionAi } from './ai';
import type {
  AiAction,
  AiMemory,
  AiMessage,
  AiProject,
  AiThread,
  ActivationProof,
  ActivationRequest,
  Attachment,
  AuditEntry,
  BadgeAward,
  Community,
  Connection,
  ConsentScope,
  Course,
  Enrollment,
  Event,
  Experience,
  Identity,
  Keyring,
  LedgerEntry,
  Membership,
  Mentorship,
  Message,
  OutboxJob,
  Page,
  Person,
  Plan,
  Post,
  Rsvp,
  Setting,
  Thread,
  TokenSymbol,
  UnionLink,
  UnionRecord
} from './types';
import { addDays, clamp, now, parseHandle, percent, uid, validHandle } from './util';
import { toasts } from './toast.svelte';
import { ACTIVATION_MIN_USD, REACTIVATION_MIN_USD } from './version';

const KEYRING_KEY = 'union.keyring.v1';
const DEFAULT_FX = 1600;

interface LedgerInput {
  kind: LedgerEntry['kind'];
  from?: string;
  to?: string;
  token: TokenSymbol;
  amount: number;
  currency: 'USD' | 'NGN';
  amountUsd: number;
  memo: string;
}

function sortByDate<T extends { createdAt?: string; at?: string; joinedAt?: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => {
    const av = a.createdAt || a.at || a.joinedAt || '';
    const bv = b.createdAt || b.at || b.joinedAt || '';
    return bv.localeCompare(av);
  });
}

class UnionStore {
  records = $state<UnionRecord[]>([]);
  identity = $state<Identity | null>(null);
  keys = $state<Keyring | null>(null);
  ready = $state(false);
  startupError = $state('');
  core = $state({ backend: 'js' as 'rust-wasm' | 'js', error: '', version: 0 });
  syncing = $state(false);
  lastSync = $state('');
  syncError = $state('');
  online = $state(typeof navigator === 'undefined' ? true : navigator.onLine);
  installPrompt = $state(false);
  private deferredPrompt: Event | null = null;
  private syncTimer: ReturnType<typeof setInterval> | null = null;

  all<T extends UnionRecord>(type: string): T[] {
    return this.records.filter((r) => r.type === type) as T[];
  }

  byId<T extends UnionRecord>(type: string, id: string): T | undefined {
    return this.records.find((r) => r.type === type && r.id === id) as T | undefined;
  }

  get people(): Person[] {
    return this.all<Person>('person');
  }

  get connections(): Connection[] {
    return this.all<Connection>('connection');
  }

  get acceptedConnections(): Connection[] {
    if (!this.identity) return [];
    return this.connections.filter(
      (c) => c.status === 'accepted' && (c.aId === this.identity!.id || c.bId === this.identity!.id)
    );
  }

  get pendingConnections(): Connection[] {
    if (!this.identity) return [];
    return this.connections.filter(
      (c) =>
        c.status === 'pending' &&
        (c.aId === this.identity!.id || c.bId === this.identity!.id)
    );
  }

  get incomingRequests(): Connection[] {
    if (!this.identity) return [];
    return this.pendingConnections.filter((c) => c.bId === this.identity!.id);
  }

  get threads(): Thread[] {
    return [...this.all<Thread>('thread')].sort((a, b) => b.lastAt.localeCompare(a.lastAt));
  }

  get messages(): Message[] {
    return this.all<Message>('message');
  }

  get communities(): Community[] {
    return sortByDate(this.all<Community>('community'));
  }

  get memberships(): Membership[] {
    return this.all<Membership>('membership');
  }

  get courses(): Course[] {
    return this.all<Course>('course');
  }

  get enrollments(): Enrollment[] {
    return this.all<Enrollment>('enrollment').filter((e) => e.identityId === this.identity?.id);
  }

  get events(): Event[] {
    return [...this.all<Event>('event')].sort((a, b) => a.start.localeCompare(b.start));
  }

  get rsvps(): Rsvp[] {
    return this.all<Rsvp>('rsvp');
  }

  get pages(): Page[] {
    return sortByDate(this.all<Page>('page'));
  }

  get links(): UnionLink[] {
    return sortByDate(this.all<UnionLink>('link'));
  }

  get aiThreads(): AiThread[] {
    return sortByDate(this.all<AiThread>('aiThread'));
  }

  get aiMessages(): AiMessage[] {
    return this.all<AiMessage>('aiMessage');
  }

  get memories(): AiMemory[] {
    return sortByDate(this.all<AiMemory>('aiMemory'));
  }

  get projects(): AiProject[] {
    return sortByDate(this.all<AiProject>('aiProject'));
  }

  get ledger(): LedgerEntry[] {
    return this.all<LedgerEntry>('ledger');
  }

  get plans(): Plan[] {
    return sortByDate(this.all<Plan>('plan'));
  }

  get awards(): BadgeAward[] {
    return this.all<BadgeAward>('badgeAward');
  }

  get mentorships(): Mentorship[] {
    return sortByDate(this.all<Mentorship>('mentorship'));
  }

  get auditEntries(): AuditEntry[] {
    return this.all<AuditEntry>('audit');
  }

  get outbox(): OutboxJob[] {
    return this.all<OutboxJob>('outbox');
  }

  get settings(): Setting[] {
    return this.all<Setting>('setting');
  }

  get relayUrl(): string {
    return this.getSetting('relay.url');
  }

  get fxRate(): number {
    const n = Number(this.getSetting('fx.rate', String(DEFAULT_FX)));
    return Number.isFinite(n) && n > 0 ? n : DEFAULT_FX;
  }

  get currency(): 'USD' | 'NGN' {
    return this.getSetting('fx.display', 'USD') === 'NGN' ? 'NGN' : 'USD';
  }

  get activation(): Identity['activation'] {
    return this.identity?.activation ?? { status: 'inactive' };
  }

  get activationRequests(): ActivationRequest[] {
    if (!this.identity) return [];
    return sortByDate(
      this.all<ActivationRequest>('activationRequest').filter((r) => r.identityId === this.identity!.id)
    );
  }

  get currentActivationRequest(): ActivationRequest | undefined {
    return this.activationRequests.find((r) => r.status !== 'rejected');
  }

  get activationVerified(): boolean {
    return this.activation.status === 'active' || this.activation.status === 'grace';
  }

  get joyPlan(): Plan | undefined {
    return this.plans.find((p) => p.kind === 'joy' && (p.status === 'trial' || p.status === 'active'));
  }

  get unreadMessages(): number {
    if (!this.identity) return 0;
    return this.messages
      .filter((m) => m.to === this.identity!.id && m.from !== m.to)
      .filter((m) => {
        const thread = this.byId<Thread>('thread', m.threadId);
        return thread ? thread.unread > 0 : false;
      }).length;
  }

  get earnedBadges() {
    if (!this.identity) return [];
    return BADGES.filter((b) => this.awards.some((a) => a.badgeId === b.id));
  }

  get nextLesson(): { course: Course; lesson: Course['lessons'][number] } | null {
    for (const course of this.courses) {
      const enrollment = this.enrollmentFor(course.id);
      const next = course.lessons.find((l) => !(enrollment?.done ?? []).includes(l.id));
      if (next) return { course, lesson: next };
    }
    return null;
  }

  /* --------------------------------------------------------------- lifecycle */

  async init(): Promise<void> {
    try {
      this.online = navigator.onLine;
      window.addEventListener('online', () => { this.online = true; void this.sync(); });
      window.addEventListener('offline', () => { this.online = false; });
      this.core = coreBackend();
      await initCore();
      this.core = coreBackend();
      this.records = await loadAll();
      const identity = this.records.find((r) => r.type === 'identity') as Identity | undefined;
      if (identity) {
        this.identity = this.normaliseIdentity(identity);
        this.keys = this.loadKeyring(identity.id);
        if (!this.keys) this.startupError = 'Identity found, but its device key is missing. Restore a backup to continue.';
        await this.ensureSeed();
        await this.refreshActivation();
      }
      this.ready = true;
      this.core = coreBackend();
      if (this.identity && this.keys) {
        this.startSyncLoop();
        void this.audit('app.opened', navigator.userAgent.slice(0, 80));
        void this.sync();
      }
    } catch (error) {
      this.startupError = error instanceof Error ? error.message : String(error);
      this.ready = true;
    }
  }

  private normaliseIdentity(identity: Identity): Identity {
    return {
      ...identity,
      experience: identity.experience === 'super' ? 'super' : 'special',
      activation: identity.activation ?? { status: 'inactive' }
    };
  }

  private loadKeyring(identityId: string): Keyring | null {
    try {
      const raw = secretGet(KEYRING_KEY);
      if (!raw) return null;
      const keyring = JSON.parse(raw) as Keyring;
      if (keyring.identityId !== identityId) return null;
      const derived = publicFromPrivate(keyring);
      if (this.identity && (derived.edPub !== this.identity.edPub || derived.xPub !== this.identity.xPub)) {
        return null;
      }
      return keyring;
    } catch {
      return null;
    }
  }

  private saveKeyring(keyring: Keyring): boolean {
    return secretSet(KEYRING_KEY, JSON.stringify(keyring));
  }

  async createIdentity(input: { name: string; handle: string; about?: string; color?: string; experience?: Experience }): Promise<void> {
    if (this.identity) throw new Error('This device already has a Union identity.');
    const handle = parseHandle(input.handle);
    if (!validHandle(handle)) throw new Error('Choose a handle with 3–24 letters, numbers or underscores.');
    const { identity, keyring } = newIdentity(input.name, handle, input.color);
    identity.about = input.about?.trim() ?? '';
    if (input.experience) identity.experience = input.experience;
    if (!this.saveKeyring(keyring)) {
      throw new Error('Could not open the device secure store. Union will not create keys without it.');
    }
    this.identity = identity;
    this.keys = keyring;
    this.records = [...this.records, identity];
    await put(identity);
    await this.setSetting('fx.rate', String(DEFAULT_FX));
    await this.setSetting('fx.display', 'USD');
    await this.ensureSeed();
    await this.audit('identity.created', `${identity.handle} · ${identity.id}`);
    this.evaluateBadges();
    toasts.push('Identity created. Your keys stay on this device.', 'success');
  }

  async updateIdentity(patch: Partial<Pick<Identity, 'name' | 'handle' | 'about' | 'color' | 'experience'>>): Promise<void> {
    if (!this.identity) return;
    if (patch.handle !== undefined) {
      const handle = parseHandle(patch.handle);
      if (!validHandle(handle)) throw new Error('That handle is not valid.');
      patch.handle = handle;
    }
    const identity: Identity = { ...this.identity, ...patch, updatedAt: now() };
    await this.commit(identity);
    this.identity = identity;
    await this.audit('identity.updated', Object.keys(patch).join(','));
  }

  async ensureSeed(): Promise<void> {
    if (!this.identity) return;
    const me = this.identity;
    const seeded = this.getSetting('seed.version');
    const hasFoundations = this.courses.some((c) => c.slug === 'union-foundations');
    if (seeded === '1' && hasFoundations) {
      if (!this.memberships.some((m) => m.communityId === UNION_COMMONS_ID && m.identityId === me.id)) {
        await this.commit({
          type: 'membership',
          id: `membership_commons_${me.id.slice(-8)}`,
          communityId: UNION_COMMONS_ID,
          identityId: me.id,
          role: 'member',
          joinedAt: now()
        } as Membership);
      }
      await this.ensurePracticeContact();
      return;
    }
    const existing = new Set(this.records.map((r) => r.id));
    const additions = officialRecords(me).filter((r) => !existing.has(r.id));
    if (additions.length) {
      await putMany(additions);
      this.records = [...this.records, ...additions];
    }
    await this.setSetting('seed.version', '1');
    await this.ensurePracticeContact();
  }

  /** A clearly-labelled official practice contact so one person on one device
   *  can experience request → scope → message → revoke without a second human.
   *  Echo's private keys are discarded at creation: replies cannot be read. */
  private async ensurePracticeContact(): Promise<void> {
    if (!this.identity) return;
    const existingId = this.getSetting('seed.echo.id');
    if (existingId) return;
    const echo = newIdentity('Echo · practice', 'echo_practice', '#0e7490');
    const person: Person = {
      type: 'person',
      id: uid('person'),
      identityId: echo.identity.id,
      handle: echo.identity.handle,
      name: echo.identity.name,
      about:
        'Official Union practice contact. Accept the pending request, choose scopes, send a note, then revoke a scope and watch what changes. Echo has no device and cannot read replies; this is a consent rehearsal, not a fake person.',
      edPub: echo.identity.edPub,
      xPub: echo.identity.xPub,
      color: echo.identity.color,
      addedAt: now(),
      source: 'self'
    };
    const thread: Thread = {
      type: 'thread',
      id: 'thread_echo_practice',
      kind: 'direct',
      participantIds: [this.identity.id, echo.identity.id],
      title: 'Echo · practice',
      lastAt: now(),
      unread: 0,
      createdAt: now()
    };
    const connection: Connection = {
      type: 'connection',
      id: 'conn_echo_practice',
      aId: echo.identity.id,
      bId: this.identity.id,
      status: 'pending',
      scopes: ['message', 'profile'],
      muted: false,
      createdAt: now(),
      note: 'Practice request from Union. No reply is possible; accept to rehearse consent.'
    };
    const message: Message = {
      type: 'message',
      id: 'msg_echo_welcome',
      threadId: thread.id,
      from: echo.identity.id,
      to: this.identity.id,
      kind: 'system',
      body:
        'This is the official practice contact. Accept the pending request in People, choose the scopes you actually want to rehearse, then send a note. Echo cannot read or reply — the private keys were discarded when this contact was created. Use it to feel the consent controls before inviting a real person.',
      attachments: [],
      createdAt: now(),
      status: 'delivered'
    };
    await this.commitMany([person, thread, connection, message]);
    await this.setSetting('seed.echo.id', echo.identity.id);
  }

  isPractice(person: Person): boolean {
    return person.identityId === this.getSetting('seed.echo.id');
  }

  async refreshActivation(): Promise<void> {
    if (!this.identity) return;
    const activation = { ...this.identity.activation };
    if (activation.status === 'active' && activation.until) {
      if (new Date(activation.until).getTime() < Date.now()) {
        const graceEnd = addDays(activation.until, 7);
        activation.status = new Date(graceEnd).getTime() > Date.now() ? 'grace' : 'expired';
        const identity = { ...this.identity, activation, updatedAt: now() };
        await this.commit(identity);
        this.identity = identity;
      }
    }
    if (activation.status === 'trial' && activation.until && new Date(activation.until).getTime() < Date.now()) {
      activation.status = 'expired';
      const identity = { ...this.identity, activation, updatedAt: now() };
      await this.commit(identity);
      this.identity = identity;
    }
    if (activation.status === 'grace' && activation.until && new Date(addDays(activation.until, 7)).getTime() < Date.now()) {
      activation.status = 'expired';
      const identity = { ...this.identity, activation, updatedAt: now() };
      await this.commit(identity);
      this.identity = identity;
    }
    const joy = this.plans.find((p) => p.kind === 'joy' && p.status === 'trial');
    if (joy?.until && new Date(joy.until).getTime() < Date.now()) {
      await this.commit({ ...joy, status: 'expired' } as Plan);
    }
  }

  /* ------------------------------------------------------------ persistence */

  private recomputeRecords(record: UnionRecord): void {
    const index = this.records.findIndex((r) => r.id === record.id);
    if (index >= 0) this.records[index] = record;
    else this.records = [...this.records, record];
  }

  async commit<T extends UnionRecord>(record: T): Promise<T> {
    this.recomputeRecords(record);
    await put(record);
    return record;
  }

  async commitMany<T extends UnionRecord>(items: T[]): Promise<T[]> {
    if (!items.length) return items;
    for (const item of items) this.recomputeRecords(item);
    await putMany(items);
    return items;
  }

  async deleteRecord(id: string): Promise<void> {
    this.records = this.records.filter((r) => r.id !== id);
    await remove(id);
  }

  async audit(action: string, detail = ''): Promise<void> {
    if (!this.identity) return;
    const last = this.auditEntries.at(-1);
    const prevHash = last?.hash ?? '0'.repeat(64);
    const at = now();
    const hash = await sha256Hex(`${prevHash}\n${at}\n${action}\n${detail}`);
    await this.commit({
      type: 'audit',
      id: uid('aud'),
      at,
      actorId: this.identity.id,
      action,
      detail,
      hash,
      prevHash
    } as AuditEntry);
  }

  evaluateBadges(): string[] {
    if (!this.identity) return [];
    const earned = earnedBadgeIds(this.records, this.identity.id);
    const existing = new Set(this.awards.map((a) => a.badgeId));
    const fresh = earned.filter((id) => !existing.has(id));
    for (const badgeId of fresh) {
      const def = BADGES.find((b) => b.id === badgeId);
      if (!def) continue;
      void this.commit({
        type: 'badgeAward',
        id: uid('badge'),
        badgeId,
        identityId: this.identity.id,
        awardedAt: now(),
        note: def.description
      } as BadgeAward);
      toasts.push(`Explorer Badge earned: ${def.name}`, 'badge');
    }
    return fresh;
  }

  /* -------------------------------------------------------------- settings */

  getSetting(id: string, fallback = ''): string {
    const found = this.settings.find((s) => s.id === id);
    return found ? found.value : fallback;
  }

  async setSetting(id: string, value: string): Promise<void> {
    const record: Setting = { type: 'setting', id, value };
    await this.commit(record);
  }

  async setSecretSetting(id: string, value: string): Promise<void> {
    if (!value) secretDelete(id);
    else secretSet(id, value);
  }

  getSecretSetting(id: string): string {
    return secretGet(id) ?? '';
  }

  /* --------------------------------------------------------------- people */

  async addPerson(card: UnionCard, source: Person['source'] = 'card'): Promise<Person> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (card.id === this.identity.id) throw new Error('That is your own card.');
    const existing = this.people.find((p) => p.identityId === card.id);
    if (existing) return existing;
    const person: Person = {
      type: 'person',
      id: uid('person'),
      identityId: card.id,
      handle: card.handle,
      name: card.name || card.handle,
      about: card.about,
      edPub: card.ed,
      xPub: card.x,
      color: card.color || colorFor(card.id),
      addedAt: now(),
      source
    };
    await this.commit(person);
    return person;
  }

  async addPersonFromText(text: string, source: Person['source'] = 'card'): Promise<Person> {
    const { decodeCard } = await import('./crypto');
    const card = decodeCard(text);
    if (!card) throw new Error('That is not a valid Union card. Ask for a fresh card link.');
    return this.addPerson(card, source);
  }

  /** Look up a public card from the relay directory. Discovery is not
   *  endorsement: this only adds a contact card, never a connection. */
  async lookupHandle(rawHandle: string): Promise<Person> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (!this.relayUrl) throw new Error('Connect a relay in Operations to use the public directory.');
    const handle = rawHandle.trim().replace(/^@/, '').toLowerCase();
    if (!handle) throw new Error('Enter a handle to look up.');
    const card = await lookup(this.relayUrl, handle);
    if (!card) throw new Error(`No public card found for @${handle}.`);
    return this.addPerson(card, 'directory');
  }

  async findPerson(identityId: string): Promise<Person | undefined> {
    return this.people.find((p) => p.identityId === identityId);
  }

  connectionWith(identityId: string): Connection | undefined {
    if (!this.identity) return undefined;
    return this.connections.find(
      (c) =>
        (c.aId === this.identity!.id && c.bId === identityId) ||
        (c.bId === this.identity!.id && c.aId === identityId)
    );
  }

  myCard(): UnionCard {
    if (!this.identity || !this.keys) throw new Error('Identity is not unlocked.');
    return makeCard(this.identity, this.keys);
  }

  myCardText(): string {
    return encodeCard(this.myCard());
  }

  private ensureConnection(identityId: string, status: Connection['status'] = 'pending'): Connection {
    if (!this.identity) throw new Error('Create an identity first.');
    const existing = this.connectionWith(identityId);
    if (existing) return existing;
    const connection: Connection = {
      type: 'connection',
      id: uid('conn'),
      aId: this.identity.id,
      bId: identityId,
      status,
      scopes: ['message', 'profile'],
      muted: false,
      createdAt: now()
    };
    return connection;
  }

  async requestConnection(person: Person, note = ''): Promise<Connection> {
    if (!this.identity || !this.keys) throw new Error('Create an identity first.');
    const existing = this.connectionWith(person.identityId);
    if (existing && existing.status === 'blocked') throw new Error('This connection is blocked.');
    if (existing && existing.status === 'accepted') return existing;
    const connection = existing ?? this.ensureConnection(person.identityId, 'pending');
    await this.commit({ ...connection, status: 'pending', note } as Connection);
    const thread = await this.ensureThread(person.identityId);
    await this.sendMessage(
      thread.id,
      JSON.stringify({
        kind: 'connection_request',
        connectionId: connection.id,
        note,
        card: this.myCard()
      }),
      [],
      'connection_request'
    );
    await this.audit('connection.requested', person.handle);
    return connection;
  }

  async respondConnection(connection: Connection, accept: boolean): Promise<void> {
    if (!this.identity) return;
    const person = await this.findPerson(connection.aId);
    if (!person) throw new Error('The request has no verifiable card.');
    if (accept) {
      const accepted: Connection = {
        ...connection,
        status: 'accepted',
        acceptedAt: now(),
        scopes: connection.scopes.length ? connection.scopes : ['message', 'profile']
      };
      await this.commit(accepted);
      const thread = await this.ensureThread(person.identityId);
      await this.sendMessage(
        thread.id,
        JSON.stringify({
          kind: 'connection_accept',
          connectionId: connection.id,
          scopes: accepted.scopes,
          card: this.myCard()
        }),
        [],
        'connection_accept'
      );
      await this.audit('connection.accepted', person.handle);
      this.evaluateBadges();
      toasts.push(`Connected with ${person.name}. Scopes are explicit and revocable.`, 'success');
    } else {
      await this.commit({ ...connection, status: 'declined' } as Connection);
      const thread = await this.ensureThread(person.identityId);
      await this.sendMessage(
        thread.id,
        JSON.stringify({ kind: 'connection_decline', connectionId: connection.id }),
        [],
        'connection_decline'
      );
      await this.audit('connection.declined', person.handle);
    }
  }

  async setScopes(connection: Connection, scopes: ConsentScope[]): Promise<void> {
    await this.commit({ ...connection, scopes } as Connection);
    await this.audit('connection.scopes', `${connection.id}: ${scopes.join(',') || 'none'}`);
  }

  async toggleMute(connection: Connection): Promise<void> {
    await this.commit({ ...connection, muted: !connection.muted } as Connection);
  }

  async blockConnection(connection: Connection): Promise<void> {
    await this.commit({ ...connection, status: 'blocked', scopes: [] } as Connection);
    await this.audit('connection.blocked', connection.id);
  }

  async removePerson(person: Person): Promise<void> {
    const connection = this.connectionWith(person.identityId);
    if (connection) await this.deleteRecord(connection.id);
    await this.deleteRecord(person.id);
  }

  /* ------------------------------------------------------------- messaging */

  threadFor(otherId: string): Thread | undefined {
    if (!this.identity) return undefined;
    const me = this.identity.id;
    return this.threads.find(
      (t) =>
        t.kind === 'direct' &&
        t.participantIds.includes(me) &&
        t.participantIds.includes(otherId)
    );
  }

  async ensureThread(otherId: string): Promise<Thread> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (otherId === this.identity.id) {
      const notes = this.threads.find((t) => t.kind === 'notes');
      if (notes) return notes;
      const thread: Thread = {
        type: 'thread',
        id: uid('thread'),
        kind: 'notes',
        participantIds: [this.identity.id],
        title: 'Notes to self',
        lastAt: now(),
        unread: 0,
        createdAt: now()
      };
      await this.commit(thread);
      return thread;
    }
    const existing = this.threadFor(otherId);
    if (existing) return existing;
    const person = await this.findPerson(otherId);
    const thread: Thread = {
      type: 'thread',
      id: uid('thread'),
      kind: 'direct',
      participantIds: [this.identity.id, otherId],
      title: person?.name || 'Union participant',
      lastAt: now(),
      unread: 0,
      createdAt: now()
    };
    await this.commit(thread);
    return thread;
  }

  messagesIn(threadId: string): Message[] {
    return this.messages
      .filter((m) => m.threadId === threadId)
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  async markThreadRead(thread: Thread): Promise<void> {
    if (thread.unread === 0) return;
    await this.commit({ ...thread, unread: 0 } as Thread);
    for (const m of this.messages.filter((m) => m.threadId === thread.id && m.to === this.identity?.id && m.status === 'delivered')) {
      await this.commit({ ...m, status: 'read' } as Message);
    }
  }

  private relayAuth(): RelayAuth | null {
    if (!this.identity || !this.keys || !this.relayUrl) return null;
    return { baseUrl: this.relayUrl, identity: this.identity, keys: this.keys };
  }

  async sendMessage(
    threadId: string,
    body: string,
    attachments: Attachment[] = [],
    kind: Message['kind'] = 'text'
  ): Promise<Message> {
    if (!this.identity || !this.keys) throw new Error('Unlock an identity first.');
    const thread = this.byId<Thread>('thread', threadId);
    if (!thread) throw new Error('Thread not found.');
    const otherId = thread.kind === 'notes' ? this.identity.id : thread.participantIds.find((id) => id !== this.identity!.id);
    if (!otherId) throw new Error('Thread has no recipient.');
    const person = await this.findPerson(otherId);
    if (otherId !== this.identity.id) {
      const connection = this.connectionWith(otherId);
      if (!connection || connection.status !== 'accepted') {
        throw new Error('A connection must be accepted before messaging.');
      }
      if (!connection.scopes.includes('message')) {
        throw new Error('The message scope is not granted for this connection.');
      }
    }
    const createdAt = now();
    const message: Message = {
      type: 'message',
      id: uid('msg'),
      threadId,
      from: this.identity.id,
      to: otherId,
      kind,
      body,
      attachments,
      createdAt,
      status: 'queued'
    };

    if (thread.kind === 'notes') {
      message.status = 'sent';
      await this.commit(message);
    } else {
      const recipientXPub = otherId === this.identity.id ? this.identity.xPub : person?.xPub;
      if (!recipientXPub) throw new Error('No verified public key for this person.');
      const recipient = { id: otherId, xPub: recipientXPub };
      const sender = {
        id: this.identity.id,
        edPriv: this.keys.edPriv,
        xPriv: this.keys.xPriv,
        xPub: this.identity.xPub
      };
      const sealed = await sealMessage(
        JSON.stringify({ kind, body, attachments, at: createdAt, card: this.myCard() }),
        sender,
        recipient
      );
      message.envelope = JSON.stringify(sealed);
      await this.commit(message);
      if (this.relayUrl) {
        try {
          const auth = this.relayAuth();
          if (!auth) throw new Error('Relay is not configured.');
          const result = await sendEnvelope(auth, otherId, message.envelope, message.id);
          message.relayId = result.id;
          message.status = 'sent';
          await this.commit(message);
          this.evaluateBadges();
        } catch (error) {
          message.status = 'queued';
          message.error = error instanceof Error ? error.message : String(error);
          await this.commit(message);
          await this.enqueue('message', {
            messageId: message.id,
            to: otherId,
            envelope: message.envelope
          });
        }
      } else {
        await this.commit({ ...message, status: 'queued', error: 'No relay configured; sealed and waiting on this device.' } as Message);
      }
    }

    const updatedThread: Thread = {
      ...thread,
      lastAt: createdAt,
      unread: 0
    };
    await this.commit(updatedThread);
    await this.audit('message.sent', `${kind} · ${threadId}`);
    this.evaluateBadges();
    return message;
  }

  async retryMessage(message: Message): Promise<void> {
    if (!message.envelope) throw new Error('This message has no sealed envelope.');
    const auth = this.relayAuth();
    if (!auth) throw new Error('Configure a relay first.');
    try {
      const result = await sendEnvelope(auth, message.to, message.envelope, message.id);
      await this.commit({ ...message, status: 'sent', relayId: result.id, error: '' } as Message);
      toasts.push('Message delivered to the relay.', 'success');
      this.evaluateBadges();
    } catch (error) {
      const text = error instanceof Error ? error.message : String(error);
      await this.commit({ ...message, status: 'queued', error: text } as Message);
      throw error;
    }
  }

  private async enqueue(job: OutboxJob['job'], payload: Record<string, unknown>): Promise<void> {
    const exists = this.outbox.find(
      (j) => j.job === job && j.status === 'pending' && (j.payload as { messageId?: string }).messageId === payload.messageId
    );
    if (exists) return;
    await this.commit({
      type: 'outbox',
      id: uid('out'),
      job,
      payload,
      tries: 0,
      nextAt: now(),
      status: 'pending'
    } as OutboxJob);
  }

  /* ----------------------------------------------------------------- relay */

  async configureRelay(url: string): Promise<void> {
    const clean = url.trim().replace(/\/+$/, '');
    if (clean && !/^https?:\/\//.test(clean)) throw new Error('Relay URL must start with https://');
    if (clean) {
      const result = await health(clean);
      if (!result.ok) throw new Error('Relay health check failed.');
    }
    await this.setSetting('relay.url', clean);
    if (clean) {
      await this.setSetting('relay.registered', '');
      await this.registerOnRelay();
      await this.sync();
      this.evaluateBadges();
    }
    await this.audit('relay.configured', clean || 'disabled');
  }

  async registerOnRelay(): Promise<void> {
    const auth = this.relayAuth();
    if (!auth) throw new Error('Configure a relay URL first.');
    await register(auth, this.myCard());
    await this.setSetting('relay.registered', now());
    await this.audit('relay.registered', this.relayUrl);
  }

  private startSyncLoop(): void {
    if (this.syncTimer) clearInterval(this.syncTimer);
    this.syncTimer = setInterval(() => {
      if (document.visibilityState === 'visible' && navigator.onLine) void this.sync();
    }, 30000);
  }

  async sync(): Promise<void> {
    if (!this.identity || !this.keys || !this.relayUrl || this.syncing) return;
    if (!navigator.onLine) { this.online = false; return; }
    this.syncing = true;
    this.syncError = '';
    try {
      const auth = this.relayAuth();
      if (!auth) return;
      if (!this.getSetting('relay.registered')) {
        await register(auth, this.myCard());
        await this.setSetting('relay.registered', now());
      }
      const since = this.getSetting('relay.since', '1970-01-01T00:00:00.000Z');
      const incoming = await fetchEnvelopes(auth, since);
      let newest = since;
      const acked: string[] = [];
      for (const envelope of incoming) {
        try {
          await this.receiveEnvelope(envelope);
          acked.push(envelope.id);
          if (envelope.createdAt > newest) newest = envelope.createdAt;
        } catch (error) {
          console.warn('Could not open envelope', envelope.id, error);
        }
      }
      if (acked.length) {
        await ackEnvelopes(auth, acked);
        await this.setSetting('relay.since', newest);
      }
      await this.flushOutbox();
      await this.syncSpaces();
      this.lastSync = now();
      this.syncError = '';
    } catch (error) {
      this.syncError = error instanceof Error ? error.message : String(error);
    } finally {
      this.syncing = false;
    }
  }

  private async receiveEnvelope(envelope: RelayEnvelope): Promise<void> {
    if (!this.identity || !this.keys) return;
    if (envelope.from === this.identity.id && envelope.to !== this.identity.id) return;
    const existing = this.messages.find((m) => m.id === envelope.id || m.relayId === envelope.id);
    if (existing && envelope.from === this.identity.id) return;
    const { openSealed } = await import('./crypto');
    const plain = await openSealed(JSON.parse(envelope.payload) as SealedMessage, this.identity.id, this.keys.xPriv);
    const payload = JSON.parse(plain) as {
      kind: Message['kind'];
      body: string;
      attachments?: Attachment[];
      at?: string;
      card?: UnionCard;
      connectionId?: string;
      note?: string;
      scopes?: ConsentScope[];
    };
    let person = await this.findPerson(envelope.from);
    if (!person && payload.card && verify(payload.card.ed, payload.card.sig ?? '', canonical({ ...payload.card, sig: undefined }))) {
      person = await this.addPerson(payload.card, 'invite');
    }
    if (!person && envelope.from !== this.identity.id) {
      const name = payload.card?.name || 'Union participant';
      person = await this.addPerson(
        {
          v: 1,
          k: 'union-card',
          id: envelope.from,
          handle: payload.card?.handle || 'unknown',
          name,
          about: payload.card?.about || '',
          ed: payload.card?.ed || '',
          x: payload.card?.x || '',
          color: payload.card?.color || colorFor(envelope.from),
          ts: now(),
          sig: ''
        },
        'invite'
      );
    }
    const otherId = envelope.from === this.identity.id ? this.identity.id : envelope.from;
    const thread = await this.ensureThread(otherId);

    if (payload.kind === 'connection_request') {
      const connectionId = payload.connectionId || uid('conn');
      let connection = this.byId<Connection>('connection', connectionId);
      if (!connection) {
        connection = {
          type: 'connection',
          id: connectionId,
          aId: envelope.from,
          bId: this.identity.id,
          status: 'pending',
          scopes: ['message', 'profile'],
          muted: false,
          createdAt: envelope.createdAt,
          note: payload.note
        };
        await this.commit(connection);
      }
      toasts.push(`${person?.name ?? 'Someone'} sent a connection request.`, 'info');
    } else if (payload.kind === 'connection_accept') {
      const connection = this.byId<Connection>('connection', payload.connectionId || '');
      if (connection) {
        await this.commit({
          ...connection,
          status: 'accepted',
          acceptedAt: now(),
          scopes: payload.scopes?.length ? payload.scopes : connection.scopes
        } as Connection);
        toasts.push(`${person?.name ?? 'Your connection'} accepted.`, 'success');
        this.evaluateBadges();
      }
    } else if (payload.kind === 'connection_decline') {
      const connection = this.byId<Connection>('connection', payload.connectionId || '');
      if (connection) await this.commit({ ...connection, status: 'declined' } as Connection);
    }

    const message: Message = {
      type: 'message',
      id: existing?.id || uid('msg'),
      threadId: thread.id,
      from: envelope.from,
      to: envelope.to,
      kind: payload.kind === 'text' || payload.kind === 'attachment' ? payload.kind : (payload.kind as Message['kind']),
      body: payload.body,
      attachments: payload.attachments ?? [],
      createdAt: payload.at || envelope.createdAt,
      status: payload.kind === 'connection_request' ? 'delivered' : 'delivered',
      relayId: envelope.id
    };
    if (existing) await this.commit({ ...existing, status: 'delivered' } as Message);
    else await this.commit(message);
    const isSelf = envelope.from === this.identity.id;
    await this.commit({
      ...thread,
      lastAt: message.createdAt,
      unread: isSelf ? thread.unread : thread.unread + (payload.kind === 'text' || payload.kind === 'attachment' ? 1 : 0)
    } as Thread);
    await this.audit('message.received', envelope.id);
  }

  private async flushOutbox(): Promise<void> {
    const auth = this.relayAuth();
    if (!auth) return;
    for (const job of this.outbox.filter((j) => j.status === 'pending')) {
      if (new Date(job.nextAt).getTime() > Date.now()) continue;
      try {
        if (job.job === 'message') {
          const payload = job.payload as { messageId: string; to: string; envelope: string };
          const result = await sendEnvelope(auth, payload.to, payload.envelope, payload.messageId);
          const message = this.byId<Message>('message', payload.messageId);
          if (message) await this.commit({ ...message, status: 'sent', relayId: result.id, error: '' } as Message);
          await this.commit({ ...job, status: 'done' } as OutboxJob);
        } else if (job.job === 'post') {
          const postId = (job.payload as { postId: string }).postId;
          const post = this.byId<Post>('post', postId);
          if (post) {
            const community = this.byId<Community>('community', post.communityId);
            if (community) await registerSpace(auth, community);
            await publishPost(auth, post);
          }
          await this.commit({ ...job, status: 'done' } as OutboxJob);
        } else {
          await this.commit({ ...job, status: 'done' } as OutboxJob);
        }
      } catch (error) {
        const tries = job.tries + 1;
        const delay = clamp(30 * 2 ** tries, 30, 3600);
        await this.commit({
          ...job,
          tries,
          nextAt: new Date(Date.now() + delay * 1000).toISOString(),
          status: tries > 8 ? 'failed' : 'pending',
          lastError: error instanceof Error ? error.message : String(error)
        } as OutboxJob);
      }
    }
  }

  /* ------------------------------------------------------------ communities */

  async createCommunity(input: { name: string; about: string; visibility?: Community['visibility']; joinPolicy?: Community['joinPolicy']; color?: string }): Promise<Community> {
    if (!this.identity) throw new Error('Create an identity first.');
    const community: Community = {
      type: 'community',
      id: uid('comm'),
      name: input.name.trim(),
      about: input.about.trim(),
      visibility: input.visibility ?? 'public',
      joinPolicy: input.joinPolicy ?? 'open',
      color: input.color ?? colorFor(input.name),
      createdBy: this.identity.id,
      createdAt: now(),
      official: false
    };
    if (!community.name) throw new Error('A community needs a name.');
    const membership: Membership = {
      type: 'membership',
      id: uid('mem'),
      communityId: community.id,
      identityId: this.identity.id,
      role: 'steward',
      joinedAt: now()
    };
    await this.commitMany([community, membership]);
    await this.audit('community.created', community.name);
    this.evaluateBadges();
    if (this.relayUrl && community.visibility === 'public') {
      try {
        const auth = this.relayAuth();
        if (auth) {
          const result = await registerSpace(auth, community);
          await this.commit({ ...community, relayId: result.id } as Community);
        }
      } catch (error) {
        toasts.push(`Community is local; relay registration failed: ${error instanceof Error ? error.message : error}`, 'warn');
      }
    }
    return community;
  }

  async joinCommunity(community: Community): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (this.memberships.some((m) => m.communityId === community.id && m.identityId === this.identity!.id)) return;
    await this.commit({
      type: 'membership',
      id: uid('mem'),
      communityId: community.id,
      identityId: this.identity.id,
      role: 'member',
      joinedAt: now()
    } as Membership);
    await this.audit('community.joined', community.name);
    this.evaluateBadges();
  }

  async leaveCommunity(community: Community): Promise<void> {
    if (!this.identity) return;
    const membership = this.memberships.find(
      (m) => m.communityId === community.id && m.identityId === this.identity!.id
    );
    if (membership) await this.deleteRecord(membership.id);
    await this.audit('community.left', community.name);
  }

  isMember(communityId: string): boolean {
    return this.memberships.some(
      (m) => m.communityId === communityId && m.identityId === this.identity?.id
    );
  }

  postsIn(communityId: string): Post[] {
    return this.all<Post>('post')
      .filter((p) => p.communityId === communityId)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  async addPost(communityId: string, body: string): Promise<Post> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (!this.isMember(communityId)) throw new Error('Join the community before posting.');
    const post: Post = {
      type: 'post',
      id: uid('post'),
      communityId,
      authorId: this.identity.id,
      authorName: this.identity.name,
      body: body.trim(),
      createdAt: now(),
      status: 'local'
    };
    if (!post.body) throw new Error('Write something first.');
    await this.commit(post);
    await this.audit('community.posted', communityId);
    this.evaluateBadges();
    const community = this.byId<Community>('community', communityId);
    if (community?.visibility === 'public' && this.relayUrl) {
      const auth = this.relayAuth();
      if (auth) {
        try {
          // Official and seeded public rooms may not exist on this relay yet.
          await registerSpace(auth, community);
          await publishPost(auth, post);
          await this.commit({ ...post, status: 'synced' } as Post);
        } catch (error) {
          await this.enqueue('post', { postId: post.id });
          toasts.push('Post saved locally; it will sync when the relay is reachable.', 'warn');
        }
      }
    }
    return post;
  }

  async deletePost(post: Post): Promise<void> {
    await this.deleteRecord(post.id);
  }

  private async syncSpaces(): Promise<void> {
    if (!this.relayUrl) return;
    const publicCommunities = this.communities.filter((c) => c.visibility === 'public');
    for (const community of publicCommunities) {
      try {
        const auth = this.relayAuth();
        if (auth) await registerSpace(auth, community);
        const posts = await fetchPosts(this.relayUrl, community.id, '1970-01-01T00:00:00.000Z');
        const known = new Set(this.postsIn(community.id).map((p) => p.id));
        const additions: Post[] = [];
        for (const post of posts) {
          if (known.has(post.id)) continue;
          additions.push({
            type: 'post',
            id: post.id,
            communityId: post.communityId,
            authorId: post.authorId,
            authorName: post.authorName,
            body: post.body,
            createdAt: post.createdAt,
            status: 'synced',
            relayId: post.id
          });
        }
        if (additions.length) await this.commitMany(additions);
      } catch { /* relay may not have this space yet */ }
    }
  }

  communityLink(community: Community): string {
    const token = encodeToken({
      v: 1,
      kind: 'community',
      community: {
        id: community.id,
        name: community.name,
        about: community.about,
        visibility: community.visibility,
        joinPolicy: community.joinPolicy,
        color: community.color,
        createdBy: community.createdBy
      }
    });
    return this.absoluteUrl(`#/u/${token}`);
  }

  /* -------------------------------------------------------------- learning */

  enrollmentFor(courseId: string): Enrollment | undefined {
    return this.enrollments.find((e) => e.courseId === courseId);
  }

  async enroll(course: Course): Promise<Enrollment> {
    if (!this.identity) throw new Error('Create an identity first.');
    const existing = this.enrollmentFor(course.id);
    if (existing) return existing;
    const enrollment: Enrollment = {
      type: 'enrollment',
      id: uid('enr'),
      courseId: course.id,
      identityId: this.identity.id,
      startedAt: now(),
      done: [],
      answers: {}
    };
    await this.commit(enrollment);
    await this.audit('course.started', course.slug);
    return enrollment;
  }

  async completeLesson(course: Course, lessonId: string, answer?: number): Promise<void> {
    if (!this.identity) return;
    let enrollment = this.enrollmentFor(course.id);
    if (!enrollment) enrollment = await this.enroll(course);
    const done = enrollment.done.includes(lessonId) ? enrollment.done : [...enrollment.done, lessonId];
    const answers = { ...enrollment.answers };
    if (answer !== undefined) answers[lessonId] = answer;
    const completed = course.lessons.every((l) => done.includes(l.id));
    const updated: Enrollment = {
      ...enrollment,
      done,
      answers,
      completedAt: completed ? enrollment.completedAt || now() : undefined
    };
    await this.commit(updated);
    await this.audit('lesson.completed', `${course.slug}/${lessonId}`);
    if (completed && !enrollment.completedAt) {
      toasts.push(`Course complete: ${course.title}`, 'badge');
    }
    this.evaluateBadges();
  }

  courseProgress(course: Course): number {
    const enrollment = this.enrollmentFor(course.id);
    return percent(enrollment?.done.length ?? 0, course.lessons.length);
  }

  /* ----------------------------------------------------------- experiences */

  async createEvent(input: { title: string; about: string; start: string; end: string; mode: Event['mode']; place: string; capacity: number }): Promise<Event> {
    if (!this.identity) throw new Error('Create an identity first.');
    const event: Event = {
      type: 'event',
      id: uid('event'),
      title: input.title.trim(),
      about: input.about.trim(),
      start: input.start,
      end: input.end,
      mode: input.mode,
      place: input.place,
      capacity: input.capacity,
      hostId: this.identity.id,
      hostName: this.identity.name,
      color: colorFor(input.title),
      createdAt: now(),
      official: false
    };
    if (!event.title) throw new Error('An experience needs a title.');
    await this.commit(event);
    await this.audit('event.created', event.title);
    return event;
  }

  rsvpFor(eventId: string): Rsvp | undefined {
    return this.rsvps.find((r) => r.eventId === eventId && r.identityId === this.identity?.id);
  }

  goingCount(eventId: string): number {
    return this.rsvps.filter((r) => r.eventId === eventId && r.status === 'going').length;
  }

  async setRsvp(event: Event, status: Rsvp['status']): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    const existing = this.rsvpFor(event.id);
    if (existing) {
      await this.commit({ ...existing, status, at: now() } as Rsvp);
    } else {
      await this.commit({
        type: 'rsvp',
        id: uid('rsvp'),
        eventId: event.id,
        identityId: this.identity.id,
        status,
        at: now()
      } as Rsvp);
    }
    await this.audit('event.rsvp', `${event.title}: ${status}`);
    this.evaluateBadges();
  }

  eventLink(event: Event): string {
    const token = encodeToken({
      v: 1,
      kind: 'event',
      event: {
        id: event.id,
        title: event.title,
        about: event.about,
        start: event.start,
        end: event.end,
        mode: event.mode,
        place: event.place,
        hostName: event.hostName
      }
    });
    return this.absoluteUrl(`#/u/${token}`);
  }

  /* -------------------------------------------------------------- presence */

  async savePage(input: { id?: string; title: string; tagline: string; about: string; sections: Page['sections']; links: Page['links']; theme: Page['theme']; published?: boolean }): Promise<Page> {
    if (!this.identity) throw new Error('Create an identity first.');
    const existing = input.id ? this.byId<Page>('page', input.id) : undefined;
    const page: Page = {
      type: 'page',
      id: existing?.id ?? uid('page'),
      ownerId: this.identity.id,
      slug: existing?.slug ?? `${this.identity.handle}-${Math.random().toString(36).slice(2, 6)}`,
      title: input.title.trim(),
      tagline: input.tagline.trim(),
      about: input.about.trim(),
      sections: input.sections.filter((s) => s.title.trim() || s.body.trim()),
      links: input.links.filter((l) => l.label.trim() && l.url.trim()),
      theme: input.theme,
      published: input.published ?? existing?.published ?? false,
      createdAt: existing?.createdAt ?? now(),
      updatedAt: now()
    };
    if (!page.title) throw new Error('Give the page a title.');
    await this.commit(page);
    await this.audit('page.saved', page.title);
    if (page.published) this.evaluateBadges();
    return page;
  }

  async publishPage(page: Page): Promise<void> {
    await this.commit({ ...page, published: true, updatedAt: now() } as Page);
    await this.audit('page.published', page.title);
    this.evaluateBadges();
  }

  async unpublishPage(page: Page): Promise<void> {
    await this.commit({ ...page, published: false, updatedAt: now() } as Page);
  }

  async deletePage(page: Page): Promise<void> {
    await this.deleteRecord(page.id);
  }

  pageLink(page: Page): string {
    const token = encodeToken({
      v: 1,
      kind: 'page',
      page: {
        id: page.id,
        title: page.title,
        tagline: page.tagline,
        about: page.about,
        sections: page.sections,
        links: page.links,
        theme: page.theme,
        author: this.identity?.name ?? '',
        handle: this.identity?.handle ?? '',
        updatedAt: page.updatedAt
      }
    });
    return this.absoluteUrl(`#/u/${token}`);
  }

  absoluteUrl(hashPath: string): string {
    const base = `${location.origin}${location.pathname}${location.search}`.split('#')[0];
    return `${base}${hashPath}`;
  }

  /* ----------------------------------------------------------------- links */

  async createLink(input: {
    kind: UnionLink['kind'];
    title: string;
    subtitle?: string;
    payload: Record<string, unknown>;
    permissions?: string[];
    expiresAt?: string;
  }): Promise<UnionLink> {
    if (!this.identity || !this.keys) throw new Error('Create an identity first.');
    const link: UnionLink = {
      type: 'link',
      id: uid('link'),
      kind: input.kind,
      title: input.title,
      subtitle: input.subtitle ?? '',
      payload: input.payload,
      permissions: input.permissions ?? ['read'],
      createdBy: this.identity.id,
      createdAt: now(),
      expiresAt: input.expiresAt
    };
    const signed = { ...link, sig: sign(this.keys, canonical({ ...link, token: undefined, sig: undefined })) };
    const token = encodeToken(signed);
    link.token = token;
    await this.commit(link);
    await this.audit('link.created', `${link.kind}: ${link.title}`);
    this.evaluateBadges();
    if (this.relayUrl) {
      try {
        const auth = this.relayAuth();
        if (auth) {
          const result = await publishLink(auth, link);
          await this.commit({ ...link, relayId: result.id } as UnionLink);
        }
      } catch { /* the self-contained token still works */ }
    }
    return link;
  }

  linkUrl(link: UnionLink): string {
    if (link.revokedAt) return '';
    if (link.token && link.token.length < 1800) return this.absoluteUrl(`#/u/${link.token}`);
    if (link.relayId) return this.absoluteUrl(`#/l/${link.id}`);
    return this.absoluteUrl(`#/l/${link.id}`);
  }

  async revokeLink(link: UnionLink): Promise<void> {
    await this.commit({ ...link, revokedAt: now() } as UnionLink);
    await this.audit('link.revoked', link.title);
    if (link.relayId && this.relayUrl) {
      const auth = this.relayAuth();
      if (auth) {
        try { await relayRevokeLink(auth, link.id); } catch { /* local revocation still stands */ }
      }
    }
  }

  async resolveRelayLink(id: string) {
    if (!this.relayUrl) throw new Error('No relay is configured to resolve this link.');
    return resolveLink(this.relayUrl, id);
  }

  /* -------------------------------------------------------------------- AI */

  async createAiThread(title = 'New conversation', projectId?: string): Promise<AiThread> {
    if (!this.identity) throw new Error('Create an identity first.');
    const thread: AiThread = {
      type: 'aiThread',
      id: uid('aith'),
      title,
      projectId,
      createdAt: now(),
      updatedAt: now()
    };
    await this.commit(thread);
    return thread;
  }

  aiMessagesIn(threadId: string): AiMessage[] {
    return this.aiMessages
      .filter((m) => m.threadId === threadId)
      .sort((a, b) => a.at.localeCompare(b.at));
  }

  async sendAi(threadId: string, text: string): Promise<AiMessage> {
    if (!this.identity) throw new Error('Create an identity first.');
    const thread = this.byId<AiThread>('aiThread', threadId);
    if (!thread) throw new Error('Conversation not found.');
    const user: AiMessage = {
      type: 'aiMessage',
      id: uid('aim'),
      threadId,
      role: 'user',
      text,
      at: now()
    };
    await this.commit(user);
    const reply = await askUnionAi({
      text,
      thread,
      identity: this.identity,
      records: this.records,
      baseUrl: this.getSetting('ai.baseUrl'),
      apiKey: this.getSecretSetting('ai.apiKey'),
      model: this.getSetting('ai.model', 'qwen3.5-0.8b'),
      memories: this.memories.map((m) => m.text)
    });
    const assistant: AiMessage = {
      type: 'aiMessage',
      id: uid('aim'),
      threadId,
      role: 'assistant',
      text: reply.text,
      at: now(),
      provider: reply.provider,
      action: reply.action
        ? { ...reply.action, state: 'proposed' }
        : undefined
    };
    await this.commit(assistant);
    await this.commit({ ...thread, updatedAt: now(), title: thread.title === 'New conversation' ? text.slice(0, 42) : thread.title } as AiThread);
    await this.audit('ai.reply', reply.provider);
    return assistant;
  }

  async resolveAiAction(message: AiMessage, approve: boolean): Promise<void> {
    if (!message.action) return;
    const action: AiAction = { ...message.action, state: approve ? 'approved' : 'declined' };
    await this.commit({ ...message, action } as AiMessage);
    if (!approve) {
      await this.audit('ai.action.declined', action.tool);
      toasts.push('Action declined. Nothing changed.', 'info');
      return;
    }
    try {
      const summary = await this.executeAiAction(action);
      await this.commit({ ...message, action: { ...action, state: 'done' } } as AiMessage);
      await this.audit('ai.action.done', `${action.tool}: ${summary}`);
      toasts.push(summary, 'success');
      this.evaluateBadges();
    } catch (error) {
      await this.commit({ ...message, action: { ...action, state: 'failed' } } as AiMessage);
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    }
  }

  private async executeAiAction(action: AiAction): Promise<string> {
    const args = action.args as Record<string, string | number>;
    switch (action.tool) {
      case 'create_community': {
        const community = await this.createCommunity({
          name: String(args.name ?? 'New community'),
          about: String(args.about ?? '')
        });
        return `Created community “${community.name}”.`;
      }
      case 'schedule_event': {
        const start = String(args.start ?? addDays(now(), 7));
        const event = await this.createEvent({
          title: String(args.title ?? 'New experience'),
          about: String(args.about ?? ''),
          start,
          end: addDays(start, 1 / 24),
          mode: (args.mode as Event['mode']) ?? 'online',
          place: String(args.place ?? 'To be confirmed'),
          capacity: Number(args.capacity ?? 20)
        });
        return `Scheduled “${event.title}”.`;
      }
      case 'add_memory': {
        const memory = {
          type: 'aiMemory' as const,
          id: uid('mem'),
          identityId: this.identity!.id,
          text: String(args.text ?? ''),
          source: 'Union AI (approved)',
          createdAt: now()
        };
        await this.commit(memory);
        return 'Memory saved with your approval.';
      }
      case 'draft_page': {
        const page = await this.savePage({
          title: String(args.title ?? `${this.identity?.name}'s page`),
          tagline: String(args.tagline ?? ''),
          about: String(args.about ?? ''),
          sections: Array.isArray(args.sections) ? (args.sections as Page['sections']) : [],
          links: [],
          theme: 'calm'
        });
        return `Drafted page “${page.title}”. Review it in Presence.`;
      }
      default:
        throw new Error(`Unknown action: ${action.tool}`);
    }
  }

  async addMemory(text: string): Promise<void> {
    if (!this.identity) return;
    await this.commit({
      type: 'aiMemory',
      id: uid('mem'),
      identityId: this.identity.id,
      text: text.trim(),
      source: 'You',
      createdAt: now()
    });
    this.evaluateBadges();
  }

  async deleteMemory(id: string): Promise<void> {
    await this.deleteRecord(id);
  }

  async createProject(name: string, about: string): Promise<void> {
    if (!this.identity) return;
    await this.commit({ type: 'aiProject', id: uid('proj'), name, about, createdAt: now() } as AiProject);
  }

  /* --------------------------------------------------------------- economy */

  private async ledgerHash(entry: Omit<LedgerEntry, 'hash' | 'prevHash'>, prevHash: string): Promise<string> {
    return hashValue({ ...entry, prevHash });
  }

  async addLedger(input: LedgerInput): Promise<LedgerEntry> {
    const prevHash = this.ledger.at(-1)?.hash ?? '0'.repeat(64);
    const base: Omit<LedgerEntry, 'hash' | 'prevHash'> = {
      type: 'ledger',
      id: uid('led'),
      kind: input.kind,
      from: input.from,
      to: input.to,
      token: input.token,
      amount: input.amount,
      currency: input.currency,
      amountUsd: input.amountUsd,
      memo: input.memo,
      at: now(),
      testMode: true
    };
    const hash = await this.ledgerHash(base, prevHash);
    const entry: LedgerEntry = { ...base, hash, prevHash };
    await this.commit(entry);
    await this.audit('ledger.entry', `${input.kind}: ${input.amount} ${input.token}`);
    this.evaluateBadges();
    return entry;
  }

  private amountFor(usd: number): { amount: number; currency: 'USD' | 'NGN' } {
    return this.currency === 'NGN'
      ? { amount: Math.round(usd * this.fxRate), currency: 'NGN' }
      : { amount: usd, currency: 'USD' };
  }

  /** Step 1 — request activation. Starts the 2-week trial while proof is reviewed. */
  async requestActivation(input: {
    experience: Identity['experience'];
    method: string;
    reference?: string;
    note?: string;
  }): Promise<ActivationRequest> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (this.activationVerified) throw new Error('This identity is already activated.');
    if (!input.method.trim()) throw new Error('Choose how the Activation Token was sent.');
    const open = this.currentActivationRequest;
    if (open && (open.status === 'requested' || open.status === 'proof-submitted')) {
      throw new Error('An activation request is already open. Add or replace its proof.');
    }
    const at = now();
    const trialUntil = addDays(at, 14);
    const request: ActivationRequest = {
      type: 'activationRequest',
      id: uid('actreq'),
      identityId: this.identity.id,
      at,
      experience: input.experience,
      amountUsd: 80,
      currency: this.currency,
      method: input.method.trim(),
      reference: (input.reference ?? '').trim(),
      note: (input.note ?? '').trim(),
      status: 'requested',
      trialUntil
    };
    const identity: Identity = {
      ...this.identity,
      experience: input.experience,
      activation: { status: 'trial', since: at, until: trialUntil, memo: input.method.trim() },
      updatedAt: at
    };
    await this.commit(identity);
    this.identity = identity;
    await this.commit(request);
    await this.audit('activation.requested', `${input.method.trim()}${input.reference ? ` · ${input.reference}` : ''}`);
    toasts.push('Activation request recorded. A 14-day trial is open while proof is reviewed.', 'success');
    return request;
  }

  /** Step 2 — attach the receipt / screenshot / text proof. */
  async attachActivationProof(requestId: string, proof: ActivationProof): Promise<ActivationRequest> {
    const request = this.byId<ActivationRequest>('activationRequest', requestId);
    if (!request) throw new Error('Activation request not found.');
    const updated: ActivationRequest = { ...request, status: 'proof-submitted', proof };
    await this.commit(updated);
    await this.audit('activation.proof.attached', `${proof.name} · ${proof.hash.slice(0, 20)}`);
    toasts.push('Proof attached. Send it to Union Assistant for verification.', 'success');
    return updated;
  }

  /** Called after the creator / assistant verifies the request. */
  async verifyActivationRequest(requestId: string, method: string, reference: string): Promise<void> {
    const request = this.byId<ActivationRequest>('activationRequest', requestId);
    if (!request) throw new Error('Activation request not found.');
    if (!request.proof) throw new Error('Attach an Activation proof before verification.');
    await this.activate(request.experience, method, reference, requestId);
  }

  async activate(experience: Identity['experience'], method: string, reference: string, requestId?: string): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (this.activation.status === 'active' || this.activation.status === 'grace') {
      throw new Error('This identity is already activated.');
    }
    if (!method.trim()) throw new Error('Record which Digital Giving Method was used.');
    const { amount, currency } = this.amountFor(80);
    const entry = await this.addLedger({
      kind: 'activation',
      from: this.identity.id,
      to: 'union',
      token: 'ACT',
      amount,
      currency,
      amountUsd: 80,
      memo: `Activation Token · ${method}${reference ? ` · ${reference}` : ''}`
    });
    const since = now();
    const until = addDays(since, 30);
    const identity: Identity = {
      ...this.identity,
      experience,
      activation: { status: 'active', since, until, tokenId: entry.id, memo: method },
      updatedAt: now()
    };
    await this.commit(identity);
    this.identity = identity;
    await this.commit({
      type: 'plan',
      id: uid('plan'),
      identityId: identity.id,
      name: 'Activation Token',
      kind: 'activation',
      status: 'active',
      priceUsd: 80,
      period: 'once',
      startedAt: since,
      until,
      createdAt: since,
      terms: 'Minimum $80 equivalent (Naira equivalent acceptable). One-time activation. Reactivation Token (Plan) is $10/month equivalent after the first 30 days.'
    } as Plan);
    await this.audit('activation.recorded', `${currency} ${amount}`);
    const verifiedRequest = requestId
      ? this.byId<ActivationRequest>('activationRequest', requestId)
      : this.currentActivationRequest;
    if (verifiedRequest) {
      await this.commit({
        ...verifiedRequest,
        status: 'verified',
        verifiedAt: since,
        verifiedBy: 'Union Creator · OriginBot'
      } as ActivationRequest);
    }
    toasts.push('Activation verified. v0.0.1 records it in test mode — no funds moved inside the app.', 'success');
  }

  async reactivate(method: string, reference: string): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (this.activation.status === 'inactive') throw new Error('Request and verify an Activation Token first.');
    if (this.activation.status === 'trial') throw new Error('Wait for the Activation proof to be verified before recording a Reactivation Token.');
    const { amount, currency } = this.amountFor(10);
    const base = this.activation.until && new Date(this.activation.until) > new Date()
      ? this.activation.until
      : now();
    const until = addDays(base, 30);
    await this.addLedger({
      kind: 'reactivation',
      from: this.identity.id,
      to: 'union',
      token: 'USD',
      amount,
      currency,
      amountUsd: 10,
      memo: `Reactivation Token (Plan) · ${method}${reference ? ` · ${reference}` : ''}`
    });
    const identity: Identity = {
      ...this.identity,
      activation: { ...this.identity.activation, status: 'active', until },
      updatedAt: now()
    };
    await this.commit(identity);
    this.identity = identity;
    await this.commit({
      type: 'plan',
      id: uid('plan'),
      identityId: identity.id,
      name: 'Reactivation Token (Plan)',
      kind: 'reactivation',
      status: 'active',
      priceUsd: 10,
      period: 'month',
      startedAt: now(),
      until,
      createdAt: now(),
      terms: 'Minimum $10/month equivalent (Naira equivalent acceptable), after either a Special or Super experience. Extends activation for 30 days.'
    } as Plan);
    await this.audit('reactivation.recorded', `${currency} ${amount}`);
    toasts.push('Reactivation recorded in test mode. No funds moved.', 'success');
  }

  async startJoyTrial(): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (this.joyPlan) throw new Error('A Joy/Giving Plan is already in trial or active.');
    const startedAt = now();
    const until = addDays(startedAt, 14);
    await this.commit({
      type: 'plan',
      id: uid('plan'),
      identityId: this.identity.id,
      name: 'Joy/Giving Plans',
      kind: 'joy',
      status: 'trial',
      priceUsd: 1,
      period: 'optional',
      startedAt,
      until,
      createdAt: startedAt,
      terms: 'Optional plan with a $1 minimum token for acknowledging the Creator of the Union concept. Requested with the courtesy signal “Joyful Giver” at t.me/CollinsRoyalMedium. 2-week trial; no automatic charge.'
    } as Plan);
    await this.setSetting('joy.requested', '');
    await this.audit('joy.trial.started', until);
    toasts.push('Joy/Giving Plans trial started for 14 days.', 'success');
    this.evaluateBadges();
  }

  async endJoyPlan(): Promise<void> {
    const plan = this.joyPlan;
    if (!plan) return;
    await this.commit({ ...plan, status: 'cancelled' } as Plan);
    await this.audit('joy.cancelled', plan.id);
  }

  markJoyRequested(): void {
    void this.setSetting('joy.requested', now());
  }

  async recordGift(amountUsd: number, method: string, reference: string, memo: string): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (!Number.isFinite(amountUsd) || amountUsd <= 0) throw new Error('Enter an amount greater than zero.');
    const { amount, currency } = this.amountFor(amountUsd);
    await this.addLedger({
      kind: 'gift',
      from: this.identity.id,
      to: 'union',
      token: 'USD',
      amount,
      currency,
      amountUsd,
      memo: `${memo || 'Digital Giving'} · ${method}${reference ? ` · ${reference}` : ''}`
    });
    toasts.push('Giving record written in test mode. No funds moved.', 'success');
  }

  joyBalance(identityId: string): number {
    return this.ledger
      .filter((l) => l.token === 'JOY')
      .reduce((sum, l) => {
        if (l.to === identityId) return sum + l.amount;
        if (l.from === identityId) return sum - l.amount;
        return sum;
      }, 0);
  }

  async giveJoy(toIdentityId: string, amount: number, memo: string): Promise<void> {
    if (!this.identity) throw new Error('Create an identity first.');
    if (amount <= 0) throw new Error('Choose a positive amount.');
    if (this.joyBalance(this.identity.id) < amount) throw new Error('Not enough Joy tokens for this appreciation.');
    await this.addLedger({
      kind: 'transfer',
      from: this.identity.id,
      to: toIdentityId,
      token: 'JOY',
      amount,
      currency: 'USD',
      amountUsd: 0,
      memo: memo || 'Joy appreciation'
    });
    toasts.push('Joy appreciation recorded.', 'success');
  }

  async verifyLedger(): Promise<{ ok: boolean; brokenAt?: string; count: number }> {
    let prev = '0'.repeat(64);
    const entries = this.ledger;
    for (const entry of entries) {
      if (entry.prevHash !== prev) return { ok: false, brokenAt: entry.id, count: entries.length };
      const expected = await this.ledgerHash(
        {
          type: entry.type,
          id: entry.id,
          kind: entry.kind,
          from: entry.from,
          to: entry.to,
          token: entry.token,
          amount: entry.amount,
          currency: entry.currency,
          amountUsd: entry.amountUsd,
          memo: entry.memo,
          at: entry.at,
          testMode: entry.testMode
        },
        prev
      );
      if (expected !== entry.hash) return { ok: false, brokenAt: entry.id, count: entries.length };
      prev = entry.hash;
    }
    return { ok: true, count: entries.length };
  }

  /* ------------------------------------------------------------ mentorship */

  mentorshipWith(person: Person): Mentorship | undefined {
    if (!this.identity) return undefined;
    return this.mentorships.find(
      (m) =>
        (m.mentorId === this.identity!.id && m.menteeId === person.identityId) ||
        (m.menteeId === this.identity!.id && m.mentorId === person.identityId)
    );
  }

  async requestMentorship(person: Person, focus: string, cadence: string, asMentor: boolean): Promise<Mentorship> {
    if (!this.identity) throw new Error('Create an identity first.');
    const existing = this.mentorshipWith(person);
    if (existing) return existing;
    const mentorship: Mentorship = {
      type: 'mentorship',
      id: uid('mentor'),
      mentorId: asMentor ? this.identity.id : person.identityId,
      menteeId: asMentor ? person.identityId : this.identity.id,
      focus,
      cadence,
      status: 'requested',
      createdAt: now(),
      notes: []
    };
    await this.commit(mentorship);
    await this.audit('mentorship.requested', person.handle);
    this.evaluateBadges();
    return mentorship;
  }

  async setMentorshipStatus(mentorship: Mentorship, status: Mentorship['status']): Promise<void> {
    await this.commit({ ...mentorship, status } as Mentorship);
    await this.audit('mentorship.status', `${mentorship.id}: ${status}`);
  }

  async addStewardshipNote(mentorship: Mentorship, text: string): Promise<void> {
    const notes = [...mentorship.notes, { at: now(), text: text.trim() }];
    await this.commit({ ...mentorship, notes } as Mentorship);
    await this.audit('mentorship.note', mentorship.id);
    this.evaluateBadges();
  }

  /* ------------------------------------------------------------- operations */

  async search(query: string): Promise<{ kind: string; id: string; title: string; subtitle: string; route: string }[]> {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    const results: { kind: string; id: string; title: string; subtitle: string; route: string }[] = [];
    for (const p of this.people) {
      if (`${p.name} ${p.handle}`.toLowerCase().includes(q)) results.push({ kind: 'Person', id: p.id, title: p.name, subtitle: `@${p.handle}`, route: `#/people/${p.id}` });
    }
    for (const t of this.threads) {
      if (t.title.toLowerCase().includes(q)) results.push({ kind: 'Conversation', id: t.id, title: t.title, subtitle: 'Messages', route: `#/messages/${t.id}` });
    }
    for (const c of this.communities) {
      if (`${c.name} ${c.about}`.toLowerCase().includes(q)) results.push({ kind: 'Community', id: c.id, title: c.name, subtitle: c.about, route: `#/communities/${c.id}` });
    }
    for (const c of this.courses) {
      if (`${c.title} ${c.subtitle}`.toLowerCase().includes(q)) results.push({ kind: 'Course', id: c.id, title: c.title, subtitle: c.subtitle, route: `#/learning/${c.slug}` });
    }
    for (const e of this.events) {
      if (`${e.title} ${e.about}`.toLowerCase().includes(q)) results.push({ kind: 'Experience', id: e.id, title: e.title, subtitle: e.place, route: `#/experiences/${e.id}` });
    }
    for (const p of this.pages) {
      if (`${p.title} ${p.tagline}`.toLowerCase().includes(q)) results.push({ kind: 'Page', id: p.id, title: p.title, subtitle: p.tagline, route: `#/presence/${p.id}` });
    }
    return results.slice(0, 30);
  }

  /* ---------------------------------------------------------- import/export */

  async exportFull(passphrase: string, includeKeys: boolean): Promise<string> {
    const payload = {
      schema: 'union.backup.v1',
      version: '0.0.1',
      exportedAt: now(),
      identity: this.identity,
      keyring: includeKeys ? this.keys : undefined,
      records: this.records
    };
    return encryptBackup(payload, passphrase);
  }

  async exportRecovery(passphrase: string): Promise<string> {
    return this.exportFull(passphrase, true);
  }

  async importBackup(text: string, passphrase: string, mode: 'replace' | 'merge'): Promise<void> {
    const data = await decryptBackup<{
      schema: string;
      identity?: Identity;
      keyring?: Keyring;
      records?: UnionRecord[];
    }>(text, passphrase);
    if (data.schema !== 'union.backup.v1' || !data.records) throw new Error('This is not a Union backup.');
    if (mode === 'replace') {
      await replaceAll(data.records);
      this.records = data.records;
      if (data.identity) {
        this.identity = this.normaliseIdentity(data.identity);
        if (data.keyring) {
          this.keys = data.keyring;
          this.saveKeyring(data.keyring);
        }
      } else {
        this.identity = null;
        this.keys = null;
      }
    } else {
      await this.commitMany(data.records);
      if (!this.identity && data.identity) {
        this.identity = this.normaliseIdentity(data.identity);
        if (data.keyring) {
          this.keys = data.keyring;
          this.saveKeyring(data.keyring);
        }
      }
    }
    await this.ensureSeed();
    await this.audit('backup.imported', mode);
    toasts.push('Backup imported.', 'success');
  }

  async wipe(): Promise<void> {
    await clearAll();
    secretDelete(KEYRING_KEY);
    this.records = [];
    this.identity = null;
    this.keys = null;
  }

  /* ------------------------------------------------------------------ misc */

  installAvailable(): void {
    this.installPrompt = true;
  }

  setDeferredPrompt(event: Event): void {
    this.deferredPrompt = event;
  }

  async promptInstall(): Promise<void> {
    const prompt = this.deferredPrompt as (Event & { prompt?: () => Promise<void> }) | null;
    if (!prompt?.prompt) return;
    await prompt.prompt();
    this.deferredPrompt = null;
    this.installPrompt = false;
  }

  dismissInstall(): void {
    this.installPrompt = false;
    void this.setSetting('install.dismissed', '1');
  }

  get relayReady(): boolean {
    return Boolean(this.relayUrl && this.getSetting('relay.registered'));
  }

  get coreLabel(): string {
    return this.core.backend === 'rust-wasm' ? 'Rust core · WebAssembly' : 'JavaScript fallback';
  }
}

export const union = new UnionStore();

export { DEFAULT_FX, UNION_COMMONS_ID };
export type { UnionRecord };
