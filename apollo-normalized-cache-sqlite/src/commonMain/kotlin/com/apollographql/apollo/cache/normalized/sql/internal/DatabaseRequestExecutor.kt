package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.sql.ApolloDatabase

internal expect class DatabaseRequestExecutor(database: ApolloDatabase) {
  suspend fun <R> execute(operation: ApolloDatabase.() -> R): R
}
