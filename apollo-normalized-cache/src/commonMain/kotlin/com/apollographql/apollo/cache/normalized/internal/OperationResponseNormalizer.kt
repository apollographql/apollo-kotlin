package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.internal.response.RealResponseWriter

fun <D: Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    normalizer: ResponseNormalizer<Map<String, Any>?>
): Set<Record> {
  val writer = RealResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  writer.resolveFields(normalizer)
  return normalizer.records().toSet()
}

fun Set<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}