package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.mpp.SingleThreadWorker
import kotlinx.coroutines.runBlocking

actual class Guard<R: Any> actual constructor(name: String, private val producer: () -> R) {
  private val worker = SingleThreadWorker(producer = producer)

  actual fun dispose() {
    worker.dispose()
  }

  actual suspend fun <T> access(block: (R) -> T) = worker.execute(block)

  actual fun writeAndForget(block: (R) -> Unit) = worker.executeAndForget(block)

  actual fun <T> blockingAccess(block: (R) -> T): T {
    return runBlocking {
      access(block)
    }
  }
}
