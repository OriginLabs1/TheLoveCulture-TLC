/** Union v0.0.1 domain types. Versioned, portable, JSON-friendly. */

export type Experience = 'special' | 'super';
export type Currency = 'USD' | 'NGN';
export type TokenSymbol = 'USD' | 'NGN' | 'JOY' | 'ACT';

export interface ActivationState {
  status: 'inactive' | 'active' | 'trial' | 'grace' | 'expired';
  since?: string;
  until?: string;
  tokenId?: string;
  memo?: string;
}

export type ActivationRequestStatus = 'requested' | 'proof-submitted' | 'verified' | 'rejected';

export interface ActivationProof {
  name: string;
  type: string;
  size: number;
  /** SHA-256 of the proof bytes or text, so a reviewer can match the exact file. */
  hash: string;
  /** Short text / markdown proof is kept inline; images keep a bounded preview. */
  text?: string;
  dataUrl?: string;
  at: string;
}

export interface ActivationRequest {
  type: 'activationRequest';
  id: string;
  identityId: string;
  at: string;
  experience: Experience;
  amountUsd: number;
  currency: Currency;
  method: string;
  reference: string;
  note: string;
  status: ActivationRequestStatus;
  proof?: ActivationProof;
  trialUntil?: string;
  verifiedAt?: string;
  verifiedBy?: string;
}

export interface Identity {
  type: 'identity';
  id: string;
  handle: string;
  name: string;
  about: string;
  edPub: string;
  xPub: string;
  color: string;
  createdAt: string;
  updatedAt: string;
  experience: Experience;
  activation: ActivationState;
}

export interface Keyring {
  type: 'keyring';
  id: string;
  identityId: string;
  edPriv: string;
  xPriv: string;
  createdAt: string;
}

export interface Person {
  type: 'person';
  id: string;
  identityId: string;
  handle: string;
  name: string;
  about: string;
  edPub: string;
  xPub: string;
  color: string;
  addedAt: string;
  source: 'card' | 'directory' | 'invite' | 'self';
}

export type ConnectionStatus = 'pending' | 'accepted' | 'declined' | 'blocked';
export type ConsentScope = 'message' | 'profile' | 'presence' | 'mentorship';

export interface Connection {
  type: 'connection';
  id: string;
  aId: string;
  bId: string;
  status: ConnectionStatus;
  scopes: ConsentScope[];
  muted: boolean;
  createdAt: string;
  acceptedAt?: string;
  note?: string;
}

export interface Thread {
  type: 'thread';
  id: string;
  kind: 'direct' | 'notes';
  participantIds: string[];
  title: string;
  lastAt: string;
  unread: number;
  createdAt: string;
}

export type MessageKind =
  | 'text'
  | 'attachment'
  | 'connection_request'
  | 'connection_accept'
  | 'connection_decline'
  | 'system';

export interface Attachment {
  name: string;
  mime: string;
  size: number;
  /** base64 ciphertext, encrypted with the message key */
  data: string;
}

export interface Message {
  type: 'message';
  id: string;
  threadId: string;
  from: string;
  to: string;
  kind: MessageKind;
  body: string;
  attachments: Attachment[];
  createdAt: string;
  status: 'queued' | 'sent' | 'delivered' | 'read' | 'failed';
  envelope?: string;
  relayId?: string;
  error?: string;
}

export interface Community {
  type: 'community';
  id: string;
  name: string;
  about: string;
  visibility: 'public' | 'private';
  joinPolicy: 'open' | 'request' | 'invite';
  color: string;
  createdBy: string;
  createdAt: string;
  official: boolean;
  relayId?: string;
}

export interface Membership {
  type: 'membership';
  id: string;
  communityId: string;
  identityId: string;
  role: 'steward' | 'host' | 'member';
  joinedAt: string;
}

export interface Post {
  type: 'post';
  id: string;
  communityId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
  status: 'local' | 'synced' | 'failed';
  relayId?: string;
}

export interface Lesson {
  id: string;
  title: string;
  minutes: number;
  body: string;
  practice?: string;
  quiz?: { question: string; options: string[]; answer: number; why: string };
}

export interface Course {
  type: 'course';
  id: string;
  slug: string;
  title: string;
  subtitle: string;
  about: string;
  level: 'foundation' | 'practical' | 'advanced';
  minutes: number;
  color: string;
  official: boolean;
  lessons: Lesson[];
}

export interface Enrollment {
  type: 'enrollment';
  id: string;
  courseId: string;
  identityId: string;
  startedAt: string;
  completedAt?: string;
  done: string[];
  answers: Record<string, number>;
}

export interface Event {
  type: 'event';
  id: string;
  title: string;
  about: string;
  start: string;
  end: string;
  mode: 'online' | 'in-person' | 'hybrid';
  place: string;
  capacity: number;
  hostId: string;
  hostName: string;
  color: string;
  createdAt: string;
  official: boolean;
}

export interface Rsvp {
  type: 'rsvp';
  id: string;
  eventId: string;
  identityId: string;
  status: 'going' | 'interested' | 'declined';
  at: string;
}

export interface PageSection {
  title: string;
  body: string;
}

export interface PresenceLink {
  label: string;
  url: string;
}

export interface Page {
  type: 'page';
  id: string;
  ownerId: string;
  slug: string;
  title: string;
  tagline: string;
  about: string;
  sections: PageSection[];
  links: PresenceLink[];
  theme: 'calm' | 'warm' | 'forest' | 'midnight';
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export type LinkKind =
  | 'identity'
  | 'invite'
  | 'connection'
  | 'message'
  | 'community'
  | 'course'
  | 'lesson'
  | 'event'
  | 'page'
  | 'giving'
  | 'agent'
  | 'external';

export interface UnionLink {
  type: 'link';
  id: string;
  kind: LinkKind;
  title: string;
  subtitle: string;
  payload: Record<string, unknown>;
  permissions: string[];
  createdBy: string;
  createdAt: string;
  expiresAt?: string;
  revokedAt?: string;
  /** compact self-contained token for use outside Union */
  token?: string;
  relayId?: string;
}

export interface AiThread {
  type: 'aiThread';
  id: string;
  title: string;
  projectId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiAction {
  tool: string;
  args: Record<string, unknown>;
  description: string;
  state: 'proposed' | 'approved' | 'declined' | 'done' | 'failed';
}

export interface AiMessage {
  type: 'aiMessage';
  id: string;
  threadId: string;
  role: 'user' | 'assistant' | 'system';
  text: string;
  at: string;
  action?: AiAction;
  provider?: string;
}

export interface AiMemory {
  type: 'aiMemory';
  id: string;
  identityId: string;
  text: string;
  source: string;
  createdAt: string;
}

export interface AiProject {
  type: 'aiProject';
  id: string;
  name: string;
  about: string;
  createdAt: string;
}

export type LedgerKind =
  | 'activation'
  | 'reactivation'
  | 'joy'
  | 'gift'
  | 'transfer'
  | 'grant'
  | 'trial';

export interface LedgerEntry {
  type: 'ledger';
  id: string;
  kind: LedgerKind;
  from?: string;
  to?: string;
  token: TokenSymbol;
  amount: number;
  currency: Currency;
  amountUsd: number;
  memo: string;
  at: string;
  hash: string;
  prevHash: string;
  /** only for transparency; no real funds move in v0.0.1 */
  testMode: boolean;
}

export interface Plan {
  type: 'plan';
  id: string;
  identityId: string;
  name: 'Activation Token' | 'Reactivation Token (Plan)' | 'Joy/Giving Plans';
  kind: 'activation' | 'reactivation' | 'joy';
  status: 'inactive' | 'trial' | 'active' | 'expired' | 'cancelled';
  priceUsd: number;
  period: 'once' | 'month' | 'optional';
  startedAt?: string;
  until?: string;
  createdAt: string;
  terms: string;
}

export interface BadgeAward {
  type: 'badgeAward';
  id: string;
  badgeId: string;
  identityId: string;
  awardedAt: string;
  note?: string;
}

export interface Mentorship {
  type: 'mentorship';
  id: string;
  mentorId: string;
  menteeId: string;
  focus: string;
  cadence: string;
  status: 'requested' | 'active' | 'paused' | 'ended';
  createdAt: string;
  notes: { at: string; text: string }[];
}

export interface AuditEntry {
  type: 'audit';
  id: string;
  at: string;
  actorId: string;
  action: string;
  detail: string;
  hash: string;
  prevHash: string;
}

export interface OutboxJob {
  type: 'outbox';
  id: string;
  job: 'message' | 'post' | 'register';
  payload: Record<string, unknown>;
  tries: number;
  nextAt: string;
  status: 'pending' | 'done' | 'failed';
  lastError?: string;
}

export interface Setting {
  type: 'setting';
  id: string;
  value: string;
}

export type UnionRecord =
  | Identity
  | Keyring
  | Person
  | Connection
  | Thread
  | Message
  | Community
  | Membership
  | Post
  | Course
  | Enrollment
  | Event
  | Rsvp
  | Page
  | UnionLink
  | AiThread
  | AiMessage
  | AiMemory
  | AiProject
  | LedgerEntry
  | ActivationRequest
  | Plan
  | BadgeAward
  | Mentorship
  | AuditEntry
  | OutboxJob
  | Setting;

export interface BadgeDef {
  id: string;
  name: string;
  description: string;
  domain:
    | 'communication'
    | 'community'
    | 'learning'
    | 'experiences'
    | 'presence'
    | 'intelligence'
    | 'economy'
    | 'operations'
    | 'exploration'
    | 'stewardship';
  icon: string;
  check: (r: UnionRecord[], me: string) => boolean;
}
