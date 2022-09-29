package com.apollographql.apollo3.cache.normalized.sql.internal

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.apollographql.apollo3.cache.normalized.sql.ApolloInitializer
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver


internal actual fun createDriver(name: String?, baseDir: String?, schema: SqlDriver.Schema): SqlDriver {
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

internal actual fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlDriver.Schema) {
  // no-op
}