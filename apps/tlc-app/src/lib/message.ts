/** Helpers for reading Union message bodies without pretending JSON is prose. */

import type { Message } from './types';

export interface ConnectionPayload {
  kind: 'connection_request' | 'connection_accept' | 'connection_decline';
  connectionId: string;
  note?: string;
  scopes?: string[];
  card?: unknown;
}

export function parseBody(message: Message): ConnectionPayload | null {
  if (!message.kind.startsWith('connection_')) return null;
  try {
    const data = JSON.parse(message.body) as ConnectionPayload;
    if (data && typeof data.connectionId === 'string') return data;
  } catch { /* not JSON */ }
  return {
    kind: message.kind as ConnectionPayload['kind'],
    connectionId: '',
    note: ''
  };
}

export function preview(message: Message | undefined): string {
  if (!message) return 'No messages yet';
  if (message.kind === 'connection_request') return 'Connection request';
  if (message.kind === 'connection_accept') return 'Connection accepted';
  if (message.kind === 'connection_decline') return 'Connection declined';
  if (message.kind === 'attachment') return message.attachments.length ? `Attachment · ${message.attachments[0].name}` : 'Attachment';
  if (message.kind === 'system') return message.body;
  return message.body.replace(/\s+/g, ' ').trim() || 'Empty note';
}

export function dayLabel(iso: string): string {
  const date = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const same = (a: Date, b: Date) => a.toDateString() === b.toDateString();
  if (same(date, today)) return 'Today';
  if (same(date, yesterday)) return 'Yesterday';
  return date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function readFile(file: File, maxBytes = 2_000_000): Promise<{ name: string; mime: string; size: number; data: string }> {
  return new Promise((resolve, reject) => {
    if (file.size > maxBytes) {
      reject(new Error(`“${file.name}” is larger than ${fileSize(maxBytes)}.`));
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result ?? '');
      const comma = result.indexOf(',');
      resolve({
        name: file.name,
        mime: file.type || 'application/octet-stream',
        size: file.size,
        data: comma >= 0 ? result.slice(comma + 1) : result
      });
    };
    reader.onerror = () => reject(new Error('Could not read that file.'));
    reader.readAsDataURL(file);
  });
}
