package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver,
    private val exceptionHandler: (Throwable) -> Unit,
) : NormalizedCacheFactory() {

  constructor() : this("apollo.db")

  constructor(name: String) : this(NativeSqliteDriver(ApolloDatabase.Schema, name), exceptionHandler = DEFAULT_EXCEPTION_HANDLER)

  constructor(name: String, exceptionHandler: (Throwable) -> Unit) : this(NativeSqliteDriver(ApolloDatabase.Schema, name), exceptionHandler)

  private val apolloDatabase = ApolloDatabase(driver)

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(
        cacheQueries = apolloDatabase.cacheQueries,
        exceptionHandler = exceptionHandler,
    )
  }
}
