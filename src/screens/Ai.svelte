<script lang="ts">
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { AiMessage } from '../lib/types';
  import { relative } from '../lib/util';

  const threads = $derived(union.aiThreads);
  const provider = $derived(union.getSetting('ai.baseUrl') ? 'Connected provider' : 'On-device guide');

  let tab = $state<'chat' | 'memory' | 'projects'>('chat');
  let activeId = $state('');
  let draft = $state('');
  let busy = $state(false);
  let memoryText = $state('');
  let projectName = $state('');
  let projectAbout = $state('');
  let showNew = $state(false);

  const active = $derived(threads.find((t) => t.id === activeId) ?? threads[0]);
  const messages = $derived(active ? union.aiMessagesIn(active.id) : []);

  // Keep the picker and the composer pointed at the same conversation once
  // the first thread exists (or after the selected one is deleted).
  $effect(() => {
    if (threads.length && !threads.some((t) => t.id === activeId)) activeId = threads[0].id;
  });

  async function newThread() {
    busy = true;
    try {
      const thread = await union.createAiThread();
      activeId = thread.id;
      tab = 'chat';
      showNew = false;
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function send() {
    const text = draft.trim();
    if (!text || !active) return;
    draft = '';
    busy = true;
    try {
      await union.sendAi(active.id, text);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function resolve(message: AiMessage, approve: boolean) {
    busy = true;
    try {
      await union.resolveAiAction(message, approve);
    } finally {
      busy = false;
    }
  }

  async function addMemory() {
    if (!memoryText.trim()) return;
    await union.addMemory(memoryText);
    memoryText = '';
    toasts.push('Memory saved with your approval.', 'success');
  }

  async function removeMemory(id: string) {
    await union.deleteMemory(id);
    toasts.push('Memory removed.', 'info');
  }

  async function addProject() {
    if (!projectName.trim()) { toasts.push('Name the project.', 'warn'); return; }
    await union.createProject(projectName.trim(), projectAbout.trim());
    projectName = '';
    projectAbout = '';
    toasts.push('Project created.', 'success');
  }

  function actionLabel(state: string): string {
    if (state === 'proposed') return 'Needs your approval';
    if (state === 'done') return 'Approved and completed';
    if (state === 'declined') return 'Declined';
    if (state === 'failed') return 'Failed';
    return state;
  }
</script>

<TopBar title="Union AI" subtitle={provider} back fallback="/more">
  {#snippet actions()}
    <button class="iconbtn sm" onclick={() => (showNew = !showNew)} aria-label="New conversation"><Icon name="plus" size={17} /></button>
  {/snippet}
</TopBar>

<main class="screen">
  <div class="tabs">
    <button class:on={tab === 'chat'} onclick={() => (tab = 'chat')}><Icon name="message" size={13} /> Conversations</button>
    <button class:on={tab === 'memory'} onclick={() => (tab = 'memory')}><Icon name="notes" size={13} /> Memory</button>
    <button class:on={tab === 'projects'} onclick={() => (tab = 'projects')}><Icon name="book" size={13} /> Projects</button>
  </div>

  {#if showNew}
    <div class="card pad stack" style="margin-top:12px">
      <p class="small muted" style="margin:0">A new conversation starts with no assumptions. Memory is shared only when you choose it.</p>
      <button class="btn primary block" onclick={newThread} disabled={busy}>Start a conversation</button>
    </div>
  {/if}

  {#if tab === 'chat'}
    <div class="banner info" style="margin-top:12px">
      <Icon name="spark" size={17} />
      <div>
        <strong>{provider === 'On-device guide' ? 'Local guide.' : 'Provider connected.'}</strong>
        {provider === 'On-device guide'
          ? ' Union answers from your own records and a small built-in reasoning guide. Nothing leaves the device.'
          : ' Requests go to the provider you configured in Operations. You can remove it at any time.'}
      </div>
    </div>

    {#if threads.length === 0}
      <Empty icon="spark" title="No conversation yet" text="Ask about a record, draft a page, prepare a gathering, or think through a decision. Actions are proposed, never taken silently.">
        {#snippet action()}<button class="btn primary" onclick={newThread} disabled={busy}>Start a conversation</button>{/snippet}
      </Empty>
    {:else}
      <div class="threadbar">
        <select bind:value={activeId} aria-label="Conversation">
          {#each threads as t (t.id)}
            <option value={t.id}>{t.title} · {relative(t.updatedAt)}</option>
          {/each}
        </select>
      </div>

      <section class="chat">
        {#if messages.length === 0}
          <div class="empty-note">Ask one useful question. Short context is enough.</div>
        {/if}
        {#each messages as m (m.id)}
          <div class="bubble {m.role}">
            {#if m.role === 'assistant'}
              <div class="who"><Icon name="spark" size={12} /> Union {m.provider ? `· ${m.provider}` : ''}</div>
            {/if}
            <div class="markdown">
              {#each m.text.split(/\n\n+/g) as para}
                {#if para.startsWith('## ')}<h4>{para.slice(3)}</h4>
                {:else if para.startsWith('- ')}<ul>{#each para.split(/\n/g) as item}<li>{item.replace(/^- /, '')}</li>{/each}</ul>
                {:else}<p>{para}</p>{/if}
              {/each}
            </div>
            {#if m.action}
              <div class="actionbox" class:done={m.action.state === 'done'} class:failed={m.action.state === 'failed'}>
                <div class="row between">
                  <span class="chip {m.action.state === 'proposed' ? 'amber' : m.action.state === 'done' ? 'green' : 'rose'}">{actionLabel(m.action.state)}</span>
                  <span class="tiny faint mono">{m.action.tool}</span>
                </div>
                <p class="small" style="margin:7px 0 0">{m.action.description}</p>
                {#if m.action.state === 'proposed'}
                  <div class="row" style="margin-top:9px">
                    <button class="btn xs primary" onclick={() => resolve(m, true)} disabled={busy}><Icon name="check" size={12} /> Approve</button>
                    <button class="btn xs ghost" onclick={() => resolve(m, false)} disabled={busy}>Decline</button>
                  </div>
                {/if}
              </div>
            {/if}
            <div class="stamp">{relative(m.at)}</div>
          </div>
        {/each}
        {#if busy}<div class="bubble assistant"><div class="thinking"><span></span><span></span><span></span></div></div>{/if}
      </section>

      <div class="composer">
        <textarea
          bind:value={draft}
          rows="2"
          placeholder="Ask Union…"
          onkeydown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
        ></textarea>
        <button class="iconbtn sendbtn" onclick={send} disabled={busy || !draft.trim()} aria-label="Send"><Icon name="send" size={18} /></button>
      </div>
      <p class="tiny faint center">Enter sends · Shift+Enter adds a line. Proposed actions change nothing until you approve them.</p>
    {/if}
  {:else if tab === 'memory'}
    <section class="section">
      <div class="card pad stack">
        <div class="row"><Icon name="notes" size={17} /><strong>What Union may remember</strong></div>
        <p class="small muted" style="margin:0">Memory is explicit, inspectable and deletable. It is not a hidden profile.</p>
        <textarea bind:value={memoryText} rows="3" placeholder="A preference, a constraint, a piece of context you want carried forward." maxlength="400"></textarea>
        <button class="btn soft block" onclick={addMemory} disabled={!memoryText.trim()}><Icon name="plus" size={14} /> Save memory</button>
      </div>

      {#if union.memories.length === 0}
        <div class="empty-note">No memories yet.</div>
      {:else}
        <div class="list" style="margin-top:12px">
          {#each union.memories as m (m.id)}
            <div class="listitem static">
              <span class="capico"><Icon name="notes" size={16} /></span>
              <div class="main"><strong class="mems">{m.text}</strong><span>{m.source} · {relative(m.createdAt)}</span></div>
              <button class="iconbtn sm" onclick={() => removeMemory(m.id)} aria-label="Delete memory"><Icon name="trash" size={14} /></button>
            </div>
          {/each}
        </div>
      {/if}
    </section>
  {:else}
    <section class="section">
      <div class="card pad stack">
        <div class="row"><Icon name="book" size={17} /><strong>Projects</strong></div>
        <p class="small muted" style="margin:0">Group a conversation around something you are actually making.</p>
        <div class="grid-2 collapse">
          <div class="field"><label for="pj-name">Name</label><input id="pj-name" bind:value={projectName} maxlength="70" placeholder="A practical project" /></div>
          <div class="field"><label for="pj-about">Purpose</label><input id="pj-about" bind:value={projectAbout} maxlength="140" placeholder="One sentence" /></div>
        </div>
        <button class="btn soft" onclick={addProject} disabled={!projectName.trim()}><Icon name="plus" size={14} /> Create project</button>
      </div>

      {#if union.projects.length === 0}
        <div class="empty-note">No projects yet.</div>
      {:else}
        <div class="list" style="margin-top:12px">
          {#each union.projects as p (p.id)}
            <div class="listitem static">
              <span class="capico"><Icon name="book" size={16} /></span>
              <div class="main"><strong>{p.name}</strong><span>{p.about || 'No purpose recorded.'}</span></div>
              <span class="chip plain">{relative(p.createdAt)}</span>
            </div>
          {/each}
        </div>
      {/if}
    </section>
  {/if}
</main>

<style>
  .threadbar { margin-top: 12px; }
  .chat { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
  .bubble {
    max-width: 92%;
    padding: 11px 13px;
    border-radius: 15px;
    border: 1px solid var(--line);
    background: var(--panel);
  }
  .bubble.user { align-self: flex-end; background: color-mix(in srgb, var(--accent) 12%, var(--panel)); border-color: color-mix(in srgb, var(--accent) 26%, var(--line)); }
  .bubble.assistant { align-self: flex-start; }
  .bubble .markdown { font-size: 0.88rem; line-height: 1.58; }
  .bubble .markdown p { margin: 0.4em 0; }
  .bubble .markdown h4 { margin: 0.7em 0 0.3em; font-size: 0.94rem; }
  .bubble .markdown ul { margin: 0.4em 0; }
  .who { display: flex; align-items: center; gap: 5px; color: var(--faint); font-size: 0.68rem; font-weight: 650; letter-spacing: 0.05em; text-transform: uppercase; margin-bottom: 4px; }
  .stamp { color: var(--faint); font-size: 0.64rem; margin-top: 6px; text-align: right; }
  .bubble.user .stamp { text-align: left; }
  .actionbox { margin-top: 10px; padding: 10px; border: 1px solid color-mix(in srgb, var(--amber) 28%, var(--line)); border-radius: 11px; background: color-mix(in srgb, var(--amber) 6%, var(--panel)); }
  .actionbox.done { border-color: color-mix(in srgb, var(--green) 28%, var(--line)); background: color-mix(in srgb, var(--green) 5%, var(--panel)); }
  .actionbox.failed { border-color: color-mix(in srgb, var(--rose) 28%, var(--line)); background: color-mix(in srgb, var(--rose) 5%, var(--panel)); }
  .composer { display: flex; align-items: flex-end; gap: 8px; margin-top: 12px; position: sticky; bottom: 78px; background: color-mix(in srgb, var(--bg) 88%, transparent); backdrop-filter: blur(10px); padding: 6px 0; }
  .composer textarea { min-height: 48px; max-height: 140px; }
  .sendbtn {
    width: 46px; height: 46px; flex: none;
    background: linear-gradient(135deg, #a9abff 0%, #7b7dfa 48%, #8f6cf0 100%);
    color: #080910; border-color: rgb(255 255 255 / 0.22);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.5), 0 12px 28px -12px rgb(123 125 250 / 0.7);
    transition: transform 240ms var(--ease-spring), filter 200ms ease;
  }
  .sendbtn:hover { filter: brightness(1.06); }
  .sendbtn:active { transform: scale(0.94); }
  .sendbtn:disabled { opacity: 0.4; }
  .thinking { display: flex; gap: 4px; padding: 4px 2px; }
  .thinking span { width: 6px; height: 6px; border-radius: 50%; background: var(--accent-2); animation: blink 1.1s infinite; }
  .thinking span:nth-child(2) { animation-delay: 0.15s; }
  .thinking span:nth-child(3) { animation-delay: 0.3s; }
  @keyframes blink { 0%, 60%, 100% { opacity: 0.25; } 30% { opacity: 1; } }
  .capico { width: 36px; height: 36px; border-radius: 11px; flex: none; display: grid; place-items: center; background: var(--panel-2); border: 1px solid var(--line); color: var(--accent-2); }
  .listitem.static:hover { border-color: var(--line); }
  .mems { white-space: normal; font-weight: 500; line-height: 1.5; }
</style>
