package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.Properties

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>() {

  @JvmOverloads
  constructor(
      /**
       * Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
       * (creating an in-memory database) or a path to a file.
       */
      url: String,
      properties: Properties = Properties()
  ) : this(JdbcSqliteDriver(url, properties))

  init {
    ApolloDatabase.Schema.create(driver)
  }

  private val apolloDatabase = ApolloDatabase(driver)

  override fun create(recordFieldAdapter: RecordFieldJsonAdapter) =
      SqlNormalizedCache(recordFieldAdapter, apolloDatabase.cacheQueries)
}
