<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import Modal from '../components/Modal.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Connection, ConsentScope, Person } from '../lib/types';
  import { formatDate, relative } from '../lib/util';

  const personId = $derived(router.segments[1] ?? '');
  const person = $derived(personId ? union.byId<Person>('person', personId) : undefined);
  const connection = $derived(person ? union.connectionWith(person.identityId) : undefined);

  let search = $state('');
  let addOpen = $state(false);
  let addText = $state('');
  let addError = $state('');
  let handleQuery = $state('');
  let lookupBusy = $state(false);
  let busy = $state(false);
  let note = $state('');
  let requestOpen = $state(false);

  const SCOPE_INFO: { id: ConsentScope; label: string; detail: string }[] = [
    { id: 'message', label: 'Message', detail: 'Start conversations with you.' },
    { id: 'profile', label: 'Profile', detail: 'See the profile details you publish.' },
    { id: 'presence', label: 'Presence', detail: 'See and open your published page.' },
    { id: 'mentorship', label: 'Mentorship', detail: 'Explore a guidance relationship.' }
  ];

  const incoming = $derived(
    union.incomingRequests
      .map((c) => ({ connection: c, person: union.people.find((p) => p.identityId === c.aId) }))
      .filter((x): x is { connection: Connection; person: Person } => Boolean(x.person))
  );

  const filtered = $derived.by(() => {
    const q = search.trim().toLowerCase();
    const list = [...union.people].sort((a, b) => a.name.localeCompare(b.name));
    if (!q) return list;
    return list.filter((p) => `${p.name} ${p.handle} ${p.about}`.toLowerCase().includes(q));
  });

  function scopesOf(c: Connection | undefined): ConsentScope[] {
    return c?.scopes ?? ['message', 'profile'];
  }

  function statusFor(p: Person): { label: string; cls: string } {
    const c = union.connectionWith(p.identityId);
    if (!c) return { label: 'Not connected', cls: 'plain' };
    if (c.status === 'accepted') return { label: 'Connected', cls: 'green' };
    if (c.status === 'pending') return c.bId === union.identity?.id ? { label: 'Request for you', cls: 'amber' } : { label: 'Request sent', cls: 'accent' };
    if (c.status === 'blocked') return { label: 'Blocked', cls: 'rose' };
    return { label: 'Declined', cls: 'plain' };
  }

  function extractCard(raw: string): string {
    const text = raw.trim();
    const idx = text.indexOf('union-card:');
    if (idx >= 0) return text.slice(idx);
    const match = text.match(/#\/u\/(.+)$/);
    if (match) {
      try { return decodeURIComponent(match[1]); } catch { return match[1]; }
    }
    return text;
  }

  async function addByCard() {
    addError = '';
    busy = true;
    try {
      const p = await union.addPersonFromText(extractCard(addText));
      addOpen = false;
      addText = '';
      toasts.push(`${p.name} added. Contact is not connection — request consent next.`, 'success');
      router.go(`/people/${p.id}`);
    } catch (error) {
      addError = error instanceof Error ? error.message : String(error);
    } finally {
      busy = false;
    }
  }

  async function addByHandle() {
    addError = '';
    lookupBusy = true;
    try {
      const p = await union.lookupHandle(handleQuery);
      addOpen = false;
      handleQuery = '';
      toasts.push(`Found @${p.handle}. Card verified from the directory.`, 'success');
      router.go(`/people/${p.id}`);
    } catch (error) {
      addError = error instanceof Error ? error.message : String(error);
    } finally {
      lookupBusy = false;
    }
  }

  async function accept(target: Connection) {
    busy = true;
    try {
      await union.respondConnection(target, true);
      const p = union.people.find((x) => x.identityId === (target.aId === union.identity?.id ? target.bId : target.aId));
      if (p) router.go(`/people/${p.id}`);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function decline(target: Connection) {
    busy = true;
    try {
      await union.respondConnection(target, false);
      toasts.push('Request declined. Nothing was shared.', 'info');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function request() {
    if (!person) return;
    busy = true;
    try {
      await union.requestConnection(person, note);
      requestOpen = false;
      note = '';
      toasts.push('Request sent. A connection needs both people.', 'success');
      const thread = await union.ensureThread(person.identityId);
      router.go(`/messages/${thread.id}`);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function toggleScope(scope: ConsentScope) {
    if (!connection) return;
    const current = scopesOf(connection);
    const next = current.includes(scope) ? current.filter((s) => s !== scope) : [...current, scope];
    busy = true;
    try {
      await union.setScopes(connection, next);
      toasts.push(next.includes(scope) ? `${scope} scope granted.` : `${scope} scope revoked for future use.`, 'info');
    } finally {
      busy = false;
    }
  }

  async function openThread() {
    if (!person) return;
    const thread = await union.ensureThread(person.identityId);
    router.go(`/messages/${thread.id}`);
  }

  async function block() {
    if (!person || !connection) return;
    if (!confirm(`Block ${person.name}? Delivery stops and the connection is hidden.`)) return;
    await union.blockConnection(connection);
    toasts.push('Contact blocked. History is unchanged.', 'info');
  }

  async function remove() {
    if (!person) return;
    if (!confirm(`Remove ${person.name} from this device? Their card and connection record will be deleted here.`)) return;
    await union.removePerson(person);
    toasts.push('Contact removed from this device.', 'info');
    router.go('/people');
  }

  async function mute() {
    if (!connection) return;
    await union.toggleMute(connection);
    toasts.push(connection.muted ? 'Notifications muted.' : 'Notifications unmuted.', 'info');
  }
</script>

{#if personId && person}
  <TopBar title={person.name} subtitle={`@${person.handle}`} back fallback="/people">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => router.go(`/mentorship?person=${person!.identityId}`)} aria-label="Mentorship"><Icon name="handshake" size={16} /></button>
    {/snippet}
  </TopBar>

  <main class="screen narrow">
    <section class="card pad profile">
      <div class="row top">
        <Avatar name={person.name} color={person.color} size={62} />
        <div class="push" style="min-width:0">
          <div class="row wrap" style="gap:6px">
            <h1 class="display">{person.name}</h1>
            <span class="chip {statusFor(person).cls}">{statusFor(person).label}</span>
            {#if union.isPractice(person)}<span class="chip teal">Official practice</span>{/if}
          </div>
          <div class="faint small">@{person.handle}</div>
          <div class="tiny faint">Added {relative(person.addedAt)} · {person.source === 'self' ? 'official seed' : person.source}</div>
        </div>
      </div>
      {#if person.about}<p class="about">{person.about}</p>{/if}
      {#if union.isPractice(person)}
        <div class="banner info">
          <Icon name="info" size={17} />
          <div>Echo has no device and cannot read replies. The private keys were discarded at creation. This contact exists so one person can rehearse the consent flow honestly.</div>
        </div>
      {/if}
    </section>

    <section class="section">
      <div class="section-head"><h2>Verification</h2></div>
      <div class="card pad">
        <div class="kv"><dt>Signing key</dt><dd class="mono">{person.edPub.slice(0, 22)}…</dd></div>
        <div class="kv"><dt>Message key</dt><dd class="mono">{person.xPub.slice(0, 22)}…</dd></div>
        <div class="kv"><dt>Identity</dt><dd class="mono">{person.identityId.slice(0, 28)}…</dd></div>
        <p class="hint" style="margin-top:8px">
          {union.isPractice(person)
            ? 'Created locally by Union as a practice contact. No remote signature to verify.'
            : 'This card carried an Ed25519 signature over its contents, checked when it was added. A card proves authorship, not good intentions.'}
        </p>
      </div>
    </section>

    <section class="section">
      <div class="section-head"><h2>Consent</h2></div>
      {#if !connection}
        <div class="card pad stack">
          <p class="small muted" style="margin:0">Contact is knowing how to reach someone. Connection is a relationship you both agree to, with explicit scopes.</p>
          <button class="btn primary block" onclick={() => (requestOpen = true)} disabled={busy}><Icon name="handshake" size={17} /> Request a connection</button>
        </div>
      {:else if connection.status === 'pending' && connection.bId === union.identity?.id}
        <div class="card pad stack">
          <div class="banner info" style="margin:0">
            <Icon name="shield" size={17} />
            <div><strong>{person.name} asked to connect.</strong>{#if connection.note} “{connection.note}”{/if} Choose only the scopes you want, then accept.</div>
          </div>
          <div class="scope-list">
            {#each SCOPE_INFO as scope (scope.id)}
              <label class="check" class:checked={scopesOf(connection).includes(scope.id)}>
                <input type="checkbox" checked={scopesOf(connection).includes(scope.id)} onchange={() => toggleScope(scope.id)} />
                <span>{scope.label}<small>{scope.detail}</small></span>
              </label>
            {/each}
          </div>
          <div class="row">
            <button class="btn primary push" onclick={() => accept(connection)} disabled={busy}>Accept with these scopes</button>
            <button class="btn ghost" onclick={() => decline(connection)} disabled={busy}>Decline</button>
          </div>
        </div>
      {:else if connection.status === 'pending'}
        <div class="card pad stack">
          <div class="banner info" style="margin:0"><Icon name="clock" size={17} /><div>Request sent. You can keep using Union; nothing is shared until they accept.</div></div>
          <button class="btn soft block" onclick={openThread}><Icon name="message" size={16} /> Open conversation</button>
        </div>
      {:else if connection.status === 'accepted'}
        <div class="card pad stack">
          <div class="row between">
            <div>
              <strong>Scopes</strong>
              <p class="hint">Changes apply immediately to future use. History remains history.</p>
            </div>
            {#if connection.muted}<span class="chip amber">Muted</span>{/if}
          </div>
          <div class="scope-list">
            {#each SCOPE_INFO as scope (scope.id)}
              <label class="check" class:checked={connection.scopes.includes(scope.id)}>
                <input type="checkbox" checked={connection.scopes.includes(scope.id)} disabled={busy} onchange={() => toggleScope(scope.id)} />
                <span>{scope.label}<small>{scope.detail}</small></span>
              </label>
            {/each}
          </div>
          <div class="row wrap">
            <button class="btn primary" onclick={openThread}><Icon name="message" size={16} /> Message</button>
            <button class="btn soft" onclick={() => router.go(`/mentorship?person=${person.identityId}`)}><Icon name="handshake" size={16} /> Mentorship</button>
            <button class="btn ghost sm push" onclick={mute}>{connection.muted ? 'Unmute' : 'Mute'}</button>
          </div>
        </div>
      {:else if connection.status === 'blocked'}
        <div class="card pad stack">
          <div class="banner danger" style="margin:0"><Icon name="lock" size={17} /><div>This contact is blocked. No new delivery or scopes are offered.</div></div>
          <button class="btn danger block" onclick={remove}>Remove contact</button>
        </div>
      {:else}
        <div class="card pad stack">
          <p class="small muted" style="margin:0">The previous request was declined. You can send a new request if circumstances change.</p>
          <button class="btn block" onclick={() => (requestOpen = true)}>Request again</button>
        </div>
      {/if}
    </section>

    <section class="section">
      <div class="card pad">
        <div class="row between">
          <div><strong class="small">Connection actions</strong><p class="tiny faint" style="margin:2px 0 0">Block is a boundary. Removing deletes the local record.</p></div>
          <button class="btn xs ghost" onclick={remove}>Remove</button>
        </div>
        {#if connection?.status === 'accepted'}
          <button class="btn xs danger" style="margin-top:10px" onclick={block}>Block {person.name}</button>
        {/if}
      </div>
    </section>
  </main>
{:else if personId}
  <TopBar title="Person" back fallback="/people" />
  <main class="screen">
    <Empty icon="user" title="This contact is not here" text="It may have been removed from this device, or the link points to a different identity." />
    <button class="btn primary block" onclick={() => router.go('/people')}>Back to People</button>
  </main>
{:else}
  <TopBar title="People" subtitle="Identity, contact, connection — different things">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => { addOpen = true; addError = ''; }} aria-label="Add a person"><Icon name="plus" size={17} /></button>
    {/snippet}
  </TopBar>

  <main class="screen">
    {#if incoming.length}
      <section class="section" style="margin-top:4px">
        <div class="section-head"><h2>Waiting for your choice</h2><span class="chip amber">{incoming.length}</span></div>
        <div class="list">
          {#each incoming as item (item.connection.id)}
            <div class="card pad reqcard">
              <div class="row top">
                <Avatar name={item.person.name} color={item.person.color} size={42} />
                <div class="push" style="min-width:0">
                  <div class="row wrap" style="gap:6px">
                    <strong>{item.person.name}</strong>
                    {#if union.isPractice(item.person)}<span class="chip teal">Practice</span>{/if}
                  </div>
                  <span class="faint small">@{item.person.handle} · asked {relative(item.connection.createdAt)}</span>
                  {#if item.connection.note}<p class="small muted" style="margin:4px 0 0">{item.connection.note}</p>{/if}
                </div>
              </div>
              <div class="row" style="margin-top:10px">
                <button class="btn sm primary" onclick={() => accept(item.connection)} disabled={busy}>Accept</button>
                <button class="btn sm ghost" onclick={() => decline(item.connection)} disabled={busy}>Decline</button>
                <button class="btn sm ghost push" onclick={() => router.go(`/people/${item.person.id}`)}>Review scopes</button>
              </div>
            </div>
          {/each}
        </div>
      </section>
    {/if}

    <div class="searchbar">
      <Icon name="search" size={17} />
      <input bind:value={search} placeholder="Search people by name, handle or note" aria-label="Search people" />
    </div>

    <section class="section" style="margin-top:14px">
      <div class="section-head">
        <h2>{search ? 'Results' : 'Your people'}</h2>
        <span class="chip plain">{filtered.length}</span>
      </div>
      {#if filtered.length}
        <div class="list">
          {#each filtered as p (p.id)}
            <button class="listitem" onclick={() => router.go(`/people/${p.id}`)}>
              <Avatar name={p.name} color={p.color} size={40} />
              <div class="main">
                <strong class="row" style="gap:6px">{p.name}{#if union.isPractice(p)}<span class="chip teal">Practice</span>{/if}</strong>
                <span>@{p.handle}{p.about ? ` · ${p.about}` : ''}</span>
              </div>
              <span class="trail"><span class="chip {statusFor(p).cls}">{statusFor(p).label}</span><Icon name="chevron-right" size={15} /></span>
            </button>
          {/each}
        </div>
      {:else}
        <Empty
          icon="people"
          title={search ? 'No one matches that' : 'No contacts yet'}
          text={search ? 'Try a different name or handle.' : 'Add a signed Union card by pasting its link, or accept the official practice request once it appears.'}
        />
      {/if}
    </section>

    <div class="banner info">
      <Icon name="shield" size={17} />
      <div><strong>Discovery is not endorsement.</strong> Adding a card means you have their public keys. It does not grant messaging, presence or mentorship. Request a connection and agree on scopes.</div>
    </div>

    <button class="btn soft block" style="margin-top:12px" onclick={() => { addOpen = true; addError = ''; }}>
      <Icon name="plus" size={17} /> Add a person
    </button>

    <p class="tiny faint center" style="margin-top:14px">Last identity change {union.identity ? formatDate(union.identity.updatedAt) : ''}</p>
  </main>

  <Modal open={requestOpen} title={`Connect with ${person?.name ?? ''}`} subtitle="A request, not an assumption. They choose the scopes." onclose={() => (requestOpen = false)}>
    <div class="stack">
      <div class="field">
        <label for="reqnote">A short note <span class="faint">(optional)</span></label>
        <textarea id="reqnote" bind:value={note} rows="3" maxlength="200" placeholder="Why would you like to connect?"></textarea>
      </div>
      <div class="banner info"><Icon name="shield" size={17} /><div>The default scopes are message and profile. {person?.name ?? 'They'} can accept, decline, or change them.</div></div>
      <button class="btn primary block" onclick={request} disabled={busy}>Send request</button>
    </div>
  </Modal>

  <Modal open={addOpen} title="Add a person" subtitle="Paste a signed card link or look up a handle" onclose={() => (addOpen = false)}>
    <div class="stack">
      <div class="field">
        <label for="cardlink">Card text or link</label>
        <textarea id="cardlink" bind:value={addText} rows="4" spellcheck="false" placeholder="https://…/#/u/union-card:… or paste the whole card"></textarea>
        <p class="hint">The signature is checked before the card is stored. A valid card proves authorship, not intent.</p>
      </div>
      <button class="btn soft block" onclick={addByCard} disabled={busy}>{busy ? 'Verifying…' : 'Verify and add card'}</button>

      <div class="hr"></div>

      <div class="field">
        <label for="handlelookup">Public directory handle</label>
        <div class="row">
          <input id="handlelookup" bind:value={handleQuery} placeholder="@handle" autocapitalize="off" />
          <button class="btn" onclick={addByHandle} disabled={lookupBusy}>{lookupBusy ? 'Looking…' : 'Look up'}</button>
        </div>
        <p class="hint">{union.relayUrl ? `Uses ${union.relayUrl}. The relay returns a signed card; the signature is checked here.` : 'Connect a relay in Operations to use the public directory. You can still add cards directly.'}</p>
      </div>

      {#if addError}<div class="banner danger"><Icon name="alert" size={17} /><div>{addError}</div></div>{/if}
    </div>
  </Modal>
{/if}

<style>
  .profile { display: flex; flex-direction: column; gap: 12px; }
  .profile h1 { margin: 0; font-size: 1.4rem; }
  .profile .about { margin: 0; color: var(--muted); font-size: 0.88rem; line-height: 1.55; }
  .scope-list { display: flex; flex-direction: column; gap: 7px; }
  .reqcard { display: flex; flex-direction: column; }
  .searchbar {
    display: flex; align-items: center; gap: 9px;
    margin-top: 18px; padding: 0 13px;
    border: 1px solid var(--line); border-radius: 13px; background: var(--bg-2);
  }
  .searchbar :global(svg) { color: var(--faint); flex: none; }
  .searchbar input { border: 0; background: transparent; box-shadow: none !important; padding-left: 0; min-height: 46px; }
</style>
