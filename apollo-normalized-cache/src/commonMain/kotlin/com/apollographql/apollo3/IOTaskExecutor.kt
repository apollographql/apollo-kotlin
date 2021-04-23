package com.apollographql.apollo3.cache.normalized.sql.internal

expect class IOTaskExecutor(name: String) {
  suspend fun <R> execute(operation: () -> R): R
  fun executeAndForget(operation: () -> Unit)
}
