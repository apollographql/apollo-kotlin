package com.apollographql.apollo.cache.normalized.sql.internal

internal expect class IOTaskExecutor() {
  suspend fun <R> execute(operation: () -> R): R
}
