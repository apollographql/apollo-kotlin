package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo.cache.normalized.sql.internal.getSchema
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import java.util.Properties

actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
) : NormalizedCacheFactory() {
  /**
   * @param url Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
   * (creating an in-memory database) or a path to a file.
   * @param properties
   */
  constructor(
      url: String,
      properties: Properties = Properties(),
  ) : this(JdbcSqliteDriver(url, properties))

  /**
   * @param name the name of the database or null for an in-memory database
   * @param baseDir the baseDirectory where to store the database.
   * If [baseDir] does not exist, it will be created
   * If [baseDir] is a relative path, it will be interpreted relative to the current working directory
   */
  constructor(name: String?,  baseDir: String?) : this(createDriver(name, baseDir, getSchema()), )
  actual constructor(name: String?, ) : this(name, null)

  actual override fun create(): NormalizedCache {
    return SqlNormalizedCache(createRecordDatabase(driver))
  }
}

