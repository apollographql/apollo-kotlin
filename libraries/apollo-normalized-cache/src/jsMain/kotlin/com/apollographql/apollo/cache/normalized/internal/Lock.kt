package com.apollographql.apollo.cache.normalized.internal

internal actual class Lock {
  actual fun <T> read(block: () -> T): T {
    return block()
  }

  actual fun <T> write(block: () -> T): T {
    return block()
  }
}
