package com.apollographql.apollo3.cache.normalized.sql.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Performs IO operation on background thread via [Dispatchers.IO].
 */
actual class IOTaskExecutor actual constructor(name: String){
  actual suspend fun <R> execute(operation: () -> R): R {
    return withContext(Dispatchers.IO) {
      operation()
    }
  }

  actual fun executeAndForget(operation: () -> Unit) {

  }
}
