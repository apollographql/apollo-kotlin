package com.apollographql.apollo3.cache.normalized.lru

import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.RecordFieldJsonAdapter

@Deprecated("Will be removed soon")
class LruNormalizedCacheFactory(
    /**
     * [EvictionPolicy] to manage the primary cache.
     */
    private val evictionPolicy: EvictionPolicy
) : NormalizedCacheFactory() {

  override fun create(): LruNormalizedCache = LruNormalizedCache(evictionPolicy)
}
