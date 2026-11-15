<script lang="ts">
  import { onMount } from 'svelte';
  import Icon from './Icon.svelte';
  import { router } from '../lib/router.svelte';
  import { union } from '../lib/store.svelte';
  import { haptic } from '../lib/native';

  let {
    items = [
      { path: '/', label: 'Home', icon: 'home' },
      { path: '/explore', label: 'Explore', icon: 'compass' },
      { path: '/people', label: 'People', icon: 'people' },
      { path: '/messages', label: 'Messages', icon: 'message' },
      { path: '/more', label: 'More', icon: 'more' }
    ]
  }: { items?: { path: string; label: string; icon: string }[] } = $props();

  const pending = $derived(union.incomingRequests.length);
  const unread = $derived(union.unreadMessages);
  const activeIndex = $derived(
    Math.max(0, items.findIndex((item) => (item.path === '/' ? router.path === '/' || router.path === '' : router.at(item.path))))
  );

  function active(path: string): boolean {
    if (path === '/') return router.path === '/' || router.path === '';
    return router.at(path);
  }

  function badgeFor(path: string): number {
    if (path === '/messages') return unread;
    if (path === '/people') return pending;
    return 0;
  }

  function go(path: string, isActive: boolean) {
    if (isActive) return;
    haptic('select');
    router.go(path);
  }

  /* The rail needs room beside the content, so the shell marks the body
     while the viewport is wide enough for it. */
  onMount(() => {
    const mq = window.matchMedia('(min-width: 1080px)');
    const sync = () => document.body.classList.toggle('has-rail', mq.matches);
    sync();
    mq.addEventListener('change', sync);
    return () => {
      mq.removeEventListener('change', sync);
      document.body.classList.remove('has-rail');
    };
  });
</script>

<nav aria-label="Primary">
  <div class="dock" style={`--active:${activeIndex}`}>
    <span class="glow" aria-hidden="true"></span>
    {#each items as item (item.path)}
      <button
        class:active={active(item.path)}
        onclick={() => go(item.path, active(item.path))}
        aria-current={active(item.path) ? 'page' : undefined}
      >
        <span class="iconwrap">
          <Icon name={item.icon} size={20} stroke={active(item.path) ? 2 : 1.65} />
          {#if badgeFor(item.path) > 0}
            <span class="dot" aria-label={`${badgeFor(item.path)} waiting`}>{badgeFor(item.path) > 9 ? '9+' : badgeFor(item.path)}</span>
          {/if}
        </span>
        <span class="label">{item.label}</span>
      </button>
    {/each}
  </div>
</nav>

<style>
  nav {
    position: fixed;
    z-index: 40;
    left: 50%;
    bottom: calc(12px + env(safe-area-inset-bottom));
    translate: -50% 0;
    width: min(calc(100% - 24px), 560px);
  }
  .dock {
    position: relative;
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    padding: 6px;
    border-radius: 30px;
    background: var(--glass-strong);
    border: 1px solid var(--glass-edge);
    backdrop-filter: blur(30px) saturate(190%);
    -webkit-backdrop-filter: blur(30px) saturate(190%);
    box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.7),
      0 30px 70px -24px rgb(23 24 29 / 0.36),
      0 2px 8px rgb(23 24 29 / 0.06);
  }
  @media (prefers-color-scheme: dark) {
    .dock {
      box-shadow:
        inset 0 1px 0 rgb(255 255 255 / 0.09),
        0 30px 70px -24px rgb(0 0 0 / 0.85),
        0 2px 8px rgb(0 0 0 / 0.4);
    }
  }
  .glow {
    position: absolute;
    z-index: 0;
    top: 6px;
    left: 6px;
    width: calc((100% - 12px) / 5);
    height: calc(100% - 12px);
    border-radius: 25px;
    background: radial-gradient(60% 70% at 50% 40%, color-mix(in srgb, var(--accent) 13%, transparent), transparent 74%);
    transform: translateX(calc(var(--active) * 100%));
    transition: transform 480ms var(--ease);
    pointer-events: none;
  }
  button {
    position: relative;
    z-index: 1;
    background: none;
    border: 0;
    color: var(--faint);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 7px 2px 5px;
    min-height: 58px;
    border-radius: 25px;
    transition: color 240ms ease;
    -webkit-tap-highlight-color: transparent;
  }
  button:active { transform: scale(0.94); }
  button.active { color: var(--ink); }
  .iconwrap {
    position: relative;
    display: grid;
    place-items: center;
    width: 50px;
    height: 30px;
    border-radius: 15px;
    transition: background 300ms var(--ease), box-shadow 300ms var(--ease), transform 340ms var(--ease-spring);
  }
  button.active .iconwrap {
    background: var(--accent-soft);
    color: var(--accent);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.4);
    transform: translateY(-1px);
  }
  .label {
    font-size: 0.61rem;
    font-weight: 640;
    letter-spacing: 0.045em;
    opacity: 0.78;
    transition: opacity 240ms ease;
  }
  button.active .label { opacity: 1; }
  .dot {
    position: absolute;
    top: -5px;
    right: 4px;
    min-width: 17px;
    height: 17px;
    padding: 0 4px;
    border-radius: 9px;
    background: var(--rose);
    color: #fff;
    font-size: 0.58rem;
    font-weight: 700;
    display: grid;
    place-items: center;
    border: 2px solid var(--surface);
    box-shadow: 0 4px 10px -2px color-mix(in srgb, var(--rose) 55%, transparent);
  }
  @media (prefers-color-scheme: dark) {
    /* The dark rose is light; ink keeps the count legible. */
    .dot { color: #240910; }
  }

  /* Desktop: a quiet navigation rail instead of a dock. */
  @media (min-width: 1080px) {
    nav {
      left: 18px;
      bottom: auto;
      top: 50%;
      translate: 0 -50%;
      width: auto;
    }
    .dock {
      grid-template-columns: 1fr;
      gap: 4px;
      padding: 10px;
      border-radius: 32px;
    }
    .glow {
      top: 10px;
      left: 10px;
      width: calc(100% - 20px);
      height: 64px;
      border-radius: 22px;
      transform: translateY(calc(var(--active) * 68px));
      transition: transform 480ms var(--ease);
    }
    button {
      width: 72px;
      min-height: 64px;
      padding: 8px 2px 6px;
      border-radius: 22px;
    }
    .label { font-size: 0.6rem; }
  }
</style>
