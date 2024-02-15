package com.apollographql.apollo3.cache.normalized.api.internal

internal expect fun <K, V> ConcurrentMap(): MutableMap<K, V>
