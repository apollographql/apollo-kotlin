package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.MapResponseParser
import com.apollographql.apollo.api.internal.MapResponseReader
import com.apollographql.apollo.api.internal.StreamResponseReader
import com.apollographql.apollo.api.internal.ValueResolver
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
    val rootRecord = readableStore.read(CacheKeyResolver.rootKey().key, cacheHeaders) ?: return null
    val fieldValueResolver = CacheValueResolver(
        readableStore,
        variables(),
        cacheKeyResolver,
        cacheHeaders,
        cacheKeyBuilder)

    val reader = MapResponseReader(
        root = rootRecord,
        variable = variables(),
        valueResolver = fieldValueResolver,
        customScalarAdapters = customScalarAdapters,
    )

    adapter().fromResponse(reader)
  } catch (e: Exception) {
    null
  }
}

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    cacheKey: CacheKey
): D? {
  return try {
    val cacheKeyBuilder = RealCacheKeyBuilder()
    val rootRecord = readableStore.read(cacheKey.key, cacheHeaders) ?: return null
    val fieldValueResolver = CacheValueResolver(
        readableStore,
        variables(),
        cacheKeyResolver,
        cacheHeaders,
        cacheKeyBuilder)

    val reader = MapResponseReader(
        root = rootRecord,
        variable = variables(),
        valueResolver = fieldValueResolver,
        customScalarAdapters = customScalarAdapters,
    )

    adapter().fromResponse(reader)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun <D : Operation.Data> Operation<D>.batchDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
): D? {
  return try {
    val map = CacheMapBuilder(
        readableStore = readableStore,
        cacheHeaders = cacheHeaders,
        cacheKeyResolver = cacheKeyResolver,
        variables = variables(),
        rootKey = CacheKeyResolver.rootKey().key,
        rootResponseFields = responseFields()
    ).toMap()

    val cacheKeyBuilder = RealCacheKeyBuilder()
    val reader = MapResponseReader(
        root = map,
        valueResolver = object: ValueResolver<Map<String, Any?>> {
          override fun <T> valueFor(map: Map<String, Any?>, field: ResponseField): T? {
            return map[cacheKeyBuilder.build(field, variables())] as T?
          }
        },
        variable = variables(),
        customScalarAdapters = customScalarAdapters,
    )
    adapter().fromResponse(reader)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
