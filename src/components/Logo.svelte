<script lang="ts">
  let { size = 40, glow = false }: { size?: number; glow?: boolean } = $props();
</script>

<svg
  width={size}
  height={size}
  viewBox="0 0 64 64"
  fill="none"
  role="img"
  aria-label="Union"
  class:glow
>
  <defs>
    <linearGradient id="union-mark" x1="12" y1="9" x2="53" y2="57" gradientUnits="userSpaceOnUse">
      <stop stop-color="#e2e3ff" />
      <stop offset="0.45" stop-color="#9a9cff" />
      <stop offset="1" stop-color="#5eead4" />
    </linearGradient>
    <linearGradient id="union-ring" x1="8" y1="6" x2="56" y2="58" gradientUnits="userSpaceOnUse">
      <stop stop-color="#ffffff" stop-opacity="0.42" />
      <stop offset="0.6" stop-color="#8b8dff" stop-opacity="0.28" />
      <stop offset="1" stop-color="#e8c690" stop-opacity="0.35" />
    </linearGradient>
  </defs>
  <circle cx="32" cy="32" r="29" stroke="url(#union-ring)" stroke-width="1" stroke-dasharray="3 5" opacity="0.75" />
  <path d="M20.5 16.5v19a11.5 11.5 0 0 0 23 0v-19" stroke="url(#union-mark)" stroke-width="3.6" stroke-linecap="round" />
  <circle cx="32" cy="32" r="3.2" fill="#5eead4" />
  <circle cx="32" cy="32" r="7.5" stroke="#5eead4" stroke-opacity="0.28" stroke-width="0.8" />
</svg>

<style>
  svg { display: block; overflow: visible; }
  .glow { filter: drop-shadow(0 0 24px rgb(139 141 255 / 0.4)); }
</style>
