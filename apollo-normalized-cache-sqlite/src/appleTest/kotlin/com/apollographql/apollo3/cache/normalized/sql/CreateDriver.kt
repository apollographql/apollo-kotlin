package com.apollographql.apollo3.cache.normalized.sql

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual fun createDriver(): SqlDriver =
    NativeSqliteDriver(ApolloDatabase.Schema, "apollo.db")
