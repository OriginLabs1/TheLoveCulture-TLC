#!/usr/bin/env node
'use strict'

const fs = require('node:fs/promises')
const os = require('node:os')
const path = require('node:path')
const { PeerNode } = require('./node')
const { RecordStore } = require('./store')
const { fromHandoff } = require('./records')
const { topicForCircle, MIN_CIRCLE_LENGTH } = require('./topic')

function parse(argv) {
  const flags = {}
  const rest = []
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index]
    if (!token.startsWith('--')) {
      rest.push(token)
      continue
    }
    const [name, inline] = token.slice(2).split('=', 2)
    if (inline !== undefined) {
      flags[name] = inline
    } else if (argv[index + 1] && !argv[index + 1].startsWith('--')) {
      flags[name] = argv[index + 1]
      index += 1
    } else {
      flags[name] = true
    }
  }
  return { flags, rest }
}

function requireCircle(flags) {
  const circle = String(flags.circle || process.env.TLC_PEER_CIRCLE || '')
  if (circle.trim().length < MIN_CIRCLE_LENGTH) {
    throw new Error(`pass --circle with at least ${MIN_CIRCLE_LENGTH} characters (or TLC_PEER_CIRCLE)`)
  }
  return circle
}

function peerDir(flags) {
  return path.resolve(String(flags.dir || process.env.TLC_PEER_DIR || path.join(os.homedir(), '.tlc-peer')))
}

function logger(quiet) {
  return (event, detail) => {
    if (quiet) return
    const extra = detail && typeof detail === 'object' ? ` ${JSON.stringify(detail)}` : detail ? ` ${detail}` : ''
    process.stderr.write(`[tlc-peer] ${event}${extra}\n`)
  }
}

async function host(flags, { seconds = 0 } = {}) {
  const circle = requireCircle(flags)
  const dir = peerDir(flags)
  const log = logger(Boolean(flags.quiet))
  const node = new PeerNode({
    dir,
    circle,
    handle: String(flags.handle || os.hostname()),
    display: String(flags.display || flags.handle || 'TLC peer'),
    log,
  })
  await node.start()
  process.stdout.write(`${JSON.stringify(node.stats())}\n`)
  const shutdown = async () => {
    await node.stop()
    process.exit(0)
  }
  process.on('SIGINT', shutdown)
  process.on('SIGTERM', shutdown)
  if (seconds > 0) {
    setTimeout(async () => {
      process.stdout.write(`${JSON.stringify(node.stats())}\n`)
      await node.stop()
      process.exit(0)
    }, seconds * 1000)
  }
  return node
}

async function main() {
  const { flags, rest } = parse(process.argv.slice(2))
  const command = rest[0] || 'host'

  if (command === 'topic') {
    process.stdout.write(`${topicForCircle(requireCircle(flags)).toString('hex')}\n`)
    return
  }

  if (command === 'list') {
    const store = await new RecordStore(peerDir(flags)).open()
    const records = await store.list()
    process.stdout.write(`${JSON.stringify({ dir: peerDir(flags), count: records.length, records }, null, 2)}\n`)
    await store.close()
    return
  }

  if (command === 'import') {
    const file = rest[1]
    if (!file) throw new Error('usage: tlc-peer import <handoff.json> --circle <code>')
    const parsed = JSON.parse(await fs.readFile(path.resolve(file), 'utf8'))
    const record = fromHandoff(parsed)
    const dir = peerDir(flags)
    const store = await new RecordStore(dir).open()
    const added = await store.append(record)
    process.stdout.write(`${JSON.stringify({ dir, id: record.id, added, records: store.length })}\n`)
    await store.close()
    if (flags['no-host']) return
    await host(flags, { seconds: Number(flags.seconds) || 30 })
    return
  }

  if (command === 'host' || command === 'once') {
    await host(flags, {
      seconds: command === 'once' ? Number(flags.seconds) || 20 : Number(flags.seconds) || 0,
    })
    return
  }

  throw new Error(`unknown command: ${command}`)
}

if (require.main === module) {
  main().catch((error) => {
    process.stderr.write(`tlc-peer: ${error.message}\n`)
    process.exit(1)
  })
}

module.exports = { parse, main }
