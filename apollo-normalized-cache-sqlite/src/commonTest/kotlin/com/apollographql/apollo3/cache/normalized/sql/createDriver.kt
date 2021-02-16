package com.apollographql.apollo3.cache.normalized.sql

import com.squareup.sqldelight.db.SqlDriver

expect fun createDriver(): SqlDriver
