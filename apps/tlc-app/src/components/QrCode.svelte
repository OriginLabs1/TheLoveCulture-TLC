<script lang="ts">
  import { qrCode } from '../lib/qr';

  let {
    text = '',
    size = 208,
    caption = ''
  }: { text?: string; size?: number; caption?: string } = $props();

  const code = $derived(qrCode(text));
  const span = $derived(code ? code.count + code.margin * 2 : 0);
</script>

{#if code}
  <figure class="qr" style={`--size:${size}px`}>
    <svg viewBox={`${-code.margin} ${-code.margin} ${span} ${span}`} role="img" aria-label="QR code">
      <rect x={-code.margin} y={-code.margin} width={span} height={span} fill="#ffffff" />
      <path d={code.path} fill="#08090d" />
    </svg>
    {#if caption}<figcaption>{caption}</figcaption>{/if}
  </figure>
{:else}
  <div class="nofit" style={`--size:${size}px`}>
    <p>This content is too large for a QR code.</p>
    <p class="hint">Use the copy or share action instead.</p>
  </div>
{/if}

<style>
  .qr {
    margin: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .qr svg {
    width: var(--size);
    height: var(--size);
    border-radius: 12px;
    background: #fff;
    padding: 6px;
    box-shadow: 0 8px 24px rgb(0 0 0 / 0.3);
  }
  figcaption { color: var(--muted); font-size: 0.76rem; }
  .nofit {
    width: var(--size);
    height: var(--size);
    display: grid;
    place-content: center;
    text-align: center;
    border: 1px dashed var(--line);
    border-radius: 16px;
    padding: 16px;
    color: var(--muted);
    font-size: 0.8rem;
  }
  .hint { color: var(--faint); font-size: 0.72rem; margin-top: 4px; }
</style>
