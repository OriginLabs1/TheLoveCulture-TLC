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
  <span class="orb"><Icon name={icon} size={22} stroke={1.6} /></span>
  <h3>{title}</h3>
  <p>{text}</p>
  {#if action}<div class="action">{@render action()}</div>{/if}
</div>

<style>
  .empty {
    text-align: center;
    padding: 46px 22px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .orb {
    position: relative;
    width: 60px;
    height: 60px;
    border-radius: 22px 22px 22px 10px;
    display: grid;
    place-items: center;
    color: var(--accent);
    background:
      radial-gradient(80% 80% at 28% 18%, rgb(255 255 255 / 0.7), transparent 60%),
      var(--accent-soft);
    border: 1px solid color-mix(in srgb, var(--accent) 20%, transparent);
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.5), 0 16px 34px -20px color-mix(in srgb, var(--accent) 55%, transparent);
    margin-bottom: 8px;
  }
  h3 {
    margin: 0;
    font-family: var(--font-display);
    font-weight: 600;
    letter-spacing: -0.026em;
    font-size: 1.16rem;
  }
  p { margin: 0; color: var(--muted); font-size: 0.84rem; line-height: 1.6; max-width: 38ch; }
  .action { margin-top: 12px; }
</style>
