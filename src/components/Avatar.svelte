<script lang="ts">
  import { initials } from '../lib/util';

  let {
    name = '',
    color = '#5b5bd6',
    size = 44,
    ring = false,
    square = false
  }: { name?: string; color?: string; size?: number; ring?: boolean; square?: boolean } = $props();

  const text = $derived(initials(name));
</script>

<span
  class="avatar"
  class:ring
  class:square
  style={`--size:${size}px; --tint:${color}`}
  title={name}
  aria-label={name}
>
  <span class="glyph">{text}</span>
</span>

<style>
  .avatar {
    position: relative;
    width: var(--size);
    height: var(--size);
    border-radius: 50%;
    display: inline-grid;
    place-items: center;
    flex: none;
    overflow: hidden;
    background:
      radial-gradient(circle at 30% 20%, color-mix(in srgb, var(--tint) 54%, white), transparent 62%),
      radial-gradient(circle at 78% 92%, color-mix(in srgb, var(--tint) 62%, black), transparent 70%),
      var(--tint);
    color: #fff;
    font-size: calc(var(--size) * 0.35);
    font-weight: 650;
    letter-spacing: 0.03em;
    text-shadow: 0 1px 2px rgb(0 0 0 / 0.25);
    box-shadow:
      inset 0 0 0 1px rgb(255 255 255 / 0.17),
      inset 0 -8px 16px rgb(0 0 0 / 0.16);
    user-select: none;
  }
  .avatar::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: linear-gradient(155deg, rgb(255 255 255 / 0.26), transparent 42%);
    pointer-events: none;
  }
  .glyph { position: relative; z-index: 1; }
  .square { border-radius: calc(var(--size) * 0.3); }
  .ring {
    box-shadow:
      0 0 0 2px var(--bg),
      0 0 0 3.5px color-mix(in srgb, var(--tint) 62%, transparent),
      0 0 26px -6px color-mix(in srgb, var(--tint) 60%, transparent);
  }
</style>
