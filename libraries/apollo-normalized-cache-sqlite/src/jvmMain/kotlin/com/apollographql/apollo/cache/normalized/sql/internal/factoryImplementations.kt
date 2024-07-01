package com.apollographql.apollo.cache.normalized.sql.internal

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

private fun String?.toUrl(baseDir: String?): String {
  return if (this == null) {
    JdbcSqliteDriver.IN_MEMORY
  } else {
    val dir = baseDir?.let { File(it) } ?: File(System.getProperty("user.home")).resolve(".apollo")
    dir.mkdirs()
    "${JdbcSqliteDriver.IN_MEMORY}${dir.resolve(this).absolutePath}"
  }
}

private const val versionPragma = "user_version"

internal fun createDriver(name: String?, baseDir: String?, properties: Properties): SqlDriver {
  return JdbcSqliteDriver(name.toUrl(baseDir), properties)
}

internal actual fun createDriver(name: String?, baseDir: String?, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
  return createDriver(name, baseDir, Properties())
}

internal actual fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlSchema<QueryResult.Value<Unit>>) {
  val oldVersion = driver.executeQuery(
      null,
      "PRAGMA $versionPragma",
      { cursor ->
        val ret = if (cursor.next().value) {
          cursor.getLong(0)?.toInt()
        } else {
          null
        }
        QueryResult.Value(ret ?: 0)
      },
      0
  ).value.toLong()

  val newVersion = schema.version

  if (oldVersion == 0L) {
    schema.create(driver)
    driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
  } else if (oldVersion < newVersion) {
    schema.migrate(driver, oldVersion, newVersion)
    driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
  }
}
