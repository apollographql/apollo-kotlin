package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver

expect class SqlNormalizedCacheFactory internal constructor(
    driver: SqlDriver,
    exceptionHandler: (Throwable) -> Unit,
) : NormalizedCacheFactory

internal val DEFAULT_EXCEPTION_HANDLER: (Throwable) -> Unit = {
  println("Apollo: an unexpected exception occurred in SqlNormalizedCache")
  it.printStackTrace()
}
