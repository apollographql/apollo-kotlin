package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver

expect class SqlNormalizedCacheFactory internal constructor(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>
