'use strict'

const Hyperswarm = require('hyperswarm')
const { RecordStore } = require('./store')
const { createLineReader, encodeLine } = require('./records')
const { topicForCircle } = require('./topic')

/**
 * A small Pear/Holepunch node for TLC handoffs.
 *
 * Hyperswarm finds peers that know the same circle code; each connection is a direct encrypted
 * stream. Every node keeps its own append-only Hypercore and copies validated handoff records it
 * receives. There is no server, no account and no central directory. The relay stays independent:
 * a circle can work with the Pear node, the relay, or both.
 */
class PeerNode {
  constructor(options) {
    this.dir = options.dir
    this.circle = options.circle
    this.handle = options.handle || 'tlc-peer'
    this.display = options.display || this.handle
    this.log = options.log || (() => {})
    this.store = new RecordStore(this.dir)
    this.swarm = null
    this.discovery = null
    this.sockets = new Set()
    this.received = 0
    this.startedAt = null
  }

  async start() {
    await this.store.open()
    this.topic = topicForCircle(this.circle)
    this.swarm = new Hyperswarm()
    this.swarm.on('connection', (socket, info) => this.onConnection(socket, info))
    this.swarm.on('error', (error) => this.log('swarm', error.message))
    this.discovery = this.swarm.join(this.topic, { server: true, client: true })
    await this.discovery.flushed()
    this.startedAt = Date.now()
    this.log('ready', {
      topic: this.topic.toString('hex'),
      core: this.store.key,
      records: this.store.length,
    })
    return this
  }

  onConnection(socket, info) {
    this.sockets.add(socket)
    const remote = info?.publicKey ? info.publicKey.toString('hex').slice(0, 12) : 'unknown'
    this.log('peer-connected', { remote })
    socket.on('error', (error) => this.log('peer-error', error.message))
    socket.on('close', () => {
      this.sockets.delete(socket)
      this.log('peer-closed', { remote })
    })

    const send = (value) => {
      if (!socket.destroyed) socket.write(encodeLine(value))
    }
    const sendHello = () => send({
      type: 'hello',
      v: 1,
      handle: this.handle,
      display: this.display,
      core: this.store.key,
      at: Date.now(),
    })
    sendHello()

    let chain = Promise.resolve()
    const session = { socket, send, remote, remoteHello: false, sent: false }
    const onLine = createLineReader(
      (message) => {
        chain = chain.then(() => this.onMessage(message, session)).catch((error) => {
          this.log('protocol-error', error.message)
        })
      },
      (error) => this.log('protocol-error', error.message),
    )
    socket.on('data', onLine)
  }

  async onMessage(message, session) {
    const { socket, send, remote } = session
    switch (message?.type) {
      case 'hello':
        if (session.remoteHello) return
        session.remoteHello = true
        this.log('hello', { remote, handle: String(message.handle || '').slice(0, 64) })
        send({ type: 'sync-request' })
        return

      case 'sync-request': {
        if (session.sent) return
        session.sent = true
        const records = await this.store.list()
        for (const record of records) send({ type: 'record', record })
        send({ type: 'sync-done', count: records.length })
        return
      }

      case 'record': {
        const added = await this.store.append(message.record)
        if (added) {
          this.received += 1
          this.log('record-added', {
            id: String(message.record?.id || '').slice(0, 12),
            from: message.record?.from?.handle || '',
          })
        }
        return
      }

      case 'sync-done':
        this.log('sync-done', { remote, count: Number(message.count) || 0 })
        return

      case 'ping':
        send({ type: 'pong', at: Date.now() })
        return

      case 'bye':
        socket.end()
        return

      default:
        this.log('ignored', { type: String(message?.type || '') })
    }
  }

  stats() {
    return {
      topic: this.topic.toString('hex'),
      core: this.store.key,
      records: this.store.length,
      received: this.received,
      connected: this.sockets.size,
      uptimeMs: this.startedAt ? Date.now() - this.startedAt : 0,
    }
  }

  async stop() {
    for (const socket of this.sockets) socket.destroy()
    this.sockets.clear()
    if (this.swarm) await this.swarm.destroy()
    await this.store.close()
    this.swarm = null
  }
}

module.exports = { PeerNode }
