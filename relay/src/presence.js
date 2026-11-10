// Presence is a consent record, not an activity guess.
//
// This module is deliberately free of Cloudflare APIs so the rule can be tested with plain Node and
// is reviewed as the single source of truth for the relay. The app mirrors the same four signals and
// four audiences; the relay is the side that actually filters what another person receives.

export const SIGNALS = ['online_now', 'last_seen', 'typing', 'read_receipts'];

export const AUDIENCES = ['NOBODY', 'SELECTED', 'CIRCLE', 'EVERYONE'];

export const PRIVATE_CONSENT = Object.freeze({
  onlineNow: 'NOBODY',
  lastSeen: 'NOBODY',
  typing: 'NOBODY',
  readReceipts: 'NOBODY',
  invisible: false,
  exactLastSeen: false,
  selected: [],
  hidden: [],
});

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

export function normalizeConsent(raw = {}) {
  const audience = (value) => (AUDIENCES.includes(value) ? value : 'NOBODY');
  const handles = (value) =>
    Array.isArray(value)
      ? [...new Set(value.map((item) => String(item).trim().toLowerCase()).filter(Boolean))].slice(0, 500)
      : [];
  return {
    onlineNow: audience(raw.onlineNow),
    lastSeen: audience(raw.lastSeen),
    typing: audience(raw.typing),
    readReceipts: audience(raw.readReceipts),
    invisible: raw.invisible === true,
    exactLastSeen: raw.exactLastSeen === true,
    selected: handles(raw.selected),
    hidden: handles(raw.hidden),
  };
}

export function coarseLastSeen(lastSeen, now) {
  const elapsed = Math.max(0, now - Number(lastSeen || 0));
  if (elapsed < HOUR) return Math.floor(now / HOUR) * HOUR;
  if (elapsed < DAY) return Math.floor(now / DAY) * DAY;
  if (elapsed < 7 * DAY) return Math.floor(now / (7 * DAY)) * (7 * DAY);
  return Math.floor(now / (30 * DAY)) * (30 * DAY);
}

/**
 * @param {object} input
 * @param {object} input.consent normalised consent of the owner
 * @param {string} input.viewer canonical viewer handle
 * @param {Set<string>} input.circle accepted-connection handles
 * @param {boolean} input.online owner has a live stream
 * @param {number} input.lastSeen owner's last accepted heartbeat
 * @param {number} input.now server time
 * @param {boolean} [input.typing] owner is typing to this viewer
 */
export function projectPresence({
  consent,
  viewer,
  circle,
  online,
  lastSeen,
  now,
  typing = false,
  readReceipts = false,
}) {
  const normalized = normalizeConsent(consent);
  const target = String(viewer || '').toLowerCase();
  const visible = (audience) => {
    if (normalized.invisible) return false;
    if (normalized.hidden.includes(target)) return false;
    switch (audience) {
      case 'EVERYONE':
        return true;
      case 'CIRCLE':
        return circle.has(target);
      case 'SELECTED':
        return normalized.selected.includes(target);
      default:
        return false;
    }
  };

  const onlineVisible = visible(normalized.onlineNow);
  const lastVisible = visible(normalized.lastSeen);
  return {
    onlineNow: onlineVisible ? Boolean(online) : null,
    lastSeen: lastVisible
      ? normalized.exactLastSeen
        ? Number(lastSeen || 0)
        : coarseLastSeen(lastSeen, now)
      : null,
    lastSeenCoarse: lastVisible && !normalized.exactLastSeen,
    typing: visible(normalized.typing) ? Boolean(typing) : null,
    readReceipts: visible(normalized.readReceipts) ? Boolean(readReceipts) : null,
  };
}

export function canShareSignal(consent, signal, viewer, circle) {
  const normalized = normalizeConsent(consent);
  const target = String(viewer || '').toLowerCase();
  if (normalized.invisible || normalized.hidden.includes(target)) return false;
  const key = {
    online_now: 'onlineNow',
    last_seen: 'lastSeen',
    typing: 'typing',
    read_receipts: 'readReceipts',
  }[signal];
  if (!key) return false;
  const audience = normalized[key];
  if (audience === 'EVERYONE') return true;
  if (audience === 'CIRCLE') return circle.has(target);
  if (audience === 'SELECTED') return normalized.selected.includes(target);
  return false;
}
