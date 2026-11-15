<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Icon from '../components/Icon.svelte';
  import QrCode from '../components/QrCode.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import { decodeCard } from '../lib/crypto';
  import type { UnionCard } from '../lib/crypto';
  import { readToken } from '../lib/links';
  import type { AnyToken } from '../lib/links';
  import type { RelayLink } from '../lib/relay';
  import { copy, formatDate, formatDateTime, money } from '../lib/util';
  import { shareText } from '../lib/native';

  const head = $derived(router.segments[0] ?? '');
  const raw = $derived.by(() => {
    const part = router.segments[1] ?? '';
    try { return decodeURIComponent(part); } catch { return part; }
  });

  const card = $derived<UnionCard | null>(raw.startsWith('union-card:') ? decodeCard(raw) : null);
  const token = $derived<AnyToken | null>(card ? null : readToken(raw));

  let remote = $state<RelayLink | null>(null);
  let loading = $state(false);
  let error = $state('');

  $effect(() => {
    if (head !== 'l' || !raw) return;
    let cancelled = false;
    loading = true;
    error = '';
    union.resolveRelayLink(raw)
      .then((link) => { if (!cancelled) remote = link; })
      .catch((err) => { if (!cancelled) error = err instanceof Error ? err.message : String(err); })
      .finally(() => { if (!cancelled) loading = false; });
    return () => { cancelled = true; };
  });

  const broken = $derived(!card && !token && head === 'u');

  const pageTheme = $derived.by(() => {
    if (token?.kind !== 'page') return '';
    const t = token.page.theme;
    if (t === 'warm') return 'linear-gradient(160deg, #241b16, var(--panel))';
    if (t === 'forest') return 'linear-gradient(160deg, #122019, var(--panel))';
    if (t === 'midnight') return 'linear-gradient(160deg, #101528, var(--panel))';
    return '';
  });

  async function addCard() {
    if (!card) return;
    try {
      const person = await union.addPerson(card);
      toasts.push(`${person.name} added as a contact. Connection still needs consent.`, 'success');
      router.go(`/people/${person.id}`);
    } catch (err) {
      toasts.push(err instanceof Error ? err.message : String(err), 'error');
    }
  }

  async function addByHandle(handle: string) {
    try {
      const person = await union.lookupHandle(handle);
      router.go(`/people/${person.id}`);
    } catch (err) {
      toasts.push(err instanceof Error ? err.message : String(err), 'error');
    }
  }

  async function join(community: { id: string; name: string }) {
    try {
      await union.joinCommunity({ ...community, type: 'community', about: '', visibility: 'public', joinPolicy: 'open', color: '#5b5bd6', createdBy: 'link', createdAt: new Date().toISOString(), official: false });
      toasts.push(`Joined ${community.name}.`, 'success');
      router.go(`/communities/${community.id}`);
    } catch (err) {
      toasts.push(err instanceof Error ? err.message : String(err), 'error');
    }
  }

  function pretty(value: unknown): string {
    try { return JSON.stringify(value, null, 2); } catch { return String(value); }
  }
</script>

<TopBar title="Union link" subtitle={raw ? `${head === 'l' ? 'Relay' : 'Self-contained'} · ${raw.length} characters` : ''} back fallback="/" />

<main class="screen narrow">
  {#if broken}
    <div class="card pad stack center">
      <div class="badge-medal locked" style="margin:0 auto"><Icon name="alert" size={22} /></div>
      <h1 class="display">This link could not be read</h1>
      <p class="muted small">The token is incomplete, damaged or from an incompatible version. Ask the sender for a fresh Union card or page link.</p>
      <button class="btn primary" onclick={() => router.go('/')}>Open Union</button>
      <button class="btn ghost" onclick={() => router.go('/people')}>Add by card text</button>
    </div>
  {:else if card}
    <section class="card pad stack center">
      <div class="eyebrow">Signed identity card</div>
      <Avatar name={card.name} color={card.color} size={78} ring />
      <div>
        <h1 class="display" style="margin:0">{card.name}</h1>
        <div class="faint">@{card.handle}</div>
      </div>
      {#if card.about}<p class="small muted" style="margin:0">{card.about}</p>{/if}
      <div class="banner good" style="text-align:left"><Icon name="shield" size={17} /><div><strong>Signature verified.</strong> This card was signed by the Ed25519 key it names. Verification is not endorsement.</div></div>
      <div class="card tight pad" style="width:100%;text-align:left">
        <div class="kv"><dt>Identity</dt><dd class="mono truncate">{card.id}</dd></div>
        <div class="kv"><dt>Signed</dt><dd>{formatDate(card.ts)}</dd></div>
        <div class="kv"><dt>Card</dt><dd class="mono truncate">{card.ed.slice(0, 22)}…</dd></div>
      </div>
      {#if union.identity}
        <button class="btn primary block" onclick={addCard}><Icon name="user" size={16} /> Add {card.name} as a contact</button>
        <p class="hint">Adding a contact is not connection. You can request a scoped connection next, and either side can decline or revoke.</p>
      {:else}
        <button class="btn primary block" onclick={() => router.go('/')}>Create your identity first</button>
      {/if}
      <div class="row" style="justify-content:center"><QrCode text={`union-card:${raw.replace(/^union-card:/, '')}`} size={150} /></div>
    </section>
  {:else if token?.kind === 'page'}
    <article class="page" style={pageTheme}>
      <p class="eyebrow">{token.page.handle ? `@${token.page.handle}` : 'A Union page'}</p>
      <h1 class="display">{token.page.title}</h1>
      {#if token.page.tagline}<p class="lead">{token.page.tagline}</p>{/if}
      {#if token.page.about}<p>{token.page.about}</p>{/if}
      {#each token.page.sections as section (section.title + section.body)}
        <section><h2 class="display">{section.title}</h2><p>{section.body}</p></section>
      {/each}
      {#if token.page.links.length}
        <section class="links">
          {#each token.page.links as link (link.url)}
            <a class="btn soft block" href={link.url} target="_blank" rel="noopener noreferrer"><Icon name="external" size={15} /> {link.label}</a>
          {/each}
        </section>
      {/if}
      <p class="tiny faint">Last updated {formatDate(token.page.updatedAt)} · self-contained Union page. No tracking.</p>
    </article>
  {:else if token?.kind === 'community'}
    <section class="card pad stack">
      <div class="row"><span class="capico"><Icon name="people" size={18} /></span><div><div class="eyebrow" style="margin:0">Community invitation</div><strong>{token.community.name}</strong></div></div>
      <p class="small muted" style="margin:0">{token.community.about}</p>
      <div class="row wrap"><span class="chip">{token.community.visibility}</span><span class="chip">{token.community.joinPolicy}</span></div>
      {#if union.identity}
        <button class="btn primary block" onclick={() => join(token.community)}>Join this community</button>
      {:else}
        <button class="btn primary block" onclick={() => router.go('/')}>Create an identity to join</button>
      {/if}
    </section>
  {:else if token?.kind === 'event'}
    <section class="card pad stack">
      <div class="eyebrow">Experience</div>
      <h1 class="display" style="margin:0">{token.event.title}</h1>
      <p class="small muted" style="margin:0">{token.event.about}</p>
      <div class="kv"><dt>Starts</dt><dd>{formatDateTime(token.event.start)}</dd></div>
      <div class="kv"><dt>Ends</dt><dd>{formatDateTime(token.event.end)}</dd></div>
      <div class="kv"><dt>Mode</dt><dd>{token.event.mode}</dd></div>
      <div class="kv"><dt>Place</dt><dd>{token.event.place}</dd></div>
      <div class="kv"><dt>Host</dt><dd>{token.event.hostName}</dd></div>
      <button class="btn soft block" onclick={() => router.go('/experiences')}>Open Experiences</button>
    </section>
  {:else if token?.kind === 'course'}
    <section class="card pad stack">
      <div class="eyebrow">Course</div>
      <h1 class="display" style="margin:0">{token.course.title}</h1>
      <p class="muted" style="margin:0">{token.course.subtitle}</p>
      <button class="btn primary block" onclick={() => router.go(`/learning/${token.course.slug}`)}>Open course</button>
    </section>
  {:else if token?.kind === 'giving'}
    <section class="card pad stack center">
      <div class="badge-medal" style="margin:0 auto"><Icon name="heart" size={22} /></div>
      <h1 class="display" style="margin:0">{token.title}</h1>
      <p class="muted">{token.memo || 'A transparent Digital Giving note.'}</p>
      <div class="metric" style="width:100%"><strong>{money(token.suggestedUsd, 'USD')}</strong><span>suggested · nothing is charged automatically</span></div>
      <button class="btn soft block" onclick={() => router.go('/economy')}>Open Economy</button>
      <p class="hint">v0.0.1 records giving in test mode. No withdrawable value is created or promised.</p>
    </section>
  {:else if token?.kind === 'invite'}
    <section class="card pad stack center">
      <div class="eyebrow">Invitation</div>
      <Avatar name={token.name} color="#5b5bd6" size={68} ring />
      <h1 class="display" style="margin:0">{token.name || `@${token.handle}`}</h1>
      <p class="muted">@{token.handle}</p>
      {#if token.note}<p class="quote" style="text-align:left">{token.note}</p>{/if}
      {#if union.identity}
        <button class="btn primary block" onclick={() => addByHandle(token.handle)}>Look up @{token.handle}</button>
        <p class="hint">Lookup needs a relay directory. Discovery is not endorsement; a connection still needs consent.</p>
      {:else}
        <button class="btn primary block" onclick={() => router.go('/')}>Open Union</button>
      {/if}
    </section>
  {:else if token?.kind === 'link'}
    <section class="card pad stack">
      <div class="row wrap" style="gap:6px"><span class="chip accent">{token.link.kind}</span><span class="chip plain">{token.link.permissions.join(' · ')}</span></div>
      <h1 class="display" style="margin:0">{token.link.title}</h1>
      {#if token.link.subtitle}<p class="muted" style="margin:0">{token.link.subtitle}</p>{/if}
      <div class="kv"><dt>Created</dt><dd>{formatDate(token.link.createdAt)}</dd></div>
      {#if token.link.expiresAt}<div class="kv"><dt>Expires</dt><dd>{formatDate(token.link.expiresAt)}</dd></div>{/if}
      {#if token.link.revokedAt}<div class="banner danger"><Icon name="alert" size={16} /><div><strong>Revoked.</strong> This link was withdrawn by its creator on {formatDate(token.link.revokedAt)}.</div></div>{/if}
      <pre class="payload">{pretty(token.link.payload)}</pre>
    </section>
  {:else if token}
    <section class="card pad stack">
      <div class="eyebrow">{token.kind}</div>
      <h1 class="display" style="margin:0">A Union link</h1>
      <pre class="payload">{pretty(token)}</pre>
    </section>
  {:else if head === 'l'}
    {#if loading}
      <div class="card pad center"><div class="loader"></div><p class="muted small">Fetching the link from the relay…</p></div>
    {:else if error}
      <div class="card pad stack center">
        <div class="badge-medal locked" style="margin:0 auto"><Icon name="wifi-off" size={22} /></div>
        <h1 class="display" style="margin:0">Relay link unavailable</h1>
        <p class="muted small">{error}</p>
        <button class="btn soft" onclick={() => router.go('/operations')}>Configure relay</button>
        <button class="btn ghost" onclick={() => router.go('/')}>Open Union</button>
      </div>
    {:else if remote}
      <section class="card pad stack">
        <div class="row wrap" style="gap:6px"><span class="chip accent">{remote.kind}</span>{#if remote.revokedAt}<span class="chip rose">revoked</span>{/if}</div>
        <h1 class="display" style="margin:0">{remote.title}</h1>
        {#if remote.subtitle}<p class="muted" style="margin:0">{remote.subtitle}</p>{/if}
        <div class="kv"><dt>Created</dt><dd>{formatDate(remote.createdAt)}</dd></div>
        {#if remote.expiresAt}<div class="kv"><dt>Expires</dt><dd>{formatDate(remote.expiresAt)}</dd></div>{/if}
        <pre class="payload">{pretty(remote.payload)}</pre>
        <p class="tiny faint">Resolved from your configured relay. A relay can withhold data; it cannot forge a signed card.</p>
      </section>
    {/if}
  {:else}
    <div class="card pad stack center">
      <div class="badge-medal locked" style="margin:0 auto"><Icon name="link" size={22} /></div>
      <h1 class="display" style="margin:0">Nothing to open</h1>
      <p class="muted small">This address does not contain a Union link.</p>
      <button class="btn primary" onclick={() => router.go('/')}>Open Union</button>
    </div>
  {/if}
</main>

<style>
  .capico { width: 40px; height: 40px; border-radius: 12px; flex: none; display: grid; place-items: center; background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2); }
  .page {
    padding: 22px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--panel);
    display: flex;
    flex-direction: column;
    gap: 13px;
    animation: screen-in 260ms ease-out;
  }
  .page h1 { margin: 0; font-size: clamp(1.8rem, 7vw, 2.4rem); }
  .page .lead { margin: 0; }
  .page p { margin: 0; color: color-mix(in srgb, var(--ink) 84%, var(--muted)); line-height: 1.64; }
  .page section h2 { margin: 0 0 4px; font-size: 1.16rem; }
  .page .links { display: flex; flex-direction: column; gap: 7px; margin-top: 4px; }
  .payload { margin: 0; padding: 12px; background: var(--bg-2); border: 1px solid var(--line-soft); border-radius: 10px; color: var(--muted); font-family: var(--font-mono); font-size: 0.7rem; line-height: 1.5; overflow-x: auto; max-height: 280px; }
  .loader { width: 24px; height: 24px; border-radius: 50%; border: 2px solid rgb(255 255 255 / 0.12); border-top-color: var(--accent-2); animation: spin 900ms linear infinite; margin: 6px auto; }
  @keyframes spin { to { transform: rotate(360deg); } }
</style>
