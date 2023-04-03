package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo3.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.getSchema
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
    private val withDates: Boolean,
) : NormalizedCacheFactory() {

  /**
   * @param name the name of the database or null for an in-memory database
   * @param withDates whether to account for dates in the database.
   * @param baseDir the baseDirectory where to store the database.
   * [baseDir] must exist and be a directory
   * If [baseDir] is a relative path, it will be interpreted relative to the current working directory
   */
  constructor(name: String?, withDates: Boolean, baseDir: String?) : this(createDriver(name, baseDir, getSchema(withDates)), withDates)
  actual constructor(name: String?, withDates: Boolean) : this(name, withDates, null)
  constructor(name: String) : this(name, false)
  constructor() : this("apollo.db", false)

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(
        recordDatabase = createRecordDatabase(driver, withDates)
    )
  }
}
