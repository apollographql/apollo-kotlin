package com.apollographql.apollo3.cache.normalized.sql

actual fun createSqlNormalizedCacheFactory(name: String, withAge: Boolean): SqlNormalizedCacheFactory = error("On Android, use SqlNormalizedCacheFactory(context, ...)")