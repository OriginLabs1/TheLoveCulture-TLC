<script lang="ts">
  import { tick } from 'svelte';
  import Avatar from '../components/Avatar.svelte';
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import Modal from '../components/Modal.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Attachment, Connection, Message, Person } from '../lib/types';
  import { dayLabel, fileSize, parseBody, preview, readFile } from '../lib/message';
  import { relative } from '../lib/util';

  const threadId = $derived(router.segments[1] ?? '');
  const thread = $derived(threadId ? union.byId<import('../lib/types').Thread>('thread', threadId) : undefined);
  const messages = $derived(thread ? union.messagesIn(thread.id) : []);
  const otherId = $derived(
    thread && union.identity
      ? thread.kind === 'notes'
        ? union.identity.id
        : thread.participantIds.find((id) => id !== union.identity!.id) ?? ''
      : ''
  );
  const other = $derived(union.people.find((p) => p.identityId === otherId));
  const connection = $derived(other ? union.connectionWith(other.identityId) : undefined);
  const notesThread = $derived(union.threads.find((t) => t.kind === 'notes'));

  let search = $state('');
  let draft = $state('');
  let attachments = $state<Attachment[]>([]);
  let busy = $state(false);
  let newOpen = $state(false);
  let fileInput: HTMLInputElement | undefined = $state();
  let scroller: HTMLDivElement | undefined = $state();

  const threads = $derived.by(() => {
    const q = search.trim().toLowerCase();
    return union.threads.filter((t) => {
      if (!q) return true;
      const last = preview(union.messagesIn(t.id).at(-1));
      return `${t.title} ${last}`.toLowerCase().includes(q);
    });
  });

  const accepted = $derived(
    union.acceptedConnections
      .map((c) => ({ connection: c, person: union.people.find((p) => p.identityId === (c.aId === union.identity?.id ? c.bId : c.aId)) }))
      .filter((x): x is { connection: Connection; person: Person } => Boolean(x.person))
  );

  function lastOf(id: string): Message | undefined {
    return union.messagesIn(id).at(-1);
  }

  function otherPerson(t: import('../lib/types').Thread): Person | undefined {
    if (t.kind === 'notes') return undefined;
    const id = t.participantIds.find((x) => x !== union.identity?.id);
    return union.people.find((p) => p.identityId === id);
  }

  async function markRead() {
    if (thread && thread.unread > 0) await union.markThreadRead(thread);
  }

  $effect(() => {
    if (!threadId) return;
    const count = messages.length;
    void count;
    void tick().then(() => {
      if (scroller) scroller.scrollTop = scroller.scrollHeight;
    });
    void markRead();
  });

  async function startNotes() {
    if (!union.identity) return;
    const notes = await union.ensureThread(union.identity.id);
    newOpen = false;
    router.go(`/messages/${notes.id}`);
  }

  async function startWith(person: Person) {
    const t = await union.ensureThread(person.identityId);
    newOpen = false;
    router.go(`/messages/${t.id}`);
  }

  async function pickFiles(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    for (const file of files) {
      try {
        attachments = [...attachments, await readFile(file)];
      } catch (error) {
        toasts.push(error instanceof Error ? error.message : String(error), 'error');
      }
    }
  }

  async function send() {
    if (!thread || busy) return;
    if (!draft.trim() && !attachments.length) return;
    busy = true;
    const text = draft;
    const files = attachments;
    try {
      const message = await union.sendMessage(thread.id, text, files, files.length ? 'attachment' : 'text');
      draft = '';
      attachments = [];
      if (message.status === 'queued' && thread.kind !== 'notes') {
        toasts.push('Sealed and queued on this device. It will send when a relay is reachable.', 'info');
      }
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  function keydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
      event.preventDefault();
      void send();
    }
  }

  async function retry(m: Message) {
    try {
      await union.retryMessage(m);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    }
  }

  async function requestAction(payload: NonNullable<ReturnType<typeof parseBody>>, approve: boolean) {
    const conn = union.byId<Connection>('connection', payload.connectionId);
    if (!conn) return;
    await union.respondConnection(conn, approve);
  }

  function attachmentUrl(a: Attachment): string {
    return `data:${a.mime};base64,${a.data}`;
  }

  function submitNew() {
    void startNotes();
  }
</script>

{#if threadId && thread}
  <TopBar title={thread.title} subtitle={thread.kind === 'notes' ? 'Private to this device' : other ? `@${other.handle}` : 'Union participant'} back fallback="/messages">
    {#snippet actions()}
      {#if other}
        <button class="iconbtn sm" onclick={() => router.go(`/people/${other.id}`)} aria-label="Contact details"><Icon name="user" size={16} /></button>
      {/if}
    {/snippet}
  </TopBar>

  <main class="thread">
    <div class="messages" bind:this={scroller}>
      {#if thread.kind !== 'notes' && !union.relayUrl}
        <div class="banner warn" style="margin:10px 0 4px">
          <Icon name="wifi-off" size={17} />
          <div><strong>No relay configured.</strong> Notes are sealed on this device and wait in the outbox. Configure a relay in Operations for store-and-forward delivery.</div>
        </div>
      {/if}
      {#if thread.kind !== 'notes' && connection && !connection.scopes.includes('message')}
        <div class="banner danger" style="margin:10px 0 4px">
          <Icon name="lock" size={17} />
          <div><strong>Messaging scope is revoked.</strong> You can read history but not start new messages. <button class="inline" onclick={() => router.go(`/people/${other?.id}`)}>Adjust consent</button></div>
        </div>
      {/if}

      {#each messages as m, i (m.id)}
        {@const label = dayLabel(m.createdAt)}
        {#if i === 0 || dayLabel(messages[i - 1].createdAt) !== label}
          <div class="daysep"><span>{label}</span></div>
        {/if}
        {@const mine = m.from === union.identity?.id}
        {@const payload = parseBody(m)}
        {#if m.kind === 'system'}
          <div class="systemnote"><Icon name="info" size={14} /><span>{m.body}</span></div>
        {:else if payload}
          <div class="connreq" class:mine>
            <div class="row"><Icon name="handshake" size={17} /><strong>{payload.kind === 'connection_request' ? 'Connection request' : payload.kind === 'connection_accept' ? 'Connection accepted' : 'Connection declined'}</strong></div>
            {#if payload.note}<p class="small muted">{payload.note}</p>{/if}
            {#if payload.kind === 'connection_request' && connection?.status === 'pending' && connection.bId === union.identity?.id}
              <div class="row" style="margin-top:8px">
                <button class="btn xs primary" onclick={() => requestAction(payload, true)}>Accept</button>
                <button class="btn xs ghost" onclick={() => requestAction(payload, false)}>Decline</button>
                <button class="btn xs ghost push" onclick={() => router.go(`/people/${other?.id}`)}>Scopes</button>
              </div>
            {/if}
            <span class="tiny faint" style="margin-top:6px;display:block">{relative(m.createdAt)}</span>
          </div>
        {:else}
          <article class="bubble" class:mine class:theirs={!mine}>
            {#if m.body}<p>{m.body}</p>{/if}
            {#if m.attachments?.length}
              <div class="attachments">
                {#each m.attachments as a (a.name + a.size)}
                  {#if a.mime.startsWith('image/')}
                    <a href={attachmentUrl(a)} target="_blank" rel="noopener noreferrer" class="imgwrap"><img src={attachmentUrl(a)} alt={a.name} /></a>
                  {:else}
                    <a class="attach" href={attachmentUrl(a)} download={a.name}>
                      <Icon name="download" size={15} />
                      <span>{a.name}<small>{fileSize(a.size)}</small></span>
                    </a>
                  {/if}
                {/each}
              </div>
            {/if}
            <footer>
              <span class="tiny">{new Date(m.createdAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
              {#if mine}
                <span class="status {m.status}">
                  <Icon name={m.status === 'queued' ? 'clock' : m.status === 'failed' ? 'alert' : m.status === 'read' || m.status === 'delivered' ? 'check' : 'check'} size={12} />
                  {m.status === 'queued' ? 'queued' : m.status === 'sent' ? 'sent' : m.status === 'delivered' ? 'delivered' : m.status === 'read' ? 'read' : 'failed'}
                </span>
              {/if}
            </footer>
            {#if mine && m.status === 'queued' && union.relayUrl}
              <button class="retry" onclick={() => retry(m)}>Retry now</button>
            {:else if mine && m.status === 'failed'}
              <button class="retry" onclick={() => retry(m)}>Retry</button>
            {/if}
          </article>
        {/if}
      {/each}

      {#if !messages.length}
        <Empty
          icon={thread.kind === 'notes' ? 'notes' : 'message'}
          title={thread.kind === 'notes' ? 'A private page for yourself' : 'Start the conversation'}
          text={thread.kind === 'notes' ? 'Write a thought, a decision, or something you want to remember. These notes never leave the device.' : 'Messages are sealed end-to-end. Only the people in this conversation can read them.'}
        />
      {/if}
    </div>

    <div class="composer">
      {#if attachments.length}
        <div class="pending">
          {#each attachments as a, idx (a.name + a.size + idx)}
            <span class="chip">{a.name} · {fileSize(a.size)}<button aria-label="Remove attachment" onclick={() => (attachments = attachments.filter((_, j) => j !== idx))}><Icon name="close" size={11} /></button></span>
          {/each}
        </div>
      {/if}
      <div class="composebar">
        <button class="iconbtn sm" onclick={() => fileInput?.click()} aria-label="Attach a file"><Icon name="plus" size={17} /></button>
        <textarea
          bind:value={draft}
          onkeydown={keydown}
          rows="1"
          placeholder={thread.kind === 'notes' ? 'Write a note to yourself…' : 'Write a sealed note…'}
          aria-label="Message"
        ></textarea>
        <button class="sendbtn" onclick={send} disabled={busy || (!draft.trim() && !attachments.length)} aria-label="Send">
          <Icon name="send" size={18} />
        </button>
      </div>
      <p class="tiny faint composehint">
        {thread.kind === 'notes' ? 'Local only. No relay, no recipient.' : 'End-to-end sealed. The relay sees routing metadata and ciphertext, not content.'}
      </p>
      <input type="file" hidden bind:this={fileInput} onchange={pickFiles} multiple />
    </div>
  </main>
{:else if threadId}
  <TopBar title="Conversation" back fallback="/messages" />
  <main class="screen">
    <Empty icon="message" title="Conversation not found" text="It may have been removed from this device." />
    <button class="btn primary block" onclick={() => router.go('/messages')}>Back to Messages</button>
  </main>
{:else}
  <TopBar title="Messages" subtitle="Sealed notes, honest delivery states">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => (newOpen = true)} aria-label="New conversation"><Icon name="plus" size={17} /></button>
    {/snippet}
  </TopBar>

  <main class="screen">
    <div class="row wrap" style="gap:8px">
      <button class="btn sm soft" onclick={startNotes}><Icon name="notes" size={15} /> Notes to self</button>
      <button class="btn sm" onclick={() => union.sync()} disabled={!union.relayUrl || union.syncing}>
        <Icon name="refresh" size={15} /> {union.syncing ? 'Syncing…' : 'Sync now'}
      </button>
      <span class="chip plain push">{union.relayUrl ? 'relay on' : 'local only'}</span>
    </div>

    {#if !union.relayUrl}
      <div class="banner warn" style="margin-top:12px">
        <Icon name="wifi-off" size={17} />
        <div><strong>Local-first.</strong> Without a relay, direct messages are sealed here and queued. Notes to self always work. <button class="inline" onclick={() => router.go('/operations')}>Configure relay</button></div>
      </div>
    {/if}
    {#if union.syncError}
      <div class="banner danger" style="margin-top:12px"><Icon name="alert" size={17} /><div>{union.syncError}</div></div>
    {/if}

    <div class="searchbar">
      <Icon name="search" size={17} />
      <input bind:value={search} placeholder="Search conversations" aria-label="Search conversations" />
    </div>

    <div class="list" style="margin-top:14px">
      {#each threads as t (t.id)}
        {@const p = otherPerson(t)}
        {@const last = lastOf(t.id)}
        <button class="listitem" onclick={() => router.go(`/messages/${t.id}`)}>
          {#if t.kind === 'notes'}
            <span class="threadico"><Icon name="notes" size={19} /></span>
          {:else}
            <Avatar name={p?.name ?? t.title} color={p?.color ?? '#5b5bd6'} size={42} />
          {/if}
          <div class="main">
            <strong class="row" style="gap:6px">{t.title}{#if t.unread > 0}<span class="chip accent">{t.unread} new</span>{/if}</strong>
            <span>{preview(last)}</span>
          </div>
          <span class="trail">{relative(t.lastAt)}<Icon name="chevron-right" size={15} /></span>
        </button>
      {/each}
    </div>

    {#if !threads.length}
      <Empty
        icon="message"
        title="No conversations yet"
        text="Accept a connection, then start a sealed note. Or write privately to yourself — that needs nobody."
      >
        {#snippet action()}
          <button class="btn primary" onclick={startNotes}>Write a note to self</button>
        {/snippet}
      </Empty>
    {/if}

    <section class="section">
      <div class="section-head"><h2>Delivery, honestly</h2></div>
      <div class="card pad">
        <div class="kv"><dt>Queued</dt><dd>written here, not yet handed over</dd></div>
        <div class="kv"><dt>Sent</dt><dd>relay accepted the sealed envelope</dd></div>
        <div class="kv"><dt>Delivered</dt><dd>recipient device acknowledged it</dd></div>
        <div class="kv"><dt>Failed</dt><dd>retry, or check the relay configuration</dd></div>
        <p class="hint" style="margin-top:8px">The relay can see who, when and how large. It cannot read bodies or attachments. Union does not pretend otherwise.</p>
      </div>
    </section>
  </main>

  <Modal open={newOpen} title="New conversation" subtitle="A direct thread requires an accepted connection" onclose={() => (newOpen = false)}>
    <div class="stack">
      <button class="listitem" onclick={startNotes}>
        <span class="threadico"><Icon name="notes" size={18} /></span>
        <div class="main"><strong>Notes to self</strong><span>Private, local, no recipient</span></div>
        <Icon name="chevron-right" size={15} />
      </button>
      {#each accepted as item (item.connection.id)}
        <button class="listitem" onclick={() => startWith(item.person)}>
          <Avatar name={item.person.name} color={item.person.color} size={38} />
          <div class="main"><strong>{item.person.name}</strong><span>@{item.person.handle} · {item.connection.scopes.join(', ')}</span></div>
          <Icon name="chevron-right" size={15} />
        </button>
      {/each}
      {#if !accepted.length}
        <div class="banner info"><Icon name="shield" size={17} /><div>No accepted connections yet. Accept the practice request from Echo in People, or invite someone with your card.</div></div>
        <button class="btn soft block" onclick={() => { newOpen = false; router.go('/people'); }}>Open People</button>
      {/if}
      <button class="btn ghost block" onclick={() => submitNew()}>Notes to self</button>
    </div>
  </Modal>
{/if}

<style>
  .thread {
    max-width: var(--maxw);
    margin: 0 auto;
    padding: 0 var(--pad) calc(88px + env(safe-area-inset-bottom));
    display: flex;
    flex-direction: column;
    min-height: calc(100dvh - 62px);
  }
  .messages {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 7px;
    padding: 10px 0 16px;
  }
  .daysep { display: flex; align-items: center; gap: 10px; margin: 16px 0 6px; color: var(--faint); font-size: 0.66rem; text-transform: uppercase; letter-spacing: 0.1em; }
  .daysep::before, .daysep::after { content: ""; height: 1px; background: var(--line-soft); flex: 1; }
  .bubble {
    align-self: flex-start;
    max-width: 84%;
    background: var(--panel);
    border: 1px solid var(--line);
    border-radius: 16px 16px 16px 5px;
    padding: 9px 13px 7px;
  }
  .bubble.mine {
    align-self: flex-end;
    background: linear-gradient(160deg, color-mix(in srgb, var(--accent) 24%, var(--panel-2)), color-mix(in srgb, var(--accent) 12%, var(--panel)));
    border-color: color-mix(in srgb, var(--accent) 30%, var(--line));
    border-radius: 16px 16px 5px 16px;
  }
  .bubble p { margin: 0; font-size: 0.9rem; line-height: 1.5; white-space: pre-wrap; overflow-wrap: anywhere; }
  .bubble footer { display: flex; align-items: center; justify-content: flex-end; gap: 6px; margin-top: 3px; color: var(--faint); }
  .status { display: inline-flex; align-items: center; gap: 2px; font-size: 0.64rem; }
  .status.queued { color: var(--amber); }
  .status.failed { color: var(--rose); }
  .status.sent { color: var(--faint); }
  .status.delivered, .status.read { color: var(--teal); }
  .retry { border: 0; background: none; padding: 0; margin-top: 2px; color: var(--accent); font-size: 0.68rem; }
  .systemnote {
    align-self: center; max-width: 92%;
    display: flex; gap: 7px; align-items: flex-start;
    padding: 10px 12px; border-radius: 12px;
    border: 1px dashed var(--line);
    color: var(--muted); font-size: 0.76rem; line-height: 1.5;
  }
  .systemnote :global(svg) { color: var(--accent-2); margin-top: 2px; flex: none; }
  .connreq {
    align-self: flex-start; max-width: 88%;
    border: 1px solid color-mix(in srgb, var(--accent) 30%, var(--line));
    background: color-mix(in srgb, var(--accent) 7%, var(--panel));
    border-radius: 14px; padding: 11px 13px;
  }
  .connreq.mine { align-self: flex-end; }
  .connreq strong { font-size: 0.84rem; }
  .connreq p { margin: 5px 0 0; }
  .attachments { display: flex; flex-direction: column; gap: 6px; margin-top: 6px; }
  .attach { display: flex; align-items: center; gap: 8px; padding: 7px 9px; border-radius: 9px; background: var(--surface-2); border: 1px solid var(--line-soft); color: var(--ink); font-size: 0.76rem; text-decoration: none; }
  .attach small { display: block; color: var(--faint); font-size: 0.66rem; }
  .imgwrap img { max-height: 220px; border-radius: 10px; display: block; }
  .composer {
    position: sticky; bottom: 0;
    margin: 0 calc(var(--pad) * -1);
    padding: 8px var(--pad) calc(8px + env(safe-area-inset-bottom));
    background: color-mix(in srgb, var(--bg) 92%, transparent);
    backdrop-filter: blur(14px);
    border-top: 1px solid var(--line-soft);
  }
  .composebar {
    display: flex; align-items: flex-end; gap: 8px;
    background: var(--panel); border: 1px solid var(--line); border-radius: 18px;
    padding: 6px 7px 6px 8px;
  }
  .composebar textarea {
    flex: 1; min-height: 38px; max-height: 140px; resize: none;
    border: 0; background: transparent; box-shadow: none !important;
    padding: 8px 2px; font-size: 0.9rem;
  }
  .sendbtn {
    width: 40px; height: 40px; flex: none; border-radius: 13px;
    border: 1px solid transparent;
    background: var(--accent);
    color: var(--accent-ink);
    display: grid; place-items: center;
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.3), 0 10px 24px -12px color-mix(in srgb, var(--accent) 70%, transparent);
    transition: transform 240ms var(--ease-spring), filter 200ms ease;
  }
  .sendbtn:hover { filter: brightness(1.06); }
  .sendbtn:active { transform: scale(0.94); }
  .sendbtn:disabled { opacity: 0.4; }
  .composehint { margin: 5px 2px 0; text-align: center; }
  .pending { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 7px; }
  .pending .chip button { border: 0; background: none; color: inherit; padding: 0 0 0 3px; display: inline-flex; }
  .threadico {
    width: 42px; height: 42px; border-radius: 50%; flex: none;
    display: grid; place-items: center;
    background: var(--accent-soft); border: 1px solid color-mix(in srgb, var(--accent) 16%, transparent); color: var(--accent);
  }
  .searchbar {
    display: flex; align-items: center; gap: 9px;
    margin-top: 14px; padding: 0 13px;
    border: 1px solid var(--line); border-radius: 16px; background: var(--surface-2);
  }
  .searchbar :global(svg) { color: var(--faint); flex: none; }
  .searchbar input { border: 0; background: transparent; box-shadow: none !important; padding-left: 0; min-height: 44px; }
  .inline { border: 0; background: none; padding: 0; color: var(--accent); font-size: inherit; text-decoration: underline; }
</style>
