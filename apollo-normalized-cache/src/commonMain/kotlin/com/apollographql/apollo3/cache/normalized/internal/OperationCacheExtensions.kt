package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    responseAdapterCache: ResponseAdapterCache,
    cacheKeyResolver: CacheKeyResolver,
) = normalizeInternal(
    data,
    responseAdapterCache,
    cacheKeyResolver,
    CacheKeyResolver.rootKey().key,
    adapter(),
    variables(responseAdapterCache),
    responseFields())

fun <D : Fragment.Data> Fragment<D>.normalize(
    data: D,
    responseAdapterCache: ResponseAdapterCache,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String,
) = normalizeInternal(
    data,
    responseAdapterCache,
    cacheKeyResolver,
    rootKey,
    adapter(),
    variables(responseAdapterCache),
    responseFields())

private fun <D> normalizeInternal(
    data: D,
    responseAdapterCache: ResponseAdapterCache,
    cacheKeyResolver: CacheKeyResolver,
    rootKey: String,
    adapter: ResponseAdapter<D>,
    variables: Executable.Variables,
    fieldSets: List<ResponseField.FieldSet>,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter.toResponse(writer, responseAdapterCache, data)
  return Normalizer(variables) { responseField, fields ->
    cacheKeyResolver.fromFieldRecordSet(responseField, variables, fields).let { if (it == CacheKey.NO_KEY) null else it.key }
  }.normalize(writer.root() as Map<String, Any?>, null, rootKey, fieldSets)
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
    responseAdapterCache: ResponseAdapterCache,
    cache: ReadOnlyNormalizedCache,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.BATCH,
) = readInternal(
    cache = cache,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(responseAdapterCache),
    adapter = adapter(),
    responseAdapterCache = responseAdapterCache,
    mode = mode,
    cacheKey = CacheKeyResolver.rootKey(),
    fieldSets = responseFields()
)

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    cacheKey: CacheKey,
    responseAdapterCache: ResponseAdapterCache,
    cache: ReadOnlyNormalizedCache,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    mode: ReadMode = ReadMode.SEQUENTIAL,
) = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheKeyResolver = cacheKeyResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(responseAdapterCache),
    adapter = adapter(),
    responseAdapterCache = responseAdapterCache,
    mode = mode,
    fieldSets = responseFields()
)


private fun <D> readInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheKeyResolver: CacheKeyResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
    adapter: ResponseAdapter<D>,
    responseAdapterCache: ResponseAdapterCache,
    mode: ReadMode = ReadMode.SEQUENTIAL,
    fieldSets: List<ResponseField.FieldSet>,
): D? {
  val map = if (mode == ReadMode.BATCH) {
    CacheBatchReader(
        cache = cache,
        cacheHeaders = cacheHeaders,
        cacheKeyResolver = cacheKeyResolver,
        variables = variables,
        rootKey = cacheKey.key,
        rootFieldSets = fieldSets
    ).toMap()
  } else {
    CacheSequentialReader(
        cache = cache,
        cacheHeaders = cacheHeaders,
        cacheKeyResolver = cacheKeyResolver,
        variables = variables,
        rootKey = cacheKey.key,
        rootFieldSets = fieldSets
    ).toMap()
  }

  val reader = MapJsonReader(
      root = map,
  )
  return adapter.fromResponse(reader, responseAdapterCache)
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys() + it.key
  }?.toSet() ?: emptySet()
}
