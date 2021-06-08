package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheResolver: CacheResolver,
) = normalizeInternal(
    data,
    customScalarAdapters,
    cacheResolver,
    CacheResolver.rootKey().key,
    adapter(),
    variables(customScalarAdapters),
    selections())

fun <D : Fragment.Data> Fragment<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheResolver: CacheResolver,
    rootKey: String,
) = normalizeInternal(
    data,
    customScalarAdapters,
    cacheResolver,
    rootKey,
    adapter(),
    variables(customScalarAdapters),
    selections())

private fun <D> normalizeInternal(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheResolver: CacheResolver,
    rootKey: String,
    adapter: Adapter<D>,
    variables: Executable.Variables,
    selections: List<CompiledSelection>,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter.toJson(writer, customScalarAdapters, data)
  return Normalizer(variables) { compiledField, fields ->
    cacheResolver.cacheKeyForObject(compiledField, variables, fields)?.key
  }.normalize(writer.root() as Map<String, Any?>, null, rootKey, selections)
}

fun <D : Operation.Data> Operation<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
) = readInternal(
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(customScalarAdapters),
    adapter = adapter(),
    customScalarAdapters = customScalarAdapters,
    cacheKey = CacheResolver.rootKey(),
    selections = selections()
)

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
) = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables(customScalarAdapters),
    adapter = adapter(),
    customScalarAdapters = customScalarAdapters,
    selections = selections()
)


private fun <D> readInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
    adapter: Adapter<D>,
    customScalarAdapters: CustomScalarAdapters,
    selections: List<CompiledSelection>,
): D? {
  val map = CacheBatchReader(
      cache = cache,
      cacheHeaders = cacheHeaders,
      cacheResolver = cacheResolver,
      variables = variables,
      rootKey = cacheKey.key,
      rootSelections = selections
  ).toMap()

  val reader = MapJsonReader(
      root = map,
  )
  return adapter.fromJson(reader, customScalarAdapters)
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys() + it.key
  }?.toSet() ?: emptySet()
}
