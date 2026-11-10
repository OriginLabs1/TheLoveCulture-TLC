'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const { topicForCircle, MIN_CIRCLE_LENGTH } = require('../src/topic')
const {
  FORMAT,
  canonical,
  createLineReader,
  encodeLine,
  fromHandoff,
  recordId,
  validateRecord,
} = require('../src/records')

const HASH = 'a'.repeat(64)

function handoff(overrides = {}) {
  return {
    format: FORMAT,
    handle: 'ada',
    display: 'Ada',
    device: 'test',
    exportedAt: 1_700_000_000_000,
    headHash: HASH,
    entryCount: 2,
    entries: [
      { seq: 1, kind: 'RECOGNITION', hash: HASH, occurredAt: 1_700_000_000_000 },
      { seq: 2, kind: 'TOKEN', hash: 'b'.repeat(64), occurredAt: 1_700_000_100_000 },
    ],
    ...overrides,
  }
}

test('circle topic requires a real shared secret', () => {
  assert.throws(() => topicForCircle('short'), new RegExp(String(MIN_CIRCLE_LENGTH)))
  assert.equal(topicForCircle('  Correct Horse Battery Staple ').length, 32)
  assert.deepEqual(
    topicForCircle('Correct Horse Battery Staple'),
    topicForCircle('correct horse battery staple'),
  )
  assert.notDeepEqual(topicForCircle('Correct Horse Battery Staple'), topicForCircle('Another circle code 123'))
})

test('handoff keeps shape only and gets a stable id', () => {
  const record = fromHandoff(handoff({ note: 'x'.repeat(400) }))
  assert.equal(record.kind, 'tlc.peer.handoff')
  assert.equal(record.entries.length, 2)
  assert.equal(record.note.length, 280)
  assert.deepEqual(Object.keys(record.entries[0]).sort(), ['hash', 'kind', 'occurredAt', 'seq'])
  assert.equal(record.id, recordId(record))
  assert.equal(recordId(record), recordId({ ...record }))
  assert.equal(canonical({ b: 1, a: 2 }), '{"a":2,"b":1}')
})

test('a record with mismatched or unsafe content is rejected', () => {
  const record = fromHandoff(handoff())
  assert.equal(validateRecord(record).id, record.id)
  assert.throws(() => validateRecord({ ...record, id: '0'.repeat(64) }), /does not match/)
  assert.throws(() => validateRecord({ ...record, entries: [{ seq: 'x' }] }), /invalid seq/)
  assert.throws(() => fromHandoff(handoff({ entries: [{ seq: 1, kind: 'X', hash: 'nope', occurredAt: 1 }] })), /invalid hash/)
  assert.throws(() => fromHandoff({ ...handoff(), format: 'something.else' }), /expected format/)
})

test('newline framing survives split chunks and rejects junk', () => {
  const seen = []
  const errors = []
  const feed = createLineReader((value) => seen.push(value), (error) => errors.push(error))
  const line = encodeLine({ type: 'hello' })
  feed(Buffer.from(line.slice(0, 5)))
  feed(Buffer.from(line.slice(5) + encodeLine({ type: 'sync-request' })))
  assert.deepEqual(seen, [{ type: 'hello' }, { type: 'sync-request' }])
  assert.equal(errors.length, 0)
  feed(Buffer.from('not json\n'))
  assert.equal(errors.length, 1)
})

test('store appends once and persists across reopen', async () => {
  const fs = require('node:fs/promises')
  const os = require('node:os')
  const path = require('node:path')
  const { RecordStore } = require('../src/store')
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'tlc-peer-test-'))
  try {
    const record = fromHandoff(handoff())
    const first = await new RecordStore(dir).open()
    assert.equal(await first.append(record), true)
    assert.equal(await first.append(record), false)
    assert.equal(first.length, 1)
    await first.close()

    const second = await new RecordStore(dir).open()
    assert.equal(second.length, 1)
    assert.equal((await second.list())[0].id, record.id)
    await second.close()
  } finally {
    await fs.rm(dir, { recursive: true, force: true })
  }
})
