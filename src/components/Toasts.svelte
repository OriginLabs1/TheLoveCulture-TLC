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
          size={16}
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
    bottom: calc(94px + env(safe-area-inset-bottom));
    translate: -50% 0;
    z-index: 90;
    width: min(calc(100% - 28px), 540px);
    display: flex;
    flex-direction: column;
    gap: 9px;
    pointer-events: none;
  }
  .toast {
    pointer-events: auto;
    display: flex;
    align-items: flex-start;
    gap: 11px;
    text-align: left;
    padding: 13px 15px;
    border-radius: 16px;
    background: linear-gradient(170deg, rgb(34 39 60 / 0.94), rgb(16 18 29 / 0.96));
    border: 1px solid rgb(255 255 255 / 0.12);
    color: var(--ink);
    font-size: 0.84rem;
    line-height: 1.45;
    box-shadow:
      0 24px 60px -20px rgb(0 0 0 / 0.85),
      inset 0 1px 0 rgb(255 255 255 / 0.08);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    animation: pop 340ms var(--ease-spring);
  }
  .ttext { flex: 1; min-width: 0; padding-top: 1px; }
  .ticon {
    width: 28px; height: 28px;
    border-radius: 9px;
    display: grid; place-items: center;
    flex: none;
    background: rgb(139 141 255 / 0.14);
    color: var(--accent-2);
  }
  .toast.success .ticon { background: rgb(82 224 164 / 0.14); color: var(--green); }
  .toast.error .ticon, .toast.warn .ticon { background: rgb(255 143 163 / 0.14); color: var(--rose); }
  .toast.badge .ticon { background: rgb(232 198 144 / 0.15); color: var(--gold); }
  .toast.badge { border-color: rgb(232 198 144 / 0.32); }
  @keyframes pop {
    from { transform: translateY(14px) scale(0.97); opacity: 0 }
  }
</style>
