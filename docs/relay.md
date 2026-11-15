# Running the optional relay

Union works without a server. The relay adds four conveniences:

1. handle lookup by signed card,
2. store-and-forward delivery,
3. shared public rooms,
4. resolvable `https://relay.example/l/<id>` links.

It cannot read message bodies. It verifies every write with Ed25519 and stores ciphertext.

## A live deployment

Union's own relay is deployed at **https://union-relay.access1tutamailcom.workers.dev**.
The Worker also declares the custom domain `https://union-relay.cculture.net`; use the
`workers.dev` address until that hostname resolves directly. It runs with D1 database
`union-relay` (`5994448e-1e86-4c38-8867-d11189898670`) and allows these browser origins:

```
https://union.cculture.net
https://union-app.cculture.net
https://union-app-9ni.pages.dev
```

It is optional: the app is complete offline, and a self-hosted relay works the same way.
Requests from any other origin receive no `access-control-allow-origin` header, so a
browser refuses them; a same-origin or server-to-server caller is unaffected.

## What you need

- A Cloudflare account (the free plan is enough for personal use).
- Node 20+ and `npx`.
- `wrangler` (invoked with `npx wrangler@4`; no global install needed).

## Deploy

```sh
cd server
npx wrangler@4 login                 # or set CLOUDFLARE_API_TOKEN

# Create the database and copy its id into wrangler.toml
npx wrangler@4 d1 create union-relay

# Apply the schema
npx wrangler@4 d1 execute union-relay --remote --file schema.sql

# Set your Pages origin instead of "*"
#   [vars]
#   ALLOWED_ORIGINS = "https://union.cculture.net,https://union-app.cculture.net"

npx wrangler@4 deploy
```

The deploy prints a URL such as `https://union-relay.<account>.workers.dev`. Paste it into **Operations → Relay** in Union, then tap **Check**.

`wrangler.toml` also declares a `union-relay.cculture.net` custom domain. If you fork
this for your own zone, replace that route (or remove it and use the `workers.dev`
URL) and set `ALLOWED_ORIGINS` to your Pages origin.

For CI, set `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` and run the same commands.

## Local development

```sh
cd server
npx wrangler@4 d1 create union-relay --local   # creates the local D1 binding
npx wrangler@4 d1 execute union-relay --local --file schema.sql
npx wrangler@4 dev
```

`wrangler dev` serves on `http://127.0.0.1:8787`. Union accepts an `http://` relay URL for local development; use `https://` for anything reachable from the internet.

## Tests

The relay handler is tested against an in-memory store, with no Cloudflare account:

```sh
npm run test:relay
```

The suite covers card signature and key/id binding, replay-window rejection, tampered bodies, handle uniqueness, delivery/acknowledgement, link ownership and revocation, public/private room posting, and refusal to echo a disallowed origin.

## API

All responses are JSON. Writes require three headers:

```
x-union-id:  did:union:…
x-union-ts:  unix seconds (±300 s)
x-union-sig: base64 Ed25519 signature
```

The signed message is exactly:

```
METHOD \n path-with-query \n timestamp \n sha256(request-body-text)
```

`path` includes the query string, e.g. `/v1/messages?since=1970-01-01T00%3A00%3A00.000Z`. The signature is produced by the identity's Ed25519 private key; the relay looks up the registered card and verifies it.

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/health` | — | liveness |
| `POST` | `/v1/identities` | card signature | register or refresh a signed card |
| `GET` | `/v1/identities/:handle` | — | resolve a handle to a card |
| `POST` | `/v1/messages` | signed | store a sealed envelope |
| `GET` | `/v1/messages?since=…` | signed | fetch unacknowledged envelopes |
| `POST` | `/v1/messages/ack` | signed | delete delivered envelopes |
| `POST` | `/v1/links` | signed | publish a link |
| `GET` | `/l/:id` | — | resolve a link |
| `POST` | `/v1/links/:id/revoke` | signed | revoke your link |
| `POST` | `/v1/spaces` | signed | register a room |
| `GET` | `/v1/spaces/:id` | — | read a room |
| `POST` | `/v1/spaces/:id/posts` | signed | post to a public room |
| `GET` | `/v1/spaces/:id/posts?since=…` | — | read public posts |

### Limits

| Item | Limit |
| --- | --- |
| Request body | 96 KB |
| Card | 8 KB |
| Message payload | 64 KB |
| Link payload | 32 KB |
| Post body | 8 KB |
| Signature clock skew | ±300 s |
| Per-IP writes / reads | 120 / 600 per minute (best effort, per isolate) |
| Message retention | 30 days unacknowledged, 1 day after acknowledgement |

Rate limiting is deliberately described as best effort: it runs in a Worker isolate and is not a billing-grade quota. Add Cloudflare WAF rules or Turnstile if you need stronger protection.

## Data model

```sql
identities(id, handle UNIQUE COLLATE NOCASE, card, created_at, updated_at)
messages(id, from_id, to_id, payload, created_at, acked_at)
links(id, owner_id, kind, title, subtitle, payload, permissions, created_at, expires_at, revoked_at)
spaces(id, owner_id, name, about, color, visibility, created_at)
posts(id, community_id, author_id, author_name, body, created_at)
```

The full schema is in [`schema.sql`](../server/schema.sql). Apply it before the first deploy; migrations are additive in v0.0.1.

## Operating honestly

- Publish what the relay can see. Do not describe it as anonymous.
- Keep `ALLOWED_ORIGINS` narrow in production.
- Back up D1 if you care about the data; Union never treats the relay as the source of truth.
- A relay can drop messages. Clients retry from the outbox, but a hostile relay can deny service.
- Do not put the Telegram bot token, any Creator WhatsApp number, or any user's proof in this repository.

## The Telegram assistant is separate

[@Oneorigin_bot](https://t.me/Oneorigin_bot) is the public assistant that receives activation proofs and reports to the Creator. Its bot token belongs in a secret store (for example `wrangler secret put TELEGRAM_BOT_TOKEN` on a separate Worker), never in git. v0.0.1 packages the proof inside Union and opens the bot with the package ready to send; it does not ship a bot token.
