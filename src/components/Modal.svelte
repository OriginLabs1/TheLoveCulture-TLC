<script lang="ts">
  import type { Snippet } from 'svelte';
  import Icon from './Icon.svelte';

  let {
    open = false,
    title = '',
    subtitle = '',
    onclose,
    children,
    footer,
    wide = false
  }: {
    open?: boolean;
    title?: string;
    subtitle?: string;
    onclose?: () => void;
    children?: Snippet;
    footer?: Snippet;
    wide?: boolean;
  } = $props();

  function close() {
    onclose?.();
  }

  function onkey(event: KeyboardEvent) {
    if (event.key === 'Escape') close();
  }
</script>

<svelte:window onkeydown={onkey} />

{#if open}
  <div class="scrim" role="presentation" onclick={close}></div>
  <div class="sheet" class:wide role="dialog" aria-modal="true" aria-label={title}>
    <div class="grab" aria-hidden="true"></div>
    <header>
      <div class="headtext">
        <h2>{title}</h2>
        {#if subtitle}<p>{subtitle}</p>{/if}
      </div>
      <button class="closebtn" onclick={close} aria-label="Close"><Icon name="close" size={17} /></button>
    </header>
    <div class="body">
      {#if children}{@render children()}{/if}
    </div>
    {#if footer}
      <footer>{@render footer()}</footer>
    {/if}
  </div>
{/if}

<style>
  .scrim {
    position: fixed;
    inset: 0;
    background: rgb(23 24 29 / 0.34);
    backdrop-filter: blur(12px) saturate(120%);
    -webkit-backdrop-filter: blur(12px) saturate(120%);
    z-index: 70;
    animation: fade 240ms ease-out;
  }
  .sheet {
    position: fixed;
    z-index: 71;
    left: 50%;
    bottom: 0;
    translate: -50% 0;
    width: min(100%, 660px);
    max-height: 90vh;
    max-height: 90dvh;
    display: flex;
    flex-direction: column;
    color: var(--ink);
    background:
      radial-gradient(24rem 16rem at 92% -16%, color-mix(in srgb, var(--accent) 10%, transparent), transparent 62%),
      var(--surface);
    border: 1px solid var(--line);
    border-bottom: none;
    border-radius: 34px 34px 0 0;
    box-shadow:
      0 -28px 80px -20px rgb(23 24 29 / 0.4),
      inset 0 1px 0 rgb(255 255 255 / 0.7);
    animation: rise 460ms var(--ease-spring);
  }
  @media (prefers-color-scheme: dark) {
    .sheet { box-shadow: 0 -28px 80px -20px rgb(0 0 0 / 0.8), inset 0 1px 0 rgb(255 255 255 / 0.08); }
  }
  .grab {
    width: 40px;
    height: 4px;
    border-radius: 999px;
    background: var(--line-strong);
    margin: 10px auto 0;
    flex: none;
  }
  header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 22px 15px;
    border-bottom: 1px solid var(--line-soft);
  }
  .headtext { flex: 1; min-width: 0; }
  h2 {
    margin: 0;
    font-family: var(--font-display);
    font-size: 1.36rem;
    font-weight: 600;
    letter-spacing: -0.032em;
    line-height: 1.14;
  }
  p { margin: 5px 0 0; color: var(--muted); font-size: 0.82rem; line-height: 1.5; }
  .body {
    padding: 20px 22px;
    overflow: auto;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
    min-height: 0;
    flex: 1;
  }
  footer {
    padding: 14px 22px calc(20px + env(safe-area-inset-bottom));
    border-top: 1px solid var(--line-soft);
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    flex-wrap: wrap;
    background: var(--surface);
  }
  .closebtn {
    width: 38px; height: 38px;
    display: grid; place-items: center;
    border-radius: 50%;
    border: 1px solid var(--line);
    background: var(--surface-2);
    color: var(--muted);
    flex: none;
    transition: transform 260ms var(--ease-spring), color 200ms ease, border-color 200ms ease;
  }
  .closebtn:hover { color: var(--ink); border-color: var(--line-strong); }
  .closebtn:active { transform: scale(0.92); }
  @media (min-width: 700px) {
    .sheet {
      bottom: 7vh;
      border-radius: 34px;
      border-bottom: 1px solid var(--line);
      animation: dialog-in 380ms var(--ease-spring);
    }
    .grab { display: none; }
    header { padding-top: 22px; }
    footer { border-radius: 0 0 34px 34px; padding-bottom: 20px; }
  }
  @keyframes fade { from { opacity: 0 } }
  @keyframes rise { from { transform: translateY(52px); opacity: 0 } }
  @keyframes dialog-in { from { transform: translateY(16px) scale(0.97); opacity: 0 } }
</style>
