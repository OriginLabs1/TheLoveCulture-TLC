<script lang="ts">
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { ActivationProof, LedgerEntry } from '../lib/types';
  import { hashValue } from '../lib/crypto';
  import { copy, download, formatDate, formatDateTime, money, naira } from '../lib/util';
  import {
    ACTIVATION_MIN_USD,
    ASSISTANT_HANDLE,
    ASSISTANT_NAME,
    ASSISTANT_TELEGRAM,
    JOY_MIN_USD,
    REACTIVATION_MIN_USD,
    TELEGRAM_CREATOR,
    TRIAL_DAYS
  } from '../lib/version';

  const activation = $derived(union.activation);
  const verified = $derived(union.activationVerified);
  const request = $derived(union.currentActivationRequest);
  const joy = $derived(union.joyPlan);
  const plans = $derived(union.plans.filter((p) => p.kind !== 'joy'));
  const ledger = $derived([...union.ledger].reverse());

  let experience = $state<'special' | 'super'>(union.identity?.experience ?? 'special');
  let method = $state('Bank transfer');
  let reference = $state('');
  let note = $state('');
  let busy = $state(false);
  let verify = $state<{ ok: boolean; brokenAt?: string; count: number } | null>(null);
  let verifying = $state(false);

  let proofFile = $state<File | null>(null);
  let proofText = $state('');
  let proofPreview = $state('');
  let proofError = $state('');
  let proofBusy = $state(false);

  let giftAmount = $state(10);
  let giftMethod = $state('Bank transfer');
  let giftRef = $state('');
  let giftMemo = $state('Digital Giving');

  const METHODS = ['Bank transfer', 'Card', 'Mobile money', 'Naira transfer', 'Cash', 'Other'];

  const statusChip = $derived.by(() => {
    if (activation.status === 'active') return { label: 'Verified', cls: 'green' };
    if (activation.status === 'trial') return { label: 'Trial', cls: 'teal' };
    if (activation.status === 'grace') return { label: 'Grace period', cls: 'amber' };
    if (activation.status === 'expired') return { label: 'Expired', cls: 'rose' };
    return { label: 'Not requested', cls: 'plain' };
  });

  const flow = $derived.by(() => {
    let step = 0;
    if (request) step = 1;
    if (request?.proof) step = 2;
    if (request?.status === 'verified' || activation.status === 'active') step = 3;
    return step;
  });

  const trialDaysLeft = $derived.by(() => {
    if (!activation.until || activation.status !== 'trial') return 0;
    const ms = new Date(activation.until).getTime() - Date.now();
    return Math.max(0, Math.ceil(ms / 86400000));
  });

  const requestText = $derived.by(() => {
    if (!request || !union.identity) return '';
    const id = union.identity;
    return [
      'UNION ACTIVATION REQUEST',
      '',
      `Name: ${id.name} (@${id.handle})`,
      `Identity: ${id.id}`,
      `Request: ${request.id}`,
      `Created: ${request.at}`,
      `Experience: ${request.experience === 'super' ? 'Super' : 'Special'}`,
      `Token: $${request.amountUsd} USD minimum · ${naira(request.amountUsd, union.fxRate)} equivalent`,
      `Method: ${request.method}`,
      request.reference ? `Reference: ${request.reference}` : '',
      request.note ? `Note: ${request.note}` : '',
      `Proof: ${request.proof ? `${request.proof.name} (${request.proof.type || 'file'})` : 'not attached yet'}`,
      request.proof ? `Proof SHA-256: ${request.proof.hash}` : '',
      `Trial: ${request.trialUntil ? formatDate(request.trialUntil) : '—'}`,
      `Status: ${request.status}`,
      '',
      'Sent from Union v0.0.1 · test mode. No funds move inside the app.'
    ].filter(Boolean).join('\n');
  });

  function display(entry: LedgerEntry): string {
    return money(entry.amount, entry.currency);
  }

  async function submitRequest() {
    if (!union.identity) return;
    busy = true;
    try {
      await union.requestActivation({ experience, method, reference, note });
      note = '';
      reference = '';
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  function readAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result ?? ''));
      reader.onerror = () => reject(reader.error ?? new Error('Could not read the file.'));
      reader.readAsDataURL(file);
    });
  }

  async function readProofFile(event: Event) {
    proofError = '';
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 1_200_000) {
      proofError = 'Keep the proof under 1.2 MB. A compressed screenshot or a short text file works well.';
      input.value = '';
      return;
    }
    const isImage = file.type.startsWith('image/');
    const isText = file.type.startsWith('text/') || /\.(txt|md|markdown)$/i.test(file.name);
    if (!isImage && !isText) {
      proofError = 'Choose a screenshot, photo, .txt or .md file.';
      input.value = '';
      return;
    }
    proofFile = file;
    proofText = '';
    proofPreview = isImage ? await readAsDataUrl(file) : '';
    if (isText) proofText = (await file.text()).slice(0, 120_000);
  }

  function clearProof() {
    proofFile = null;
    proofText = '';
    proofPreview = '';
    proofError = '';
  }

  async function attachProof() {
    if (!request) return;
    proofError = '';
    if (!proofFile && !proofText.trim()) {
      proofError = 'Choose a screenshot or type / paste the receipt details.';
      return;
    }
    proofBusy = true;
    try {
      const content = proofPreview || proofText.trim() || proofFile?.name || '';
      const hash = await hashValue(content);
      const proof: ActivationProof = {
        name: proofFile?.name ?? 'typed-proof.txt',
        type: proofFile?.type || 'text/plain',
        size: proofFile?.size ?? content.length,
        hash,
        at: new Date().toISOString(),
        ...(proofPreview ? { dataUrl: proofPreview } : { text: proofText.trim() })
      };
      await union.attachActivationProof(request.id, proof);
      clearProof();
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      proofBusy = false;
    }
  }

  async function copyPackage() {
    const ok = await copy(requestText);
    toasts.push(ok ? 'Activation package copied.' : 'Could not copy the package.', ok ? 'success' : 'warn');
  }

  function downloadPackage() {
    if (!requestText) return;
    download(`union-activation-${union.identity?.handle ?? 'request'}.md`, requestText, 'text/markdown');
    toasts.push('Activation package saved. Attach it with your proof in Telegram.', 'info');
  }

  function downloadProofFile() {
    const proof = request?.proof;
    if (!proof) return;
    if (proof.dataUrl) {
      const [head, data] = proof.dataUrl.split(',');
      const mime = head.match(/data:([^;]+)/)?.[1] ?? 'image/png';
      const bytes = Uint8Array.from(atob(data ?? ''), (c) => c.charCodeAt(0));
      download(proof.name || 'activation-proof.png', new Blob([bytes], { type: mime }), mime);
      return;
    }
    download(proof.name || 'activation-proof.txt', proof.text ?? '', proof.type || 'text/plain');
  }

  async function openAssistant() {
    await copyPackage();
    window.open(ASSISTANT_TELEGRAM, '_blank', 'noopener');
    toasts.push('Paste the copied package in the chat, then attach your proof.', 'info');
  }

  async function verifyInTestMode() {
    if (!request) return;
    busy = true;
    try {
      await union.verifyActivationRequest(request.id, request.method, request.reference || 'verified');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function reactivate() {
    busy = true;
    try {
      await union.reactivate(method, reference);
      reference = '';
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function startTrial() {
    busy = true;
    try {
      await union.startJoyTrial();
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function endJoy() {
    busy = true;
    try {
      await union.endJoyPlan();
      toasts.push('Joy/Giving Plan cancelled. Nothing will be charged.', 'info');
    } finally {
      busy = false;
    }
  }

  function requestJoy() {
    union.markJoyRequested();
    window.open(TELEGRAM_CREATOR, '_blank', 'noopener');
    toasts.push('Send the courtesy words “Joyful Giver” in Telegram. A trial is already recorded here.', 'info');
  }

  async function recordGift() {
    busy = true;
    try {
      await union.recordGift(Number(giftAmount), giftMethod, giftRef, giftMemo);
      giftRef = '';
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function runVerify() {
    verifying = true;
    try {
      verify = await union.verifyLedger();
      toasts.push(verify.ok ? `Ledger verified: ${verify.count} entries.` : 'Ledger verification failed.', verify.ok ? 'success' : 'error');
    } finally {
      verifying = false;
    }
  }
</script>

<TopBar title="Economic Coordination" subtitle="Activation, plans, transparent records" back fallback="/more" />

<main class="screen economy">
  <div class="banner warn risk-stripes">
    <Icon name="alert" size={17} />
    <div><strong>v0.0.1 test mode.</strong> Every economic record here is honest about itself: no real funds move inside the app, and no withdrawable value is promised. A production economy needs a backed provider, custody and legal support.</div>
  </div>

  <section class="section">
    <div class="card hero-card pad activation-hero">
      <div class="hero-top">
        <div class="hero-copy">
          <span class="eyebrow accent">Activation Token</span>
          <div class="price"><span class="cur">$</span>{ACTIVATION_MIN_USD}<span class="per">minimum</span></div>
          <p class="small muted">One-time. Naira equivalent accepted. A {TRIAL_DAYS}-day trial opens while your proof is reviewed.</p>
        </div>
        <div class="meter-ring {verified ? '' : 'gold'}" style={`--p:${flow * 25};--ring-size:78px`}><span>{flow}/4</span></div>
      </div>

      <div class="flow">
        {#each ['Request', 'Proof', 'Review', 'Verified'] as label, i (label)}
          <div class="flowstep" class:done={flow > i} class:on={flow === i}>
            <span class="fnum">{i + 1}</span>
            <span class="flabel">{label}</span>
          </div>
        {/each}
      </div>

      <div class="herostatus">
        <span class="chip {statusChip.cls}">{statusChip.label}</span>
        {#if activation.status === 'trial' && trialDaysLeft}
          <span class="small muted">{trialDaysLeft} trial days left</span>
        {:else if activation.until}
          <span class="small muted">Period ends {formatDate(activation.until)}</span>
        {/if}
      </div>
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>1 · Activation Request</h2>{#if request}<span class="chip {request.status === 'verified' ? 'green' : request.status === 'proof-submitted' ? 'teal' : 'accent'}">{request.status.replace('-', ' ')}</span>{/if}</div>

    {#if !request}
      <div class="card pad stack">
        <p class="small muted" style="margin:0">Tell Union how the $80-equivalent Activation Token was sent. This creates a signed request and opens the trial; it does not take money from this app.</p>

        <div class="grid-2 collapse">
          <div class="field">
            <label for="eco-exp">Experience</label>
            <select id="eco-exp" bind:value={experience}><option value="special">Special</option><option value="super">Super</option></select>
          </div>
          <div class="field">
            <label for="eco-method">How it was sent</label>
            <select id="eco-method" bind:value={method}>{#each METHODS as m (m)}<option value={m}>{m}</option>{/each}</select>
          </div>
        </div>
        <div class="field"><label for="eco-ref">Reference <span class="faint">(optional)</span></label><input id="eco-ref" bind:value={reference} maxlength="80" placeholder="Receipt number or short note" /></div>
        <div class="field"><label for="eco-note">Message to the reviewer <span class="faint">(optional)</span></label><textarea id="eco-note" bind:value={note} rows="2" maxlength="240" placeholder="Anything that helps match your payment"></textarea></div>

        <div class="amountline">
          <div><span class="faint tiny">Payable equivalent</span><strong class="serif">{naira(ACTIVATION_MIN_USD, union.fxRate)}</strong></div>
          <span class="chip plain">{money(ACTIVATION_MIN_USD, 'USD')} min · rate {union.fxRate.toLocaleString()}</span>
        </div>

        <button class="btn primary block lg" onclick={submitRequest} disabled={busy}>
          <Icon name="send" size={16} /> {busy ? 'Recording request…' : 'Send Activation Request'}
        </button>
        <p class="tiny faint" style="margin:0">Requesting starts a {TRIAL_DAYS}-day trial. Verification requires the proof in step 2.</p>
      </div>
    {:else}
      <div class="card pad stack">
        <div class="requesthead">
          <span class="orb gold"><Icon name="key" size={18} /></span>
          <div class="push">
            <strong class="serif" style="font-size:1.06rem">Request {request.id.slice(-6).toUpperCase()}</strong>
            <p class="tiny faint" style="margin:1px 0 0">Opened {formatDateTime(request.at)} · {request.experience === 'super' ? 'Super' : 'Special'} experience</p>
          </div>
          <span class="chip {request.status === 'verified' ? 'green' : request.status === 'proof-submitted' ? 'teal' : 'accent'}">{request.status.replace('-', ' ')}</span>
        </div>

        <dl class="kvlist">
          <div class="kv"><dt>Amount</dt><dd>{money(request.amountUsd, 'USD')} · {naira(request.amountUsd, union.fxRate)}</dd></div>
          <div class="kv"><dt>Method</dt><dd>{request.method}</dd></div>
          {#if request.reference}<div class="kv"><dt>Reference</dt><dd class="mono">{request.reference}</dd></div>{/if}
          {#if request.note}<div class="kv"><dt>Note</dt><dd>{request.note}</dd></div>{/if}
          {#if request.trialUntil}<div class="kv"><dt>Trial until</dt><dd>{formatDate(request.trialUntil)}</dd></div>{/if}
          {#if request.verifiedAt}<div class="kv"><dt>Verified</dt><dd>{formatDate(request.verifiedAt)} · {request.verifiedBy}</dd></div>{/if}
        </dl>

        {#if request.status === 'requested'}
          <div class="banner info"><Icon name="info" size={16} /><div>Request recorded. Add your Activation Proof below to complete the signal.</div></div>
        {/if}
      </div>
    {/if}
  </section>

  {#if request}
    <section class="section">
      <div class="section-head"><h2>2 · Activation Proof</h2>{#if request.proof}<span class="chip green"><Icon name="check" size={12} /> attached</span>{/if}</div>
      <div class="card pad stack">
        {#if request.proof}
          <div class="proofcard">
            {#if request.proof.dataUrl}
              <img class="proofimg" src={request.proof.dataUrl} alt="Activation proof preview" />
            {:else}
              <div class="prooftext mono">{(request.proof.text ?? '').slice(0, 700)}</div>
            {/if}
            <div class="proofmeta">
              <strong>{request.proof.name}</strong>
              <span class="tiny faint">{Math.max(1, Math.round(request.proof.size / 1024))} KB · attached {formatDateTime(request.proof.at)}</span>
              <span class="tiny faint mono break">SHA-256 {request.proof.hash.slice(0, 32)}…</span>
            </div>
          </div>
          <div class="row wrap">
            <button class="btn sm soft" onclick={attachProof} disabled={proofBusy || !proofFile && !proofText.trim()}>Replace with selection</button>
            <button class="btn sm ghost" onclick={downloadProofFile}><Icon name="download" size={14} /> Save proof copy</button>
            <button class="btn sm ghost" onclick={clearProof}>Clear selection</button>
          </div>
        {/if}

        <label class="dropzone" class:hasproof={Boolean(request.proof)}>
          <input class="vh" type="file" accept="image/*,.txt,.md,text/plain,text/markdown" onchange={readProofFile} />
          <span class="orb"><Icon name="upload" size={19} /></span>
          <span class="dropcopy">
            <strong>{proofFile ? proofFile.name : request.proof ? 'Choose a replacement proof' : 'Upload a receipt'}</strong>
            <span class="tiny faint">Screenshot, photo, .txt or .md · under 1.2 MB</span>
          </span>
        </label>

        {#if proofPreview}
          <img class="proofimg preview" src={proofPreview} alt="Selected proof preview" />
        {/if}

        <div class="field">
          <label for="eco-prooftext">Or type / paste the receipt details</label>
          <textarea id="eco-prooftext" bind:value={proofText} rows="4" placeholder="Amount, date, bank, sender name, receipt number, and anything else that verifies the transfer." class="mono"></textarea>
        </div>

        {#if proofError}<div class="banner danger"><Icon name="alert" size={16} /><div>{proofError}</div></div>{/if}

        <button class="btn primary block" onclick={attachProof} disabled={proofBusy || (!proofFile && !proofText.trim())}>
          <Icon name="shield" size={16} /> {proofBusy ? 'Hashing proof…' : request.proof ? 'Attach replacement proof' : 'Attach Activation Proof'}
        </button>
        <p class="tiny faint" style="margin:0">The proof is hashed locally first, so the reviewer can confirm the exact file. It stays on this device until you send it.</p>
      </div>
    </section>

    <section class="section">
      <div class="section-head"><h2>3 · Send & verify</h2></div>
      <div class="card pad stack assistantcard">
        <div class="assistanthead">
          <span class="orb"><Icon name="spark" size={19} /></span>
          <div class="push">
            <strong>{ASSISTANT_NAME}</strong>
            <p class="tiny faint" style="margin:1px 0 0">{ASSISTANT_HANDLE} · reviews proofs and reports to Union's Creator</p>
          </div>
          <span class="live"><span></span> online</span>
        </div>

        <pre class="package mono">{requestText}</pre>

        <div class="grid-2 collapse">
          <button class="btn soft" onclick={copyPackage}><Icon name="copy" size={15} /> Copy package</button>
          <button class="btn soft" onclick={downloadPackage}><Icon name="download" size={15} /> Save package</button>
        </div>
        <button class="btn primary block" onclick={openAssistant}>
          <Icon name="external" size={15} /> Open {ASSISTANT_HANDLE} & send
        </button>

        <ol class="steps">
          <li>Copy or save the activation package above.</li>
          <li>Open the assistant and paste the package.</li>
          <li>Attach your receipt file (or the saved proof copy).</li>
          <li>The Creator verifies it and your status becomes <strong>Verified</strong>.</li>
        </ol>

        <div class="banner info">
          <Icon name="lock" size={16} />
          <div>Your personal phone number is never shown to users. Only the public assistant and the Creator's Telegram are linked.</div>
        </div>

        {#if request.proof && request.status !== 'verified'}
          <details class="testverify">
            <summary class="small muted">Test mode · simulate verification</summary>
            <p class="hint">For v0.0.1 demonstrations only. Real verification happens with {ASSISTANT_NAME} after the proof is reviewed.</p>
            <button class="btn sm soft" onclick={verifyInTestMode} disabled={busy}><Icon name="check" size={14} /> Mark verified in test mode</button>
          </details>
        {:else if request.status === 'verified'}
          <div class="banner good"><Icon name="check" size={16} /><div><strong>Activation verified.</strong> Reactivation is now available at ${REACTIVATION_MIN_USD}/month equivalent.</div></div>
        {/if}
      </div>
    </section>
  {/if}

  {#if verified}
    <section class="section">
      <div class="section-head"><h2>Reactivation Token (Plan)</h2><span class="chip gold">${REACTIVATION_MIN_USD}/month</span></div>
      <div class="card pad stack">
        <div class="row">
          <span class="orb gold"><Icon name="refresh" size={18} /></span>
          <div class="push">
            <strong>Keep the environment open</strong>
            <p class="tiny faint" style="margin:2px 0 0">After a verified Activation Request and Proof. Naira equivalent accepted. 30 days per period.</p>
          </div>
        </div>
        <div class="amountline">
          <div><span class="faint tiny">Payable equivalent</span><strong class="serif">{naira(REACTIVATION_MIN_USD, union.fxRate)}</strong></div>
          <span class="chip plain">{money(REACTIVATION_MIN_USD, 'USD')} / month</span>
        </div>
        {#if activation.until}
          <div class="kv"><dt>Current period ends</dt><dd>{formatDate(activation.until)}</dd></div>
        {/if}
        <div class="grid-2 collapse">
          <div class="field"><label for="re-method">How it was sent</label><select id="re-method" bind:value={method}>{#each METHODS as m (m)}<option value={m}>{m}</option>{/each}</select></div>
          <div class="field"><label for="re-ref">Reference <span class="faint">(optional)</span></label><input id="re-ref" bind:value={reference} maxlength="80" /></div>
        </div>
        <button class="btn soft block" onclick={reactivate} disabled={busy}><Icon name="refresh" size={15} /> {busy ? 'Recording…' : `Record reactivation · $${REACTIVATION_MIN_USD}/month`}</button>
        <p class="tiny faint" style="margin:0">Records a test-mode ledger entry. It does not charge you.</p>
      </div>
    </section>
  {:else if request}
    <section class="section">
      <div class="card pad">
        <div class="row">
          <span class="orb"><Icon name="clock" size={18} /></span>
          <div class="push">
            <strong>Reactivation opens after verification</strong>
            <p class="tiny faint" style="margin:2px 0 0">The ${REACTIVATION_MIN_USD}/month Reactivation Token (Plan) becomes available once your Activation Request and Proof are verified.</p>
          </div>
        </div>
      </div>
    </section>
  {/if}

  <section class="section">
    <div class="section-head"><h2>Joy/Giving Plans</h2><span class="hint">optional · ${JOY_MIN_USD} min</span></div>
    <div class="card pad stack">
      <div class="row">
        <span class="orb gold"><Icon name="heart" size={18} /></span>
        <div class="push">
          <strong>Appreciation, not a subscription trap</strong>
          <div class="faint small">The exact-named plan for acknowledging the Creator of the Union concept.</div>
        </div>
      </div>
      {#if joy}
        <div class="banner good"><Icon name="gift" size={17} /><div><strong>{joy.status === 'trial' ? `Trial active until ${formatDate(joy.until ?? '')}` : 'Plan active'}</strong> · no automatic charge. Cancel whenever you wish.</div></div>
        <div class="row wrap">
          <button class="btn sm ghost" onclick={endJoy} disabled={busy}>End plan</button>
          <button class="btn sm soft" onclick={requestJoy}><Icon name="external" size={13} /> Open Telegram</button>
        </div>
      {:else}
        <p class="small muted" style="margin:0">Request the exact-named plan by sending the courtesy words <strong>“Joyful Giver”</strong> to <a href={TELEGRAM_CREATOR} target="_blank" rel="noopener noreferrer">t.me/CollinsRoyalMedium</a>. A {TRIAL_DAYS}-day trial starts here immediately and nothing is charged automatically.</p>
        <div class="row wrap">
          <button class="btn primary" onclick={startTrial} disabled={busy}><Icon name="gift" size={14} /> Start {TRIAL_DAYS}-day trial</button>
          <button class="btn soft" onclick={requestJoy}><Icon name="external" size={14} /> Request “Joyful Giver”</button>
        </div>
      {/if}
    </div>
  </section>

  <section class="section">
    <div class="section-head"><h2>Digital Giving record</h2><span class="hint">test mode</span></div>
    <div class="card pad stack">
      <div class="grid-2 collapse">
        <div class="field"><label for="gift-amt">Amount (USD)</label><input id="gift-amt" type="number" min="1" step="0.01" bind:value={giftAmount} /></div>
        <div class="field"><label for="gift-method">Method</label><select id="gift-method" bind:value={giftMethod}>{#each METHODS as m (m)}<option value={m}>{m}</option>{/each}</select></div>
      </div>
      <div class="field"><label for="gift-ref">Reference (optional)</label><input id="gift-ref" bind:value={giftRef} maxlength="80" /></div>
      <div class="field"><label for="gift-memo">Memo</label><input id="gift-memo" bind:value={giftMemo} maxlength="120" /></div>
      <button class="btn soft block" onclick={recordGift} disabled={busy || Number(giftAmount) <= 0}><Icon name="heart" size={15} /> Record a gift (no funds move)</button>
      <p class="tiny faint" style="margin:0">A gift is recorded as a transparent entry. It is not a purchase of status, reach or favour.</p>
    </div>
  </section>

  {#if union.ledger.length}
    <section class="section">
      <div class="section-head">
        <h2>Ledger</h2>
        <button class="more" onclick={runVerify} disabled={verifying}>{verifying ? 'Verifying…' : 'Verify chain'}</button>
      </div>
      {#if verify}
        <div class="banner {verify.ok ? 'good' : 'danger'}" style="margin-bottom:10px">
          <Icon name={verify.ok ? 'shield' : 'alert'} size={17} />
          <div>{verify.ok ? `Hash chain verified across ${verify.count} entries.` : `Chain broken at ${verify.brokenAt}. Do not trust later entries without inspection.`}</div>
        </div>
      {/if}
      <div class="list">
        {#each ledger.slice(0, 40) as entry (entry.id)}
          <div class="listitem static ledgeritem">
            <span class="capico"><Icon name={entry.kind === 'activation' ? 'key' : entry.kind === 'reactivation' ? 'refresh' : entry.kind === 'joy' ? 'gift' : entry.kind === 'transfer' ? 'send' : 'coins'} size={16} /></span>
            <div class="main">
              <strong>{entry.memo || entry.kind}</strong>
              <span>{formatDateTime(entry.at)} · {entry.kind} · {entry.token}</span>
              <span class="tiny faint mono truncate" title={entry.hash}>{entry.hash.slice(0, 18)}… → {entry.prevHash.slice(0, 10)}…</span>
            </div>
            <div class="amount">
              <strong>{display(entry)}</strong>
              {#if entry.currency !== 'USD'}<span class="tiny faint">${entry.amountUsd.toFixed(2)}</span>{/if}
              <span class="chip plain tiny">{entry.testMode ? 'test' : 'live'}</span>
            </div>
          </div>
        {/each}
      </div>
      <p class="tiny faint center" style="margin-top:12px">Each entry hashes the previous hash. Changing history breaks the chain visibly.</p>
    </section>
  {/if}

  {#if plans.length}
    <section class="section">
      <div class="section-head"><h2>Plans on this identity</h2></div>
      <div class="list">
        {#each plans as p (p.id)}
          <div class="listitem static">
            <span class="capico"><Icon name="wallet" size={16} /></span>
            <div class="main"><strong>{p.name}</strong><span>{money(p.priceUsd, 'USD')} · {p.period} · {p.status}</span></div>
            <span class="chip {p.status === 'active' ? 'green' : p.status === 'cancelled' ? 'rose' : ''}">{p.status}</span>
          </div>
        {/each}
      </div>
    </section>
  {/if}
</main>

<style>
  .economy { padding-top: 16px; }
  .activation-hero { padding: 21px; }
  .hero-top { display: flex; align-items: flex-start; gap: 16px; }
  .hero-copy { flex: 1; min-width: 0; }
  .hero-copy .eyebrow { margin-bottom: 8px; }
  .price {
    display: flex;
    align-items: baseline;
    gap: 3px;
    font-family: var(--font-display);
    font-size: 2.9rem;
    font-weight: 500;
    line-height: 0.98;
    letter-spacing: -0.045em;
  }
  .price .cur { font-size: 1.5rem; align-self: flex-start; margin-top: 8px; color: var(--gold); }
  .price .per {
    font-family: var(--font-sans);
    font-size: 0.62rem;
    font-weight: 650;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: var(--faint);
    margin-left: 7px;
  }
  .hero-copy p { margin: 9px 0 0; max-width: 34ch; line-height: 1.5; }

  .flow {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 7px;
    margin-top: 20px;
  }
  .flowstep { display: flex; flex-direction: column; gap: 6px; }
  .fnum {
    height: 4px;
    border-radius: 999px;
    background: var(--surface-3);
    font-size: 0;
    transition: background 500ms var(--ease);
  }
  .flowstep.done .fnum { background: linear-gradient(90deg, var(--accent), var(--teal)); box-shadow: 0 0 10px color-mix(in srgb, var(--accent) 40%, transparent); }
  .flowstep.on .fnum { background: linear-gradient(90deg, var(--gold), var(--gold-2)); box-shadow: 0 0 10px color-mix(in srgb, var(--gold) 40%, transparent); }
  .flabel { font-size: 0.64rem; font-weight: 600; letter-spacing: 0.04em; color: var(--faint); }
  .flowstep.done .flabel, .flowstep.on .flabel { color: var(--ink-2); }
  .herostatus { display: flex; align-items: center; gap: 10px; margin-top: 14px; }

  .amountline {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 13px 15px;
    border: 1px solid var(--line-soft);
    border-radius: 14px;
    background: var(--surface-2);
  }
  .amountline > div { display: flex; flex-direction: column; gap: 2px; }
  .amountline strong { font-size: 1.2rem; line-height: 1; }

  .requesthead { display: flex; align-items: flex-start; gap: 12px; }
  .kvlist { margin: 0; }
  .kvlist .kv dd { max-width: 62%; }

  .dropzone {
    display: flex;
    align-items: center;
    gap: 13px;
    padding: 16px;
    border: 1px dashed var(--line-strong);
    border-radius: 18px;
    background: var(--surface);
    cursor: pointer;
    transition: border-color 220ms ease, background 220ms ease;
  }
  .dropzone:hover { border-color: color-mix(in srgb, var(--accent) 45%, var(--line)); background: var(--accent-mist); }
  .dropzone.hasproof { border-style: solid; }
  .dropcopy { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .dropcopy strong { font-size: 0.86rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

  .proofcard {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px;
    border: 1px solid var(--line);
    border-radius: 16px;
    background: var(--surface-2);
  }
  .proofimg { width: 100%; max-height: 260px; object-fit: contain; border-radius: 10px; background: #05060a; }
  .proofimg.preview { max-height: 220px; }
  .prooftext { font-size: 0.72rem; color: var(--muted); white-space: pre-wrap; max-height: 180px; overflow: auto; line-height: 1.5; }
  .proofmeta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .proofmeta strong { font-size: 0.84rem; }
  .break { overflow-wrap: anywhere; }

  .assistanthead { display: flex; align-items: flex-start; gap: 12px; }
  .live {
    display: inline-flex; align-items: center; gap: 5px;
    font-size: 0.64rem; font-weight: 620; color: var(--green);
    border: 1px solid color-mix(in srgb, var(--green) 28%, transparent);
    background: var(--green-soft);
    border-radius: 999px; padding: 3px 8px; flex: none;
  }
  .live span { width: 5px; height: 5px; border-radius: 50%; background: var(--green); box-shadow: 0 0 8px var(--green); animation: pulse 2.4s ease-in-out infinite; }
  @keyframes pulse { 50% { opacity: 0.35; } }

  .package {
    margin: 0;
    padding: 13px 14px;
    max-height: 190px;
    overflow: auto;
    font-size: 0.66rem;
    line-height: 1.55;
    white-space: pre-wrap;
    color: var(--muted);
    background: var(--surface-2);
    border: 1px solid var(--line-soft);
    border-radius: 12px;
  }
  .steps { margin: 0; padding-left: 20px; color: var(--muted); font-size: 0.79rem; line-height: 1.65; }
  .steps li::marker { color: var(--accent); font-weight: 700; }
  .steps strong { color: var(--ink); }

  .testverify summary { cursor: pointer; padding: 4px 0; }
  .testverify { display: flex; flex-direction: column; gap: 6px; }
  .testverify .hint { margin: 0 0 4px; }

  .ledgeritem .main span { white-space: normal; }
  .ledgeritem .amount { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; flex: none; }
  .ledgeritem .amount strong { font-size: 0.86rem; }
  .capico { width: 36px; height: 36px; border-radius: 12px; flex: none; display: grid; place-items: center; background: var(--accent-soft); border: 1px solid color-mix(in srgb, var(--accent) 16%, transparent); color: var(--accent); }
  .listitem.static:hover { border-color: var(--line); }
</style>
