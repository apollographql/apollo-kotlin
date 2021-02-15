package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record

interface ReadableStore {
  fun read(key: String, cacheHeaders: CacheHeaders): Record?
  fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record>
}
