<script lang="ts">
  import Icon from './Icon.svelte';
  import { router } from '../lib/router.svelte';
  import { union } from '../lib/store.svelte';

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

  function active(path: string): boolean {
    if (path === '/') return router.path === '/' || router.path === '';
    return router.at(path);
  }

  function badgeFor(path: string): number {
    if (path === '/messages') return unread;
    if (path === '/people') return pending;
    return 0;
  }
</script>

<nav aria-label="Primary">
  <div class="dock">
    {#each items as item (item.path)}
      <button class:active={active(item.path)} onclick={() => router.go(item.path)} aria-current={active(item.path) ? 'page' : undefined}>
        <span class="iconwrap">
          <Icon name={item.icon} size={20} stroke={active(item.path) ? 1.9 : 1.6} />
          {#if badgeFor(item.path) > 0}
            <span class="dot">{badgeFor(item.path) > 9 ? '9+' : badgeFor(item.path)}</span>
          {/if}
        </span>
        <span class="label">{item.label}</span>
      </button>
    {/each}
    <span class="active-glow" aria-hidden="true" style={`transform:translateX(${Math.max(0, items.findIndex((i) => active(i.path))) * 100}%)`}></span>
  </div>
</nav>

<style>
  nav {
    position: fixed;
    z-index: 40;
    left: 50%;
    bottom: calc(10px + env(safe-area-inset-bottom));
    translate: -50% 0;
    width: min(calc(100% - 20px), 560px);
    padding: 0;
  }
  .dock {
    position: relative;
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    padding: 7px;
    border-radius: 26px;
    background: linear-gradient(180deg, rgb(26 30 47 / 0.82), rgb(11 13 21 / 0.92));
    border: 1px solid rgb(255 255 255 / 0.1);
    backdrop-filter: blur(26px) saturate(165%);
    -webkit-backdrop-filter: blur(26px) saturate(165%);
    box-shadow:
      0 28px 70px -20px rgb(0 0 0 / 0.92),
      0 2px 10px rgb(0 0 0 / 0.4),
      inset 0 1px 0 rgb(255 255 255 / 0.09);
    overflow: hidden;
  }
  .dock::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(140deg, rgb(255 255 255 / 0.16), transparent 36%, transparent 68%, rgb(139 141 255 / 0.22));
    -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
    -webkit-mask-composite: xor;
    mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
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
    gap: 2px;
    padding: 5px 2px 4px;
    min-height: 56px;
    border-radius: 19px;
    transition: color 240ms ease;
  }
  button:active { transform: scale(0.94); }
  button.active { color: var(--accent-2); }
  .iconwrap {
    position: relative;
    display: grid;
    place-items: center;
    width: 48px;
    height: 29px;
    border-radius: 10px;
    transition: background 300ms var(--ease), box-shadow 300ms var(--ease), transform 300ms var(--ease-spring);
  }
  button.active .iconwrap {
    background: linear-gradient(180deg, rgb(139 141 255 / 0.28), rgb(139 141 255 / 0.1));
    box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.16),
      0 6px 18px -8px rgb(139 141 255 / 0.7);
    transform: translateY(-1px);
  }
  .label {
    font-size: 0.62rem;
    font-weight: 600;
    letter-spacing: 0.035em;
    opacity: 0.82;
    transition: opacity 240ms ease;
  }
  button.active .label { opacity: 1; }
  .dot {
    position: absolute;
    top: -4px;
    right: 6px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    background: linear-gradient(180deg, #ff8fa3, #f4637a);
    color: #fff;
    font-size: 0.6rem;
    font-weight: 700;
    display: grid;
    place-items: center;
    border: 2px solid #10131e;
    box-shadow: 0 4px 10px -2px rgb(255 143 163 / 0.5);
  }
  /* A soft wash follows the active item without moving layout. */
  .active-glow {
    position: absolute;
    z-index: 0;
    left: 7px;
    bottom: 6px;
    width: calc(20% - 2.8px);
    height: 52px;
    border-radius: 18px;
    background: radial-gradient(62% 74% at 50% 58%, rgb(139 141 255 / 0.15), transparent 72%);
    transition: transform 460ms var(--ease);
    pointer-events: none;
  }
</style>
