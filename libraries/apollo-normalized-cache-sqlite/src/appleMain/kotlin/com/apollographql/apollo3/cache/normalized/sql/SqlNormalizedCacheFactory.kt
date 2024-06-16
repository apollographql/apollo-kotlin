package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo3.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.getSchema
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

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

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(
        recordDatabase = createRecordDatabase(driver)
    )
  }
}
