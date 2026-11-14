/** In-memory version of the relay store, used by the test suite. */

export function memoryStore() {
  const identities = new Map();
  const messages = new Map();
  const links = new Map();
  const spaces = new Map();
  const posts = new Map();

  const clone = (value) => JSON.parse(JSON.stringify(value));

  return {
    async identityGetById(id) {
      const found = identities.get(id);
      return found ? { id, handle: found.handle, card: clone(found.card) } : null;
    },
    async identityGetByHandle(handle) {
      for (const [id, value] of identities) {
        if (value.handle.toLowerCase() === handle.toLowerCase()) {
          return { id, handle: value.handle, card: clone(value.card) };
        }
      }
      return null;
    },
    async identityPut(card) {
      identities.set(card.id, { handle: card.handle, card: clone(card) });
    },

    async messageGet(id) {
      const found = messages.get(id);
      return found ? { id } : null;
    },
    async messagePut(message) {
      if (!messages.has(message.id)) messages.set(message.id, { ...message, ackedAt: null });
    },
    async messagesFor(id, since) {
      return [...messages.values()]
        .filter((m) => m.to === id && !m.ackedAt && m.createdAt > since)
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
        .slice(0, 200)
        .map(({ ackedAt, ...m }) => clone(m));
    },
    async messagesAck(id, ids) {
      const now = new Date().toISOString();
      for (const messageId of ids) {
        const found = messages.get(messageId);
        if (found && found.to === id) found.ackedAt = now;
      }
    },

    async linkPut(link) {
      links.set(link.id, { ...clone(link), revokedAt: null });
    },
    async linkGet(id) {
      const found = links.get(id);
      return found ? clone(found) : null;
    },
    async linkRevoke(id, ownerId, now) {
      const found = links.get(id);
      if (found && found.ownerId === ownerId) found.revokedAt = now;
    },

    async spacePut(space) {
      spaces.set(space.id, clone(space));
    },
    async spaceGet(id) {
      const found = spaces.get(id);
      return found ? clone(found) : null;
    },

    async postPut(post) {
      if (!posts.has(post.id)) posts.set(post.id, clone(post));
    },
    async postsSince(communityId, since) {
      return [...posts.values()]
        .filter((p) => p.communityId === communityId && p.createdAt > since)
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
        .map(clone);
    },

    async prune() {
      return undefined;
    }
  };
}
