package com.apollographql.apollo3.mpp

actual class Guard<R: Any> actual constructor(name: String, private val producer: () -> R) {
  private val worker = SingleThreadWorker(producer = producer)

  actual fun dispose() {
    worker.dispose()
  }

  actual suspend fun <T> readAccess(block: (R) -> T) = worker.execute(block)

  actual suspend fun <T> writeAccess(block: (R) -> T) = worker.execute(block)

  actual fun writeAndForget(block: (R) -> Unit) = worker.executeAndForget(block)
}