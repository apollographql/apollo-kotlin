package com.apollographql.apollo3.cache.normalized.api.internal

/**
 * A lock with read/write semantics where possible.
 *
 * - uses Java's `ReentrantReadWriteLock` on the JVM
 * - uses AtomicFu's [ReentrantLock] on Native (read and write are not distinguished)
 */
actual class Lock actual constructor() {
  actual fun <T> read(block: () -> T): T {
    return block()
  }

  actual fun <T> write(block: () -> T): T {
    return block()
  }
}