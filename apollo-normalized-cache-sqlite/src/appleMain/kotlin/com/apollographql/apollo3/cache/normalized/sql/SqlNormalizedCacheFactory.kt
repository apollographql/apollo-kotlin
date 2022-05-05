package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver,
    withAge: Boolean,
) : NormalizedCacheFactory() {

  constructor(name: String, withAge: Boolean) : this(NativeSqliteDriver(getSchema(withAge), name), withAge)

  constructor(name: String) : this(name, false)

  constructor() : this("apollo.db", false)

  private val driver = driver
  private val withAge = withAge

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(
        recordDatabase = createRecordDatabase(driver, withAge)
    )
  }
}

actual fun createSqlNormalizedCacheFactory(name: String, withAge: Boolean): SqlNormalizedCacheFactory = SqlNormalizedCacheFactory(name, withAge)
