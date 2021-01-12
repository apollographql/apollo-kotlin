package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.StreamResponseReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver
): Set<Record> {
  val writer = NormalizationIRResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(cacheKeyResolver).normalize(writer.root, null).values.toSet()
}

fun <D : Fragment.Data> Fragment<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String
): Set<Record> {
  val writer = NormalizationIRResponseWriter(variables(), customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(cacheKeyResolver).normalize(writer.root, rootKey).values.toSet()
}

fun <D : Operation.Data> Operation<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
): D? {
  return try {
    val cacheKeyBuilder = RealCacheKeyBuilder()
    val jsonReader = CacheJsonReader(
        rootKey = CacheKeyResolver.rootKey().key,
        readableCache = readableStore,
        cacheHeaders = cacheHeaders,
    )
    val reader = StreamResponseReader(
        jsonReader = jsonReader,
        variables = variables(),
        customScalarAdapters = customScalarAdapters,
    ) { field ->
      var cacheKey = CacheKey.NO_KEY
      if (field.type == ResponseField.Type.OBJECT) {
        // this could be a CacheReference,
        cacheKey = cacheKeyResolver.fromFieldArguments(field, variables())
      }
      if (cacheKey != CacheKey.NO_KEY) {
        cacheKey.key
      } else {
        cacheKeyBuilder.build(field, variables())
      }
    }

    jsonReader.beginObject()
    adapter().fromResponse(reader)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun Set<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
