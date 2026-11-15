<script lang="ts">
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { BADGES, DOMAINS, domainProgress } from '../lib/badges';
  import { percent } from '../lib/util';
  import { CONSTITUTION_TAGLINE } from '../lib/version';

  const me = $derived(union.identity);
  const earnedIds = $derived(new Set(union.awards.map((a) => a.badgeId)));
  const earned = $derived(union.earnedBadges.length);
  const total = BADGES.length;
  const nextLesson = $derived(union.nextLesson);
  const completedLessons = $derived(union.enrollments.reduce((n, e) => n + e.done.length, 0));

  const practices = [
    { title: 'Life', text: 'Tend the ordinary: sleep, work, people, attention. Progress that costs your life is not progress.', icon: 'heart' },
    { title: 'Reality', text: 'Prefer what can be checked. A claim with a hash, a date and a witness is stronger than a slogan.', icon: 'shield' },
    { title: 'Discovery', text: 'Follow one honest question until it produces something you can use or share.', icon: 'compass' }
  ];
</script>

<TopBar title="Exploration & Progress" subtitle="A record of practice, not a score" back fallback="/more" />

<main class="screen">
  <section class="card hero-card progress-hero">
    <div class="row between">
      <div>
        <p class="eyebrow">Explorer progress</p>
        <h1 class="display">{earned} of {total}</h1>
        <p class="muted small">badges earned through use</p>
      </div>
      <div class="ring" style={`--p:${percent(earned, total)}`}>
        <Icon name="star" size={22} />
      </div>
    </div>
    <div class="progress" style={`--p:${percent(earned, total)}`}><span></span></div>
    <div class="row wrap" style="gap:6px;margin-top:12px">
      <span class="chip">{completedLessons} lessons completed</span>
      <span class="chip">{union.acceptedConnections.length} connections</span>
      <span class="chip">{union.ledger.length} ledger entries</span>
    </div>
  </section>

  {#if nextLesson}
    <button class="banner info tappable" onclick={() => router.go(`/learning/${nextLesson.course.slug}`)}>
      <Icon name="book" size={18} />
      <div><strong>Continue {nextLesson.course.title}</strong><div>Next: {nextLesson.lesson.title} · {nextLesson.lesson.minutes} min</div></div>
      <Icon name="chevron-right" size={16} />
    </button>
  {/if}

  <section class="section">
    <div class="section-head"><h2>The practice</h2></div>
    <div class="list">
      {#each practices as p (p.title)}
        <div class="listitem static">
          <span class="capico"><Icon name={p.icon} size={17} /></span>
          <div class="main"><strong>{p.title}</strong><span class="wrap">{p.text}</span></div>
        </div>
      {/each}
    </div>
    <p class="quote" style="margin-top:14px">{CONSTITUTION_TAGLINE}</p>
  </section>

  <section class="section">
    <div class="section-head"><h2>Ten domains</h2><span class="hint">progress is evidence</span></div>
    <div class="list">
      {#each DOMAINS as d (d.id)}
        {@const p = me ? domainProgress(union.records, me.id, d.id) : { done: 0, total: 0 }}
        <button class="listitem" onclick={() => router.go(routeFor(d.id))}>
          <span class="capico"><Icon name={d.icon} size={17} /></span>
          <div class="main">
            <strong>{d.name}</strong>
            <span>{d.blurb}</span>
            <div class="progress tinybar" style={`--p:${percent(p.done, p.total)}`}><span></span></div>
          </div>
          <span class="trail"><span class="faint tiny">{p.done}/{p.total}</span><Icon name="chevron-right" size={16} /></span>
        </button>
      {/each}
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Explorer badges</h2><span class="hint">{total - earned} remaining</span></div>
    <div class="badge-grid">
      {#each BADGES as b (b.id)}
        {@const have = earnedIds.has(b.id)}
        <div class="badge-card" class:locked={!have}>
          <span class="badge-medal" class:locked={!have}><Icon name={b.icon} size={19} /></span>
          <div>
            <strong>{b.name}</strong>
            <p>{b.description}</p>
          </div>
          {#if have}<Icon name="check" size={15} />{:else}<Icon name="lock" size={13} />{/if}
        </div>
      {/each}
    </div>
  </section>

  <section class="section">
    <div class="card pad about">
      <div class="row"><Icon name="info" size={17} /><strong>How progress works</strong></div>
      <p class="small muted">
        Badges are derived from records on this device: an accepted scoped connection, a completed
        lesson, a verified ledger, a published page. They are not purchased, advertised or ranked.
        Nothing here is sent to a server unless you configure a relay yourself.
      </p>
      <button class="btn sm soft" onclick={() => router.go('/learning')}><Icon name="book" size={14} /> Open Learning</button>
    </div>
  </section>
</main>

<script module lang="ts">
  function routeFor(id: string): string {
    const map: Record<string, string> = {
      communication: '/messages',
      community: '/communities',
      learning: '/learning',
      experiences: '/experiences',
      presence: '/presence',
      intelligence: '/ai',
      economy: '/economy',
      operations: '/operations',
      exploration: '/explore',
      stewardship: '/mentorship'
    };
    return map[id] ?? '/more';
  }
</script>

<style>
  .progress-hero { padding: 18px; display: flex; flex-direction: column; gap: 12px; }
  .progress-hero h1 { margin: 0; font-size: 2.1rem; }
  .ring {
    --p: 0;
    width: 62px; height: 62px; border-radius: 50%;
    display: grid; place-items: center; color: var(--accent-2);
    background:
      radial-gradient(closest-side, var(--panel) 76%, transparent 77%),
      conic-gradient(var(--accent) calc(var(--p) * 1%), rgb(255 255 255 / 0.08) 0);
    flex: none;
  }
  .capico {
    width: 36px; height: 36px; border-radius: 11px; flex: none;
    display: grid; place-items: center;
    background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2);
  }
  .listitem.static:hover { border-color: var(--line); }
  .main .wrap { white-space: normal; line-height: 1.45; padding-top: 2px; }
  .tinybar { height: 4px; margin-top: 7px; max-width: 220px; }
  .badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 9px; }
  .badge-card {
    display: flex; align-items: flex-start; gap: 11px;
    padding: 12px; border: 1px solid var(--line); border-radius: 14px;
    background: var(--panel);
  }
  .badge-card > :global(svg) { margin-left: auto; color: var(--green); flex: none; }
  .badge-card.locked { opacity: 0.62; }
  .badge-card.locked > :global(svg) { color: var(--faint); }
  .badge-card strong { display: block; font-size: 0.84rem; }
  .badge-card p { margin: 2px 0 0; color: var(--faint); font-size: 0.72rem; line-height: 1.42; }
  .about { display: flex; flex-direction: column; gap: 9px; }
  .about p { margin: 0; }
</style>
