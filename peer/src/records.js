'use strict'

const { createHash } = require('node:crypto')

const FORMAT = 'tlc.peer.handoff.v1'
const RECORD_KIND = 'tlc.peer.handoff'
const MAX_LINE_BYTES = 1024 * 1024
const MAX_ENTRIES = 5000
const HEX64 = /^[0-9a-f]{64}$/i

/** Stable ordering makes the id reproducible on every peer. */
function canonical(value) {
  if (Array.isArray(value)) return `[${value.map(canonical).join(',')}]`
  if (value && typeof value === 'object') {
    return `{${Object.keys(value)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${canonical(value[key])}`)
      .join(',')}}`
  }
  return JSON.stringify(value)
}

function recordId(record) {
  const { id, ...body } = record
  return createHash('sha256').update(canonical(body)).digest('hex')
}

function cleanString(value, max) {
  return String(value ?? '').trim().slice(0, max)
}

function cleanEntry(entry, index) {
  if (!entry || typeof entry !== 'object') {
    throw new Error(`entry ${index} is not an object`)
  }
  const seq = Number(entry.seq)
  const occurredAt = Number(entry.occurredAt)
  const hash = cleanString(entry.hash, 128)
  if (!Number.isInteger(seq) || seq < 0) throw new Error(`entry ${index} has an invalid seq`)
  if (!Number.isFinite(occurredAt) || occurredAt < 0) {
    throw new Error(`entry ${index} has an invalid time`)
  }
  if (!HEX64.test(hash)) throw new Error(`entry ${index} has an invalid hash`)
  return {
    seq,
    kind: cleanString(entry.kind, 48) || 'unknown',
    hash,
    occurredAt,
  }
}

/**
 * Turn the JSON the Android app exports into a stored record.
 *
 * Only the shape of the record crosses the wire: sequence, kind, hash and time. Subjects, notes,
 * photos, receipts, vault references and contact details are deliberately absent.
 */
function fromHandoff(input) {
  if (!input || typeof input !== 'object') throw new Error('handoff must be an object')
  if (input.format !== FORMAT) throw new Error(`expected format ${FORMAT}`)
  const entries = Array.isArray(input.entries) ? input.entries : []
  if (entries.length > MAX_ENTRIES) throw new Error('handoff has too many entries')
  const body = {
    v: 1,
    kind: RECORD_KIND,
    from: {
      handle: cleanString(input.handle, 64),
      display: cleanString(input.display, 80),
      device: cleanString(input.device, 80),
    },
    exportedAt: Number(input.exportedAt) || Date.now(),
    headHash: cleanString(input.headHash, 128),
    entryCount: Number(input.entryCount) || entries.length,
    entries: entries.map(cleanEntry),
    note: cleanString(input.note, 280),
  }
  return { ...body, id: recordId(body) }
}

function validateRecord(input) {
  if (!input || typeof input !== 'object') throw new Error('record must be an object')
  if (input.kind !== RECORD_KIND || input.v !== 1) throw new Error('unknown record kind')
  if (!HEX64.test(String(input.id || ''))) throw new Error('record id is not a sha-256 hex string')
  if (!Array.isArray(input.entries) || input.entries.length > MAX_ENTRIES) {
    throw new Error('record entries are invalid')
  }
  input.entries.forEach((entry, index) => {
    if (!entry || typeof entry !== 'object') throw new Error(`entry ${index} is not an object`)
    if (!Number.isInteger(Number(entry.seq)) || Number(entry.seq) < 0) {
      throw new Error(`entry ${index} has an invalid seq`)
    }
    if (!Number.isFinite(Number(entry.occurredAt)) || Number(entry.occurredAt) < 0) {
      throw new Error(`entry ${index} has an invalid time`)
    }
    if (!HEX64.test(String(entry.hash || ''))) throw new Error(`entry ${index} has an invalid hash`)
  })
  if (!input.from || typeof input.from !== 'object') throw new Error('record sender is invalid')
  const expected = recordId(input)
  if (expected !== input.id) throw new Error('record id does not match its content')
  return input
}

function encodeLine(value) {
  const line = JSON.stringify(value)
  if (Buffer.byteLength(line) > MAX_LINE_BYTES) throw new Error('line is too large')
  return `${line}\n`
}

/** Newline framing for a Hyperswarm duplex stream. */
function createLineReader(onValue, onError) {
  let buffer = ''
  return (chunk) => {
    buffer += chunk.toString('utf8')
    if (Buffer.byteLength(buffer) > MAX_LINE_BYTES) {
      buffer = ''
      onError(new Error('incoming line is too large'))
      return
    }
    let index = buffer.indexOf('\n')
    while (index >= 0) {
      const line = buffer.slice(0, index).trim()
      buffer = buffer.slice(index + 1)
      if (line) {
        try {
          onValue(JSON.parse(line))
        } catch (error) {
          onError(error)
        }
      }
      index = buffer.indexOf('\n')
    }
  }
}

module.exports = {
  FORMAT,
  RECORD_KIND,
  MAX_ENTRIES,
  MAX_LINE_BYTES,
  canonical,
  recordId,
  fromHandoff,
  validateRecord,
  encodeLine,
  createLineReader,
}
