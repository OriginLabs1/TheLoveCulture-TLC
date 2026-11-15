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
      <stop stop-color="#6c5cff" />
      <stop offset="0.5" stop-color="#4a3aff" />
      <stop offset="1" stop-color="#0b7a6e" />
    </linearGradient>
    <linearGradient id="union-ring" x1="8" y1="6" x2="56" y2="58" gradientUnits="userSpaceOnUse">
      <stop stop-color="#17181d" stop-opacity="0.24" />
      <stop offset="0.6" stop-color="#4a3aff" stop-opacity="0.24" />
      <stop offset="1" stop-color="#8a5c14" stop-opacity="0.3" />
    </linearGradient>
  </defs>
  <circle cx="32" cy="32" r="29" stroke="url(#union-ring)" stroke-width="1" stroke-dasharray="3 5" opacity="0.75" />
  <path d="M20.5 16.5v19a11.5 11.5 0 0 0 23 0v-19" stroke="url(#union-mark)" stroke-width="3.6" stroke-linecap="round" />
  <circle cx="32" cy="32" r="3.2" fill="#8a5c14" />
  <circle cx="32" cy="32" r="7.5" stroke="#8a5c14" stroke-opacity="0.26" stroke-width="0.8" />
</svg>

<style>
  svg { display: block; overflow: visible; }
  .glow { filter: drop-shadow(0 0 22px rgb(74 58 255 / 0.32)); }
</style>
