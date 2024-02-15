package com.apollographql.apollo3.cache.normalized.api.internal

internal actual fun <K, V> ConcurrentMap(): MutableMap<K, V> = CommonConcurrentMap()
