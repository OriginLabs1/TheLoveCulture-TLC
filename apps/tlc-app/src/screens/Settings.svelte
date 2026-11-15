<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { toasts } from '../lib/toast.svelte';
  import { COLORS } from '../lib/crypto';
  import { download, formatDate } from '../lib/util';
  import { shareText } from '../lib/native';
  import { CONSTITUTION_TAGLINE, TELEGRAM_CREATOR, VERSION } from '../lib/version';

  const me = $derived(union.identity);

  let name = $state('');
  let handle = $state('');
  let about = $state('');
  let color = $state('#5b5bd6');
  let experience = $state<'special' | 'super'>('special');
  let saving = $state(false);

  // Load the form once the identity is available, and again after a restore.
  // Typing does not touch `me`, so in-progress edits are never overwritten.
  $effect(() => {
    if (!me) return;
    name = me.name;
    handle = me.handle;
    about = me.about;
    color = me.color;
    experience = me.experience;
  });

  let pass = $state('');
  let includeKeys = $state(true);
  let exporting = $state(false);
  let restoreText = $state('');
  let restorePass = $state('');
  let restoreMode = $state<'merge' | 'replace'>('merge');
  let importing = $state(false);

  let wipeConfirm = $state('');
  let wipeOpen = $state(false);

  const display = $derived(union.currency);
  const fx = $derived(union.fxRate);

  async function saveIdentity() {
    saving = true;
    try {
      await union.updateIdentity({ name, handle, about, color, experience });
      toasts.push('Identity updated. Your keys are unchanged.', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      saving = false;
    }
  }

  async function setCurrency(next: 'USD' | 'NGN') {
    await union.setSetting('fx.display', next);
    toasts.push(`Display currency set to ${next}.`, 'success');
  }

  async function setRate(value: number) {
    if (!Number.isFinite(value) || value <= 0) return;
    await union.setSetting('fx.rate', String(value));
  }

  async function exportBackup() {
    if (pass.length < 8) { toasts.push('Use a passphrase of at least 8 characters.', 'warn'); return; }
    exporting = true;
    try {
      const data = await union.exportFull(pass, includeKeys);
      download(`union-backup-${me?.handle ?? 'identity'}-${new Date().toISOString().slice(0, 10)}.json`, data);
      pass = '';
      toasts.push(includeKeys ? 'Encrypted recovery backup downloaded.' : 'Encrypted records backup downloaded (no keys).', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      exporting = false;
    }
  }

  async function readFile(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    restoreText = await file.text();
    input.value = '';
  }

  async function importBackup() {
    if (!restoreText.trim()) { toasts.push('Choose or paste a backup first.', 'warn'); return; }
    if (restoreMode === 'replace' && !confirm('Replace everything currently on this device with the backup?')) return;
    importing = true;
    try {
      await union.importBackup(restoreText.trim(), restorePass, restoreMode);
      restoreText = '';
      restorePass = '';
      const next = union.identity;
      if (next) {
        name = next.name;
        handle = next.handle;
        about = next.about;
        color = next.color;
        experience = next.experience;
      }
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      importing = false;
    }
  }

  async function wipe() {
    if (wipeConfirm !== 'WIPE') { toasts.push('Type WIPE to confirm.', 'warn'); return; }
    await union.wipe();
    wipeOpen = false;
    wipeConfirm = '';
    toasts.push('This device is clear. The app will now start fresh.', 'info');
  }

  function shareCard() {
    try { shareText(union.myCardText()); } catch { toasts.push('Unlock your identity first.', 'warn'); }
  }
</script>

<TopBar title="Settings & identity" subtitle="Yours to change, yours to carry" back fallback="/more" />

<main class="screen">
  <section class="card pad profilecard">
    <div class="row">
      <Avatar name={name || 'Union'} color={color} size={54} ring />
      <div class="push" style="min-width:0">
        <strong>{name || 'Your name'}</strong>
        <div class="faint small">@{handle || 'handle'} · {experience === 'super' ? 'Super' : 'Special'}</div>
        <div class="mono tiny faint truncate">{me?.id}</div>
      </div>
      <button class="iconbtn sm" onclick={shareCard} aria-label="Share identity card"><Icon name="share" size={16} /></button>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Identity</h2><span class="hint">keys never change here</span></div>
    <div class="card pad stack">
      <div class="field"><label for="set-name">Name</label><input id="set-name" bind:value={name} maxlength="60" /></div>
      <div class="field"><label for="set-handle">Handle</label><input id="set-handle" bind:value={handle} maxlength="24" autocapitalize="off" spellcheck="false" /></div>
      <div class="field"><label for="set-about">About</label><textarea id="set-about" bind:value={about} rows="3" maxlength="240" placeholder="A true sentence about you."></textarea></div>
      <div class="field">
        <div class="label">Colour</div>
        <div class="colors">
          {#each COLORS as c (c)}
            <button class="swatch" class:on={color === c} style={`--sw:${c}`} onclick={() => (color = c)} aria-label={`Choose ${c}`}></button>
          {/each}
        </div>
      </div>
      <div class="field">
        <div class="label">Experience</div>
        <div class="segmented">
          <button class:on={experience === 'special'} onclick={() => (experience = 'special')}>Special</button>
          <button class:on={experience === 'super'} onclick={() => (experience = 'super')}>Super</button>
        </div>
        <p class="hint">Both are complete. Super exposes broader organisational tools; reactivation follows either.</p>
      </div>
      <button class="btn primary block" onclick={saveIdentity} disabled={saving}>{saving ? 'Saving…' : 'Save identity'}</button>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Currency</h2></div>
    <div class="card pad stack">
      <div class="segmented">
        <button class:on={display === 'USD'} onclick={() => setCurrency('USD')}>USD</button>
        <button class:on={display === 'NGN'} onclick={() => setCurrency('NGN')}>Naira</button>
      </div>
      <div class="field">
        <label for="set-rate">Naira per USD</label>
        <input id="set-rate" type="number" min="1" value={fx} onchange={(e) => setRate(Number((e.currentTarget as HTMLInputElement).value))} />
        <p class="hint">Display only. The economy stores both the display amount and its USD equivalent so records remain legible after a rate change.</p>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Backup</h2><span class="hint">encrypted on your device</span></div>
    <div class="card pad stack">
      <div class="field"><label for="set-pass">Passphrase</label><input id="set-pass" type="password" bind:value={pass} autocomplete="new-password" placeholder="At least 8 characters" /></div>
      <label class="check" class:checked={includeKeys}>
        <input type="checkbox" checked={includeKeys} onchange={() => (includeKeys = !includeKeys)} />
        <span>Include private keys<small>A recovery backup. Anyone with this file and the passphrase can become you. Store it offline.</small></span>
      </label>
      <button class="btn soft block" onclick={exportBackup} disabled={exporting || pass.length < 8}><Icon name="download" size={15} /> {exporting ? 'Encrypting…' : 'Download encrypted backup'}</button>
    </div>

    <div class="card pad stack" style="margin-top:10px">
      <div class="label">Restore a backup</div>
      <div class="field"><label for="set-file">Backup file</label><input id="set-file" type="file" accept="application/json,.json" onchange={readFile} /></div>
      <div class="field"><label for="set-text">Or paste the file contents</label><textarea id="set-text" bind:value={restoreText} rows="4" placeholder="Paste the encrypted JSON here" spellcheck="false"></textarea></div>
      <div class="field"><label for="set-rpass">Passphrase</label><input id="set-rpass" type="password" bind:value={restorePass} autocomplete="current-password" /></div>
      <div class="segmented">
        <button class:on={restoreMode === 'merge'} onclick={() => (restoreMode = 'merge')}>Merge</button>
        <button class:on={restoreMode === 'replace'} onclick={() => (restoreMode = 'replace')}>Replace</button>
      </div>
      <button class="btn block" onclick={importBackup} disabled={importing || !restoreText.trim()}>{importing ? 'Decrypting…' : 'Restore'}</button>
      <p class="hint">Replace wipes current records first. Merge keeps what is here and adds what is in the backup. Keys are written only after decryption succeeds.</p>
    </div>
  </section>

  {#if union.installPrompt}
    <section class="section">
      <div class="card pad installcard">
        <div class="row"><Icon name="download" size={18} /><strong>Install Union</strong></div>
        <p class="small muted" style="margin:0">Add Union to your home screen. The installed app opens offline and keeps its own storage.</p>
        <div class="row"><button class="btn primary sm" onclick={() => union.promptInstall()}>Install</button><button class="btn ghost sm" onclick={() => union.dismissInstall()}>Not now</button></div>
      </div>
    </section>
  {/if}

  <section class="section">
    <div class="section-head"><h2>About</h2></div>
    <div class="card pad about">
      <div class="row"><Icon name="fingerprint" size={18} /><strong>Union v{VERSION}</strong><span class="chip plain push">AGPL-3.0-only</span></div>
      <p class="quote" style="margin:2px 0">{CONSTITUTION_TAGLINE}</p>
      <div class="kv"><dt>Installed</dt><dd>{me ? formatDate(me.createdAt) : '—'}</dd></div>
      <div class="kv"><dt>Core</dt><dd>{union.coreLabel}</dd></div>
      <div class="kv"><dt>Storage</dt><dd>local IndexedDB + secure key store</dd></div>
      <div class="row wrap">
        <a class="btn xs ghost" href="https://github.com/OriginLabs1/Union" target="_blank" rel="noopener noreferrer"><Icon name="external" size={13} /> Source</a>
        <a class="btn xs ghost" href={TELEGRAM_CREATOR} target="_blank" rel="noopener noreferrer"><Icon name="send" size={13} /> Creator</a>
        <button class="btn xs ghost" onclick={() => shareCard()}><Icon name="share" size={13} /> My card</button>
      </div>
      <p class="tiny faint" style="margin:0">v0.0.1 is a real, usable foundation. It does not pretend to be a finished economy, a custodial wallet or a substitute for legal and safeguarding advice.</p>
    </div>
  </section>

  <section class="section">
    <div class="card pad dangerzone">
      <div class="row"><Icon name="trash" size={18} /><strong>Clear this device</strong></div>
      <p class="small muted" style="margin:0">Removes the identity, keys and every record on this device. Without a backup this cannot be undone. Union cannot recover it for you.</p>
      {#if !wipeOpen}
        <button class="btn danger" onclick={() => (wipeOpen = true)}>Begin clearing</button>
      {:else}
        <div class="field"><label for="wipe">Type WIPE to confirm</label><input id="wipe" bind:value={wipeConfirm} autocapitalize="characters" /></div>
        <div class="row"><button class="btn danger" onclick={wipe} disabled={wipeConfirm !== 'WIPE'}>Clear permanently</button><button class="btn ghost" onclick={() => { wipeOpen = false; wipeConfirm = ''; }}>Cancel</button></div>
      {/if}
    </div>
  </section>
</main>

<style>
  .profilecard { padding: 15px; }
  .colors { display: flex; flex-wrap: wrap; gap: 8px; }
  .swatch { width: 30px; height: 30px; border-radius: 50%; background: var(--sw); border: 2px solid transparent; box-shadow: inset 0 0 0 1px rgb(255 255 255 / 0.15); }
  .swatch.on { border-color: var(--ink); }
  .about { display: flex; flex-direction: column; gap: 9px; }
  .about .kv { padding: 2px 0; }
  .installcard { display: flex; flex-direction: column; gap: 9px; }
  .dangerzone { display: flex; flex-direction: column; gap: 9px; border-color: color-mix(in srgb, var(--rose) 24%, var(--line)); }
</style>
