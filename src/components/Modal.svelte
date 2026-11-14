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
    background: rgb(2 3 7 / 0.7);
    backdrop-filter: blur(10px) saturate(120%);
    -webkit-backdrop-filter: blur(10px) saturate(120%);
    z-index: 70;
    animation: fade 220ms ease-out;
  }
  .sheet {
    position: fixed;
    z-index: 71;
    left: 50%;
    bottom: 0;
    translate: -50% 0;
    width: min(100%, 660px);
    max-height: 88vh;
    display: flex;
    flex-direction: column;
    background:
      radial-gradient(24rem 16rem at 88% -14%, rgb(139 141 255 / 0.16), transparent 62%),
      linear-gradient(180deg, #151827, #0b0d16);
    border: 1px solid rgb(255 255 255 / 0.11);
    border-bottom: none;
    border-radius: 28px 28px 0 0;
    box-shadow:
      0 -30px 90px rgb(0 0 0 / 0.6),
      inset 0 1px 0 rgb(255 255 255 / 0.1);
    animation: rise 420ms var(--ease);
  }
  .grab {
    width: 38px;
    height: 4px;
    border-radius: 999px;
    background: rgb(255 255 255 / 0.18);
    margin: 10px auto 0;
    flex: none;
  }
  header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 22px 14px;
    border-bottom: 1px solid var(--line-soft);
  }
  .headtext { flex: 1; min-width: 0; }
  h2 { margin: 0; font-family: var(--font-display); font-size: 1.32rem; font-weight: 600; letter-spacing: -0.022em; line-height: 1.18; }
  p { margin: 4px 0 0; color: var(--muted); font-size: 0.82rem; }
  .body {
    padding: 20px 22px;
    overflow: auto;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
    /* A flex item defaults to min-height:auto and would push the footer off
       small screens instead of scrolling inside the sheet. */
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
  }
  .closebtn {
    width: 38px; height: 38px;
    display: grid; place-items: center;
    border-radius: 50%;
    border: 1px solid var(--line);
    background: rgb(255 255 255 / 0.04);
    color: var(--muted);
    flex: none;
    transition: transform 240ms var(--ease-spring), color 200ms ease;
  }
  .closebtn:hover { color: var(--ink); }
  .closebtn:active { transform: scale(0.92); }
  @media (min-width: 700px) {
    .sheet { bottom: 6vh; border-radius: 28px; border-bottom: 1px solid rgb(255 255 255 / 0.11); }
  }
  @keyframes fade { from { opacity: 0 } }
  @keyframes rise { from { transform: translateY(46px); opacity: 0 } }
</style>
