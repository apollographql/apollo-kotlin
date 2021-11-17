package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver
) : NormalizedCacheFactory() {

  constructor() : this("apollo.db")

  constructor(name: String) : this(NativeSqliteDriver(ApolloDatabase.Schema, name))

  private val apolloDatabase = ApolloDatabase(driver)

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(
        cacheQueries = apolloDatabase.cacheQueries,
    )
  }
}
