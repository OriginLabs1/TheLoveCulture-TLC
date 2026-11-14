/** Hash router. Hash routing keeps GitHub Pages and the Android asset server
 *  simple: no rewrite rules, no 404 fallbacks, and deep links survive reloads. */

function normalise(hash: string): string {
  const raw = hash.replace(/^#/, '');
  if (!raw) return '/';
  return raw.startsWith('/') ? raw : `/${raw}`;
}

class Router {
  path = $state('/');
  previous = $state('/');

  init(): void {
    if (typeof window === 'undefined') return;
    const apply = () => {
      const next = normalise(window.location.hash);
      if (next !== this.path) {
        this.previous = this.path;
        this.path = next;
      }
    };
    window.addEventListener('hashchange', apply);
    apply();
  }

  get segments(): string[] {
    return this.path.split('?')[0].split('/').filter(Boolean);
  }

  get query(): URLSearchParams {
    const q = this.path.split('?')[1];
    return new URLSearchParams(q ?? '');
  }

  at(prefix: string): boolean {
    return this.path === prefix || this.path.startsWith(`${prefix}/`) || this.path.startsWith(`${prefix}?`);
  }

  go(to: string, opts: { replace?: boolean; keepScroll?: boolean } = {}): void {
    const clean = to.startsWith('/') ? to : `/${to}`;
    const hash = `#${clean}`;
    if (typeof window === 'undefined') return;
    if (opts.replace) {
      window.history.replaceState(null, '', hash);
      this.path = clean;
    } else if (window.location.hash === hash) {
      this.path = clean;
    } else {
      window.location.hash = clean;
    }
    if (!opts.keepScroll) window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior });
  }

  back(fallback = '/'): void {
    if (typeof window === 'undefined') return;
    if (window.history.length > 1) window.history.back();
    else this.go(fallback);
  }
}

export const router = new Router();
