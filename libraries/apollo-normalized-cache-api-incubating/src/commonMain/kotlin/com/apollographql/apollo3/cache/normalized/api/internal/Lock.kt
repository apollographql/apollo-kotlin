package com.apollographql.apollo3.cache.normalized.api.internal

import com.apollographql.apollo3.annotations.ApolloInternal

/**
 * A lock with read/write semantics where possible.
 *
 * - uses Java's `ReentrantReadWriteLock` on the JVM
 * - uses AtomicFu's [ReentrantLock] on Native (read and write are not distinguished)
 */
@ApolloInternal
expect class Lock() {
  fun <T> read(block: () -> T): T
  fun <T> write(block: () -> T): T
}