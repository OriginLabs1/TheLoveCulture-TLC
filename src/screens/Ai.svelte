<script lang="ts">
  import A2UISurface from '../components/A2UISurface.svelte';
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { buildUnionSurface, surfaceKindFor, type A2UIAction } from '../lib/a2ui';
  import { TOTAL_BADGES } from '../lib/navigation';
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
  let surfaceOpen = $state(true);

  const starters = [
    'What should I do next this week?',
    'Explain how consent works here.',
    'Draft a page for my work.',
    'Where does my progress stand?'
  ];

  const active = $derived(threads.find((t) => t.id === activeId) ?? threads[0]);
  const messages = $derived(active ? union.aiMessagesIn(active.id) : []);
  const lastReply = $derived([...messages].reverse().find((m) => m.role === 'assistant'));
  const lastPrompt = $derived([...messages].reverse().find((m) => m.role === 'user'));
  const surface = $derived.by(() => {
    const ctx = {
      name: union.identity?.name || union.identity?.handle || 'friend',
      connections: union.acceptedConnections.length,
      badges: union.earnedBadges.length,
      totalBadges: TOTAL_BADGES,
      unread: union.unreadMessages,
      pending: union.incomingRequests.length,
      memories: union.memories.length,
      courses: union.enrollments.length,
      nextLesson: union.nextLesson?.lesson.title,
      nextCourse: union.nextLesson?.course.title,
      activation: union.activation.status,
      relay: Boolean(union.relayUrl)
    };
    return buildUnionSurface(lastReply ? surfaceKindFor(lastReply.text, lastPrompt?.text ?? '') : 'briefing', ctx);
  });

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
    if (!text) return;
    draft = '';
    busy = true;
    try {
      let threadId = active?.id;
      if (!threadId) {
        const thread = await union.createAiThread();
        activeId = thread.id;
        threadId = thread.id;
      }
      await union.sendAi(threadId, text);
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

  function handleSurface(action: A2UIAction, values: Record<string, string>) {
    if (action.type === 'ask') {
      draft = action.text;
      const missing = Object.entries(values).filter(([, v]) => !v.trim());
      if (missing.length) {
        toasts.push('Add the details in the surface first.', 'warn');
        return;
      }
      void send();
      return;
    }
    if (action.type === 'dismiss') surfaceOpen = false;
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

    {#if surfaceOpen}
      <div class="surface-slot">
        {#key surface.id}
          <A2UISurface {surface} onaction={handleSurface} />
        {/key}
        <button class="surface-hide" onclick={() => (surfaceOpen = false)}>Hide composed surface</button>
      </div>
    {/if}

    {#if threads.length === 0}
      <Empty icon="spark" title="No conversation yet" text="Ask about a record, draft a page, prepare a gathering, or think through a decision. Actions are proposed, never taken silently.">
        {#snippet action()}<button class="btn primary" onclick={newThread} disabled={busy}>Start a conversation</button>{/snippet}
      </Empty>
      <div class="starters">
        {#each starters as starter (starter)}
          <button class="chip plain starter" onclick={() => { if (!active) { void newThread(); } draft = starter; }}>{starter}</button>
        {/each}
      </div>
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

      <div class="starters compact">
        {#each starters.slice(0, 3) as starter (starter)}
          <button class="chip plain starter" onclick={() => (draft = starter)}>{starter}</button>
        {/each}
      </div>

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
  .surface-slot { margin-top: 14px; display: flex; flex-direction: column; gap: 7px; }
  .surface-hide {
    align-self: flex-end;
    background: none; border: 0; padding: 2px 3px;
    color: var(--faint); font-size: 0.67rem; font-weight: 620;
    letter-spacing: 0.02em;
  }
  .surface-hide:hover { color: var(--accent); }
  .starters { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
  .starters.compact { margin: 12px 0 0; overflow-x: auto; flex-wrap: nowrap; scrollbar-width: none; }
  .starters.compact::-webkit-scrollbar { display: none; }
  .starter { flex: none; min-height: 32px; padding: 6px 11px; font-size: 0.7rem; }
  .starter:hover { border-color: color-mix(in srgb, var(--accent) 26%, var(--line)); color: var(--accent); }
  .chat { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
  .bubble {
    max-width: 92%;
    padding: 12px 15px;
    border-radius: 22px 22px 22px 8px;
    border: 1px solid var(--line);
    background: var(--surface);
    box-shadow: var(--shadow-1);
  }
  .bubble.user { align-self: flex-end; border-radius: 22px 22px 8px 22px; background: var(--accent-soft); border-color: color-mix(in srgb, var(--accent) 20%, transparent); box-shadow: none; }
  .bubble.assistant { align-self: flex-start; }
  .bubble .markdown { font-size: 0.88rem; line-height: 1.58; }
  .bubble .markdown p { margin: 0.4em 0; }
  .bubble .markdown h4 { margin: 0.7em 0 0.3em; font-size: 0.94rem; }
  .bubble .markdown ul { margin: 0.4em 0; }
  .who { display: flex; align-items: center; gap: 5px; color: var(--faint); font-size: 0.68rem; font-weight: 650; letter-spacing: 0.05em; text-transform: uppercase; margin-bottom: 4px; }
  .stamp { color: var(--faint); font-size: 0.64rem; margin-top: 6px; text-align: right; }
  .bubble.user .stamp { text-align: left; }
  .actionbox { margin-top: 10px; padding: 11px 12px; border: 1px solid color-mix(in srgb, var(--amber) 26%, var(--line)); border-radius: 15px; background: var(--amber-soft); }
  .actionbox.done { border-color: color-mix(in srgb, var(--green) 26%, var(--line)); background: var(--green-soft); }
  .actionbox.failed { border-color: color-mix(in srgb, var(--rose) 26%, var(--line)); background: var(--rose-soft); }
  .composer {
    display: flex; align-items: flex-end; gap: 8px;
    margin-top: 12px;
    position: sticky;
    bottom: calc(88px + env(safe-area-inset-bottom));
    padding: 8px;
    border-radius: 26px;
    background: var(--glass-strong);
    border: 1px solid var(--glass-edge);
    backdrop-filter: blur(26px) saturate(190%);
    -webkit-backdrop-filter: blur(26px) saturate(190%);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.6), var(--shadow-2);
  }
  .composer textarea { border: 0; background: transparent; box-shadow: none; padding: 8px 6px; }
  .composer textarea:focus { box-shadow: none; background: transparent; }
  @media (min-width: 1080px) {
    /* The dock becomes a rail, so the composer can sit near the floor. */
    .composer { bottom: 18px; }
  }
  .composer textarea { min-height: 48px; max-height: 140px; }
  .sendbtn {
    width: 46px; height: 46px; flex: none;
    background: var(--accent);
    color: var(--accent-ink); border-color: transparent;
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.3), 0 12px 28px -12px color-mix(in srgb, var(--accent) 70%, transparent);
    transition: transform 240ms var(--ease-spring), filter 200ms ease;
  }
  .sendbtn:hover { filter: brightness(1.06); }
  .sendbtn:active { transform: scale(0.94); }
  .sendbtn:disabled { opacity: 0.4; }
  .thinking { display: flex; gap: 4px; padding: 4px 2px; }
  .thinking span { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); animation: blink 1.1s infinite; }
  .thinking span:nth-child(2) { animation-delay: 0.15s; }
  .thinking span:nth-child(3) { animation-delay: 0.3s; }
  @keyframes blink { 0%, 60%, 100% { opacity: 0.25; } 30% { opacity: 1; } }
  .capico { width: 36px; height: 36px; border-radius: 12px; flex: none; display: grid; place-items: center; background: var(--accent-soft); border: 1px solid color-mix(in srgb, var(--accent) 16%, transparent); color: var(--accent); }
  .listitem.static:hover { border-color: var(--line); }
  .mems { white-space: normal; font-weight: 500; line-height: 1.5; }
</style>
