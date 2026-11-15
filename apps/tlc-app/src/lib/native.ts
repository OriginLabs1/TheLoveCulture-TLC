/** Device/WebView bridge. Union keeps secrets in the Android Keystore when
 *  running as the native app, and falls back carefully on the web. */

interface UnionHost {
  secretSet(key: string, value: string): boolean;
  secretGet(key: string): string | null;
  secretDelete(key: string): boolean;
  info(): string;
  share(text: string): void;
  openExternal(url: string): void;
  setTheme(color: string): void;
  haptic?(kind: string): void;
  exit(): void;
  /** Present from v0.0.1 Android shell: writes into Downloads/Union. */
  saveFile?(name: string, mime: string, base64: string): string;
}

declare global {
  interface Window {
    UnionHost?: UnionHost;
    __UNION_NATIVE__?: boolean;
    unionAndroidBack?: () => boolean;
  }
}

const host: UnionHost | undefined = typeof window !== 'undefined' ? window.UnionHost : undefined;

export const isNative = Boolean(host);
if (typeof document !== 'undefined' && isNative) {
  document.documentElement.classList.add('union-native');
  window.__UNION_NATIVE__ = true;
}

const LS = 'union.secret.';

export function secretSet(key: string, value: string): boolean {
  if (host) {
    try { return host.secretSet(key, value); } catch { return false; }
  }
  try { localStorage.setItem(LS + key, value); return true; } catch { return false; }
}

export function secretGet(key: string): string | null {
  if (host) {
    try { return host.secretGet(key); } catch { return null; }
  }
  try { return localStorage.getItem(LS + key); } catch { return null; }
}

export function secretDelete(key: string): boolean {
  if (host) {
    try { return host.secretDelete(key); } catch { return false; }
  }
  try { localStorage.removeItem(LS + key); return true; } catch { return false; }
}

export function deviceInfo(): { platform: string; native: boolean; secureStore: boolean } {
  if (host) {
    try {
      const parsed = JSON.parse(host.info()) as { platform?: string };
      return {
        platform: parsed.platform || 'Android',
        native: true,
        secureStore: true
      };
    } catch {
      return { platform: 'Android', native: true, secureStore: true };
    }
  }
  return {
    platform: typeof navigator !== 'undefined' ? navigator.platform || 'Web' : 'Web',
    native: false,
    secureStore: false
  };
}

/**
 * Save a Blob through the native shell when it exists. Returns false on the
 * web so callers can keep the ordinary anchor download path.
 */
export function saveFileNative(name: string, blob: Blob): boolean {
  if (!host || typeof host.saveFile !== 'function') return false;
  void blob
    .arrayBuffer()
    .then((buffer) => {
      const bytes = new Uint8Array(buffer);
      let binary = '';
      const chunk = 0x8000;
      for (let i = 0; i < bytes.length; i += chunk) {
        binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
      }
      host.saveFile?.(name, blob.type || 'application/octet-stream', btoa(binary));
    })
    .catch(() => undefined);
  return true;
}

export function shareText(text: string): void {
  if (host) { host.share(text); return; }
  if (navigator.share) {
    navigator.share({ text }).catch(() => undefined);
    return;
  }
  navigator.clipboard?.writeText(text).catch(() => undefined);
}

export function openExternal(url: string): void {
  if (host) { host.openExternal(url); return; }
  window.open(url, '_blank', 'noopener,noreferrer');
}

export function setSystemTheme(color: string): void {
  if (!host) return;
  try { host.setTheme(color); } catch { /* older shell */ }
}

export function syncShellChrome(): void {
  if (typeof window === 'undefined') return;
  const dark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  const color = dark ? '#101116' : '#f4f1ea';
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', color);
  setSystemTheme(color);
}

export function watchShellChrome(): void {
  if (typeof window === 'undefined' || !window.matchMedia) return;
  const mq = window.matchMedia('(prefers-color-scheme: dark)');
  syncShellChrome();
  mq.addEventListener('change', syncShellChrome);
}

export function exitApp(): void {
  if (host) host.exit();
}

/** A small physical confirmation. Native shells use the platform haptic
 *  engine; browsers fall back to the vibration API where it exists. */
export function haptic(kind: 'select' | 'tap' | 'success' | 'warn' = 'tap'): void {
  if (host?.haptic) {
    try { host.haptic(kind); return; } catch { /* older shell */ }
  }
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
    const pattern = kind === 'success' ? [12, 34, 16] : kind === 'warn' ? [22, 40, 22] : 9;
    try { navigator.vibrate(pattern); } catch { /* not permitted */ }
  }
}
