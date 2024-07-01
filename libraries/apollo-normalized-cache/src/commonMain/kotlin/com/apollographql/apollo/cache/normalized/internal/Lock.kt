package com.apollographql.apollo.cache.normalized.internal

import kotlinx.atomicfu.locks.ReentrantLock

/**
 * A lock with read/write semantics where possible.
 *
 * - uses Java's `ReentrantReadWriteLock` on the JVM
 * - uses AtomicFu's [ReentrantLock] on Native (read and write are not distinguished)
 */
internal expect class Lock() {
  fun <T> read(block: () -> T): T
  fun <T> write(block: () -> T): T
}
