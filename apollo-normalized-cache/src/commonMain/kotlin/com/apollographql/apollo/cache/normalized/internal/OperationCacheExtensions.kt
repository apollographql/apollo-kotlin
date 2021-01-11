package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.internal.MapResponseParser
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.api.internal.response.RealResponseWriter
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.cache.normalized.CacheKeyResolver

fun <D: Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver
): Set<Record> {
  val writer = AJResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  return AJNormalizer(cacheKeyResolver).normalize(writer.root(), null).values.toSet()
}

fun Set<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
