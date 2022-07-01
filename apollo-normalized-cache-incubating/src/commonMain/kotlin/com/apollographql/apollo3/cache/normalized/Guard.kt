package com.apollographql.apollo3.cache.normalized.internal

internal expect class Guard<R: Any>(name: String, producer: () -> R) {
  /**
   * Accesses the underlying resource with 'read' semantics. Several threads can read the resource simultaneously.
   */
  suspend fun <T> readAccess(block: (R) -> T): T

  /**
   * Accesses the underlying resource with 'write' semantics. Only one thread can write to a resource at a time. During that time,
   * no readers can access the resource.
   */
  suspend fun <T> writeAccess(block: (R) -> T): T

  /**
   * Accesses the underlying resouce with 'write' semantics and does not wait for the response. The work might not be finished
   * when this function returns
   */
  fun writeAndForget(block: (R) -> Unit)

  fun dispose()
}