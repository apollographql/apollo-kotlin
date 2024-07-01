package com.apollographql.apollo.cache.normalized.sql.internal

import app.cash.sqldelight.db.QueryResult
import co.touchlab.sqliter.DatabaseConfiguration
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection


internal actual fun createDriver(name: String?, baseDir: String?, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
  val databaseConfiguration = DatabaseConfiguration(
      name = name ?: "memoryDb",
      inMemory = name == null,
      version = schema.version.toInt(),
      create = { connection ->
        wrapConnection(connection) { schema.create(it) }
      },
      upgrade = { connection, oldVersion, newVersion ->
        wrapConnection(connection) { schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
      },
      extendedConfig = DatabaseConfiguration.Extended(
          basePath = baseDir
      )
  )
  return NativeSqliteDriver(databaseConfiguration, 1)
}

internal actual fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlSchema<QueryResult.Value<Unit>>) {
  // no op
}
