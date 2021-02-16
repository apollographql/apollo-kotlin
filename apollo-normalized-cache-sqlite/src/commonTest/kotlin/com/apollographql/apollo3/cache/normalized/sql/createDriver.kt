package com.apollographql.apollo.cache.normalized.sql

import com.squareup.sqldelight.db.SqlDriver

expect fun createDriver(): SqlDriver
