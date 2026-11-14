<script lang="ts">
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { toasts } from '../lib/toast.svelte';
  import { copy, formatDateTime, relative } from '../lib/util';

  let relayInput = $state(union.relayUrl);
  let aiBase = $state(union.getSetting('ai.baseUrl'));
  let aiModel = $state(union.getSetting('ai.model', 'qwen3.5-0.8b'));
  let aiKey = $state('');
  let busy = $state(false);
  let syncing = $state(false);
  let revealKey = $state(false);

  const hasKey = $derived(Boolean(union.getSecretSetting('ai.apiKey')));
  const bytes = $derived(new TextEncoder().encode(JSON.stringify(union.records)).length);
  const audits = $derived([...union.auditEntries].reverse().slice(0, 30));
  const jobs = $derived([...union.outbox].reverse().slice(0, 20));

  function kb(n: number): string {
    if (n < 1024) return `${n} B`;
    if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
    return `${(n / (1024 * 1024)).toFixed(2)} MB`;
  }

  async function saveRelay() {
    busy = true;
    try {
      await union.configureRelay(relayInput);
      toasts.push(union.relayUrl ? 'Relay configured and directory registration attempted.' : 'Relay disabled. Union is local-only.', 'success');
      relayInput = union.relayUrl;
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function registerNow() {
    busy = true;
    try {
      await union.registerOnRelay();
      toasts.push('Card registered in the relay directory.', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function syncNow() {
    syncing = true;
    try {
      await union.sync();
      toasts.push(union.syncError ? `Sync problem: ${union.syncError}` : 'Sync complete.', union.syncError ? 'warn' : 'success');
    } finally {
      syncing = false;
    }
  }

  async function saveAi() {
    busy = true;
    try {
      await union.setSetting('ai.baseUrl', aiBase.trim().replace(/\/+$/, ''));
      await union.setSetting('ai.model', aiModel.trim() || 'qwen3.5-0.8b');
      if (aiKey.trim()) await union.setSecretSetting('ai.apiKey', aiKey.trim());
      aiKey = '';
      toasts.push('AI provider settings saved.', 'success');
    } finally {
      busy = false;
    }
  }

  async function clearAi() {
    busy = true;
    try {
      await union.setSetting('ai.baseUrl', '');
      await union.setSecretSetting('ai.apiKey', '');
      aiBase = '';
      aiKey = '';
      toasts.push('Provider removed. Union uses the on-device guide.', 'info');
    } finally {
      busy = false;
    }
  }

  async function copyAudit() {
    const text = audits.map((a) => `${a.at} · ${a.action} · ${a.detail} · ${a.hash}`).join('\n');
    const ok = await copy(text);
    toasts.push(ok ? 'Audit extract copied.' : 'Could not copy.', ok ? 'success' : 'warn');
  }
</script>

<TopBar title="Operations" subtitle="Relay, provider, storage and audit" back fallback="/more" />

<main class="screen">
  <section class="section">
    <div class="section-head"><h2>Trust core</h2></div>
    <div class="card pad">
      <div class="kv"><dt>Backend</dt><dd>{union.coreLabel}</dd></div>
      <div class="kv"><dt>Version</dt><dd>{union.core.version || 1}</dd></div>
      <div class="kv"><dt>Hash primitive</dt><dd>SHA-256</dd></div>
      <div class="kv"><dt>Records on device</dt><dd>{union.records.length} · {kb(bytes)}</dd></div>
      <p class="hint" style="margin:8px 0 0">The Rust WebAssembly core verifies the same known SHA-256 vectors as the JavaScript fallback. If WebAssembly is unavailable, Union reports the fallback rather than pretending.</p>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Relay</h2><span class="chip {union.relayReady ? 'green' : 'plain'}">{union.relayReady ? 'registered' : union.relayUrl ? 'configured' : 'local only'}</span></div>
    <div class="card pad stack">
      <p class="small muted" style="margin:0">The relay is optional store-and-forward for sealed envelopes, directory cards and public links. It never receives message plaintext. Leave empty to stay entirely local.</p>
      <div class="field"><label for="op-relay">Relay URL</label><input id="op-relay" type="url" bind:value={relayInput} placeholder="https://union-relay.example.workers.dev" autocapitalize="off" spellcheck="false" /></div>
      <div class="row wrap">
        <button class="btn primary" onclick={saveRelay} disabled={busy}>{busy ? 'Checking…' : 'Save & check health'}</button>
        {#if union.relayUrl}<button class="btn soft" onclick={registerNow} disabled={busy}>Register card</button>{/if}
        {#if union.relayUrl}<button class="btn" onclick={syncNow} disabled={syncing}>{syncing ? 'Syncing…' : 'Sync now'}</button>{/if}
      </div>
      <div class="kv"><dt>Last sync</dt><dd>{union.lastSync ? relative(union.lastSync) : 'not yet'}</dd></div>
      {#if union.syncError}<div class="banner warn"><Icon name="alert" size={16} /><div>{union.syncError}</div></div>{/if}
      <div class="kv"><dt>Network</dt><dd>{union.online ? 'online' : 'offline — local-first'}</dd></div>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Union AI provider</h2><span class="chip {aiBase ? 'green' : 'plain'}">{aiBase ? 'connected' : 'on-device'}</span></div>
    <div class="card pad stack">
      <p class="small muted" style="margin:0">Optional. Point Union at an OpenAI-compatible endpoint you control, such as a local model gateway. Without it, Union uses a deterministic on-device guide built from your records.</p>
      <div class="field"><label for="ai-base">Base URL</label><input id="ai-base" type="url" bind:value={aiBase} placeholder="http://127.0.0.1:8080/v1" autocapitalize="off" spellcheck="false" /></div>
      <div class="grid-2 collapse">
        <div class="field"><label for="ai-model">Model</label><input id="ai-model" bind:value={aiModel} placeholder="qwen3.5-0.8b" autocapitalize="off" spellcheck="false" /></div>
        <div class="field"><label for="ai-key">API key {#if hasKey}<span class="faint">(saved)</span>{/if}</label><input id="ai-key" type="password" bind:value={aiKey} placeholder={hasKey ? 'Leave blank to keep saved key' : 'Optional'} autocomplete="off" /></div>
      </div>
      <div class="row wrap">
        <button class="btn primary" onclick={saveAi} disabled={busy}>Save provider</button>
        {#if aiBase}<button class="btn ghost" onclick={clearAi} disabled={busy}>Remove</button>{/if}
      </div>
      <p class="hint">On Android the key is stored through the device secure store. On the web it is kept in this browser's local storage. A provider you configure can see the prompts you send; it never receives your private keys.</p>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Outbox</h2><span class="hint">{jobs.length}</span></div>
    {#if jobs.length === 0}
      <div class="empty-note">Nothing queued. Messages and posts are written locally first and flushed when a relay is reachable.</div>
    {:else}
      <div class="list">
        {#each jobs as job (job.id)}
          <div class="listitem static">
            <span class="capico"><Icon name="send" size={16} /></span>
            <div class="main"><strong>{job.job}</strong><span>{job.status} · {job.tries} tries{job.lastError ? ` · ${job.lastError}` : ''}</span></div>
            <span class="chip {job.status === 'done' ? 'green' : job.status === 'failed' ? 'rose' : 'amber'}">{job.status}</span>
          </div>
        {/each}
      </div>
    {/if}
  </section>

  <section class="section">
    <div class="section-head"><h2>Audit log</h2><button class="more" onclick={copyAudit}>Copy extract</button></div>
    {#if audits.length === 0}
      <div class="empty-note">No audit entries yet.</div>
    {:else}
      <div class="list">
        {#each audits as a (a.id)}
          <div class="audititem">
            <div class="row between"><span class="mono tiny accent-text">{a.action}</span><span class="tiny faint">{relative(a.at)}</span></div>
            {#if a.detail}<div class="small muted">{a.detail}</div>{/if}
            <div class="tiny faint mono truncate" title={a.hash}>{a.hash.slice(0, 28)}…</div>
          </div>
        {/each}
      </div>
      <p class="tiny faint center" style="margin-top:10px">Audit entries are hash-chained in the same spirit as the ledger. They record operations, not secrets.</p>
    {/if}
  </section>

  <section class="section">
    <div class="section-head"><h2>Integration surface</h2></div>
    <div class="card pad stack">
      <div class="kv"><dt>Portable identity</dt><dd>Ed25519 + X25519 card</dd></div>
      <div class="kv"><dt>Sealed messages</dt><dd>X25519 · HKDF-SHA256 · AES-256-GCM</dd></div>
      <div class="kv"><dt>Backups</dt><dd>PBKDF2-SHA256 310k · AES-256-GCM</dd></div>
      <div class="kv"><dt>Ledger / audit</dt><dd>SHA-256 hash chain</dd></div>
      <div class="kv"><dt>Relay API</dt><dd>register · envelopes · directory · links · spaces</dd></div>
      <p class="hint" style="margin:0">The relay contract is documented in the repository under <span class="code">docs/relay.md</span>. Every write is signed by the identity key; reads of public cards are unauthenticated.</p>
      <div class="row wrap">
        <a class="btn sm ghost" href="https://github.com/OriginLabs1/Union/tree/main/docs" target="_blank" rel="noopener noreferrer"><Icon name="external" size={13} /> Documentation</a>
        <button class="btn sm ghost" onclick={() => copy(`${location.origin}${location.pathname}#/u/${union.identity ? '' : ''}`)}><Icon name="link" size={13} /> Link format</button>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="card pad about">
      <div class="row"><Icon name="wifi-off" size={17} /><strong>Offline-first, by design</strong></div>
      <p class="small muted" style="margin:0">Identity creation, courses, communities, events, pages, AI memory, mentorship and the ledger all work with no network. A relay adds reach, not permission.</p>
      <div class="row wrap"><span class="chip plain">{union.records.length} records</span><span class="chip plain">{kb(bytes)}</span><span class="chip plain">v0.0.1</span></div>
    </div>
  </section>
</main>

<style>
  .capico { width: 36px; height: 36px; border-radius: 11px; flex: none; display: grid; place-items: center; background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2); }
  .listitem.static:hover { border-color: var(--line); }
  .audititem { padding: 9px 12px; border: 1px solid var(--line); border-radius: 11px; background: var(--panel); display: flex; flex-direction: column; gap: 3px; }
  .about { display: flex; flex-direction: column; gap: 9px; }
</style>
