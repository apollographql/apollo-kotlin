package com.apollographql.apollo3.cache.normalized.sql.internal
import com.squareup.sqldelight.db.SqlDriver
import kotlin.jvm.JvmName


internal expect fun createDriver(name: String?, baseDir: String?, schema: SqlDriver.Schema): SqlDriver
internal expect fun createOrMigrateSchema(driver: SqlDriver, schema: SqlDriver.Schema)