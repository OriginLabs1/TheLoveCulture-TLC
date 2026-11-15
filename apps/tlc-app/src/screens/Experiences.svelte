<script lang="ts">
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import Modal from '../components/Modal.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Event } from '../lib/types';
  import { addDays, copy, formatDate, formatDateTime, now, relative } from '../lib/util';

  const eventId = $derived(router.segments[1] ?? '');
  const event = $derived(eventId ? union.events.find((e) => e.id === eventId) : undefined);
  const upcoming = $derived(union.events.filter((e) => new Date(e.end) >= new Date()));
  const past = $derived([...union.events].filter((e) => new Date(e.end) < new Date()).reverse());

  let createOpen = $state(false);
  let busy = $state(false);
  let title = $state('');
  let about = $state('');
  let start = $state(addDays(now(), 7).slice(0, 16));
  let end = $state(addDays(now(), 7.05).slice(0, 16));
  let mode = $state<Event['mode']>('online');
  let place = $state('Union Commons');
  let capacity = $state(24);

  function rsvpOf(id: string) {
    return union.rsvpFor(id);
  }

  async function setRsvp(target: Event, status: 'going' | 'interested' | 'declined') {
    busy = true;
    try {
      await union.setRsvp(target, status);
      toasts.push(status === 'going' ? 'You are marked as going.' : status === 'interested' ? 'Saved as interested.' : 'RSVP declined.', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function create() {
    if (!title.trim()) { toasts.push('Give the experience a title.', 'warn'); return; }
    busy = true;
    try {
      const created = await union.createEvent({
        title,
        about,
        start: new Date(start).toISOString(),
        end: new Date(end).toISOString(),
        mode,
        place,
        capacity: Number(capacity) || 1
      });
      createOpen = false;
      title = '';
      about = '';
      toasts.push('Experience created locally.', 'success');
      router.go(`/experiences/${created.id}`);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function copyLink(target: Event) {
    const ok = await copy(union.eventLink(target));
    toasts.push(ok ? 'Event link copied. It works without an account.' : 'Could not copy the link.', ok ? 'success' : 'warn');
  }
</script>

{#if event}
  <TopBar title={event.title} subtitle={formatDateTime(event.start)} back fallback="/experiences" />
  <main class="screen">
    <section class="card hero-card eventhero" style={`--tint:${event.color}`}>
      <div class="row between">
        <span class="chip accent"><Icon name={event.mode === 'online' ? 'wifi' : event.mode === 'in-person' ? 'map' : 'compass'} size={12} /> {event.mode}</span>
        {#if event.official}<span class="chip">Union hosted</span>{/if}
      </div>
      <h1 class="display">{event.title}</h1>
      <p class="muted">{event.about}</p>
      <div class="grid-2">
        <div class="metric"><strong>{formatDate(event.start, { month: 'short', day: 'numeric' })}</strong><span>{new Date(event.start).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}</span></div>
        <div class="metric"><strong>{union.goingCount(event.id)}</strong><span>going · capacity {event.capacity || 'open'}</span></div>
      </div>
      <div class="kv"><dt>Place</dt><dd>{event.place || 'To be confirmed'}</dd></div>
      <div class="kv"><dt>Host</dt><dd>{event.hostName}</dd></div>
      <div class="row wrap">
        <button class="btn primary" onclick={() => setRsvp(event, 'going')} disabled={busy}>Going</button>
        <button class="btn" onclick={() => setRsvp(event, 'interested')} disabled={busy}>Interested</button>
        <button class="btn ghost" onclick={() => setRsvp(event, 'declined')} disabled={busy}>Can’t attend</button>
        <button class="btn ghost push" onclick={() => copyLink(event)}><Icon name="link" size={15} /> Share</button>
      </div>
      {#if rsvpOf(event.id)}
        <p class="hint">Your RSVP: <strong>{rsvpOf(event.id)?.status}</strong> · recorded {relative(rsvpOf(event.id)!.at)}</p>
      {/if}
    </section>

    <section class="section card pad">
      <div class="row"><Icon name="notes" size={17} /><strong>How to take part</strong></div>
      <p class="small muted">Arrive as yourself. An experience is a room, not a performance. If the event is online, the host will share a joining note through a connection or community you already share. Links are published by the host, never scraped.</p>
      <button class="btn sm ghost" onclick={() => router.go('/experiences')}><Icon name="arrow-left" size={14} /> All experiences</button>
    </section>
  </main>
{:else}
  <TopBar title="Experiences" subtitle="Events, assemblies and field practice" back fallback="/more">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => (createOpen = true)} aria-label="Create experience"><Icon name="plus" size={17} /></button>
    {/snippet}
  </TopBar>

  <main class="screen">
    <div class="banner info">
      <Icon name="compass" size={17} />
      <div><strong>Experiences are shared, not advertised.</strong> Discovery is not endorsement. Every event carries a host, a place and a stated purpose.</div>
    </div>

    {#if upcoming.length === 0}
      <Empty icon="calendar" title="Nothing scheduled" text="Create a small, specific gathering — a practice session, a reading, a walk.">
        {#snippet action()}<button class="btn soft" onclick={() => (createOpen = true)}>Create an experience</button>{/snippet}
      </Empty>
    {:else}
      <section class="section">
        <div class="section-head"><h2>Coming up</h2><span class="hint">{upcoming.length}</span></div>
        <div class="list">
          {#each upcoming as e (e.id)}
            {@const r = rsvpOf(e.id)}
            <button class="listitem eventitem" onclick={() => router.go(`/experiences/${e.id}`)}>
              <span class="dateblock" style={`--tint:${e.color}`}>
                <strong>{formatDate(e.start, { day: 'numeric' })}</strong>
                <span>{formatDate(e.start, { month: 'short' })}</span>
              </span>
              <div class="main">
                <strong>{e.title}</strong>
                <span>{formatDateTime(e.start)} · {e.mode} · {e.place || 'place TBC'}</span>
                <span class="tiny faint">{union.goingCount(e.id)} going{r ? ` · you are ${r.status}` : ''}</span>
              </div>
              <span class="trail"><Icon name="chevron-right" size={16} /></span>
            </button>
          {/each}
        </div>
      </section>
    {/if}

    {#if past.length}
      <section class="section">
        <div class="section-head"><h2>Past</h2><span class="hint">{past.length}</span></div>
        <div class="list">
          {#each past.slice(0, 12) as e (e.id)}
            <button class="listitem" onclick={() => router.go(`/experiences/${e.id}`)}>
              <span class="capico"><Icon name="calendar" size={16} /></span>
              <div class="main"><strong>{e.title}</strong><span>{formatDate(e.start)} · {e.hostName}</span></div>
              <span class="trail"><Icon name="chevron-right" size={16} /></span>
            </button>
          {/each}
        </div>
      </section>
    {/if}
  </main>

  <Modal open={createOpen} title="Create an experience" subtitle="Small, specific and hosted by a real identity" onclose={() => (createOpen = false)}>
    <div class="stack">
      <div class="field"><label for="ev-title">Title</label><input id="ev-title" bind:value={title} maxlength="90" placeholder="Saturday practice: consent in real life" /></div>
      <div class="field"><label for="ev-about">What is it for?</label><textarea id="ev-about" bind:value={about} rows="3" maxlength="500" placeholder="One or two honest sentences."></textarea></div>
      <div class="grid-2 collapse">
        <div class="field"><label for="ev-start">Starts</label><input id="ev-start" type="datetime-local" bind:value={start} /></div>
        <div class="field"><label for="ev-end">Ends</label><input id="ev-end" type="datetime-local" bind:value={end} /></div>
      </div>
      <div class="grid-2 collapse">
        <div class="field"><label for="ev-mode">Mode</label><select id="ev-mode" bind:value={mode}><option value="online">Online</option><option value="in-person">In person</option><option value="hybrid">Hybrid</option></select></div>
        <div class="field"><label for="ev-cap">Capacity</label><input id="ev-cap" type="number" min="1" bind:value={capacity} /></div>
      </div>
      <div class="field"><label for="ev-place">Place or joining note</label><input id="ev-place" bind:value={place} maxlength="120" placeholder="Union Commons / a link shared with guests" /></div>
    </div>
    {#snippet footer()}
      <button class="btn primary block" onclick={create} disabled={busy}>{busy ? 'Creating…' : 'Create experience'}</button>
    {/snippet}
  </Modal>
{/if}

<style>
  .eventhero { padding: 18px; display: flex; flex-direction: column; gap: 13px; }
  .eventhero h1 { margin: 0; font-size: 1.8rem; }
  .eventhero p { margin: 0; }
  .dateblock {
    width: 50px; height: 54px; border-radius: 13px; flex: none;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    background: color-mix(in srgb, var(--tint) 14%, var(--panel));
    border: 1px solid color-mix(in srgb, var(--tint) 28%, var(--line));
  }
  .dateblock strong { font-family: var(--font-display); font-size: 1.28rem; font-weight: 500; letter-spacing: -0.03em; line-height: 1; }
  .dateblock span { color: var(--faint); font-size: 0.66rem; text-transform: uppercase; letter-spacing: 0.08em; }
  .capico { width: 36px; height: 36px; border-radius: 11px; flex: none; display: grid; place-items: center; background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2); }
  .eventitem .main span { white-space: normal; }
</style>
