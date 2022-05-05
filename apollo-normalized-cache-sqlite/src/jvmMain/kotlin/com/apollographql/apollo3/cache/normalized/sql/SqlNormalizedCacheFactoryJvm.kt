package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.Properties

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver,
    withAge: Boolean,
) : NormalizedCacheFactory() {
  /**
   * @param url Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
   * (creating an in-memory database) or a path to a file.
   * @param properties
   */
  @JvmOverloads
  constructor(
      url: String,
      properties: Properties = Properties(),
      withAge: Boolean = false
  ) : this(createDriverAndDatabase(url, properties, withAge), withAge)

  private val driver = driver
  private val withAge = withAge

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(createRecordDatabase(driver, withAge))
  }

  companion object {
    private fun createDriverAndDatabase(url: String, properties: Properties, withAge: Boolean) =
        JdbcSqliteDriver(url, properties).also {
          getSchema(withAge = withAge).create(it)
        }
  }
}

