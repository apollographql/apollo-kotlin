package com.apollographql.apollo3.cache.normalized.sql.internal

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.squareup.sqldelight.drivers.native.wrapConnection


actual fun createDriver(name: String?, baseDir: String?, schema: SqlDriver.Schema): SqlDriver {
  val databaseConfiguration = DatabaseConfiguration(
      name = name,
      version = schema.version,
      create = { connection ->
        wrapConnection(connection) { schema.create(it) }
      },
      upgrade = { connection, oldVersion, newVersion ->
        wrapConnection(connection) { schema.migrate(it, oldVersion, newVersion) }
      },
      extendedConfig = DatabaseConfiguration.Extended(
          basePath = baseDir
      )
  )
  return NativeSqliteDriver(databaseConfiguration, 1)
}

actual fun createOrMigrateSchema(driver: SqlDriver, schema: SqlDriver.Schema) {
  // no op
}
