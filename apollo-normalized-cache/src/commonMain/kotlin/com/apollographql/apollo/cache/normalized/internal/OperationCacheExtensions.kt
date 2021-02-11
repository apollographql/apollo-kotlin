package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.MapResponseReader
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.SimpleResponseWriter
import com.apollographql.apollo.api.internal.ValueResolver
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver
): Set<Record> {
  val writer = SimpleResponseWriter(customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(variables()) { responseField, fields ->
    cacheKeyResolver.fromFieldRecordSet(responseField, fields).let { if (it == CacheKey.NO_KEY) null else it.key}
  }.normalize(writer.toMap(), null, CacheKeyResolver.rootKey().key, responseFields()).toSet()
}

fun <D : Fragment.Data> Fragment<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String
): Set<Record> {
  val writer = SimpleResponseWriter(customScalarAdapters)
  adapter().toResponse(writer, data)
  return Normalizer(variables()) { responseField, fields ->
    cacheKeyResolver.fromFieldRecordSet(responseField, fields).key
  }.normalize(writer.toMap(), rootKey, rootKey, responseFields()).toSet()
}

enum class ReadMode {
  /**
   * Depth-first traversal. Resolve CacheReferences as they are encountered
   */
  SEQUENTIAL,

  /**
   * Breadth-first traversal. Batches CacheReferences at a certain depth and resolve them all at once. This is useful for SQLite
   */
  BATCH,
}

fun <D : Operation.Data> Operation<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.SEQUENTIAL,
) = readInternal(
    customScalarAdapters = customScalarAdapters,
    readableStore = readableStore,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(),
    adapter = adapter(),
    mode = mode,
    cacheKey = CacheKeyResolver.rootKey(),
    fieldSets = responseFields()
)

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.SEQUENTIAL
) = readInternal(
    cacheKey = cacheKey,
    customScalarAdapters = customScalarAdapters,
    readableStore = readableStore,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(),
    adapter = adapter(),
    mode = mode,
    fieldSets = responseFields()
)


private fun <D> readInternal(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    variables: Operation.Variables,
    adapter: ResponseAdapter<D>,
    mode: ReadMode = ReadMode.SEQUENTIAL,
    fieldSets: List<ResponseField.FieldSet>,
) = when (mode) {
  ReadMode.SEQUENTIAL -> readSequential(
      cacheKey = cacheKey,
      customScalarAdapters = customScalarAdapters,
      readableStore = readableStore,
      cacheKeyResolver = cacheKeyResolver,
      cacheHeaders = cacheHeaders,
      variables = variables,
      adapter = adapter,
  )
  ReadMode.BATCH -> readBatched(
      cacheKey = cacheKey,
      customScalarAdapters = customScalarAdapters,
      readableStore = readableStore,
      cacheKeyResolver = cacheKeyResolver,
      cacheHeaders = cacheHeaders,
      variables = variables,
      adapter = adapter,
      fieldSets = fieldSets,
  )
}

private fun <D> readSequential(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    variables: Operation.Variables,
    adapter: ResponseAdapter<D>
): D? {
  return try {
    val cacheKeyBuilder = RealCacheKeyBuilder()
    val rootRecord = readableStore.read(cacheKey.key, cacheHeaders) ?: return null
    val fieldValueResolver = CacheValueResolver(
        readableStore,
        variables,
        cacheKeyResolver,
        cacheHeaders,
        cacheKeyBuilder)

    val reader = MapResponseReader(
        root = rootRecord,
        variable = variables,
        valueResolver = fieldValueResolver,
        customScalarAdapters = customScalarAdapters,
    )

    adapter.fromResponse(reader)
  } catch (e: Exception) {
    null
  }
}

private fun <D> readBatched(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    readableStore: ReadableStore,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    variables: Operation.Variables,
    adapter: ResponseAdapter<D>,
    fieldSets: List<ResponseField.FieldSet>
): D? {
  return try {
    val map = CacheBatchReader(
        readableStore = readableStore,
        cacheHeaders = cacheHeaders,
        cacheKeyResolver = cacheKeyResolver,
        variables = variables,
        rootKey = cacheKey.key,
        rootFieldSets = fieldSets
    ).toMap()

    val cacheKeyBuilder = RealCacheKeyBuilder()
    val reader = MapResponseReader(
        root = map,
        valueResolver = object : ValueResolver<Map<String, Any?>> {
          override fun <T> valueFor(map: Map<String, Any?>, field: ResponseField): T? {
            return map[cacheKeyBuilder.build(field, variables)] as T?
          }
        },
        variable = variables,
        customScalarAdapters = customScalarAdapters,
    )
    adapter.fromResponse(reader)
  } catch (e: Exception) {
    null
  }
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.keys() + it.key
  }?.toSet() ?: emptySet()
}
