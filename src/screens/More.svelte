<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { CAPABILITIES } from '../lib/navigation';
  import { VERSION } from '../lib/version';
  import { plural } from '../lib/format';
  import { relative } from '../lib/util';

  const me = $derived(union.identity);
  const joy = $derived(union.joyPlan);
  const people = $derived(union.people.length);
  const posts = $derived(union.all('post').length);
  const lessonsDone = $derived(union.enrollments.reduce((n, e) => n + e.done.length, 0));
</script>

<TopBar title="More" subtitle="Every capability, one environment" />

<main class="screen">
  <button class="card linkcard idcard" onclick={() => router.go('/settings')}>
    <div class="row">
      <Avatar name={me?.name ?? 'Union'} color={me?.color ?? '#5b5bd6'} size={46} />
      <div class="push" style="min-width:0">
        <strong>{me?.name}</strong>
        <div class="faint small truncate">@{me?.handle} · {me?.experience === 'super' ? 'Super' : 'Special'} experience</div>
      </div>
      <Icon name="settings" size={18} />
    </div>
    <div class="row wrap" style="gap:6px;margin-top:12px">
      <span class="chip {union.activation.status === 'active' ? 'green' : union.activation.status === 'inactive' ? 'amber' : 'teal'}">
        {union.activation.status === 'active' ? 'Activated' : union.activation.status === 'inactive' ? 'Activation pending' : union.activation.status}
      </span>
      {#if joy}<span class="chip accent">Joy/Giving {joy.status}</span>{/if}
      <span class="chip">{union.relayUrl ? 'Relay configured' : 'Local only'}</span>
      <span class="chip plain">{union.coreLabel}</span>
    </div>
  </button>

  <section class="section">
    <div class="section-head"><h2>Participate</h2></div>
    <div class="list">
      {#each CAPABILITIES as cap (cap.id)}
        <button class="listitem" onclick={() => router.go(cap.route)}>
          <span class="capico"><Icon name={cap.icon} size={17} /></span>
          <div class="main"><strong>{cap.name}</strong><span>{cap.blurb}</span></div>
          <span class="trail"><Icon name="chevron-right" size={16} /></span>
        </button>
      {/each}
      <button class="listitem" onclick={() => router.go('/people')}>
        <span class="capico"><Icon name="users" size={17} /></span>
        <div class="main"><strong>People & connections</strong><span>{plural(people, 'person', 'people')} · scoped consent</span></div>
        <span class="trail"><Icon name="chevron-right" size={16} /></span>
      </button>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Your environment</h2></div>
    <div class="grid-2">
      <div class="metric"><strong>{people}</strong><span>people known</span></div>
      <div class="metric"><strong>{union.communities.length}</strong><span>communities</span></div>
      <div class="metric"><strong>{lessonsDone}</strong><span>lessons completed</span></div>
      <div class="metric"><strong>{posts}</strong><span>community posts</span></div>
      <div class="metric"><strong>{union.ledger.length}</strong><span>ledger entries</span></div>
      <div class="metric"><strong>{union.earnedBadges.length}</strong><span>Explorer badges</span></div>
    </div>
    <div class="card pad" style="margin-top:10px">
      <div class="kv"><dt>Last relay sync</dt><dd>{union.lastSync ? relative(union.lastSync) : 'not yet'}</dd></div>
      <div class="kv"><dt>Trust core</dt><dd>{union.coreLabel} · v{union.core.version}</dd></div>
      <div class="kv"><dt>Sync state</dt><dd>{union.syncing ? 'syncing…' : union.syncError || (union.relayReady ? 'ready' : 'offline-first')}</dd></div>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Operate & govern</h2></div>
    <div class="list">
      <button class="listitem" onclick={() => router.go('/operations')}>
        <span class="capico"><Icon name="sliders" size={17} /></span>
        <div class="main"><strong>Operations</strong><span>Relay, AI provider, storage, audit, APIs</span></div>
        <span class="trail"><Icon name="chevron-right" size={16} /></span>
      </button>
      <button class="listitem" onclick={() => router.go('/settings')}>
        <span class="capico"><Icon name="settings" size={17} /></span>
        <div class="main"><strong>Settings & identity</strong><span>Name, experience, currency, backup, wipe</span></div>
        <span class="trail"><Icon name="chevron-right" size={16} /></span>
      </button>
    </div>
  </section>

  <section class="section">
    <div class="card pad about">
      <div class="row">
        <Icon name="fingerprint" size={18} />
        <strong>Union v{VERSION}</strong>
        <span class="push chip plain">AGPL-3.0</span>
      </div>
      <p class="small muted">
        A connected environment, built as a coherent foundation. Portable identity, scoped consent,
        honest records, local-first data, and capabilities that share the same foundations instead
        of becoming separate silos.
      </p>
      <div class="row wrap" style="gap:6px">
        <a class="btn xs ghost" href="https://github.com/OriginLabs1/Union" target="_blank" rel="noopener noreferrer"><Icon name="globe" size={13} /> Source</a>
        <button class="btn xs ghost" onclick={() => router.go('/explore')}><Icon name="book" size={13} /> Constitution in practice</button>
      </div>
      <p class="tiny faint" style="margin:10px 0 0">
        v0.0.1 disclosure: economic records are transparent but operate in test mode. No real funds
        move inside the app. Withdrawable value requires a backed provider, custody and legal support.
      </p>
    </div>
  </section>
</main>

<style>
  .idcard { width: 100%; text-align: left; padding: 15px; display: block; color: inherit; }
  .idcard strong { font-family: var(--font-display); font-size: 1.06rem; font-weight: 600; }
  .capico {
    width: 36px; height: 36px; border-radius: 11px; flex: none;
    display: grid; place-items: center;
    background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2);
  }
  .about { display: flex; flex-direction: column; gap: 10px; }
  .about p { margin: 0; line-height: 1.55; }
</style>
