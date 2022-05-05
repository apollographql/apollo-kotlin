package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.internal.json.JsonDatabase
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual fun createDriver(): SqlDriver =
    NativeSqliteDriver(JsonDatabase.Schema, "apollo.db")
