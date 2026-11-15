<script lang="ts">
  import { onMount } from 'svelte';
  import type { Snippet } from 'svelte';
  import Icon from './Icon.svelte';
  import { router } from '../lib/router.svelte';
  import { haptic } from '../lib/native';

  let {
    title = '',
    subtitle = '',
    eyebrow = '',
    back = false,
    fallback = '/',
    actions,
    transparent = false
  }: {
    title?: string;
    subtitle?: string;
    eyebrow?: string;
    back?: boolean;
    fallback?: string;
    actions?: Snippet;
    transparent?: boolean;
  } = $props();

  let scrolled = $state(false);

  onMount(() => {
    const onScroll = () => {
      scrolled = window.scrollY > 18;
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  });

  function goBack() {
    haptic('select');
    router.back(fallback);
  }
</script>

<header class="appbar" class:scrolled class:transparent>
  <div class="bar">
    {#if back}
      <button class="backbtn" onclick={goBack} aria-label="Back">
        <Icon name="arrow-left" size={18} />
      </button>
    {/if}
    <span class="compact-title" class:show={scrolled && !transparent}>{title}</span>
    {#if actions}
      <div class="actions">{@render actions()}</div>
    {/if}
  </div>
</header>

<div class="masthead" class:with-back={back}>
  <div class="masttext">
    {#if eyebrow}<p class="eyebrow accent">{eyebrow}</p>{/if}
    <h1>{title}</h1>
    {#if subtitle}<p class="sub">{subtitle}</p>{/if}
  </div>
</div>

<style>
  .appbar {
    position: sticky;
    top: 0;
    z-index: 30;
    padding-top: env(safe-area-inset-top);
    background: transparent;
    transition: background 280ms var(--ease), box-shadow 280ms var(--ease), border-color 280ms var(--ease);
    border-bottom: 1px solid transparent;
  }
  .appbar.scrolled {
    background: var(--glass);
    border-bottom-color: var(--line-soft);
    backdrop-filter: blur(26px) saturate(190%);
    -webkit-backdrop-filter: blur(26px) saturate(190%);
    box-shadow: 0 8px 30px -22px rgb(23 24 29 / 0.4);
  }
  .transparent { background: transparent; border-bottom-color: transparent; box-shadow: none; backdrop-filter: none; -webkit-backdrop-filter: none; }
  .bar {
    max-width: var(--maxw);
    margin: 0 auto;
    min-height: 58px;
    padding: 8px var(--pad);
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .backbtn {
    width: 40px;
    height: 40px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    border: 1px solid var(--line);
    background: var(--glass-strong);
    color: var(--ink);
    flex: none;
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.55), 0 1px 2px rgb(23 24 29 / 0.05);
    backdrop-filter: blur(18px) saturate(170%);
    -webkit-backdrop-filter: blur(18px) saturate(170%);
    transition: transform 260ms var(--ease-spring), border-color 200ms ease;
  }
  .backbtn:hover { border-color: var(--line-strong); transform: translateY(-1px); }
  .backbtn:active { transform: scale(0.93); }
  .compact-title {
    flex: 1;
    min-width: 0;
    font-family: var(--font-display);
    font-size: 1.02rem;
    font-weight: 600;
    letter-spacing: -0.026em;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    opacity: 0;
    transform: translateY(5px);
    transition: opacity 260ms var(--ease), transform 320ms var(--ease);
  }
  .compact-title.show { opacity: 1; transform: none; }
  .actions { display: flex; align-items: center; gap: 8px; flex: none; margin-left: auto; }

  .masthead {
    max-width: var(--maxw);
    margin: 0 auto;
    padding: 8px var(--pad) 20px;
    animation: mast-in 600ms var(--ease) both;
  }
  .masthead.with-back { padding-top: 0; }
  .masttext { max-width: 58ch; }
  h1 {
    margin: 0;
    font-family: var(--font-display);
    font-size: clamp(2rem, 7.4vw, 2.72rem);
    font-weight: 560;
    letter-spacing: -0.048em;
    line-height: 0.98;
    text-wrap: balance;
  }
  .eyebrow { margin: 0 0 9px; }
  .sub {
    margin: 12px 0 0;
    color: var(--muted);
    font-size: 0.88rem;
    line-height: 1.55;
    letter-spacing: 0.002em;
    max-width: 52ch;
  }
  @keyframes mast-in {
    from { opacity: 0; transform: translateY(10px); }
  }
</style>
