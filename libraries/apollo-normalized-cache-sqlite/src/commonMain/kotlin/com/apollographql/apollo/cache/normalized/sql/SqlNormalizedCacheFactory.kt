package com.apollographql.apollo.cache.normalized.sql

import app.cash.sqldelight.db.SqlDriver
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory

/**
 * Creates a new [NormalizedCacheFactory] that uses a persistent cache based on Sqlite
 *
 * @param name: the name of the database or null for an in-memory database
 * When not in memory, the database will be stored in a platform specific folder
 * - on Android it will use [Context.getDatabaseName](https://developer.android.com/reference/android/content/Context#getDatabasePath(java.lang.String))
 * - on MacOS, it will use "Application Support/databases/name"
 * - on the JVM, it will use "System.getProperty("user.home")/.apollo"
 * Default: "apollo.db"
 *
 */
expect class SqlNormalizedCacheFactory(name: String? = "apollo.db") : NormalizedCacheFactory {
  constructor(driver: SqlDriver)
  override fun create(): NormalizedCache
}

