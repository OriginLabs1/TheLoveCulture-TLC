<script lang="ts">
  import type { Snippet } from 'svelte';
  import Icon from './Icon.svelte';

  let {
    icon = 'feather',
    title = '',
    text = '',
    action
  }: { icon?: string; title?: string; text?: string; action?: Snippet } = $props();
</script>

<div class="empty">
  <span class="orb"><Icon name={icon} size={22} /></span>
  <h3>{title}</h3>
  <p>{text}</p>
  {#if action}<div class="action">{@render action()}</div>{/if}
</div>

<style>
  .empty {
    text-align: center;
    padding: 44px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .orb {
    width: 58px;
    height: 58px;
    border-radius: 20px;
    display: grid;
    place-items: center;
    color: var(--accent-2);
    background: linear-gradient(155deg, rgb(139 141 255 / 0.18), rgb(139 141 255 / 0.04));
    border: 1px solid rgb(139 141 255 / 0.24);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.08), 0 12px 30px -16px rgb(139 141 255 / 0.6);
    margin-bottom: 6px;
  }
  h3 { margin: 0; font-family: var(--font-display); font-weight: 600; letter-spacing: -0.018em; font-size: 1.14rem; }
  p { margin: 0; color: var(--muted); font-size: 0.84rem; line-height: 1.6; max-width: 36ch; }
  .action { margin-top: 12px; }
</style>
