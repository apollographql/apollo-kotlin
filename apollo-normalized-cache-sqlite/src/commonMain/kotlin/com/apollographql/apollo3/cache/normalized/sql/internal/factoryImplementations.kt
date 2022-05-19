package com.apollographql.apollo3.cache.normalized.sql.internal
import com.squareup.sqldelight.db.SqlDriver


internal expect fun createDriver(name: String?, baseDir: String?, schema: SqlDriver.Schema): SqlDriver
/**
 * Some implementations like Native and Android take the schema when creating the driver and the driver
 * will take care of migrations
 *
 * Others like JVM don't do this automatically. This is when [maybeCreateOrMigrateSchema] is needed
 */
internal expect fun maybeCreateOrMigrateSchema(driver: SqlDriver, schema: SqlDriver.Schema)