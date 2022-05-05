package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.internal.json.JsonDatabase
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

actual fun createDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
      JsonDatabase.Schema.create(it)
    }
