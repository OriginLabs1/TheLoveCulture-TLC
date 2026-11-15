<script lang="ts">
  import Avatar from '../components/Avatar.svelte';
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import Modal from '../components/Modal.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { router } from '../lib/router.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Community } from '../lib/types';
  import { colorFor } from '../lib/crypto';
  import { copy, relative } from '../lib/util';
  import { shareText } from '../lib/native';
  import { plural } from '../lib/format';

  const communityId = $derived(router.segments[1] ?? '');
  const community = $derived(communityId ? union.byId<Community>('community', communityId) : undefined);
  const posts = $derived(community ? union.postsIn(community.id) : []);
  const member = $derived(community ? union.isMember(community.id) : false);

  let tab = $state<'all' | 'mine' | 'official'>('all');
  let createOpen = $state(false);
  let name = $state('');
  let about = $state('');
  let visibility = $state<'public' | 'private'>('public');
  let joinPolicy = $state<'open' | 'request' | 'invite'>('open');
  let busy = $state(false);
  let draft = $state('');

  const filtered = $derived.by(() => {
    let list = union.communities;
    if (tab === 'mine') list = list.filter((c) => union.isMember(c.id));
    if (tab === 'official') list = list.filter((c) => c.official);
    return list;
  });

  function memberCount(c: Community): number {
    return union.memberships.filter((m) => m.communityId === c.id).length;
  }

  async function create() {
    busy = true;
    try {
      const c = await union.createCommunity({ name, about, visibility, joinPolicy });
      createOpen = false;
      name = '';
      about = '';
      toasts.push(`“${c.name}” created. You are its steward.`, 'success');
      router.go(`/communities/${c.id}`);
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function join(c: Community) {
    try {
      await union.joinCommunity(c);
      toasts.push(`Joined ${c.name}.`, 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    }
  }

  async function leave(c: Community) {
    if (!confirm(`Leave ${c.name}? Your posts stay unless you delete them.`)) return;
    await union.leaveCommunity(c);
    toasts.push('You left the community.', 'info');
  }

  async function post() {
    if (!community) return;
    busy = true;
    try {
      await union.addPost(community.id, draft);
      draft = '';
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function removePost(id: string) {
    if (!confirm('Delete this post? A relay copy may already have been delivered.')) return;
    const p = union.byId<import('../lib/types').Post>('post', id);
    if (p) await union.deletePost(p);
  }

  async function shareCommunity(c: Community) {
    const url = union.communityLink(c);
    const ok = await copy(url);
    toasts.push(ok ? 'Community link copied.' : 'Could not copy the link.', ok ? 'success' : 'warn');
  }

  function share(c: Community) {
    shareText(union.communityLink(c));
  }

  async function sync() {
    await union.sync();
    toasts.push(union.syncError || 'Sync attempted.', union.syncError ? 'warn' : 'info');
  }
</script>

{#if communityId && community}
  <TopBar title={community.name} subtitle={community.official ? 'Official Union community' : community.visibility === 'public' ? 'Public community' : 'Private community'} back fallback="/communities">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => shareCommunity(community!)} aria-label="Copy link"><Icon name="link" size={16} /></button>
    {/snippet}
  </TopBar>

  <main class="screen narrow">
    <section class="card pad hero">
      <div class="row top">
        <span class="commark" style={`--tint:${community.color}`}><Icon name="people" size={24} /></span>
        <div class="push">
          <div class="row wrap" style="gap:6px">
            <h1 class="display">{community.name}</h1>
            {#if community.official}<span class="chip accent">Official</span>{/if}
            <span class="chip">{community.visibility}</span>
            <span class="chip plain">{community.joinPolicy} joining</span>
          </div>
          <p class="small faint" style="margin:3px 0 0">{plural(memberCount(community), 'local member')} · created {relative(community.createdAt)}</p>
        </div>
      </div>
      <p class="about">{community.about}</p>
      <div class="row wrap" style="margin-top:12px">
        {#if member}
          <span class="chip green"><Icon name="check" size={12} /> Member</span>
          <button class="btn sm ghost push" onclick={() => leave(community)}>Leave</button>
        {:else}
          <button class="btn sm primary" onclick={() => join(community)}>Join community</button>
        {/if}
        <button class="btn sm soft" onclick={() => shareCommunity(community)}><Icon name="copy" size={14} /> Copy link</button>
        <button class="btn sm ghost" onclick={() => share(community)}><Icon name="share" size={14} /> Share</button>
      </div>
    </section>

    {#if community.visibility === 'public' && !union.relayUrl}
      <div class="banner warn">
        <Icon name="wifi-off" size={17} />
        <div><strong>This room is local to your device.</strong> Connect a relay in Operations to sync public posts with other participants. Nothing is pretending to be shared.</div>
      </div>
    {/if}

    {#if member}
      <section class="card pad compose">
        <div class="field">
          <label for="post">Share something practical</label>
          <textarea id="post" bind:value={draft} rows="3" maxlength="2000" placeholder="A question, a discovery, a useful correction…"></textarea>
        </div>
        <div class="row between">
          <span class="tiny faint">{draft.length}/2000 · plain text</span>
          <button class="btn primary sm" onclick={post} disabled={busy || !draft.trim()}>Post</button>
        </div>
      </section>
    {:else}
      <div class="banner info"><Icon name="users" size={17} /><div>Join to post. Reading is always allowed in a public room.</div></div>
    {/if}

    <section class="section">
      <div class="section-head">
        <h2>Shared room</h2>
        <div class="row" style="gap:6px">
          <span class="chip plain">{posts.length}</span>
          {#if union.relayUrl}<button class="btn xs ghost" onclick={sync}><Icon name="refresh" size={12} /> Sync</button>{/if}
        </div>
      </div>
      {#if posts.length}
        <div class="list">
          {#each posts as p (p.id)}
            <article class="card pad post">
              <div class="row">
                <Avatar name={p.authorName} color={colorFor(p.authorId)} size={34} />
                <div class="push">
                  <strong class="small">{p.authorName}{#if p.authorId === 'union'}<span class="chip accent" style="margin-left:6px">Union</span>{/if}</strong>
                  <div class="tiny faint">{relative(p.createdAt)} · {p.status}{#if p.relayId} · relay{/if}</div>
                </div>
                {#if p.authorId === union.identity?.id}
                  <button class="iconbtn sm" onclick={() => removePost(p.id)} aria-label="Delete post"><Icon name="trash" size={14} /></button>
                {/if}
              </div>
              <p class="body">{p.body}</p>
            </article>
          {/each}
        </div>
      {:else}
        <Empty icon="message" title="No posts yet" text={member ? 'Write the first useful thing. No engagement required.' : 'Join the community to begin.'} />
      {/if}
    </section>
  </main>
{:else if communityId}
  <TopBar title="Community" back fallback="/communities" />
  <main class="screen">
    <Empty icon="people" title="This community is not on this device" text="Public communities can be opened from a Union link. Join it there, then it appears here." />
    <button class="btn primary block" onclick={() => router.go('/communities')}>All communities</button>
  </main>
{:else}
  <TopBar title="Communities" subtitle="Rooms with a stated purpose">
    {#snippet actions()}
      <button class="iconbtn sm" onclick={() => (createOpen = true)} aria-label="Create a community"><Icon name="plus" size={17} /></button>
    {/snippet}
  </TopBar>

  <main class="screen">
    <div class="segmented">
      <button class:on={tab === 'all'} onclick={() => (tab = 'all')}>All</button>
      <button class:on={tab === 'mine'} onclick={() => (tab = 'mine')}>Mine</button>
      <button class:on={tab === 'official'} onclick={() => (tab = 'official')}>Official</button>
    </div>

    {#if union.syncError}
      <div class="banner danger" style="margin-top:12px"><Icon name="alert" size={17} /><div>{union.syncError}</div></div>
    {/if}

    <div class="list" style="margin-top:14px">
      {#each filtered as c (c.id)}
        <button class="listitem commitem" onclick={() => router.go(`/communities/${c.id}`)}>
          <span class="commark sm" style={`--tint:${c.color}`}><Icon name="people" size={18} /></span>
          <div class="main">
            <strong class="row" style="gap:6px">{c.name}{#if c.official}<span class="chip accent">Official</span>{/if}</strong>
            <span>{c.about}</span>
            <span class="tiny faint" style="margin-top:3px">{plural(union.postsIn(c.id).length, 'post')} · {c.visibility}{#if union.isMember(c.id)} · joined{/if}</span>
          </div>
          <Icon name="chevron-right" size={16} />
        </button>
      {/each}
    </div>

    {#if !filtered.length}
      <Empty icon="people" title={tab === 'mine' ? 'You have not joined a room yet' : 'No communities here'} text="Create a room with a clear purpose, or join Union Commons." >
        {#snippet action()}
          <button class="btn primary" onclick={() => (createOpen = true)}>Create a community</button>
        {/snippet}
      </Empty>
    {/if}

    <div class="banner info" style="margin-top:14px">
      <Icon name="flag" size={17} />
      <div><strong>A room needs a purpose.</strong> Say what it is for, who it serves and how decisions are made. Official communities are authored by Union and labelled as such.</div>
    </div>

    <button class="btn soft block" style="margin-top:12px" onclick={() => (createOpen = true)}><Icon name="plus" size={16} /> New community</button>
  </main>

  <Modal open={createOpen} title="Create a community" subtitle="A stated purpose, not a growth target" onclose={() => (createOpen = false)}>
    <div class="stack">
      <div class="field">
        <label for="cname">Name</label>
        <input id="cname" bind:value={name} maxlength="60" placeholder="Study circle, project room, local chapter…" />
      </div>
      <div class="field">
        <label for="cabout">Purpose</label>
        <textarea id="cabout" bind:value={about} rows="3" maxlength="600" placeholder="What is this room for, and who should be here?"></textarea>
      </div>
      <div class="grid-2">
        <div class="field">
          <label for="cvis">Visibility</label>
          <select id="cvis" bind:value={visibility}>
            <option value="public">Public</option>
            <option value="private">Private</option>
          </select>
        </div>
        <div class="field">
          <label for="cjoin">Joining</label>
          <select id="cjoin" bind:value={joinPolicy}>
            <option value="open">Open</option>
            <option value="request">Request</option>
            <option value="invite">Invite only</option>
          </select>
        </div>
      </div>
      <div class="banner info"><Icon name="shield" size={17} /><div>Public rooms can register with the relay for shared posts. Private rooms stay local unless you deliberately export or invite.</div></div>
      <button class="btn primary block" onclick={create} disabled={busy || !name.trim()}>{busy ? 'Creating…' : 'Create community'}</button>
    </div>
  </Modal>
{/if}

<style>
  .commark {
    width: 48px; height: 48px; border-radius: 15px; flex: none;
    display: grid; place-items: center;
    background: color-mix(in srgb, var(--tint, var(--accent)) 20%, var(--panel));
    border: 1px solid color-mix(in srgb, var(--tint, var(--accent)) 38%, var(--line));
    color: color-mix(in srgb, var(--tint, var(--accent)) 70%, white);
  }
  .commark.sm { width: 42px; height: 42px; border-radius: 13px; }
  .hero { display: flex; flex-direction: column; gap: 10px; }
  .hero h1 { margin: 0; font-size: 1.35rem; }
  .hero .about { margin: 0; color: var(--muted); font-size: 0.88rem; line-height: 1.55; }
  .compose { display: flex; flex-direction: column; gap: 9px; margin-top: 12px; }
  .post .body { margin: 9px 0 0; font-size: 0.88rem; line-height: 1.58; white-space: pre-wrap; overflow-wrap: anywhere; }
  .commitem .main span { white-space: normal; display: -webkit-box; line-clamp: 2; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
</style>
