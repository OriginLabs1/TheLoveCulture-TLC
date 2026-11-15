/** QR rendering. Uses the maintained `qrcode-generator` implementation and
 *  returns a crisp SVG path so the code stays sharp at any size and works in
 *  dark mode without a raster image. */

import * as qrcodeFactory from 'qrcode-generator';

type QrFactory = (typeNumber: 0, level: 'L' | 'M' | 'Q' | 'H') => {
  addData(data: string): void;
  make(): void;
  getModuleCount(): number;
  isDark(row: number, col: number): boolean;
};

const make = qrcodeFactory as unknown as QrFactory;

export interface QrCode {
  count: number;
  /** SVG path in a 0..count coordinate space */
  path: string;
  /** quiet zone in modules */
  margin: number;
}

export function qrCode(text: string, level: 'L' | 'M' | 'Q' | 'H' = 'M'): QrCode | null {
  if (!text || text.length > 2400) return null;
  try {
    const qr = make(0, level);
    qr.addData(text);
    qr.make();
    const count = qr.getModuleCount();
    const parts: string[] = [];
    for (let row = 0; row < count; row++) {
      for (let col = 0; col < count; col++) {
        if (qr.isDark(row, col)) parts.push(`M${col} ${row}h1v1h-1z`);
      }
    }
    return { count, path: parts.join(''), margin: 2 };
  } catch {
    return null;
  }
}
