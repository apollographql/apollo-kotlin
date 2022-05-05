package com.apollographql.apollo3.cache.normalized.sql

import java.util.Properties

actual fun createSqlNormalizedCacheFactory(name: String, withAge: Boolean) = SqlNormalizedCacheFactory(name, Properties(), withAge)