-- Union relay schema (Cloudflare D1 / SQLite).
-- Apply with: npx wrangler d1 execute union-relay --remote --file schema.sql

PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS identities (
  id          TEXT PRIMARY KEY,
  handle      TEXT NOT NULL COLLATE NOCASE,
  card        TEXT NOT NULL,
  created_at  TEXT NOT NULL,
  updated_at  TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS identities_handle ON identities(handle COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS messages (
  id          TEXT PRIMARY KEY,
  from_id     TEXT NOT NULL,
  to_id       TEXT NOT NULL,
  payload     TEXT NOT NULL,
  created_at  TEXT NOT NULL,
  acked_at    TEXT
);
CREATE INDEX IF NOT EXISTS messages_inbox ON messages(to_id, acked_at, created_at);

CREATE TABLE IF NOT EXISTS links (
  id          TEXT PRIMARY KEY,
  owner_id    TEXT NOT NULL,
  kind        TEXT NOT NULL,
  title       TEXT NOT NULL,
  subtitle    TEXT NOT NULL DEFAULT '',
  payload     TEXT NOT NULL,
  permissions TEXT NOT NULL DEFAULT '[]',
  created_at  TEXT NOT NULL,
  expires_at  TEXT,
  revoked_at  TEXT
);
CREATE INDEX IF NOT EXISTS links_owner ON links(owner_id);

CREATE TABLE IF NOT EXISTS spaces (
  id          TEXT PRIMARY KEY,
  owner_id    TEXT NOT NULL,
  name        TEXT NOT NULL,
  about       TEXT NOT NULL DEFAULT '',
  color       TEXT NOT NULL DEFAULT '',
  visibility  TEXT NOT NULL DEFAULT 'public',
  created_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS posts (
  id           TEXT PRIMARY KEY,
  community_id TEXT NOT NULL,
  author_id    TEXT NOT NULL,
  author_name  TEXT NOT NULL DEFAULT '',
  body         TEXT NOT NULL,
  created_at   TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS posts_room ON posts(community_id, created_at);
