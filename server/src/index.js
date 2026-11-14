import { createApp, appDefaults } from './app.js';
import { d1Store } from './store-d1.js';

/**
 * Cloudflare Worker entry.
 *
 * Bindings (see wrangler.toml):
 *   DB               D1 database
 *   ALLOWED_ORIGINS  optional comma-separated CORS origins (defaults to *)
 */
export default {
  async fetch(request, env) {
    if (!env.DB) {
      return new Response(JSON.stringify({ error: 'The relay has no database binding.' }), {
        status: 503,
        headers: { 'content-type': 'application/json' }
      });
    }
    const app = createApp({
      ...appDefaults,
      store: d1Store(env.DB),
      allowedOrigins: env.ALLOWED_ORIGINS || '*'
    });
    return app(request);
  },

  async scheduled(_event, env, ctx) {
    if (!env.DB) return;
    ctx.waitUntil(d1Store(env.DB).prune(30));
  }
};
