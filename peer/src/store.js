'use strict'

const fs = require('node:fs/promises')
const path = require('node:path')
const Hypercore = require('hypercore')
const { validateRecord } = require('./records')

/**
 * One append-only Hypercore per device. Records are deduplicated by their content id, so
 * re-importing the same handoff or meeting the same peer twice never grows the log twice.
 */
class RecordStore {
  constructor(dir) {
    this.dir = dir
    this.core = null
    this.ids = new Set()
    this.queue = Promise.resolve()
  }

  async open() {
    await fs.mkdir(this.dir, { recursive: true })
    this.core = new Hypercore(path.join(this.dir, 'core'))
    await this.core.ready()
    for (let index = 0; index < this.core.length; index += 1) {
      const record = this.decode(await this.core.get(index))
      if (record) this.ids.add(record.id)
    }
    return this
  }

  decode(block) {
    try {
      return validateRecord(JSON.parse(block.toString('utf8')))
    } catch {
      return null
    }
  }

  get key() {
    return this.core.key.toString('hex')
  }

  get length() {
    return this.core.length
  }

  /** Serialized so two sockets cannot append past each other. */
  append(record) {
    this.queue = this.queue.then(async () => {
      const clean = validateRecord(record)
      if (this.ids.has(clean.id)) return false
      await this.core.append(Buffer.from(JSON.stringify(clean)))
      this.ids.add(clean.id)
      return true
    })
    return this.queue
  }

  async list() {
    const out = []
    for (let index = 0; index < this.core.length; index += 1) {
      const record = this.decode(await this.core.get(index))
      if (record) out.push(record)
    }
    return out
  }

  async close() {
    await this.queue
    if (this.core) await this.core.close()
    this.core = null
  }
}

module.exports = { RecordStore }
