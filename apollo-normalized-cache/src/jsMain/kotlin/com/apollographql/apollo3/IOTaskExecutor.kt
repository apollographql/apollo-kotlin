package com.apollographql.apollo3.cache.normalized.sql.internal

actual class IOTaskExecutor actual constructor(name: String) {
  actual suspend fun <R> execute(operation: () -> R): R {
    return operation()
  }

  actual fun executeAndForget(operation: () -> Unit) {
    operation()
  }
}