package com.apollographql.apollo.cache.normalized.sql.internal

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.db.QueryResult
import com.apollographql.apollo.cache.normalized.sql.ApolloInitializer
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema


internal actual fun createDriver(name: String?, baseDir: String?, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
  check(baseDir == null) {
    "Apollo: Android SqlNormalizedCacheFactory doesn't support 'baseDir'"
  }
  return AndroidSqliteDriver(
      schema,
      ApolloInitializer.context,
      name,
      FrameworkSQLiteOpenHelperFactory(),
  )
}

internal actual fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlSchema<QueryResult.Value<Unit>>) {
  // no-op
}