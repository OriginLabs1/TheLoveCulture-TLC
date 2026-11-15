<script lang="ts">
  /** A single stroke icon set drawn on a 24×24 grid. */
  let {
    name = 'spark',
    size = 20,
    stroke = 1.7,
    label = ''
  }: { name?: string; size?: number; stroke?: number; label?: string } = $props();

  const paths: Record<string, string> = {
    home: '<path d="M3.5 10.5 12 3l8.5 7.5"/><path d="M5.5 9.5V20h5v-5.5h3V20h5V9.5"/>',
    compass: '<circle cx="12" cy="12" r="8.5"/><path d="m14.8 9.2-1.7 3.9-3.9 1.7 1.7-3.9 3.9-1.7Z"/>',
    people: '<circle cx="9" cy="8.5" r="3.2"/><path d="M3.5 20c.5-3.4 2.8-5.3 5.5-5.3s5 1.9 5.5 5.3"/><path d="M16 5.6a3 3 0 0 1 0 5.8"/><path d="M17.5 14.9c2 .5 3.2 2.3 3.5 5.1"/>',
    message: '<path d="M20.5 12.2c0 4-3.8 7.2-8.5 7.2a10 10 0 0 1-2.6-.3L4.5 21l1.2-4.2A6.8 6.8 0 0 1 3.5 12c0-4 3.8-7.2 8.5-7.2s8.5 3.2 8.5 7.4Z"/><path d="M8.5 12h.01M12 12h.01M15.5 12h.01"/>',
    more: '<circle cx="5" cy="12" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="19" cy="12" r="1.3"/>',
    send: '<path d="m21 3-7.2 18-3.4-7.4L3 10.2 21 3Z"/><path d="m10.4 13.6 4.3-4.3"/>',
    shield: '<path d="M12 3.2 5 6v5.3c0 4.4 3 7.8 7 9.5 4-1.7 7-5.1 7-9.5V6l-7-2.8Z"/><path d="m9 12 2.2 2.2L15.5 10"/>',
    book: '<path d="M4 4.5h6a3 3 0 0 1 3 3V20a2.6 2.6 0 0 0-2.6-2.6H4V4.5Z"/><path d="M20 4.5h-6a3 3 0 0 0-3 3V20a2.6 2.6 0 0 1 2.6-2.6H20V4.5Z"/>',
    cap: '<path d="m2.8 9 9.2-4.5L21.2 9 12 13.5 2.8 9Z"/><path d="M6.5 11v4.6c0 1.6 2.5 2.9 5.5 2.9s5.5-1.3 5.5-2.9V11"/><path d="M21.2 9v5"/>',
    layout: '<rect x="3.5" y="4.5" width="17" height="15" rx="2.4"/><path d="M3.5 9.5h17M9.5 9.5v10"/>',
    spark: '<path d="M12 2.8 13.9 9l6.1 1.9-6.1 1.9L12 19l-1.9-6.2L4 10.9 10.1 9 12 2.8Z"/><path d="M18.8 16.5 19.7 19l2.5.9-2.5.9-.9 2.5-.9-2.5-2.5-.9 2.5-.9.9-2.5Z"/>',
    coins: '<ellipse cx="9" cy="6.5" rx="5.5" ry="2.5"/><path d="M3.5 6.5v4c0 1.4 2.5 2.5 5.5 2.5s5.5-1.1 5.5-2.5v-4"/><path d="M3.5 10.5v4c0 1.4 2.5 2.5 5.5 2.5 1.1 0 2.1-.1 3-.4"/><ellipse cx="16" cy="14" rx="4.5" ry="2"/><path d="M11.5 14v3.5c0 1.1 2 2 4.5 2s4.5-.9 4.5-2V14"/>',
    sliders: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2.2"/><circle cx="9" cy="17" r="2.2"/>',
    handshake: '<path d="m3 11 4-4 4 3 1.5-1.5L16 12l-3 3-2-2"/><path d="m13 15 2 2 3-3 3-3-4-4-3 3"/><path d="M3 11v5l3 3 4-3"/>',
    heart: '<path d="M12 20.2S3.8 15.4 3.8 9.6A4.4 4.4 0 0 1 12 7a4.4 4.4 0 0 1 8.2 2.6c0 5.8-8.2 10.6-8.2 10.6Z"/>',
    check: '<path d="m5 12.5 4.4 4.4L19.5 6.8"/>',
    search: '<circle cx="10.8" cy="10.8" r="6.3"/><path d="m15.5 15.5 4.5 4.5"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    'arrow-left': '<path d="M19 12H5"/><path d="m11 6-6 6 6 6"/>',
    'arrow-right': '<path d="M5 12h14"/><path d="m13 6 6 6-6 6"/>',
    'chevron-right': '<path d="m9 5 7 7-7 7"/>',
    'chevron-down': '<path d="m5 9 7 7 7-7"/>',
    close: '<path d="m6 6 12 12M18 6 6 18"/>',
    copy: '<rect x="8.5" y="8.5" width="11" height="11" rx="2"/><path d="M15.5 8.5v-3a2 2 0 0 0-2-2h-8a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3"/>',
    share: '<circle cx="6" cy="12" r="2.4"/><circle cx="18" cy="6" r="2.4"/><circle cx="18" cy="18" r="2.4"/><path d="m8.2 10.8 7.6-3.6M8.2 13.2l7.6 3.6"/>',
    qr: '<rect x="3.5" y="3.5" width="6" height="6" rx="1"/><rect x="14.5" y="3.5" width="6" height="6" rx="1"/><rect x="3.5" y="14.5" width="6" height="6" rx="1"/><path d="M14.5 14.5h2.6v2.6h-2.6zM20.5 14.5v6h-6"/><path d="M14.5 20.5h.01M20.5 11h.01"/>',
    link: '<path d="M10.2 13.8a4 4 0 0 0 5.7 0l2.4-2.4a4 4 0 0 0-5.7-5.7l-1.4 1.4"/><path d="M13.8 10.2a4 4 0 0 0-5.7 0l-2.4 2.4a4 4 0 0 0 5.7 5.7l1.4-1.4"/>',
    globe: '<circle cx="12" cy="12" r="8.5"/><path d="M3.7 9.5h16.6M3.7 14.5h16.6"/><path d="M12 3.5c2.4 2.3 3.6 5.2 3.6 8.5s-1.2 6.2-3.6 8.5c-2.4-2.3-3.6-5.2-3.6-8.5S9.6 5.8 12 3.5Z"/>',
    flag: '<path d="M5.5 21V4"/><path d="M5.5 4.5C9 2.7 11.8 2.7 15 4.5s4.2 1.4 4.2 1.4v8.6s-1 .4-4.2-1.4-6-.2-9.5 1.6"/>',
    notes: '<path d="M6 3.5h9l4 4v13H6v-17Z"/><path d="M15 3.5v4h4"/><path d="M9 12h7M9 15.5h7M9 8.5h2"/>',
    external: '<path d="M13 4h7v7"/><path d="M20 4 11 13"/><path d="M18 14v4.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 18.5v-11A1.5 1.5 0 0 1 5.5 6H10"/>',
    mail: '<rect x="3.5" y="5" width="17" height="14" rx="2.4"/><path d="m4.5 6.5 7.5 6 7.5-6"/>',
    lock: '<rect x="4.5" y="10" width="15" height="10" rx="2.4"/><path d="M8 10V7.5a4 4 0 0 1 8 0V10"/><path d="M12 14v2.5"/>',
    unlock: '<rect x="4.5" y="10" width="15" height="10" rx="2.4"/><path d="M8 10V7.5a4 4 0 0 1 7.4-2.1"/><path d="M12 14v2.5"/>',
    download: '<path d="M12 3.5v11"/><path d="m7.5 10.5 4.5 4.5 4.5-4.5"/><path d="M4 17.5v1.5A1.5 1.5 0 0 0 5.5 20.5h13a1.5 1.5 0 0 0 1.5-1.5v-1.5"/>',
    upload: '<path d="M12 15V4"/><path d="m7.5 8.5 4.5-4.5 4.5 4.5"/><path d="M4 17.5v1.5A1.5 1.5 0 0 0 5.5 20.5h13a1.5 1.5 0 0 0 1.5-1.5v-1.5"/>',
    trash: '<path d="M4.5 6.5h15"/><path d="M9 6.5V4.8A1.3 1.3 0 0 1 10.3 3.5h3.4A1.3 1.3 0 0 1 15 4.8v1.7"/><path d="M6.5 6.5 7.4 20a1.5 1.5 0 0 0 1.5 1.4h6.2a1.5 1.5 0 0 0 1.5-1.4l.9-13.5"/><path d="M10 10.5v7M14 10.5v7"/>',
    edit: '<path d="M4 20h4.2L20 8.2a2.1 2.1 0 0 0-3-3L5.2 17V20Z"/><path d="m15.5 6.5 2 2"/>',
    calendar: '<rect x="3.5" y="5.5" width="17" height="15" rx="2.4"/><path d="M3.5 10h17M8 3.5v4M16 3.5v4"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 1.8"/>',
    map: '<path d="M9 4 3.5 6v14L9 18l6 2 5.5-2V4L15 6 9 4Z"/><path d="M9 4v14M15 6v14"/>',
    bell: '<path d="M6 9.5a6 6 0 0 1 12 0c0 4 1.4 5.4 1.4 5.4H4.6S6 13.5 6 9.5Z"/><path d="M10 18.2a2.2 2.2 0 0 0 4 0"/>',
    menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.4 1Z"/>',
    fingerprint: '<path d="M12 3.5a8.5 8.5 0 0 0-8.4 7.2"/><path d="M9.2 20.2a13 13 0 0 0 1.1-5.4 1.7 1.7 0 1 1 3.4 0c0 1.8-.2 3.5-.7 5.1"/><path d="M20.4 12.6A8.5 8.5 0 0 0 12 3.5"/><path d="M5.4 16.8a8.5 8.5 0 0 1-.6-3.1 7.2 7.2 0 1 1 14.4 1.4"/><path d="M12 12.5c0 2.8-.5 5.4-1.5 7.6"/>',
    refresh: '<path d="M20 11a8 8 0 0 0-13.7-5L3 9"/><path d="M3 4v5h5"/><path d="M4 13a8 8 0 0 0 13.7 5l3.3-3"/><path d="M21 20v-5h-5"/>',
    wifi: '<path d="M2.5 8.8a14 14 0 0 1 19 0"/><path d="M5.8 12.2a9.4 9.4 0 0 1 12.4 0"/><path d="M9 15.5a4.8 4.8 0 0 1 6 0"/><circle cx="12" cy="19" r="1"/>',
    'wifi-off': '<path d="M2.5 8.8a14 14 0 0 1 6-3.4M15 5.7a14 14 0 0 1 6.5 3.1"/><path d="M5.8 12.2a9.4 9.4 0 0 1 2.8-1.6M15.5 10.8a9.4 9.4 0 0 1 2.7 1.4"/><path d="M9 15.5a4.8 4.8 0 0 1 6 0"/><circle cx="12" cy="19" r="1"/><path d="m3 3 18 18"/>',
    alert: '<path d="M12 3.8 21 19.5H3L12 3.8Z"/><path d="M12 9.5v4.5"/><circle cx="12" cy="17" r=".6"/>',
    info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5"/><circle cx="12" cy="8" r=".7"/>',
    badge: '<circle cx="12" cy="9.5" r="5.5"/><path d="m8.4 14.2-1.2 6.3 4.8-2.3 4.8 2.3-1.2-6.3"/>',
    star: '<path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8L3.5 9.7l5.9-.9L12 3.5Z"/>',
    user: '<circle cx="12" cy="8" r="3.8"/><path d="M4.5 20.5c.6-4.2 3.4-6.5 7.5-6.5s6.9 2.3 7.5 6.5"/>',
    users: '<circle cx="9" cy="8.5" r="3.2"/><path d="M3.5 20c.5-3.4 2.8-5.3 5.5-5.3s5 1.9 5.5 5.3"/><path d="M16 5.6a3 3 0 0 1 0 5.8"/><path d="M17.5 14.9c2 .5 3.2 2.3 3.5 5.1"/>',
    logout: '<path d="M14 4.5h4.5A1.5 1.5 0 0 1 20 6v12a1.5 1.5 0 0 1-1.5 1.5H14"/><path d="M10 8.5 6.5 12l3.5 3.5M6.5 12H16"/>',
    play: '<path d="M8 5.5v13l10-6.5-10-6.5Z"/>',
    gift: '<rect x="3.5" y="8.5" width="17" height="4" rx="1"/><path d="M5 12.5v7.5h14v-7.5M12 8.5V20"/><path d="M12 8.5C10 8.5 7 7.9 7 5.8A2.3 2.3 0 0 1 11.5 5c.3.9.5 2 .5 3.5ZM12 8.5c2 0 5-.6 5-2.7A2.3 2.3 0 0 0 12.5 5c-.3.9-.5 2-.5 3.5Z"/>',
    wallet: '<rect x="3.5" y="5.5" width="17" height="14" rx="2.4"/><path d="M3.5 10h17"/><path d="M16.5 15h.01"/>',
    database: '<ellipse cx="12" cy="5.5" rx="7.5" ry="2.7"/><path d="M4.5 5.5v6c0 1.5 3.4 2.7 7.5 2.7s7.5-1.2 7.5-2.7v-6"/><path d="M4.5 11.5v6c0 1.5 3.4 2.7 7.5 2.7s7.5-1.2 7.5-2.7v-6"/>',
    key: '<circle cx="8" cy="15.5" r="4"/><path d="m10.8 12.7 8-8M16.5 7l2 2M14.5 9l1.5 1.5"/>',
    eye: '<path d="M2.5 12S6 5.8 12 5.8 21.5 12 21.5 12 18 18.2 12 18.2 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="3"/>',
    filter: '<path d="M3.5 5.5h17l-6.5 7.6v6.2l-4 1.7v-7.9L3.5 5.5Z"/>',
    tune: '<path d="M4 7h5M15 7h5M4 17h9M19 17h1"/><circle cx="12" cy="7" r="2.4"/><circle cx="16" cy="17" r="2.4"/>',
    'log-in': '<path d="M14 4.5h4.5A1.5 1.5 0 0 1 20 6v12a1.5 1.5 0 0 1-1.5 1.5H14"/><path d="M10 8.5 13.5 12 10 15.5M13.5 12H3.5"/>',
    crown: '<path d="m3 8 4 3 5-6 5 6 4-3-2 10H5L3 8Z"/><path d="M6 21h12"/>',
    feather: '<path d="M20 4c-6 0-11 3.5-11 9v5l5-5c2-2 4-4.5 6-9Z"/><path d="M9 18v3M4 21c1-4 3-6 7-8"/>'
  };
</script>

<svg
  width={size}
  height={size}
  viewBox="0 0 24 24"
  fill="none"
  stroke="currentColor"
  stroke-width={stroke}
  stroke-linecap="round"
  stroke-linejoin="round"
  role={label ? 'img' : 'presentation'}
  aria-label={label || undefined}
  aria-hidden={label ? undefined : 'true'}
  class="icon"
>
  {@html paths[name] ?? paths.spark}
</svg>

<style>
  .icon {
    display: inline-block;
    flex: none;
    vertical-align: -0.18em;
  }
</style>
