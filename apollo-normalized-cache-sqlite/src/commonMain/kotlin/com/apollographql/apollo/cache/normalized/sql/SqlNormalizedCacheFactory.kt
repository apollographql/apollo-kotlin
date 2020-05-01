package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.squareup.sqldelight.db.SqlDriver

expect class SqlNormalizedCacheFactory internal constructor(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>
