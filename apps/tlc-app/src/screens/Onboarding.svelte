<script lang="ts">
  import Logo from '../components/Logo.svelte';
  import Icon from '../components/Icon.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { download, parseHandle, validHandle } from '../lib/util';
  import { ACTIVATION_MIN_USD, TRIAL_DAYS, VERSION } from '../lib/version';
  import { CAPABILITIES } from '../lib/navigation';

  type Step = 'welcome' | 'create' | 'recover' | 'restore';

  let { onDone }: { onDone?: () => void } = $props();

  let step = $state<Step>('welcome');
  let name = $state('');
  let handle = $state('');
  let about = $state('');
  let experience = $state<'special' | 'super'>('special');
  let busy = $state(false);
  let error = $state('');
  let handleTouched = $state(false);

  let backupPass = $state('');
  let backupConfirm = $state('');
  let backupDone = $state(false);

  let restoreText = $state('');
  let restorePass = $state('');

  const handlePreview = $derived(
    validHandle(parseHandle(handle || name)) ? parseHandle(handle || name) : ''
  );

  function suggest(value: string) {
    if (!handleTouched) handle = parseHandle(value.replace(/\s+/g, '_'));
  }

  function finish() {
    const next = sessionStorage.getItem('union.afterOnboarding');
    sessionStorage.removeItem('union.afterOnboarding');
    onDone?.();
    router.go(next && next.startsWith('/') ? next : '/', { replace: true });
  }

  async function create() {
    error = '';
    if (name.trim().length < 2) { error = 'Give the name you want people to see (at least 2 characters).'; return; }
    if (!validHandle(parseHandle(handle))) { error = 'Choose a handle with 3–24 letters, numbers or underscores.'; return; }
    busy = true;
    try {
      await union.createIdentity({ name, handle, about, experience });
      step = 'recover';
    } catch (err) {
      error = err instanceof Error ? err.message : String(err);
    } finally {
      busy = false;
    }
  }

  async function downloadBackup() {
    error = '';
    if (backupPass.length < 8) { error = 'Use a passphrase of at least 8 characters.'; return; }
    if (backupPass !== backupConfirm) { error = 'The passphrases do not match.'; return; }
    busy = true;
    try {
      const data = await union.exportRecovery(backupPass);
      download(`union-backup-${union.identity?.handle ?? 'identity'}.json`, data);
      backupDone = true;
      backupPass = '';
      backupConfirm = '';
    } catch (err) {
      error = err instanceof Error ? err.message : String(err);
    } finally {
      busy = false;
    }
  }

  async function readBackupFile(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    restoreText = await file.text();
    input.value = '';
  }

  async function restore() {
    error = '';
    if (!restoreText.trim()) { error = 'Paste the encrypted backup file or choose it above.'; return; }
    busy = true;
    try {
      await union.importBackup(restoreText.trim(), restorePass, 'replace');
      finish();
    } catch (err) {
      error = err instanceof Error ? err.message : String(err);
    } finally {
      busy = false;
    }
  }
</script>

<div class="onboard" class:welcome={step === 'welcome'}>
  {#if step === 'welcome'}
    <div class="aurora" aria-hidden="true"></div>

    <header class="cover">
      <div class="orbital" aria-hidden="true">
        <span class="orbit o1"></span>
        <span class="orbit o2"></span>
        <span class="orbit o3"></span>
        <span class="sat s1"></span>
        <span class="sat s2"></span>
        <span class="sat s3"></span>
        <span class="mark"><Logo size={54} glow /></span>
      </div>

      <div class="wordmark"><span>UNION</span><em>one connected environment</em></div>
      <h1 class="display">A place to <em>belong</em><br />without being owned.</h1>
      <p class="lead">
        Communication, community, learning, experiences, presence, intelligence,
        economic coordination and stewardship — in one environment where your identity
        lives on this device and consent is always scoped.
      </p>
    </header>

    <div class="principles">
      <div><span class="pico"><Icon name="fingerprint" size={17} /></span><span><strong>Verifiable identity.</strong> A key pair, not a profile rented from a platform.</span></div>
      <div><span class="pico"><Icon name="shield" size={17} /></span><span><strong>Consent is scoped.</strong> Contact is not permission. Every scope can be revoked.</span></div>
      <div><span class="pico gold"><Icon name="coins" size={17} /></span><span><strong>Honest records.</strong> Digital Giving is transparent — and in test mode for v0.0.1.</span></div>
    </div>

    <div class="cta">
      <button class="btn primary block lg" onclick={() => { step = 'create'; error = ''; }} disabled={busy}>
        Create my identity <Icon name="arrow-right" size={17} />
      </button>
      <button class="btn ghost block" onclick={() => { step = 'restore'; error = ''; }} disabled={busy}>
        Restore an encrypted backup
      </button>
    </div>

    <div class="domains">
      <div class="domainhead"><span class="eyebrow">One foundation · ten capabilities</span><span class="hair"></span></div>
      <div class="domain-list">
        {#each CAPABILITIES as cap (cap.id)}
          <span class="chip plain"><Icon name={cap.icon} size={13} />{cap.name}</span>
        {/each}
      </div>
    </div>

    <p class="fine">
      No account is created on a server. No email is required. Private keys never leave this
      device except inside a backup you encrypt yourself. Union v{VERSION} is a real, usable
      foundation — not a promise of every future integration.
    </p>
  {:else if step === 'create'}
    <header class="sub">
      <button class="iconbtn" onclick={() => (step = 'welcome')} aria-label="Back"><Icon name="arrow-left" size={18} /></button>
      <div class="subtext">
        <div class="stepdots"><span class="on"></span><span></span></div>
        <span class="eyebrow">Identity · step 1 of 2</span>
        <h1 class="display">Who is arriving?</h1>
        <p class="muted small">A name for people to see, and a handle they can trust.</p>
      </div>
    </header>

    <div class="card pad stack formcard">
      <div class="field">
        <label for="ob-name">Name people will see</label>
        <input id="ob-name" bind:value={name} oninput={(e) => suggest((e.currentTarget as HTMLInputElement).value)} placeholder="Ada Lovelace" maxlength="60" autocomplete="name" />
      </div>
      <div class="field">
        <label for="ob-handle">Handle</label>
        <div class="handlebox">
          <span>@</span>
          <input id="ob-handle" bind:value={handle} oninput={() => (handleTouched = true)} placeholder="ada" maxlength="24" autocapitalize="off" autocomplete="off" spellcheck="false" />
        </div>
        <p class="hint">{handlePreview ? `Your handle will be @${handlePreview}` : '3–24 letters, numbers or underscores.'} Handles are for people, not for sale.</p>
      </div>
      <div class="field">
        <label for="ob-about">A true sentence about you <span class="faint">(optional)</span></label>
        <textarea id="ob-about" bind:value={about} rows="3" maxlength="240" placeholder="What are you exploring, building or learning?"></textarea>
      </div>
    </div>

    <div class="card pad stack experience">
      <div>
        <div class="label">Experience level</div>
        <p class="hint">Both are complete ways to use Union. Reactivation follows either.</p>
      </div>
      <div class="grid-2 collapse">
        <button class="choice" class:on={experience === 'special'} onclick={() => (experience = 'special')}>
          <span class="choicehead"><Icon name="feather" size={19} /><span class="checkmark"><Icon name="check" size={12} /></span></span>
          <strong>Special</strong>
          <span>Focused: communication, community, learning, presence, exploration.</span>
        </button>
        <button class="choice" class:on={experience === 'super'} onclick={() => (experience = 'super')}>
          <span class="choicehead"><Icon name="crown" size={19} /><span class="checkmark"><Icon name="check" size={12} /></span></span>
          <strong>Super</strong>
          <span>Expanded: adds AI projects, broader operations and organisational tools.</span>
        </button>
      </div>
    </div>

    <div class="banner info">
      <Icon name="shield" size={17} />
      <div>
        <strong>Keys stay here.</strong> Union creates Ed25519 signing keys and X25519 message keys
        inside your device secure store. We never see them.
      </div>
    </div>

    {#if error}<div class="banner danger"><Icon name="alert" size={17} /><div>{error}</div></div>{/if}

    <button class="btn primary block lg" onclick={create} disabled={busy}>
      {busy ? 'Creating keys…' : 'Create identity'} {#if !busy}<Icon name="fingerprint" size={18} />{/if}
    </button>
    <p class="fine center">You can change your name, handle, about text and experience later.</p>
  {:else if step === 'recover'}
    <header class="sub">
      <div class="subtext">
        <div class="stepdots"><span class="on"></span><span class="on"></span></div>
        <span class="eyebrow">Identity · step 2 of 2</span>
        <h1 class="display">Keep a way back in.</h1>
        <p class="muted small">Identity created. Now make one encrypted backup before you settle in.</p>
      </div>
    </header>

    <div class="card hero-card pad stack idpreview">
      <div class="row">
        <div class="bigavatar" style={`--tint:${union.identity?.color}`}>{(union.identity?.name ?? 'U').slice(0, 1).toUpperCase()}</div>
        <div class="push">
          <strong class="serif previewname">{union.identity?.name}</strong>
          <div class="faint small">@{union.identity?.handle}</div>
        </div>
        <span class="chip green"><Icon name="check" size={12} /> Keys on device</span>
      </div>
      <div class="mono faint truncate">{union.identity?.id}</div>
      <p class="hint">This identifier is derived from your public key. It is how others verify a card really came from you.</p>
    </div>

    <div class="card pad stack">
      <div class="row between">
        <div>
          <div class="label">Encrypted recovery file</div>
          <p class="hint">Contains your private keys, records and settings. Encrypted with PBKDF2-SHA256 + AES-256-GCM.</p>
        </div>
        {#if backupDone}<span class="chip green"><Icon name="check" size={12} />Downloaded</span>{/if}
      </div>
      <div class="field">
        <label for="bp">Passphrase</label>
        <input id="bp" type="password" bind:value={backupPass} autocomplete="new-password" placeholder="At least 8 characters" />
      </div>
      <div class="field">
        <label for="bp2">Repeat passphrase</label>
        <input id="bp2" type="password" bind:value={backupConfirm} autocomplete="new-password" placeholder="Same passphrase" />
      </div>
      {#if error}<div class="banner danger"><Icon name="alert" size={17} /><div>{error}</div></div>{/if}
      <button class="btn soft block" onclick={downloadBackup} disabled={busy}>
        <Icon name="download" size={17} /> {busy ? 'Encrypting…' : backupDone ? 'Download another copy' : 'Download encrypted backup'}
      </button>
      <p class="hint">Store it offline. Anyone with the file and passphrase can restore this identity. Union cannot recover either for you.</p>
    </div>

    <div class="banner warn">
      <Icon name="alert" size={17} />
      <div>
        <strong>Activation Token:</strong> minimum ${ACTIVATION_MIN_USD} equivalent (Naira
        accepted). Request activation in Economy and upload proof for a {TRIAL_DAYS}-day trial
        while it is reviewed. v0.0.1 records this in test mode — no funds move inside the app.
      </div>
    </div>

    <button class="btn primary block lg" onclick={finish} disabled={busy}>
      {backupDone ? 'Enter Union' : 'Continue without a backup'} <Icon name="arrow-right" size={17} />
    </button>
    {#if !backupDone}
      <button class="btn ghost block" onclick={downloadBackup} disabled={busy}>Download a backup first</button>
    {/if}
    <p class="fine center">A {TRIAL_DAYS}-day Joy/Giving trial is optional and separate. Nothing is charged automatically.</p>
  {:else if step === 'restore'}
    <header class="sub">
      <button class="iconbtn" onclick={() => (step = 'welcome')} aria-label="Back"><Icon name="arrow-left" size={18} /></button>
      <div class="subtext">
        <span class="eyebrow">Returning</span>
        <h1 class="display">Restore a backup.</h1>
        <p class="muted small">Your identity, records and settings come back together.</p>
      </div>
    </header>

    <div class="card pad stack formcard">
      <div class="field">
        <label for="bfile">Backup file</label>
        <input id="bfile" type="file" accept="application/json,.json" onchange={readBackupFile} />
        <p class="hint">Choose the encrypted JSON file you downloaded from Union.</p>
      </div>
      <div class="field">
        <label for="btext">Or paste the file contents</label>
        <textarea id="btext" bind:value={restoreText} rows="5" placeholder="Paste the encrypted JSON here" spellcheck="false"></textarea>
      </div>
      <div class="field">
        <label for="rpass">Backup passphrase</label>
        <input id="rpass" type="password" bind:value={restorePass} autocomplete="current-password" placeholder="The passphrase you chose" />
      </div>
      {#if error}<div class="banner danger"><Icon name="alert" size={17} /><div>{error}</div></div>{/if}
      <button class="btn primary block" onclick={restore} disabled={busy}>
        {busy ? 'Decrypting…' : 'Restore identity and records'}
      </button>
      <p class="hint">Restoring replaces whatever is currently on this device. Your keys are written to the device secure store only after decryption succeeds.</p>
    </div>

    <div class="banner warn">
      <Icon name="lock" size={17} />
      <div>Union never receives your passphrase and cannot reset it. If the passphrase is lost, the backup is unreadable.</div>
    </div>
  {/if}
</div>

<style>
  .onboard {
    position: relative;
    max-width: 620px;
    margin: 0 auto;
    padding: calc(26px + env(safe-area-inset-top)) var(--pad) calc(40px + env(safe-area-inset-bottom));
    display: flex;
    flex-direction: column;
    gap: 17px;
    animation: screen-in 460ms var(--ease) both;
  }
  .onboard.welcome { padding-top: calc(20px + env(safe-area-inset-top)); }

  .aurora {
    position: fixed;
    inset: -20% -10% auto;
    height: 80vh;
    z-index: -1;
    pointer-events: none;
    background:
      radial-gradient(34% 40% at 50% 18%, rgb(139 141 255 / 0.22), transparent 70%),
      radial-gradient(26% 30% at 78% 32%, rgb(94 234 212 / 0.12), transparent 72%),
      radial-gradient(24% 26% at 24% 30%, rgb(232 198 144 / 0.08), transparent 70%);
    filter: blur(34px);
    animation: breathe 12s ease-in-out infinite alternate;
  }
  @keyframes breathe {
    to { transform: scale(1.08) translateY(2%); opacity: 0.8; }
  }

  .cover { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; padding: 6px 0 2px; }
  .orbital {
    position: relative;
    width: 132px;
    height: 132px;
    display: grid;
    place-items: center;
    margin-bottom: 6px;
  }
  .mark {
    width: 74px; height: 74px;
    border-radius: 50%;
    display: grid; place-items: center;
    background:
      radial-gradient(circle at 32% 26%, rgb(255 255 255 / 0.92), transparent 62%),
      var(--surface);
    border: 1px solid var(--line);
    box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.8),
      0 22px 54px -22px rgb(23 24 29 / 0.34),
      0 0 60px -14px color-mix(in srgb, var(--accent) 30%, transparent);
    position: relative;
    z-index: 2;
  }
  .orbit {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 1px solid var(--line);
  }
  .o1 { inset: 12px; border-style: dashed; animation: spin 40s linear infinite; }
  .o2 { inset: 0; border-color: color-mix(in srgb, var(--accent) 20%, transparent); animation: spin 26s linear infinite reverse; }
  .o3 { inset: 26px; border-color: color-mix(in srgb, var(--teal) 22%, transparent); animation: spin 18s linear infinite; }
  .sat {
    position: absolute;
    width: 5px; height: 5px;
    border-radius: 50%;
    background: var(--accent);
    box-shadow: 0 0 12px 2px color-mix(in srgb, var(--accent) 60%, transparent);
  }
  .s1 { top: 6px; left: 50%; animation: pulse 3.4s ease-in-out infinite; }
  .s2 { bottom: 20px; right: 12px; background: var(--teal); box-shadow: 0 0 12px 2px color-mix(in srgb, var(--teal) 50%, transparent); animation: pulse 4.2s ease-in-out infinite 0.8s; }
  .s3 { top: 34px; left: 8px; background: var(--gold); box-shadow: 0 0 12px 2px color-mix(in srgb, var(--gold) 45%, transparent); animation: pulse 3.8s ease-in-out infinite 1.4s; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @keyframes pulse { 50% { opacity: 0.35; transform: scale(0.7); } }

  .wordmark { display: flex; flex-direction: column; gap: 1px; margin-top: 2px; }
  .wordmark span { font-size: 0.66rem; font-weight: 650; letter-spacing: 0.4em; color: var(--ink-2); }
  .wordmark em { font-family: var(--font-display); font-style: italic; font-size: 0.92rem; color: var(--faint); letter-spacing: 0.01em; }

  .cover h1 { margin: 4px 0 0; font-size: clamp(2.3rem, 10.5vw, 3.3rem); letter-spacing: -0.03em; line-height: 0.96; }
  .cover .lead { margin: 6px 0 0; font-size: 0.95rem; line-height: 1.66; max-width: 44ch; }

  .principles { display: flex; flex-direction: column; gap: 9px; margin-top: 4px; }
  .principles > div {
    display: flex; align-items: flex-start; gap: 12px;
    color: var(--muted); font-size: 0.83rem; line-height: 1.55;
    padding: 12px 14px;
    border: 1px solid var(--line-soft);
    border-radius: 16px;
    background: var(--glass);
    backdrop-filter: blur(18px) saturate(170%);
    -webkit-backdrop-filter: blur(18px) saturate(170%);
    box-shadow: var(--shadow-1);
  }
  .pico {
    width: 30px; height: 30px; border-radius: 10px; flex: none;
    display: grid; place-items: center;
    background: var(--accent-soft);
    border: 1px solid color-mix(in srgb, var(--accent) 18%, transparent);
    color: var(--accent);
    margin-top: 1px;
  }
  .pico.gold { background: var(--gold-soft); border-color: color-mix(in srgb, var(--gold) 24%, transparent); color: var(--gold); }
  .principles strong { color: var(--ink); font-weight: 620; }

  .cta { display: flex; flex-direction: column; gap: 4px; margin-top: 4px; }
  .btn.lg { min-height: 52px; font-size: 0.95rem; border-radius: 16px; }

  .domains { margin-top: 12px; }
  .domainhead { display: flex; align-items: center; gap: 12px; }
  .domainhead .eyebrow { margin: 0; white-space: nowrap; }
  .hair { flex: 1; height: 1px; background: linear-gradient(90deg, var(--line), transparent); }
  .domain-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
  .domain-list .chip { background: var(--surface-2); }
  .fine { color: var(--faint); font-size: 0.72rem; line-height: 1.6; margin: 6px 0 0; }

  .sub { display: flex; align-items: flex-start; gap: 13px; padding: 4px 0 2px; }
  .subtext { flex: 1; min-width: 0; }
  .sub h1 { font-size: clamp(1.9rem, 7.5vw, 2.5rem); margin: 2px 0 5px; line-height: 1; }
  .sub .eyebrow { margin: 0; }
  .sub p { margin: 0; }
  .stepdots { display: flex; gap: 5px; margin-bottom: 10px; }
  .stepdots span { width: 22px; height: 3px; border-radius: 999px; background: var(--surface-3); }
  .stepdots span.on { background: linear-gradient(90deg, var(--accent), var(--teal)); box-shadow: 0 0 10px color-mix(in srgb, var(--accent) 35%, transparent); }

  .formcard { background: var(--surface); }
  .handlebox {
    display: flex; align-items: center; gap: 2px;
    border: 1px solid var(--line); background: var(--surface-2); border-radius: 16px;
    padding-left: 14px; color: var(--faint);
  }
  .handlebox:focus-within { border-color: color-mix(in srgb, var(--accent) 64%, var(--line)); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 13%, transparent); }
  .handlebox input { border: 0; box-shadow: none !important; background: transparent; padding-left: 2px; }

  .experience .label { font-size: 0.84rem; color: var(--ink); }
  .choice {
    position: relative;
    display: flex; flex-direction: column; gap: 5px; align-items: flex-start;
    padding: 14px; text-align: left;
    border: 1px solid var(--line); border-radius: 16px;
    background: var(--surface-2);
    color: var(--muted);
    transition: border-color 220ms ease, background 220ms ease, transform 260ms var(--ease);
  }
  .choice:active { transform: scale(0.985); }
  .choicehead { display: flex; align-items: center; justify-content: space-between; width: 100%; }
  .choice strong { color: var(--ink); font-size: 0.92rem; font-weight: 620; }
  .choice span { font-size: 0.72rem; line-height: 1.45; }
  .choice :global(svg) { color: var(--faint); }
  .checkmark {
    width: 20px; height: 20px; border-radius: 50%;
    display: grid; place-items: center;
    border: 1px solid var(--line);
    opacity: 0; transform: scale(0.7);
    transition: opacity 200ms ease, transform 260ms var(--ease-spring);
  }
  .choice.on {
    border-color: color-mix(in srgb, var(--accent) 50%, var(--line));
    background: var(--accent-mist);
    box-shadow: 0 12px 30px -20px color-mix(in srgb, var(--accent) 60%, transparent);
  }
  .choice.on .checkmark { opacity: 1; transform: none; background: var(--accent); border-color: var(--accent); color: var(--accent-ink); }
  .choice.on .checkmark :global(svg) { color: var(--accent-ink); }
  .choice.on :global(svg), .choice.on strong { color: var(--accent); }

  .idpreview .previewname { font-size: 1.2rem; }
  .bigavatar {
    width: 52px; height: 52px; border-radius: 50%;
    display: grid; place-items: center;
    background:
      radial-gradient(circle at 30% 22%, color-mix(in srgb, var(--tint) 60%, white), transparent 65%),
      var(--tint);
    font-weight: 700; color: #fff; flex: none;
    box-shadow: inset 0 0 0 1px rgb(255 255 255 / 0.24);
  }
</style>
