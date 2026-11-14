<script lang="ts">
  import Empty from '../components/Empty.svelte';
  import Icon from '../components/Icon.svelte';
  import Modal from '../components/Modal.svelte';
  import QrCode from '../components/QrCode.svelte';
  import TopBar from '../components/TopBar.svelte';
  import { union } from '../lib/store.svelte';
  import { toasts } from '../lib/toast.svelte';
  import type { Page, PageSection, PresenceLink } from '../lib/types';
  import { copy, formatDate } from '../lib/util';
  import { shareText } from '../lib/native';

  const pages = $derived(union.pages);
  const published = $derived(pages.filter((p) => p.published));

  let editOpen = $state(false);
  let shareOpen = $state(false);
  let sharePage = $state<Page | null>(null);
  let busy = $state(false);
  let editId = $state('');
  let title = $state('');
  let tagline = $state('');
  let about = $state('');
  let theme = $state<Page['theme']>('calm');
  let sections = $state<PageSection[]>([]);
  let links = $state<PresenceLink[]>([]);

  function openNew() {
    editId = '';
    title = union.identity?.name ? `${union.identity.name}` : 'My page';
    tagline = 'A small, honest place on the web.';
    about = '';
    theme = 'calm';
    sections = [{ title: 'What I am exploring', body: '' }];
    links = [];
    editOpen = true;
  }

  function openEdit(page: Page) {
    editId = page.id;
    title = page.title;
    tagline = page.tagline;
    about = page.about;
    theme = page.theme;
    sections = page.sections.map((s) => ({ ...s }));
    links = page.links.map((l) => ({ ...l }));
    editOpen = true;
  }

  async function save(publish = false) {
    if (!title.trim()) { toasts.push('Give the page a title.', 'warn'); return; }
    busy = true;
    try {
      const saved = await union.savePage({
        id: editId || undefined,
        title,
        tagline,
        about,
        sections,
        links,
        theme,
        published: publish || undefined
      });
      editOpen = false;
      toasts.push(publish ? 'Page published. The link is yours to share.' : 'Page saved on this device.', 'success');
      if (publish) {
        sharePage = saved;
        shareOpen = true;
      }
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function togglePublish(page: Page) {
    busy = true;
    try {
      if (page.published) await union.unpublishPage(page);
      else await union.publishPage(page);
      toasts.push(page.published ? 'Page moved back to draft.' : 'Page published.', 'success');
    } catch (error) {
      toasts.push(error instanceof Error ? error.message : String(error), 'error');
    } finally {
      busy = false;
    }
  }

  async function remove(page: Page) {
    busy = true;
    try {
      await union.deletePage(page);
      toasts.push('Page deleted from this device.', 'info');
    } finally {
      busy = false;
    }
  }

  async function copyLink(page: Page) {
    const ok = await copy(union.pageLink(page));
    toasts.push(ok ? 'Page link copied. It reads without an account.' : 'Could not copy the link.', ok ? 'success' : 'warn');
  }

  function share(page: Page) {
    sharePage = page;
    shareOpen = true;
  }

  function shareNative(page: Page) {
    shareText(union.pageLink(page));
  }

  function openPreview(page: Page) {
    window.open(union.pageLink(page), '_blank', 'noopener');
  }
</script>

<TopBar title="Digital Presence" subtitle="A page that respects the visitor" back fallback="/more">
  {#snippet actions()}
    <button class="iconbtn sm" onclick={openNew} aria-label="New page"><Icon name="plus" size={17} /></button>
  {/snippet}
</TopBar>

<main class="screen">
  <div class="banner info">
    <Icon name="layout" size={17} />
    <div><strong>A page is a place, not a funnel.</strong> No tracking pixels, no pop-ups, no fake urgency. Your page travels as a self-contained link that anyone can read.</div>
  </div>

  {#if pages.length === 0}
    <Empty icon="layout" title="No page yet" text="Write a short page about what you are exploring, building or offering. Publish it only when it is true.">
      {#snippet action()}<button class="btn primary" onclick={openNew}>Create my page</button>{/snippet}
    </Empty>
  {:else}
    <section class="section">
      <div class="section-head"><h2>Your pages</h2><span class="hint">{published.length} published</span></div>
      <div class="list">
        {#each pages as page (page.id)}
          <div class="card pad pagecard">
            <div class="row top">
              <div class="push" style="min-width:0">
                <div class="row wrap" style="gap:6px">
                  <strong>{page.title}</strong>
                  <span class="chip {page.published ? 'green' : ''}">{page.published ? 'Published' : 'Draft'}</span>
                  <span class="chip plain">{page.theme}</span>
                </div>
                <p class="small muted" style="margin:4px 0 0">{page.tagline || 'No tagline yet.'}</p>
                <p class="tiny faint" style="margin:3px 0 0">Updated {formatDate(page.updatedAt)} · @{union.identity?.handle}/{page.slug.split('-').pop()}</p>
              </div>
            </div>
            {#if page.about}<p class="small" style="margin:10px 0 0">{page.about}</p>{/if}
            {#if page.sections.length}
              <div class="row wrap" style="gap:6px;margin-top:10px">
                {#each page.sections as s (s.title + s.body)}<span class="chip plain">{s.title}</span>{/each}
              </div>
            {/if}
            <div class="card-actions">
              <button class="btn xs soft" onclick={() => openEdit(page)}><Icon name="edit" size={13} /> Edit</button>
              <button class="btn xs" onclick={() => togglePublish(page)} disabled={busy}>{page.published ? 'Unpublish' : 'Publish'}</button>
              <button class="btn xs ghost" onclick={() => share(page)}><Icon name="qr" size={13} /> Share</button>
              <button class="btn xs ghost" onclick={() => copyLink(page)}><Icon name="link" size={13} /> Copy link</button>
              <button class="btn xs ghost push" onclick={() => remove(page)} aria-label="Delete page"><Icon name="trash" size={13} /></button>
            </div>
          </div>
        {/each}
      </div>
    </section>
  {/if}

  <section class="section">
    <div class="card pad" style="display:flex;flex-direction:column;gap:9px">
      <div class="row"><Icon name="link" size={17} /><strong>Links with terms</strong></div>
      <p class="small muted" style="margin:0">Share a page, a signed identity card, a community invitation or an event. Every Union link carries its own payload; nothing is discovered by crawling you.</p>
      <button class="btn sm soft" onclick={() => { editOpen = true; openNew(); }}><Icon name="plus" size={14} /> Start a page</button>
    </div>
  </section>
</main>

<Modal open={editOpen} wide title={editId ? 'Edit page' : 'Create a page'} subtitle="Only publish what you would stand beside" onclose={() => (editOpen = false)}>
  <div class="stack">
    <div class="field"><label for="pg-title">Title</label><input id="pg-title" bind:value={title} maxlength="80" /></div>
    <div class="field"><label for="pg-tag">Tagline</label><input id="pg-tag" bind:value={tagline} maxlength="140" placeholder="One line that tells the truth." /></div>
    <div class="field"><label for="pg-about">About</label><textarea id="pg-about" bind:value={about} rows="4" maxlength="900" placeholder="Who you are, what you are exploring, how to approach you."></textarea></div>

    <div class="row between">
      <div class="label">Sections</div>
      <button class="btn xs ghost" onclick={() => sections.push({ title: '', body: '' })}><Icon name="plus" size={12} /> Add section</button>
    </div>
    {#each sections as section, i (i)}
      <div class="card tight pad stack">
        <input bind:value={section.title} placeholder="Section title" maxlength="80" aria-label="Section title" />
        <textarea bind:value={section.body} rows="3" placeholder="What belongs here?" maxlength="1200" aria-label="Section body"></textarea>
        <button class="btn xs ghost" onclick={() => sections.splice(i, 1)}><Icon name="trash" size={12} /> Remove section</button>
      </div>
    {/each}

    <div class="row between">
      <div class="label">Links</div>
      <button class="btn xs ghost" onclick={() => links.push({ label: '', url: '' })}><Icon name="plus" size={12} /> Add link</button>
    </div>
    {#each links as link, i (i)}
      <div class="row">
        <input bind:value={link.label} placeholder="Label" aria-label="Link label" style="max-width:38%" />
        <input bind:value={link.url} placeholder="https://" aria-label="Link URL" />
        <button class="iconbtn sm" onclick={() => links.splice(i, 1)} aria-label="Remove link"><Icon name="close" size={14} /></button>
      </div>
    {/each}

    <div class="field">
      <div class="label">Theme</div>
      <div class="segmented">
        {#each ['calm', 'warm', 'forest', 'midnight'] as t (t)}
          <button class:on={theme === t} onclick={() => (theme = t as Page['theme'])}>{t}</button>
        {/each}
      </div>
    </div>
  </div>
  {#snippet footer()}
    <div class="row wrap">
      <button class="btn primary" onclick={() => save(false)} disabled={busy}>{busy ? 'Saving…' : 'Save draft'}</button>
      <button class="btn soft push" onclick={() => save(true)} disabled={busy}><Icon name="globe" size={14} /> Save & publish</button>
    </div>
  {/snippet}
</Modal>

<Modal open={shareOpen && Boolean(sharePage)} title="Share your page" subtitle={sharePage?.title} onclose={() => (shareOpen = false)}>
  {#if sharePage}
    <div class="stack center">
      <QrCode text={union.pageLink(sharePage)} size={214} caption="Scan to open the page" />
      <div class="card tight pad mono" style="word-break:break-all;text-align:left">{union.pageLink(sharePage)}</div>
      <div class="row wrap" style="justify-content:center">
        <button class="btn sm soft" onclick={() => shareNative(sharePage!)}><Icon name="share" size={14} /> Share</button>
        <button class="btn sm" onclick={() => openPreview(sharePage!)}><Icon name="external" size={14} /> Preview</button>
        <button class="btn sm ghost" onclick={() => copyLink(sharePage!)}><Icon name="copy" size={14} /> Copy</button>
      </div>
      <p class="hint">The page travels inside the link. Union does not need to host it for it to work. Only publish what you are willing to have read.</p>
    </div>
  {:else}
    <p class="muted">Choose a page first.</p>
  {/if}
</Modal>

<style>
  .pagecard { display: flex; flex-direction: column; gap: 2px; }
  .card-actions .push { margin-left: auto; }
</style>
