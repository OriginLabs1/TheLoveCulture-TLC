import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

const CSP = [
  "default-src 'self'",
  "script-src 'self' 'wasm-unsafe-eval'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self'",
  "media-src 'self' blob:",
  "connect-src 'self' https: http://127.0.0.1:* http://localhost:*",
  "object-src 'none'",
  "base-uri 'none'",
  "form-action 'none'"
].join('; ');

export default defineConfig({
  base: './',
  plugins: [
    svelte(),
    {
      name: 'union-csp',
      apply: 'build',
      transformIndexHtml(html) {
        return html.replace(
          '<!--CSP-->',
          `<meta http-equiv="Content-Security-Policy" content="${CSP}">`
        );
      }
    }
  ],
  build: {
    target: 'es2020',
    assetsInlineLimit: 4096,
    sourcemap: false,
    reportCompressedSize: false
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true
  },
  preview: {
    host: '127.0.0.1',
    port: 4173,
    strictPort: true
  }
});
