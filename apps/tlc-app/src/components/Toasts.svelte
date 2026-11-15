<script lang="ts">
  import Icon from './Icon.svelte';
  import { toasts } from '../lib/toast.svelte';
</script>

<div class="toasts" aria-live="polite">
  {#each toasts.items as toast (toast.id)}
    <button class="toast {toast.kind}" onclick={() => toasts.dismiss(toast.id)}>
      <span class="ticon">
        <Icon
          name={toast.kind === 'success' ? 'check' : toast.kind === 'error' ? 'alert' : toast.kind === 'badge' ? 'badge' : toast.kind === 'warn' ? 'alert' : 'info'}
          size={15}
        />
      </span>
      <span class="ttext">{toast.text}</span>
    </button>
  {/each}
</div>

<style>
  .toasts {
    position: fixed;
    left: 50%;
    bottom: calc(96px + env(safe-area-inset-bottom));
    translate: -50% 0;
    z-index: 90;
    width: min(calc(100% - 28px), 520px);
    display: flex;
    flex-direction: column;
    gap: 8px;
    pointer-events: none;
  }
  .toast {
    pointer-events: auto;
    display: flex;
    align-items: flex-start;
    gap: 11px;
    text-align: left;
    padding: 12px 15px 12px 12px;
    border-radius: 20px;
    background: var(--glass-strong);
    border: 1px solid var(--glass-edge);
    color: var(--ink);
    font-size: 0.83rem;
    line-height: 1.45;
    box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.65),
      0 22px 50px -18px rgb(23 24 29 / 0.3);
    backdrop-filter: blur(26px) saturate(190%);
    -webkit-backdrop-filter: blur(26px) saturate(190%);
    animation: pop 380ms var(--ease-spring);
  }
  @media (prefers-color-scheme: dark) {
    .toast { box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.08), 0 22px 50px -18px rgb(0 0 0 / 0.8); }
  }
  .ttext { flex: 1; min-width: 0; padding-top: 2px; }
  .ticon {
    width: 29px; height: 29px;
    border-radius: 10px;
    display: grid; place-items: center;
    flex: none;
    background: var(--accent-soft);
    color: var(--accent);
  }
  .toast.success .ticon { background: var(--green-soft); color: var(--green); }
  .toast.error .ticon, .toast.warn .ticon { background: var(--rose-soft); color: var(--rose); }
  .toast.badge .ticon { background: var(--gold-soft); color: var(--gold); }
  .toast.badge { border-color: color-mix(in srgb, var(--gold) 32%, var(--glass-edge)); }
  .toast.error { border-color: color-mix(in srgb, var(--rose) 26%, var(--glass-edge)); }
  @media (min-width: 1080px) {
    .toasts {
      left: auto;
      right: 26px;
      bottom: 26px;
      translate: 0 0;
    }
  }
  @keyframes pop {
    from { transform: translateY(14px) scale(0.97); opacity: 0 }
  }
</style>
