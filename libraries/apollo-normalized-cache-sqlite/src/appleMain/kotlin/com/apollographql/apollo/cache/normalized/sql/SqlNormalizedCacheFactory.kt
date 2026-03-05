@file:Suppress("DEPRECATION")

package com.apollographql.apollo.cache.normalized.sql

import app.cash.sqldelight.db.SqlDriver
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo.cache.normalized.sql.internal.getSchema

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
) : NormalizedCacheFactory() {

  /**
   * @param name the name of the database or null for an in-memory database
   * @param baseDir the baseDirectory where to store the database.
   * [baseDir] must exist and be a directory
   * If [baseDir] is a relative path, it will be interpreted relative to the current working directory
   */
  constructor(name: String?, baseDir: String?) : this(createDriver(name, baseDir, getSchema()))
  actual constructor(name: String?) : this(name, null)
  constructor() : this("apollo.db")

  actual override fun create(): NormalizedCache {
    return SqlNormalizedCache(
        recordDatabase = createRecordDatabase(driver)
    )
  }
}
