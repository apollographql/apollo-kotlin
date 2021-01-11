package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.CacheKeyResolver

fun <D: Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver
): Set<Record> {
  val writer = NormalizationIRResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(cacheKeyResolver).normalize(writer.root, null).values.toSet()
}

fun <D: Fragment.Data> Fragment<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String
): Set<Record> {
  val writer = NormalizationIRResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(cacheKeyResolver).normalize(writer.root, rootKey).values.toSet()
}

fun Set<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
