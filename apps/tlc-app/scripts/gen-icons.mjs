#!/usr/bin/env node
/** Generate Union's PWA icons without external image libraries.
 *  A small procedural renderer keeps the repository reproducible on a machine
 *  with no ImageMagick, Pillow or native canvas. */

import { deflateSync } from 'node:zlib';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(new URL('.', import.meta.url)));
const outDir = join(root, 'public/icons');
mkdirSync(outDir, { recursive: true });

const BG = [8, 9, 13];
const ACCENT = [124, 131, 255];
const TEAL = [45, 212, 191];

const clamp01 = (n) => Math.min(1, Math.max(0, n));
const smooth = (edge0, edge1, x) => {
  const t = clamp01((x - edge0) / (edge1 - edge0));
  return t * t * (3 - 2 * t);
};
const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));

/** Signed distance to a ring: negative inside the band. */
function ring(x, y, cx, cy, radius, width) {
  const d = Math.hypot(x - cx, y - cy);
  return Math.abs(d - radius) - width / 2;
}

function render(size) {
  const ss = 3;
  const pixels = Buffer.alloc(size * size * 4);
  for (let py = 0; py < size; py++) {
    for (let px = 0; px < size; px++) {
      let r = 0, g = 0, b = 0, a = 0;
      for (let sy = 0; sy < ss; sy++) {
        for (let sx = 0; sx < ss; sx++) {
          const x = (px + (sx + 0.5) / ss) / size;
          const y = (py + (sy + 0.5) / ss) / size;

          // Background: a full square with a very soft lifted centre.
          const cx = Math.hypot(x - 0.46, y - 0.38);
          let col = mix(BG, [18, 21, 32], clamp01(1 - cx * 1.5) * 0.75);
          let alpha = 1;

          // Two interlocking rings: one violet, one teal. The union is the mark.
          const left = ring(x, y, 0.407, 0.5, 0.215, 0.070);
          const right = ring(x, y, 0.593, 0.5, 0.215, 0.070);
          const leftCover = 1 - smooth(-0.006, 0.006, left);
          const rightCover = 1 - smooth(-0.006, 0.006, right);
          col = mix(col, ACCENT, leftCover);
          col = mix(col, TEAL, rightCover * (1 - leftCover * 0.15));

          // A small central question-mark dot keeps the mark legible at 32px.
          const dot = Math.hypot(x - 0.5, y - 0.5) - 0.027;
          col = mix(col, [242, 243, 247], 1 - smooth(-0.004, 0.004, dot));

          r += col[0]; g += col[1]; b += col[2]; a += alpha;
        }
      }
      const n = ss * ss;
      const i = (py * size + px) * 4;
      pixels[i] = Math.round(r / n);
      pixels[i + 1] = Math.round(g / n);
      pixels[i + 2] = Math.round(b / n);
      pixels[i + 3] = Math.round((a / n) * 255);
    }
  }
  return pixels;
}

/* ------------------------------------------------------------------- PNG */

const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  crcTable[n] = c >>> 0;
}
function crc32(buf) {
  let c = 0xffffffff;
  for (const byte of buf) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const typeBuf = Buffer.from(type, 'ascii');
  const body = Buffer.concat([typeBuf, data]);
  const out = Buffer.alloc(12 + data.length);
  out.writeUInt32BE(data.length, 0);
  body.copy(out, 4);
  out.writeUInt32BE(crc32(body), 8 + data.length);
  return out;
}
function png(size, pixels) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // RGBA
  const raw = Buffer.alloc(size * (size * 4 + 1));
  for (let y = 0; y < size; y++) {
    raw[y * (size * 4 + 1)] = 0;
    pixels.copy(raw, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

const outputs = [
  [192, 'icon-192.png'],
  [512, 'icon-512.png'],
  [180, 'apple-touch-icon.png']
];
for (const [size, name] of outputs) {
  const file = join(outDir, name);
  writeFileSync(file, png(size, render(size)));
  console.log(`icons: wrote ${name} (${size}×${size})`);
}

const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" role="img" aria-label="Union">
  <rect width="64" height="64" rx="14" fill="#08090d"/>
  <circle cx="26" cy="32" r="13.4" fill="none" stroke="#7c83ff" stroke-width="4.4"/>
  <circle cx="38" cy="32" r="13.4" fill="none" stroke="#2dd4bf" stroke-width="4.4"/>
  <circle cx="32" cy="32" r="1.8" fill="#f2f3f7"/>
</svg>`;
writeFileSync(join(outDir, 'favicon.svg'), svg);
console.log('icons: wrote favicon.svg');
