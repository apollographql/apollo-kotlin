package com.apollographql.apollo3.cache.normalized.internal

internal actual class Guard<R: Any> actual constructor(name: String, producer: () -> R) {
  private val resource = producer()

  actual suspend fun <T> readAccess(block: (R) -> T): T {
    return block(resource)
  }

  actual suspend fun <T> writeAccess(block: (R) -> T): T {
    return block(resource)
  }

  actual fun writeAndForget(block: (R) -> Unit) {
      block(resource)
  }

  actual fun dispose() {
  }
}