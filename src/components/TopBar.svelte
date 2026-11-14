<script lang="ts">
  import type { Snippet } from 'svelte';
  import Icon from './Icon.svelte';
  import { router } from '../lib/router.svelte';

  let {
    title = '',
    subtitle = '',
    back = false,
    fallback = '/',
    actions,
    transparent = false
  }: {
    title?: string;
    subtitle?: string;
    back?: boolean;
    fallback?: string;
    actions?: Snippet;
    transparent?: boolean;
  } = $props();
</script>

<header class="topbar" class:transparent>
  <div class="inner">
    {#if back}
      <button class="iconbtn" onclick={() => router.back(fallback)} aria-label="Back">
        <Icon name="arrow-left" size={18} />
      </button>
    {/if}
    <div class="titles">
      <h1>{title}</h1>
      {#if subtitle}<p>{subtitle}</p>{/if}
    </div>
    {#if actions}
      <div class="actions">{@render actions()}</div>
    {/if}
  </div>
</header>

<style>
  .topbar {
    position: sticky;
    top: 0;
    z-index: 30;
    padding-top: env(safe-area-inset-top);
    background: linear-gradient(180deg, rgb(7 8 13 / 0.94), rgb(7 8 13 / 0.72) 72%, rgb(7 8 13 / 0.42));
    backdrop-filter: blur(22px) saturate(150%);
    -webkit-backdrop-filter: blur(22px) saturate(150%);
    border-bottom: 1px solid var(--line-soft);
  }
  .transparent { background: transparent; border-bottom-color: transparent; backdrop-filter: none; -webkit-backdrop-filter: none; }
  .inner {
    max-width: var(--maxw);
    margin: 0 auto;
    min-height: 64px;
    padding: 9px var(--pad) 10px;
    display: flex;
    align-items: center;
    gap: 11px;
  }
  .titles { flex: 1; min-width: 0; }
  h1 {
    margin: 0;
    font-family: var(--font-display);
    font-size: 1.26rem;
    font-weight: 600;
    letter-spacing: -0.02em;
    line-height: 1.1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  p { margin: 3px 0 0; color: var(--faint); font-size: 0.72rem; letter-spacing: 0.01em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .iconbtn {
    width: 40px;
    height: 40px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    border: 1px solid var(--line);
    background: linear-gradient(180deg, rgb(28 32 51 / 0.8), rgb(16 18 28 / 0.9));
    color: var(--ink);
    flex: none;
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.06);
    transition: transform 240ms var(--ease-spring), border-color 200ms ease;
  }
  .iconbtn:hover { border-color: var(--line-strong); transform: translateY(-1px); }
  .iconbtn:active { transform: scale(0.94); }
  .actions { display: flex; align-items: center; gap: 8px; flex: none; }
</style>
