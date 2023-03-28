package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver

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
 * @param withDates: whether to store dates (receive dates or expiration dates)
 * Once a database is created, this parameter cannot change
 * Default: false
 */
expect class SqlNormalizedCacheFactory(name: String? = "apollo.db", withDates: Boolean = false) : NormalizedCacheFactory {
  constructor(driver: SqlDriver, withDates: Boolean = false)
}

