package com.apollographql.apollo3.cache.normalized.sql

actual fun createCacheFactory(
    baseDir: String,
    withDates: Boolean,
): SqlNormalizedCacheFactory {
  TODO("Allow to have both the Jdbc and the Android normalized caches at the same time")
}