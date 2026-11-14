<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Icon from '../components/Icon.svelte';
  import Logo from '../components/Logo.svelte';
  import Modal from '../components/Modal.svelte';
  import QrCode from '../components/QrCode.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import { BADGES } from '../lib/badges';
  import { CAPABILITIES, TOTAL_BADGES } from '../lib/navigation';
  import { copy, formatDate, money, percent, relative } from '../lib/util';
  import { shareText } from '../lib/native';
  import { ACTIVATION_MIN_USD, CONSTITUTION_TAGLINE, VERSION } from '../lib/version';

  let shareOpen = $state(false);

  const me = $derived(union.identity);
  const firstName = $derived((me?.name || me?.handle || 'friend').split(/\s+/)[0]);
  const today = $derived(new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' }));
  const greeting = $derived.by(() => {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
  });
  const next = $derived(union.nextLesson);
  const pending = $derived(union.incomingRequests.length);
  const earned = $derived(union.earnedBadges.length);
  const cardText = $derived.by(() => {
    if (!me || !union.keys) return '';
    try { return union.myCardText(); } catch { return ''; }
  });
  const shareUrl = $derived(cardText ? union.absoluteUrl(`#/u/${cardText}`) : '');
  const recentThreads = $derived(union.threads.filter((t) => t.kind === 'direct').slice(0, 3));
  const recentLedger = $derived([...union.ledger].reverse().slice(0, 2));
  const locked = $derived(BADGES.filter((b) => !union.awards.some((a) => a.badgeId === b.id)).slice(0, 3));
  const progress = $derived(percent(earned, TOTAL_BADGES));
  const activationChip = $derived.by(() => {
    const s = union.activation.status;
    if (s === 'active') return { label: 'Activated', cls: 'green' };
    if (s === 'trial') return { label: 'Trial', cls: 'teal' };
    if (s === 'grace') return { label: 'Grace period', cls: 'amber' };
    if (s === 'expired') return { label: 'Reactivation due', cls: 'rose' };
    return { label: 'Activation pending', cls: 'amber' };
  });

  async function copyCard() {
    const ok = await copy(shareUrl || cardText);
    toasts.push(ok ? 'Card link copied.' : 'Could not copy. Select the link instead.', ok ? 'success' : 'warn');
  }

  function shareCard() {
    shareText(shareUrl || cardText);
  }

  function stepIcon(): string {
    if (next) return 'book';
    if (!union.acceptedConnections.length) return 'people';
    return 'compass';
  }
</script>

<header class="masthead">
  <div class="brandrow">
    <span class="wordmark"><Logo size={21} /><span>UNION</span></span>
    <span class="chip plain version">v{VERSION}</span>
    <button class="avatar-btn push" onclick={() => router.go('/settings')} aria-label="Settings">
      <Avatar name={me?.name ?? 'Union'} color={me?.color ?? '#5b5bd6'} size={42} ring />
    </button>
  </div>
  <p class="eyebrow">{today}</p>
  <h1 class="display">{greeting}, <em>{firstName}</em>.</h1>
  <p class="tagline">{CONSTITUTION_TAGLINE}</p>
</header>

<main class="screen home">
  <section class="card hero-card identity">
    <div class="idtop">
      <Avatar name={me?.name ?? 'Union'} color={me?.color ?? '#5b5bd6'} size={62} ring />
      <div class="idtext">
        <span class="eyebrow accent">Your Union card</span>
        <strong class="name">{me?.name}</strong>
        <span class="handle">@{me?.handle}</span>
      </div>
      <span class="chip {activationChip.cls} statuschip">{activationChip.label}</span>
    </div>

    {#if me?.about}<p class="about">{me.about}</p>{/if}

    <div class="idstats">
      <div><strong>{earned}</strong><span>Badges</span></div>
      <div><strong>{union.acceptedConnections.length}</strong><span>Connections</span></div>
      <div><strong>{me?.experience === 'super' ? 'Super' : 'Special'}</strong><span>Experience</span></div>
    </div>

    <div class="card-actions">
      <button class="btn sm soft" onclick={() => (shareOpen = true)}><Icon name="qr" size={15} /> Share card</button>
      <button class="btn sm" onclick={() => router.go('/people')}><Icon name="user" size={15} /> People</button>
      <button class="iconbtn sm push" onclick={() => router.go('/settings')} aria-label="Settings"><Icon name="settings" size={15} /></button>
    </div>

    <div class="idfoot">
      <span class="mono faint truncate">{me?.id.slice(0, 34)}…</span>
      <span class="chip plain">{union.core.backend === 'rust-wasm' ? 'Rust core' : 'JS core'}</span>
    </div>
  </section>

  {#if union.activation.status === 'inactive' || union.activation.status === 'grace' || union.activation.status === 'expired'}
    <button class="banner warn tappable" onclick={() => router.go('/economy')}>
      <Icon name="coins" size={18} />
      <div>
        <strong>Activation Token · ${ACTIVATION_MIN_USD} minimum</strong>
        <div>Naira equivalent accepted. Request activation in Economy for a 2-week trial while your proof is reviewed.</div>
      </div>
      <Icon name="chevron-right" size={16} />
    </button>
  {/if}

  {#if pending > 0}
    <button class="banner info tappable" onclick={() => router.go('/people')}>
      <Icon name="users" size={18} />
      <div><strong>{pending} connection {pending === 1 ? 'request' : 'requests'} waiting.</strong> Consent is a choice, not a formality. Review the scopes.</div>
      <Icon name="chevron-right" size={16} />
    </button>
  {/if}

  <section class="section">
    <div class="section-head"><h2>{next ? 'Continue exploring' : 'A useful next step'}</h2></div>
    {#if next}
      <button class="card linkcard nextcard" onclick={() => router.go(`/learning/${next.course.slug}`)}>
        <div class="row top nextrow">
          <span class="nextorb"><Icon name={stepIcon()} size={20} /></span>
          <div class="nextbody">
            <span class="eyebrow" style="margin-bottom:4px">{next.course.title}</span>
            <strong class="nexttitle">{next.lesson.title}</strong>
            <p class="small muted">{next.lesson.minutes} min · {next.course.lessons.length} lessons · practice included</p>
          </div>
          <span class="chip accent">{union.courseProgress(next.course)}%</span>
        </div>
        <div class="progress" style={`--p:${union.courseProgress(next.course)}`}><span></span></div>
      </button>
    {:else if !union.acceptedConnections.length}
      <button class="card linkcard nextcard" onclick={() => router.go('/people')}>
        <div class="row top nextrow">
          <span class="nextorb"><Icon name="handshake" size={20} /></span>
          <div class="nextbody">
            <span class="eyebrow" style="margin-bottom:4px">Communication</span>
            <strong class="nexttitle">Rehearse consent with Echo</strong>
            <p class="small muted">Accept the official practice request, choose scopes, send a note, then revoke one. One device is enough.</p>
          </div>
          <Icon name="chevron-right" size={18} />
        </div>
      </button>
    {:else}
      <button class="card linkcard nextcard" onclick={() => router.go('/experiences')}>
        <div class="row top nextrow">
          <span class="nextorb"><Icon name="compass" size={20} /></span>
          <div class="nextbody">
            <span class="eyebrow" style="margin-bottom:4px">Experiences</span>
            <strong class="nexttitle">Take part in a real gathering</strong>
            <p class="small muted">RSVP to the Union Assembly or the Explorer Field Practice. Bring one question.</p>
          </div>
          <Icon name="chevron-right" size={18} />
        </div>
      </button>
    {/if}
  </section>

  <section class="section">
    <div class="section-head">
      <h2>The environment</h2>
      <button class="more" onclick={() => router.go('/explore')}>Ten capabilities</button>
    </div>
    <div class="grid-auto caps">
      {#each CAPABILITIES as cap, i (cap.id)}
        <button class="tile cap" onclick={() => router.go(cap.route)}>
          <span class="capindex">{String(i + 1).padStart(2, '0')}</span>
          <span class="ico"><Icon name={cap.icon} size={18} /></span>
          <strong>{cap.name}</strong>
          <span>{cap.blurb}</span>
        </button>
      {/each}
    </div>
  </section>

  <section class="section">
    <div class="section-head">
      <h2>Explorer progress</h2>
      <button class="more" onclick={() => router.go('/explore')}>{earned} of {TOTAL_BADGES}</button>
    </div>
    <div class="card pad progresscard">
      <div class="row progressrow">
        <div class="meter-ring" style={`--p:${progress};--ring-size:82px`}><span>{progress}%</span></div>
        <div class="progresscopy">
          <strong class="serif">Life · Reality · Discovery · Progress</strong>
          <p class="small faint">Badges mark demonstrated participation. They cannot be bought, sold or lost by resting.</p>
        </div>
      </div>
      {#if locked.length}
        <div class="nextbadges">
          {#each locked as badge (badge.id)}
            <span class="chip plain"><Icon name={badge.icon} size={12} /> {badge.name}</span>
          {/each}
        </div>
      {:else}
        <p class="small green-text" style="margin:12px 0 0">Every Explorer badge is present. Steward someone else well now.</p>
      {/if}
    </div>
  </section>

  {#if recentThreads.length}
    <section class="section">
      <div class="section-head">
        <h2>In motion</h2>
        <button class="more" onclick={() => router.go('/messages')}>Messages</button>
      </div>
      <div class="list">
        {#each recentThreads as thread (thread.id)}
          <button class="listitem" onclick={() => router.go(`/messages/${thread.id}`)}>
            <span class="threadmark"><Icon name="message" size={17} /></span>
            <div class="main">
              <strong>{thread.title}</strong>
              <span>{thread.unread > 0 ? `${thread.unread} new` : `Last note ${relative(thread.lastAt)}`}</span>
            </div>
            <span class="trail">{relative(thread.lastAt)}<Icon name="chevron-right" size={15} /></span>
          </button>
        {/each}
      </div>
    </section>
  {/if}

  {#if recentLedger.length}
    <section class="section">
      <div class="section-head">
        <h2>Recent records</h2>
        <button class="more" onclick={() => router.go('/economy')}>Economy</button>
      </div>
      <div class="list">
        {#each recentLedger as entry (entry.id)}
          <div class="listitem">
            <span class="threadmark coin"><Icon name="coins" size={16} /></span>
            <div class="main">
              <strong>{money(entry.amount, entry.currency)} <span class="faint tiny">· {entry.token}</span></strong>
              <span>{entry.memo}</span>
            </div>
            <span class="chip plain">Test</span>
          </div>
        {/each}
      </div>
    </section>
  {/if}

  {#if union.installPrompt && !union.identity?.id.startsWith('did:union:native')}
    <section class="section">
      <div class="card pad install">
        <Icon name="download" size={20} />
        <div class="push">
          <strong>Install Union</strong>
          <p class="small faint" style="margin:2px 0 0">Keep it on the home screen. Works offline once installed.</p>
        </div>
        <button class="btn sm primary" onclick={() => union.promptInstall()}>Install</button>
        <button class="iconbtn sm" onclick={() => union.dismissInstall()} aria-label="Dismiss"><Icon name="close" size={14} /></button>
      </div>
    </section>
  {/if}

  <footer class="homefoot">
    <div class="footmark"><Logo size={18} /><span>UNION</span></div>
    <p class="quote">{CONSTITUTION_TAGLINE}</p>
    <p class="tiny faint">Union v{VERSION} · {union.relayUrl ? 'relay configured' : 'local first'} · {union.coreLabel}</p>
    <p class="tiny faint">{formatDate(new Date().toISOString(), { year: 'numeric' })}</p>
  </footer>
</main>

<Modal open={shareOpen} title="Your Union card" subtitle="Public keys and a signature. Never your private keys." onclose={() => (shareOpen = false)}>
  <div class="sharecard">
    {#if shareUrl}
      <QrCode text={shareUrl} size={200} caption="Scan or open to verify this card" />
    {:else}
      <div class="banner warn"><Icon name="alert" size={17} /><div>This device is locked. Restore the keyring to share a signed card.</div></div>
    {/if}
    <div class="row">
      <div class="push">
        <strong>{me?.name}</strong>
        <div class="faint small">@{me?.handle}</div>
      </div>
      <span class="chip accent"><Icon name="shield" size={12} /> Signed</span>
    </div>
    <div class="linkbox mono">{shareUrl || 'No link available'}</div>
    <div class="grid-2">
      <button class="btn soft" onclick={copyCard}><Icon name="copy" size={16} /> Copy link</button>
      <button class="btn soft" onclick={shareCard}><Icon name="share" size={16} /> Share</button>
    </div>
    <details class="cardtext">
      <summary class="small muted">Show portable card text</summary>
      <textarea readonly rows="4" class="mono">{cardText}</textarea>
      <button class="btn sm soft" onclick={async () => toasts.push((await copy(cardText)) ? 'Card text copied.' : 'Could not copy.', 'info')}>Copy card text</button>
    </details>
    <p class="hint">Anyone who opens this link can verify the signature without trusting the relay. They still cannot read your messages or act as you.</p>
  </div>
</Modal>

<style>
  .masthead {
    max-width: var(--maxw);
    margin: 0 auto;
    padding: calc(14px + env(safe-area-inset-top)) var(--pad) 6px;
    animation: mast-in 620ms var(--ease) both;
  }
  @keyframes mast-in {
    from { opacity: 0; transform: translateY(-8px); }
  }
  .brandrow { display: flex; align-items: center; gap: 10px; margin-bottom: 22px; }
  .wordmark { display: inline-flex; align-items: center; gap: 9px; }
  .wordmark span {
    font-size: 0.68rem;
    font-weight: 650;
    letter-spacing: 0.34em;
    color: var(--ink-2);
    padding-left: 1px;
  }
  .version { font-size: 0.6rem; letter-spacing: 0.07em; }
  .avatar-btn { background: none; border: 0; padding: 0; border-radius: 50%; transition: transform 240ms var(--ease-spring); }
  .avatar-btn:active { transform: scale(0.94); }
  .masthead .eyebrow { margin-bottom: 6px; }
  .masthead h1 {
    margin: 0;
    font-size: clamp(2.15rem, 9vw, 3rem);
    letter-spacing: -0.025em;
    line-height: 0.98;
  }
  .tagline {
    margin: 12px 0 0;
    max-width: 38ch;
    color: var(--faint);
    font-size: 0.79rem;
    line-height: 1.55;
  }
  .home { padding-top: 16px; }

  .identity { padding: 19px; }
  .idtop { display: flex; align-items: flex-start; gap: 13px; }
  .idtext { min-width: 0; flex: 1; }
  .idtext .eyebrow { margin: 1px 0 3px; }
  .identity .name {
    display: block;
    font-family: var(--font-display);
    font-size: 1.45rem;
    font-weight: 600;
    line-height: 1.12;
    letter-spacing: -0.028em;
  }
  .handle { display: block; color: var(--faint); font-size: 0.8rem; margin-top: 2px; }
  .statuschip { flex: none; margin-top: 2px; }
  .about { color: var(--muted); font-size: 0.85rem; line-height: 1.58; margin: 14px 0 0; max-width: 46ch; }
  .idstats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    margin-top: 16px;
    border-top: 1px solid var(--line-soft);
    border-bottom: 1px solid var(--line-soft);
    padding: 12px 0;
  }
  .idstats > div { display: flex; flex-direction: column; gap: 1px; padding: 0 12px; }
  .idstats > div:first-child { padding-left: 0; }
  .idstats > div + div { border-left: 1px solid var(--line-soft); }
  .idstats strong { font-family: var(--font-display); font-weight: 500; letter-spacing: -0.025em; font-size: 1.22rem; line-height: 1.05; }
  .idstats span { color: var(--faint); font-size: 0.64rem; letter-spacing: 0.07em; text-transform: uppercase; }
  .idfoot { display: flex; align-items: center; gap: 8px; margin-top: 13px; }
  .idfoot .mono { font-size: 0.66rem; min-width: 0; }
  .tappable { width: 100%; text-align: left; margin-top: 12px; }
  .tappable > :global(svg:last-child) { margin-left: auto; align-self: center; color: var(--faint); flex: none; }

  .nextcard { padding: 17px; display: flex; flex-direction: column; gap: 14px; }
  .nextrow { gap: 13px; }
  .nextorb {
    width: 46px; height: 46px; border-radius: 15px; flex: none;
    display: grid; place-items: center;
    background: linear-gradient(150deg, rgb(139 141 255 / 0.22), rgb(139 141 255 / 0.05));
    color: var(--accent-2);
    border: 1px solid rgb(139 141 255 / 0.24);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.08);
  }
  .nextbody { min-width: 0; flex: 1; }
  .nexttitle { display: block; font-family: var(--font-display); font-size: 1.12rem; font-weight: 500; letter-spacing: -0.018em; line-height: 1.2; }
  .nextcard p { margin: 5px 0 0; }
  .cap { padding-top: 18px; }
  .capindex {
    position: absolute;
    top: 14px;
    right: 15px;
    font-size: 0.62rem;
    font-weight: 600;
    letter-spacing: 0.1em;
    color: rgb(255 255 255 / 0.22);
  }
  .progresscard .progressrow { align-items: center; gap: 16px; }
  .progresscopy { min-width: 0; }
  .progresscopy strong { display: block; font-size: 1.02rem; line-height: 1.25; }
  .progresscopy p { margin: 5px 0 0; line-height: 1.5; }
  .nextbadges { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
  .threadmark {
    width: 38px; height: 38px; border-radius: 50%; flex: none;
    display: grid; place-items: center;
    background: rgb(255 255 255 / 0.04); border: 1px solid var(--line); color: var(--accent-2);
  }
  .threadmark.coin { color: var(--gold); }
  .install { display: flex; align-items: center; gap: 12px; }
  .install > :global(svg) { color: var(--accent-2); }
  .homefoot { margin: 42px 0 10px; text-align: center; }
  .footmark {
    display: inline-flex; align-items: center; gap: 8px;
    color: var(--faint); font-size: 0.62rem; font-weight: 650; letter-spacing: 0.3em;
    margin-bottom: 16px;
  }
  .homefoot .quote { margin: 0 auto 14px; border: 0; padding: 0; font-size: 1.02rem; max-width: 32ch; }
  .homefoot .tiny + .tiny { margin-top: 2px; }
  .sharecard { display: flex; flex-direction: column; gap: 14px; }
  .linkbox {
    background: rgb(0 0 0 / 0.3); border: 1px solid var(--line); border-radius: 12px;
    padding: 11px 13px; font-size: 0.7rem; color: var(--muted);
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  .cardtext summary { cursor: pointer; padding: 4px 0; }
  .cardtext { display: flex; flex-direction: column; gap: 8px; }
  .cardtext textarea { font-size: 0.68rem; }
</style>
