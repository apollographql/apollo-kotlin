package com.apollographql.apollo3.cache.normalized.api.internal

internal actual fun <Key : Any, Value : Any> LruCache(
    maxSize: Int,
    expireAfterMillis: Long,
    weigher: Weigher<Key, Value>,
): LruCache<Key, Value> = CommonLruCache(maxSize, expireAfterMillis, weigher)
