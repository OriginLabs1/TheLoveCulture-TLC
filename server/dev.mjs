/**
 * Local relay for development: the real Worker handler over node:http,
 * backed by memory. Nothing is persisted; restart wipes it.
 *
 *   node server/dev.mjs          # http://127.0.0.1:8787
 *   PORT=9000 node server/dev.mjs
 */

import http from 'node:http';
import { createApp, appDefaults } from './src/app.js';
import { memoryStore } from './src/store-memory.js';

const port = Number(process.env.PORT || 8787);
const app = createApp({
  ...appDefaults,
  store: memoryStore(),
  allowedOrigins: process.env.ALLOWED_ORIGINS || '*'
});

const server = http.createServer(async (request, response) => {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  const body = Buffer.concat(chunks);

  const url = `http://127.0.0.1:${port}${request.url || '/'}`;
  const headers = new Headers();
  for (const [key, value] of Object.entries(request.headers)) {
    if (Array.isArray(value)) value.forEach((item) => headers.append(key, item));
    else if (value !== undefined) headers.set(key, value);
  }
  const hasBody = request.method !== 'GET' && request.method !== 'HEAD';
  const full = new Request(url, {
    method: request.method,
    headers,
    body: hasBody ? body : undefined
  });

  try {
    const result = await app(full);
    response.writeHead(result.status, Object.fromEntries(result.headers));
    response.end(Buffer.from(await result.arrayBuffer()));
  } catch (error) {
    console.error(error);
    response.writeHead(500, { 'content-type': 'application/json' });
    response.end(JSON.stringify({ error: 'Local relay failed.' }));
  }
});

server.listen(port, '127.0.0.1', () => {
  console.log(`Union relay (local, in-memory) on http://127.0.0.1:${port}`);
});
