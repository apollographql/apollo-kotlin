package com.apollographql.apollo3.cache.normalized.sql

import com.squareup.sqldelight.db.SqlDriver

actual fun createDriver(): SqlDriver {
  // stuck on https://github.com/cashapp/sqldelight/pull/1486
}