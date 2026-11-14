/** Toast notifications. A tiny reactive queue shared by every screen. */

export type ToastKind = 'info' | 'success' | 'warn' | 'error' | 'badge';

export interface ToastItem {
  id: string;
  text: string;
  kind: ToastKind;
}

class ToastStore {
  items = $state<ToastItem[]>([]);

  push(text: string, kind: ToastKind = 'info', ms = 4200): string {
    const id = `t_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`;
    this.items = [...this.items, { id, text, kind }];
    if (typeof window !== 'undefined') {
      window.setTimeout(() => this.dismiss(id), ms);
    }
    return id;
  }

  dismiss(id: string): void {
    this.items = this.items.filter((item) => item.id !== id);
  }

  clear(): void {
    this.items = [];
  }
}

export const toasts = new ToastStore();
