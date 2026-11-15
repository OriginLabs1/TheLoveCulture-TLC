<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Mentorship, Person } from '../lib/types';
  import { formatDate, relative } from '../lib/util';

  const queryPerson = $derived(router.query.get('person') ?? '');
  const people = $derived([...union.people].sort((a, b) => a.name.localeCompare(b.name)));
  const focusPerson = $derived(people.find((p) => p.identityId === queryPerson));

  let selectedId = $state('');
  let focus = $state('');
  let cadence = $state('Every two weeks');
  let asMentor = $state(false);
  let noteText = $state('');
  let busy = $state(false);
  let openId = $state('');

  const selected = $derived(people.find((p) => p.identityId === selectedId) ?? focusPerson);
  const existing = $derived(selected ? union.mentorshipWith(selected) : undefined);

  const rows = $derived(
    union.mentorships.map((m) => {
      const otherId = m.mentorId === union.identity?.id ? m.menteeId : m.mentorId;
      return {
        mentorship: m,
        other: people.find((p) => p.identityId === otherId),
        role: m.mentorId === union.identity?.id ? 'Mentor' : 'Mentee'
      };
    })
  );

  async function request() {
    if (!selected) { toasts.push('Choose a person first.', 'warn'); return; }
    if (!focus.trim()) { toasts.push('Say what the guidance is for.', 'warn'); return; }
    busy = true;
    try {
      await union.requestMentorship(selected, focus.trim(), cadence, asMentor);
      focus = '';
      toasts.push('Mentorship request recorded. It begins as a request, not an obligation.', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function setStatus(m: Mentorship, status: Mentorship['status']) {
    busy = true;
    try {
      await union.setMentorshipStatus(m, status);
      toasts.push(`Mentorship ${status}.`, 'success');
    } finally {
      busy = false;
    }
  }

  async function addNote(m: Mentorship) {
    if (!noteText.trim()) return;
    busy = true;
    try {
      await union.addStewardshipNote(m, noteText);
      noteText = '';
      toasts.push('Stewardship note recorded.', 'success');
    } finally {
      busy = false;
    }
  }

  function otherName(person: Person | undefined): string {
    return person?.name ?? 'Unknown person';
  }
</script>

<TopBar title="Mentorship & Stewardship" subtitle="Guidance that leaves someone stronger" back fallback="/more" />

<main class="screen">
  <div class="banner info">
    <Icon name="handshake" size={17} />
    <div><strong>Guidance is not control.</strong> A mentorship has a focus, a cadence and an exit. Notes exist to serve the other person, never to build a file on them.</div>
  </div>

  {#if focusPerson && !existing}
    <section class="section">
      <div class="section-head"><h2>Begin with {focusPerson.name}</h2></div>
      <div class="card pad stack">
        <div class="row">
          <Avatar name={focusPerson.name} color={focusPerson.color} size={44} />
          <div class="push"><strong>{focusPerson.name}</strong><div class="faint small">@{focusPerson.handle}</div></div>
        </div>
        <div class="field"><label for="mt-focus">What is this guidance for?</label><textarea id="mt-focus" bind:value={focus} rows="3" maxlength="240" placeholder="A concrete area where you want to be useful, or where you want help."></textarea></div>
        <div class="grid-2 collapse">
          <div class="field"><label for="mt-cad">Cadence</label><select id="mt-cad" bind:value={cadence}><option>One conversation</option><option>Weekly</option><option>Every two weeks</option><option>Monthly</option><option>As needed</option></select></div>
          <div class="field"><div class="label">Your role</div><div class="segmented"><button class:on={asMentor} onclick={() => (asMentor = true)}>Mentor</button><button class:on={!asMentor} onclick={() => (asMentor = false)}>Mentee</button></div></div>
        </div>
        <button class="btn primary block" onclick={request} disabled={busy}><Icon name="handshake" size={15} /> Record request</button>
      </div>
    </section>
  {/if}

  <section class="section">
    <div class="section-head"><h2>Relationships</h2><span class="hint">{rows.length}</span></div>
    {#if rows.length === 0}
      <Empty icon="handshake" title="No mentorship yet" text="Choose someone you already know. A request can be paused or ended at any time.">
        {#snippet action()}<button class="btn soft" onclick={() => router.go('/people')}>Open People</button>{/snippet}
      </Empty>
    {:else}
      <div class="list">
        {#each rows as row (row.mentorship.id)}
          <div class="card pad mentcard">
            <div class="row">
              <Avatar name={otherName(row.other)} color={row.other?.color ?? '#5b5bd6'} size={42} />
              <div class="push" style="min-width:0">
                <div class="row wrap" style="gap:6px">
                  <strong>{otherName(row.other)}</strong>
                  <span class="chip plain">{row.role}</span>
                  <span class="chip {row.mentorship.status === 'active' ? 'green' : row.mentorship.status === 'requested' ? 'amber' : row.mentorship.status === 'ended' ? 'rose' : ''}">{row.mentorship.status}</span>
                </div>
                <div class="faint small">{row.mentorship.focus}</div>
                <div class="tiny faint">{row.mentorship.cadence} · opened {formatDate(row.mentorship.createdAt)}</div>
              </div>
              <button class="iconbtn sm" onclick={() => (openId = openId === row.mentorship.id ? '' : row.mentorship.id)} aria-label="Details"><Icon name={openId === row.mentorship.id ? 'chevron-down' : 'chevron-right'} size={16} /></button>
            </div>

            {#if openId === row.mentorship.id}
              <div class="details">
                <div class="row wrap">
                  {#if row.mentorship.status === 'requested'}<button class="btn xs primary" onclick={() => setStatus(row.mentorship, 'active')} disabled={busy}>Accept / begin</button>{/if}
                  {#if row.mentorship.status === 'active'}<button class="btn xs" onclick={() => setStatus(row.mentorship, 'paused')} disabled={busy}>Pause</button>{/if}
                  {#if row.mentorship.status === 'paused'}<button class="btn xs soft" onclick={() => setStatus(row.mentorship, 'active')} disabled={busy}>Resume</button>{/if}
                  {#if row.mentorship.status !== 'ended'}<button class="btn xs ghost" onclick={() => setStatus(row.mentorship, 'ended')} disabled={busy}>End</button>{/if}
                  {#if row.other}<button class="btn xs ghost" onclick={() => router.go(`/people/${row.other!.id}`)}>Open person</button>{/if}
                </div>

                <div class="notes">
                  <div class="label">Stewardship log</div>
                  {#if row.mentorship.notes.length === 0}
                    <p class="hint">No notes yet. Record what was agreed or what changed, never a judgement.</p>
                  {:else}
                    {#each row.mentorship.notes as n (n.at + n.text)}
                      <div class="note"><p>{n.text}</p><span>{relative(n.at)}</span></div>
                    {/each}
                  {/if}
                  <div class="row" style="align-items:flex-start">
                    <textarea bind:value={noteText} rows="2" maxlength="400" placeholder="A useful note for this relationship."></textarea>
                    <button class="btn sm" onclick={() => addNote(row.mentorship)} disabled={busy || !noteText.trim()}>Add</button>
                  </div>
                </div>
              </div>
            {/if}
          </div>
        {/each}
      </div>
    {/if}
  </section>

  <section class="section">
    <div class="section-head"><h2>New relationship</h2></div>
    <div class="card pad stack">
      {#if people.length === 0}
        <p class="small muted">Add someone from People first. Mentorship begins with a person, not a directory listing.</p>
        <button class="btn soft" onclick={() => router.go('/people')}>Open People</button>
      {:else}
        <div class="field"><label for="mt-person">Person</label><select id="mt-person" bind:value={selectedId}><option value="">Choose a person</option>{#each people as p (p.id)}<option value={p.identityId}>{p.name} · @{p.handle}</option>{/each}</select></div>
        <div class="field"><label for="mt-focus2">Focus</label><textarea id="mt-focus2" bind:value={focus} rows="2" maxlength="240" placeholder="A concrete area of guidance."></textarea></div>
        <div class="grid-2 collapse">
          <div class="field"><label for="mt-cad2">Cadence</label><select id="mt-cad2" bind:value={cadence}><option>One conversation</option><option>Weekly</option><option>Every two weeks</option><option>Monthly</option><option>As needed</option></select></div>
          <div class="field"><div class="label">Role</div><div class="segmented"><button class:on={asMentor} onclick={() => (asMentor = true)}>Mentor</button><button class:on={!asMentor} onclick={() => (asMentor = false)}>Mentee</button></div></div>
        </div>
        {#if existing}<p class="hint">A relationship with this person already exists above.</p>{/if}
        <button class="btn primary block" onclick={request} disabled={busy || !selected || Boolean(existing)}><Icon name="handshake" size={15} /> Record request</button>
      {/if}
    </div>
  </section>
</main>

<style>
  .mentcard { display: flex; flex-direction: column; gap: 10px; }
  .details { border-top: 1px solid var(--line-soft); padding-top: 11px; display: flex; flex-direction: column; gap: 11px; }
  .notes { display: flex; flex-direction: column; gap: 7px; }
  .note { padding: 9px 11px; border: 1px solid var(--line); border-radius: 10px; background: var(--bg-2); }
  .note p { margin: 0; font-size: 0.82rem; line-height: 1.5; }
  .note span { color: var(--faint); font-size: 0.68rem; }
  .details textarea { min-height: 62px; }
</style>
