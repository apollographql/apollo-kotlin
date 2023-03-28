package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo3.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.getSchema
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.Properties

actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
    private val withDates: Boolean,
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
      withDates: Boolean = false,
  ) : this(JdbcSqliteDriver(url, properties), withDates)

  /**
   * @param name the name of the database or null for an in-memory database
   * @param withDates whether to account for dates in the database.
   * @param baseDir the baseDirectory where to store the database.
   * If [baseDir] does not exist, it will be created
   * If [baseDir] is a relative path, it will be interpreted relative to the current working directory
   */
  constructor(name: String?, withDates: Boolean, baseDir: String?) : this(createDriver(name, baseDir, getSchema(withDates)), withDates)
  actual constructor(name: String?, withDates: Boolean) : this(name, withDates, null)

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(createRecordDatabase(driver, withDates))
  }
}

