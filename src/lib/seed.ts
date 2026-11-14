/** Official starter content. Seeded once, clearly authored by Union, and never
 *  presented as another participant's activity. */

import type { Community, Course, Event, Identity, Membership, Post, UnionRecord } from './types';
import { now } from './util';

export const UNION_COMMONS_ID = 'community_union_commons';
export const WELCOME_COURSE_SLUG = 'union-foundations';

const lessons = (
  ...items: Course['lessons']
): Course['lessons'] => items;

export function officialRecords(identity: Identity): UnionRecord[] {
  const ts = now();
  const commons: Community = {
    type: 'community',
    id: UNION_COMMONS_ID,
    name: 'Union Commons',
    about:
      'The shared room of the Union environment. Ask questions, share a practical discovery, and help keep the place generous and calm.',
    visibility: 'public',
    joinPolicy: 'open',
    color: '#4f46e5',
    createdBy: 'union',
    createdAt: ts,
    official: true
  };
  const membership: Membership = {
    type: 'membership',
    id: 'membership_commons_self',
    communityId: commons.id,
    identityId: identity.id,
    role: 'member',
    joinedAt: ts
  };
  const welcome: Post = {
    type: 'post',
    id: 'post_commons_welcome',
    communityId: commons.id,
    authorId: 'union',
    authorName: 'Union',
    body:
      'Welcome to Union Commons. This is an official, quiet room for the first participants. Introduce yourself, ask a real question, or post one thing you discovered while exploring. No engagement mechanics, no streak pressure — just useful exchange.',
    createdAt: ts,
    status: 'local'
  };

  const assembly: Event = {
    type: 'event',
    id: 'event_union_assembly_001',
    title: 'Union Assembly — v0.0.1',
    about:
      'A first open assembly for people testing Union. We will walk through identity, consent, Digital Giving records and the Explorer journey together. Bring one question.',
    start: '2026-11-21T17:00:00.000Z',
    end: '2026-11-21T18:00:00.000Z',
    mode: 'online',
    place: 'Union live room (link shared after RSVP)',
    capacity: 100,
    hostId: 'union',
    hostName: 'Union',
    color: '#0d9488',
    createdAt: ts,
    official: true
  };
  const fieldPractice: Event = {
    type: 'event',
    id: 'event_field_practice_001',
    title: 'Explorer Field Practice: one useful connection',
    about:
      'A practical hour. Invite one person you already trust, agree on one consent scope, send one message, then revoke a scope together. The point is to feel how consent works, not to collect contacts.',
    start: '2026-11-28T16:00:00.000Z',
    end: '2026-11-28T17:00:00.000Z',
    mode: 'hybrid',
    place: 'Online + local circles',
    capacity: 50,
    hostId: 'union',
    hostName: 'Union',
    color: '#b45309',
    createdAt: ts,
    official: true
  };

  const foundations: Course = {
    type: 'course',
    id: 'course_union_foundations',
    slug: WELCOME_COURSE_SLUG,
    title: 'Union Foundations',
    subtitle: 'The ideas that make the environment coherent',
    about:
      'Five short lessons on identity, consent, communication, Digital Giving and the Explorer habit. This is the recommended first course.',
    level: 'foundation',
    minutes: 28,
    color: '#4f46e5',
    official: true,
    lessons: lessons(
      {
        id: 'lesson_uf_1',
        title: 'One environment, many capabilities',
        minutes: 5,
        body:
          '# A connected environment\n\nUnion is not a feed and it is not a shopping centre. It is a **connected environment**: communication, community, learning, experiences, digital presence, intelligence, economic coordination and operations share the same foundations.\n\nThose foundations are identity, relationships, consent, security, data, notifications, links, synchronisation, context and extensibility. A capability that bypasses them is not a Union capability.\n\n> Identity is not a profile. A profile is something you present. An identity is something you can prove.\n\n## What that means for you\n\n- You create your identity on your device. The private keys stay with you.\n- You decide who can reach you. Discoverability is never permission.\n- You can use one part of Union without being forced into every other part.\n- You can leave with your data in a portable format.\n\n## A small practice\n\nOpen **People** and look at what is stored about you. If something is not true, change it. If something is unnecessary, remove it.',
        practice: 'Review the identity card on Home. Change one detail so it says something true about you.',
        quiz: {
          question: 'Which statement matches the Constitution?',
          options: [
            'Discoverability implies permission to message.',
            'Identity and profile are the same thing.',
            'Discovery is not endorsement, and contact is not consent.',
            'Every member must participate in every capability.'
          ],
          answer: 2,
          why: 'The Constitution separates identity from profile, contact from connection, and discovery from endorsement. Consent is always explicit and revocable.'
        }
      },
      {
        id: 'lesson_uf_2',
        title: 'Identity, connection and consent',
        minutes: 6,
        body:
          '# Three different things\n\n**Identity** is your verifiable key pair. **Contact** is someone knowing how to reach you. **Connection** is a relationship you both agreed to.\n\nA connection request is not a follow. It is a small agreement. In Union you choose which scopes you grant:\n\n- *Message* — the person may start conversations with you.\n- *Profile* — the person may see the profile details you publish.\n- *Presence* — the person may see your published page.\n- *Mentorship* — you may explore a guidance relationship.\n\nEvery scope is revocable. Revoking a scope stops future use; it does not rewrite history.\n\n## Blocking is not a punishment\n\nBlocking is a boundary. Muting is a volume control. Reporting is a safety signal. Union treats them as different tools because they mean different things.\n\n## The connection card\n\nYour card contains your public keys and a signature. It lets another device verify that the card really came from you. Share it as a link or QR code — never share your private keys.\n\n## Practice\n\nMake a connection with the **Echo** contact in People to see the full consent flow without needing a second person. Then change the scopes and watch what becomes unavailable.',
        practice: 'Accept or decline a connection request and adjust its scopes.',
        quiz: {
          question: 'What does accepting a connection request do?',
          options: [
            'It gives the other person unlimited access to everything.',
            'It grants only the scopes both people agreed to, and those scopes can be revoked.',
            'It publishes your private keys so the other device can verify you.',
            'It permanently links both identities.'
          ],
          answer: 1,
          why: 'Connections are scoped and revocable. Keys are never shared; public keys and signatures do the verification.'
        }
      },
      {
        id: 'lesson_uf_3',
        title: 'Digital Giving without confusion',
        minutes: 6,
        body:
          '# Clear language, clear obligations\n\nUnion uses human language: **Digital Giving Methods**, **Packages**, **Participants**, **Plans** and **Giving & Compensation Records**. Softer words must never hide a real obligation.\n\nThree things are deliberately separate:\n\n1. **Tokens** can represent monetary or non-monetary value.\n2. **Recognition** (for example Joy tokens) is not automatically redeemable money.\n3. **Withdrawable balances** require proper backing, custody and legal support.\n\n## The initial plans\n\n- **Activation Token** — a minimum of **$80 equivalent** (a Naira equivalent is acceptable). It activates a participant.\n- **Reactivation Token (Plan)** — a minimum of **$10/month equivalent**, after either a **Special** or **Super** experience of the Union app.\n- **Joy/Giving Plans** — optional. Request the exact plan by sending **“Joyful Giver”** as a courtesy signal to Union’s Creator on Telegram: **t.me/CollinsRoyalMedium**. A **2-week trial** can be started before deciding.\n\nThe app records every entry in a transparent, hash-chained ledger. In v0.0.1 these records are in **test mode**: no real funds move inside the app. That disclosure is part of the design, not a footnote.\n\n## Practice\n\nOpen **Economy** and read one ledger entry from top to bottom. Find the amount, the currency, the reason, the hash and the test-mode disclosure.',
        practice: 'Record a test gift and verify its hash chain.',
        quiz: {
          question: 'Which statement is honest about Union v0.0.1?',
          options: [
            'Every token can be withdrawn as cash immediately.',
            'Joy tokens are always money.',
            'Records are transparent, and monetary movement is in test mode until a backed provider is connected.',
            'The Activation Token is optional for everyone forever.'
          ],
          answer: 2,
          why: 'v0.0.1 records real intentions and transparent entries, but does not pretend to custody or move funds. Monetary tokens need backing, custody and legal support.'
        }
      },
      {
        id: 'lesson_uf_4',
        title: 'Communication that survives a bad network',
        minutes: 6,
        body:
          '# Local first, relay when needed\n\nUnion composes on your device. A message is written to local storage first. If a relay is configured, it is sealed end-to-end and handed to the relay for store-and-forward delivery. If the network is gone, the message waits in the outbox and sends when connectivity returns.\n\nMessage status is visible:\n\n- **Queued** — written here, not yet handed over.\n- **Sent** — the relay accepted the sealed envelope.\n- **Delivered** — the recipient’s device acknowledged it.\n- **Failed** — retry, or check the relay configuration.\n\n## What the relay can see\n\nThe relay sees routing metadata (who to, when, size) and ciphertext. It cannot read message bodies or attachments. Union does not claim metadata privacy from the relay; that would be dishonest. Self-hosting or replacing the relay is supported.\n\n## Practice\n\nSend a note to yourself, turn on airplane mode, send another note, then reconnect. Both notes remain readable and the queued one keeps its state.',
        practice: 'Send a note to yourself while offline.',
        quiz: {
          question: 'What does the relay store?',
          options: [
            'Plaintext messages and attachments',
            'Only ciphertext envelopes plus routing metadata',
            'Your private keys',
            'Your contacts and consent grants'
          ],
          answer: 1,
          why: 'Messages are sealed on the device. The relay stores ciphertext and the routing information needed for store-and-forward delivery.'
        }
      },
      {
        id: 'lesson_uf_5',
        title: 'Exploration is a practice, not a score',
        minutes: 5,
        body:
          '# Progress with meaning\n\nUnion recognises **Life, Reality, Discovery and Progress**. Explorer Badges mark demonstrated participation: finishing a practical lesson, making a real connection, publishing a page, keeping a transparent record, or stewarding another person well.\n\nBadges are not streaks. They do not expire because you rested. They are not sold. They cannot be bought with Joy tokens.\n\n## A useful week\n\n1. Make one connection and agree on one scope.\n2. Finish one lesson and do its practice.\n3. Attend or host one experience.\n4. Publish one page or one link that is genuinely useful.\n5. Record one gift or compensation honestly.\n\nThat is enough. Union is designed for a life, not for a timeline.\n\n## The final constitutional principle\n\nBuild the best coherent foundation possible. Preserve meaning. Standardise capabilities and verification, not implementations. Enable meaningful participation. Transform intelligently. Adapt continuously. Verify universally.',
        practice: 'Choose one item from the useful week and put a date on it.',
        quiz: {
          question: 'What do Explorer Badges recognise?',
          options: [
            'Time spent scrolling',
            'Money paid into the app',
            'Demonstrated, meaningful participation and practice',
            'The number of people you follow'
          ],
          answer: 2,
          why: 'Badges recognise demonstrated participation and practical achievement, not artificial engagement or payment.'
        }
      }
    )
  };

  const consent: Course = {
    type: 'course',
    id: 'course_consent_in_practice',
    slug: 'consent-in-practice',
    title: 'Consent in Practice',
    subtitle: 'Scopes, revocation and safety without drama',
    about: 'A short practical course for using the consent tools with confidence.',
    level: 'practical',
    minutes: 14,
    color: '#0d9488',
    official: true,
    lessons: lessons(
      {
        id: 'lesson_cp_1',
        title: 'Consent is contextual',
        minutes: 4,
        body:
          '# More than a checkbox\n\nConsent is not one switch. A person may want to hear from you about a shared course but not receive marketing, or may trust you with a mentorship conversation but not with their public page.\n\nUnion models consent as **scopes on a relationship**. The scopes are understandable, enforceable and revocable. The app shows what a scope allows before you grant it.\n\n## Test\n\nAsk: *Could the other person explain this permission back to me in one sentence?* If not, the permission is too vague.',
        quiz: {
          question: 'Why are permissions scoped instead of one “allow everything” switch?',
          options: [
            'To make the interface longer',
            'Because different relationships need different, understandable boundaries',
            'Because scopes are required by the relay',
            'To prevent people from ever connecting'
          ],
          answer: 1,
          why: 'Scopes keep consent understandable, contextual and enforceable.'
        }
      },
      {
        id: 'lesson_cp_2',
        title: 'Grant, change, revoke',
        minutes: 5,
        body:
          '# Consent is a living agreement\n\nOpen a connection and you will see its current scopes. Turning a scope off takes effect immediately for future actions. The other person is not silently punished; the interface simply stops offering the capability.\n\nRevocation is not deletion of history. A message already received remains received. Union is honest about this instead of pretending revocation rewrites the past.\n\n## Blocking, muting, reporting\n\n- **Mute** reduces notifications.\n- **Block** stops delivery and hides the relationship.\n- **Report** records a safety signal for review.\n\nUse the smallest tool that solves the problem.',
        practice: 'Open a connection, revoke one scope, then grant it again. Notice what changes.',
        quiz: {
          question: 'What does revocation do?',
          options: [
            'Deletes every message the person ever received',
            'Stops future use of that scope while history remains history',
            'Deletes the other person’s identity',
            'Notifies the relay to ban the person'
          ],
          answer: 1,
          why: 'Revocation is forward-looking and immediately effective. It does not rewrite what already happened.'
        }
      },
      {
        id: 'lesson_cp_3',
        title: 'The smallest sufficient boundary',
        minutes: 5,
        body:
          '# Safety without theatre\n\nIf someone is noisy, mute them. If someone is harmful, block them. If someone is dangerous, report and keep the evidence.\n\nUnion keeps these as separate actions because conflating them creates bad outcomes: muting should not look like a accusation, and blocking should not be a public spectacle.\n\n## A calm script\n\n1. Name the boundary to yourself.\n2. Use the smallest tool that enforces it.\n3. Record what happened, if safety may matter later.\n4. Tell someone you trust.\n\nUnion is software. It cannot replace community care or emergency services.',
        quiz: {
          question: 'Which is the smallest sufficient boundary for ordinary noise?',
          options: ['Blocking', 'Muting', 'Reporting to authorities', 'Deleting your identity'],
          answer: 1,
          why: 'Muting is the lightest tool. Blocking and reporting are for stronger boundaries.'
        }
      }
    )
  };

  const giving: Course = {
    type: 'course',
    id: 'course_digital_giving_literacy',
    slug: 'digital-giving-literacy',
    title: 'Digital Giving Literacy',
    subtitle: 'Tokens, plans, records and honest limits',
    about:
      'Learn the exact Union economic vocabulary and the three initial plans, including what v0.0.1 does and does not do.',
    level: 'practical',
    minutes: 18,
    color: '#b45309',
    official: true,
    lessons: lessons(
      {
        id: 'lesson_dg_1',
        title: 'Money, tokens and recognition',
        minutes: 5,
        body:
          '# Three ledgers in your head\n\nA **monetary token** represents value that may eventually be redeemable. A **non-monetary token** represents recognition, access or participation. A **record** documents what happened.\n\nUnion keeps these distinctions visible:\n\n| Thing | Is it money? | Can it be withdrawn? |\n| --- | --- | --- |\n| Activation Token | Monetary-equivalent plan | Only through a backed provider |\n| Reactivation Token (Plan) | Recurring plan | Not a balance |\n| Joy token | Recognition | No |\n\nIf a product blurs these lines, ask what is actually owed, to whom, and under which law.',
        quiz: {
          question: 'Can a Joy token be withdrawn as cash by default?',
          options: [
            'Yes, always',
            'No — it is recognition unless a specific backed arrangement says otherwise',
            'Only on Sundays',
            'Yes, after $80'
          ],
          answer: 1,
          why: 'Joy is recognition. Withdrawable monetary value requires explicit backing, custody and disclosure.'
        }
      },
      {
        id: 'lesson_dg_2',
        title: 'Activation and Reactivation',
        minutes: 5,
        body:
          '# The first two plans\n\nThe **Activation Token** has a minimum of **$80 equivalent**. A Naira equivalent is acceptable. It is a one-time activation of a participant.\n\nThe **Reactivation Token (Plan)** has a minimum of **$10/month equivalent**. It applies after either a **Special** or a **Super** experience of the Union mobile app. Special is the lighter, focused experience. Super is the expanded experience with broader AI and organisational tools.\n\nNeither plan should be described as a subscription in ordinary language if that would obscure the obligation. Union says *Plan* and shows the period.\n\n## In the app\n\nEconomy → Plans shows the amount in USD and in Naira at the reference rate you set. Every activation creates a transparent record. In v0.0.1, payments are in test mode and no money moves.',
        practice: 'Open Economy → Plans and compare the USD and Naira display.',
        quiz: {
          question: 'When does the Reactivation Token (Plan) apply?',
          options: [
            'Before creating an identity',
            'After either a Special or Super experience of the app',
            'Only after one year',
            'Only for organisations'
          ],
          answer: 1,
          why: 'Reactivation follows either experience level. Both are valid ways to use Union.'
        }
      },
      {
        id: 'lesson_dg_3',
        title: 'Joy/Giving Plans and the courteous signal',
        minutes: 5,
        body:
          '# Optional, not accidental\n\n**Joy/Giving Plans** are optional. They contain a **$1 minimum token** for acknowledging or appreciating the Creator of the Union concept with love and the joy of experiencing Union.\n\nThe exact plan is requested by sending the message **“Joyful Giver”** as a courtesy request signal to Union’s Creator on Telegram: **t.me/CollinsRoyalMedium**. A **2-week trial mode** lets you experience the plan before deciding.\n\nJoy/Giving Plans may include scoped AI, key feature gates, the Email Hub extension, free migration and token tools. Each inclusion is disclosed in the plan terms. Voluntary appreciation must remain voluntary.\n\n## What the app does\n\n- Starts a local 14-day trial with an end date.\n- Records the courtesy signal and the Telegram link.\n- Never pretends a message was sent automatically.\n- Lets you end the trial at any time.',
        practice: 'Start a Joy/Giving trial, then read the end date in Plans.',
        quiz: {
          question: 'How is a Joy/Giving Plan requested?',
          options: [
            'By sending “Joyful Giver” to the Creator on Telegram as a courtesy signal',
            'By purchasing it automatically at signup',
            'By writing a public post',
            'It cannot be requested in v0.0.1'
          ],
          answer: 0,
          why: 'The courtesy signal goes to t.me/CollinsRoyalMedium, and a 2-week trial mode is available.'
        }
      },
      {
        id: 'lesson_dg_4',
        title: 'Records that can be checked',
        minutes: 3,
        body:
          '# A chain, not a promise\n\nEach ledger entry contains a hash of its own content and the hash of the previous entry. The Rust trust core computes SHA-256 digests. If someone edits an old entry, the chain stops verifying at that point.\n\nA hash chain proves **integrity**, not truth. It shows that the record you are reading is the record that was written. It does not prove that a bank transfer happened.\n\n## Practice\n\nOpen Economy → Ledger → Verify. Then export the ledger and verify it independently.',
        quiz: {
          question: 'What does the ledger hash chain prove?',
          options: [
            'That every payment was legal',
            'That the recorded entries have not been altered since they were written',
            'That the relay is honest',
            'That a token is redeemable'
          ],
          answer: 1,
          why: 'A hash chain gives integrity. Truth and legal backing require separate evidence.'
        }
      }
    )
  };

  const presence: Course = {
    type: 'course',
    id: 'course_presence',
    slug: 'build-your-presence',
    title: 'Build a Digital Presence',
    subtitle: 'A page and links that respect the visitor',
    about: 'Create a small, useful destination and share it with permissions and expiry.',
    level: 'practical',
    minutes: 12,
    color: '#7c3aed',
    official: true,
    lessons: lessons(
      {
        id: 'lesson_pr_1',
        title: 'A page is a place, not a funnel',
        minutes: 4,
        body:
          '# Make one useful place\n\nA Union page is a small destination: who you are, what you are working on, and how to participate. It does not need to convert anyone.\n\nGood pages answer three questions quickly:\n\n1. What is this?\n2. Who is it for?\n3. What is the next respectful step?\n\n## Practice\n\nOpen **Presence** and draft a page with a title, one honest sentence and two sections. Do not publish yet.',
        quiz: {
          question: 'What makes a page useful?',
          options: [
            'A popup asking for an email',
            'Clear answers about what it is, who it is for and the next step',
            'As many links as possible',
            'Autoplay video'
          ],
          answer: 1,
          why: 'Clarity and respect beat conversion tricks.'
        }
      },
      {
        id: 'lesson_pr_2',
        title: 'Links with terms',
        minutes: 4,
        body:
          '# Links are objects\n\nIn Union, a link has a kind, a creator, permissions, an optional expiry and a revocation state. It can carry its content so it remains useful outside the app, or point to the relay for a short address.\n\nEvery shared link should answer: *What does opening this allow?* A read-only page should not silently grant messaging rights.\n\n## Practice\n\nCreate a link to your page, set an expiry one week out, and open it in a new tab.',
        quiz: {
          question: 'What should a Union link declare?',
          options: [
            'Nothing; links are just URLs',
            'Its permissions, creator, and any expiry or revocation',
            'The recipient’s private key',
            'A hidden tracking identifier'
          ],
          answer: 1,
          why: 'Links are first-class objects with permissions and lifecycle.'
        }
      },
      {
        id: 'lesson_pr_3',
        title: 'Publish, then listen',
        minutes: 4,
        body:
          '# Shipping is a beginning\n\nPublish the page. Share it with one person. Ask one question: *Was anything unclear?* Then improve that sentence.\n\nIf the page changes, update the link or issue a new one. If something should no longer be available, revoke it. Union link resolution checks revocation when the relay is in use.\n\n## Practice\n\nPublish your page and export a static copy. Open the copy offline. If it still communicates, it is a real page.',
        quiz: {
          question: 'What is a good first step after publishing?',
          options: [
            'Add analytics and track every click',
            'Ask one person what was unclear and improve it',
            'Rewrite everything immediately',
            'Delete the page'
          ],
          answer: 1,
          why: 'One real question beats a dashboard of vanity metrics.'
        }
      }
    )
  };

  const stewardship: Course = {
    type: 'course',
    id: 'course_stewardship',
    slug: 'mentorship-and-stewardship',
    title: 'Mentorship & Stewardship',
    subtitle: 'Guidance that leaves the other person stronger',
    about: 'Optional practices for mentorship, discipleship and stewardship relationships.',
    level: 'advanced',
    minutes: 12,
    color: '#be123c',
    official: true,
    lessons: lessons(
      {
        id: 'lesson_st_1',
        title: 'Guidance is not control',
        minutes: 4,
        body:
          '# Leave them stronger\n\nMentorship is strategic and optional. Its purpose is meaningful guidance, not dependence. A good steward makes themselves less necessary over time.\n\nAgree on three things at the start:\n\n- **Focus** — what is actually being explored?\n- **Cadence** — how often, and for how long?\n- **Ending** — what would make this complete?\n\nUnion records these in a Mentorship agreement. Either person can pause or end it.',
        quiz: {
          question: 'What is the aim of stewardship?',
          options: [
            'Permanent dependence',
            'The other person becoming stronger and more autonomous',
            'Growing the mentor’s audience',
            'Selling a plan'
          ],
          answer: 1,
          why: 'Stewardship serves the growth and autonomy of the other person.'
        }
      },
      {
        id: 'lesson_st_2',
        title: 'Consent in guidance',
        minutes: 4,
        body:
          '# Permission for the conversation\n\nA mentorship scope is separate from messaging. It says that both people are exploring a guidance relationship. It does not grant access to private journals, contacts or money.\n\nKeep notes factual and kind. Share them when it helps. Do not use a guidance relationship to extract information.\n\n## Practice\n\nCreate a mentorship request with one focus and one cadence. Write the ending condition in the note.',
        quiz: {
          question: 'Does a mentorship scope grant access to money or private data?',
          options: ['Yes', 'No; it is a separate, limited scope', 'Only for Super experience', 'Only if the mentor asks'],
          answer: 1,
          why: 'Scopes are limited to their purpose. Mentorship is not a master key.'
        }
      },
      {
        id: 'lesson_st_3',
        title: 'The stewardship log',
        minutes: 4,
        body:
          '# Memory with humility\n\nA short note after a conversation helps continuity: what was explored, what was decided, what to try next. Keep it observable and avoid diagnosing the other person.\n\nUnion keeps these notes local to the relationship. Export them when the relationship ends so each person keeps their own copy.\n\nThis is a practice, not a certificate. The proof is the other person’s progress and their freedom to leave.',
        quiz: {
          question: 'What belongs in a stewardship note?',
          options: [
            'Diagnoses and judgments',
            'What was explored, decided, and what to try next',
            'Gossip',
            'Financial details'
          ],
          answer: 1,
          why: 'Observable, useful continuity — not judgment.'
        }
      }
    )
  };

  return [
    commons,
    membership,
    welcome,
    assembly,
    fieldPractice,
    foundations,
    consent,
    giving,
    presence,
    stewardship
  ];
}
