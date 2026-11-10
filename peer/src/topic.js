'use strict'

const { createHash } = require('node:crypto')

/**
 * A circle code is a shared secret, never a username. Anyone who knows it can derive the
 * Hyperswarm topic and therefore find the other peers; nobody else can. The code never leaves
 * this process and is never written to the Hypercore.
 */
const MIN_CIRCLE_LENGTH = 12

function normalizeCircle(value) {
  return String(value || '').trim().toLowerCase()
}

function topicForCircle(circle) {
  const clean = normalizeCircle(circle)
  if (clean.length < MIN_CIRCLE_LENGTH) {
    throw new Error(`circle code must be at least ${MIN_CIRCLE_LENGTH} characters`)
  }
  return createHash('sha256').update(`tlc-peer-v1|${clean}`).digest()
}

module.exports = { MIN_CIRCLE_LENGTH, normalizeCircle, topicForCircle }
