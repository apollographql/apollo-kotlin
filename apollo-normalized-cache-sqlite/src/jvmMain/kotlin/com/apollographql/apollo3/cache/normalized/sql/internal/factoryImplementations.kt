package com.apollographql.apollo3.cache.normalized.sql.internal

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
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

internal actual fun createDriver(name: String?, baseDir: String?, schema: SqlDriver.Schema): SqlDriver {
  return createDriver(name, baseDir, Properties())
}

internal actual fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlDriver.Schema) {
  val oldVersion = driver.executeQuery(null, "PRAGMA $versionPragma", 0).use { cursor ->
    if (cursor.next()) {
      cursor.getLong(0)?.toInt()
    } else {
      null
    }
  } ?: 0

  val newVersion = schema.version

  if (oldVersion == 0) {
    schema.create(driver)
    driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
  } else if (oldVersion < newVersion) {
    schema.migrate(driver, oldVersion, newVersion)
    driver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
  }
}
