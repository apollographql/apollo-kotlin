package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.sql.ApolloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Performs database operation on background thread via [Dispatchers.IO].
 */
internal actual class DatabaseRequestExecutor actual constructor(
    private val database: ApolloDatabase
) {

  actual suspend fun <R> execute(operation: ApolloDatabase.() -> R): R {
    return withContext(Dispatchers.IO) {
      operation(database)
    }
  }
}
