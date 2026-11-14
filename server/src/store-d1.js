/** D1 persistence for the Union relay. One prepared statement per operation. */

export function d1Store(db) {
  const one = async (sql, ...args) => db.prepare(sql).bind(...args).first();
  const all = async (sql, ...args) => (await db.prepare(sql).bind(...args).all()).results || [];
  const run = async (sql, ...args) => db.prepare(sql).bind(...args).run();

  return {
    async identityGetById(id) {
      const row = await one('SELECT id, handle, card FROM identities WHERE id = ?', id);
      return row ? { id: row.id, handle: row.handle, card: JSON.parse(row.card) } : null;
    },

    async identityGetByHandle(handle) {
      const row = await one('SELECT id, handle, card FROM identities WHERE handle = ? COLLATE NOCASE', handle);
      return row ? { id: row.id, handle: row.handle, card: JSON.parse(row.card) } : null;
    },

    async identityPut(card, now) {
      await run(
        `INSERT INTO identities (id, handle, card, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET handle = excluded.handle, card = excluded.card, updated_at = excluded.updated_at`,
        card.id,
        card.handle,
        JSON.stringify(card),
        now,
        now
      );
    },

    async messageGet(id) {
      const row = await one('SELECT id FROM messages WHERE id = ?', id);
      return row ? { id: row.id } : null;
    },

    async messagePut(message) {
      await run(
        'INSERT OR IGNORE INTO messages (id, from_id, to_id, payload, created_at) VALUES (?, ?, ?, ?, ?)',
        message.id,
        message.from,
        message.to,
        message.payload,
        message.createdAt
      );
    },

    async messagesFor(id, since) {
      const rows = await all(
        `SELECT id, from_id, to_id, payload, created_at FROM messages
         WHERE to_id = ? AND acked_at IS NULL AND created_at > ?
         ORDER BY created_at ASC LIMIT 200`,
        id,
        since
      );
      return rows.map((row) => ({
        id: row.id,
        from: row.from_id,
        to: row.to_id,
        payload: row.payload,
        createdAt: row.created_at
      }));
    },

    async messagesAck(id, ids) {
      if (!ids.length) return;
      const now = new Date().toISOString();
      for (const messageId of ids) {
        await run('UPDATE messages SET acked_at = ? WHERE id = ? AND to_id = ?', now, messageId, id);
      }
    },

    async linkPut(link) {
      await run(
        `INSERT INTO links (id, owner_id, kind, title, subtitle, payload, permissions, created_at, expires_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           title = excluded.title,
           subtitle = excluded.subtitle,
           payload = excluded.payload,
           permissions = excluded.permissions,
           expires_at = excluded.expires_at
         WHERE links.owner_id = excluded.owner_id`,
        link.id,
        link.ownerId,
        link.kind,
        link.title,
        link.subtitle,
        JSON.stringify(link.payload),
        JSON.stringify(link.permissions),
        link.createdAt,
        link.expiresAt
      );
    },

    async linkGet(id) {
      const row = await one(
        'SELECT id, owner_id, kind, title, subtitle, payload, permissions, created_at, expires_at, revoked_at FROM links WHERE id = ?',
        id
      );
      if (!row) return null;
      return {
        id: row.id,
        ownerId: row.owner_id,
        kind: row.kind,
        title: row.title,
        subtitle: row.subtitle,
        payload: JSON.parse(row.payload),
        permissions: JSON.parse(row.permissions),
        createdAt: row.created_at,
        expiresAt: row.expires_at,
        revokedAt: row.revoked_at
      };
    },

    async linkRevoke(id, ownerId, now) {
      await run('UPDATE links SET revoked_at = ? WHERE id = ? AND owner_id = ?', now, id, ownerId);
    },

    async spacePut(space) {
      await run(
        `INSERT INTO spaces (id, owner_id, name, about, color, visibility, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET name = excluded.name, about = excluded.about, color = excluded.color
         WHERE spaces.owner_id = excluded.owner_id`,
        space.id,
        space.ownerId,
        space.name,
        space.about,
        space.color,
        space.visibility,
        space.createdAt
      );
    },

    async spaceGet(id) {
      const row = await one(
        'SELECT id, owner_id, name, about, color, visibility, created_at FROM spaces WHERE id = ?',
        id
      );
      if (!row) return null;
      return {
        id: row.id,
        ownerId: row.owner_id,
        name: row.name,
        about: row.about,
        color: row.color,
        visibility: row.visibility,
        createdAt: row.created_at
      };
    },

    async postPut(post) {
      await run(
        'INSERT OR IGNORE INTO posts (id, community_id, author_id, author_name, body, created_at) VALUES (?, ?, ?, ?, ?, ?)',
        post.id,
        post.communityId,
        post.authorId,
        post.authorName,
        post.body,
        post.createdAt
      );
    },

    async postsSince(communityId, since) {
      const rows = await all(
        `SELECT id, community_id, author_id, author_name, body, created_at FROM posts
         WHERE community_id = ? AND created_at > ?
         ORDER BY created_at ASC LIMIT 300`,
        communityId,
        since
      );
      return rows.map((row) => ({
        id: row.id,
        communityId: row.community_id,
        authorId: row.author_id,
        authorName: row.author_name,
        body: row.body,
        createdAt: row.created_at
      }));
    },

    /** Retention: acked mail goes after a day, unacked after 30 days. */
    async prune(days = 30) {
      const cutoff = new Date(Date.now() - days * 86400_000).toISOString();
      const ackCutoff = new Date(Date.now() - 86400_000).toISOString();
      await run('DELETE FROM messages WHERE acked_at IS NOT NULL AND acked_at < ?', ackCutoff);
      await run('DELETE FROM messages WHERE created_at < ?', cutoff);
      await run('DELETE FROM links WHERE expires_at IS NOT NULL AND expires_at < ?', cutoff);
    }
  };
}
