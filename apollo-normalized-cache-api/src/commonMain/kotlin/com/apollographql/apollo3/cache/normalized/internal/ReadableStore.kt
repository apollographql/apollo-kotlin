package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.Record

interface ReadableStore {
  fun read(key: String, cacheHeaders: CacheHeaders): Record?
  fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record>
}
