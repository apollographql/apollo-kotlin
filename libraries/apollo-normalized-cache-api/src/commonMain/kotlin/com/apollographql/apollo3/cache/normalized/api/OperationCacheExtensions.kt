package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.api.ApolloAdapter
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.booleanVariables
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.normalized.api.internal.CacheBatchReader
import com.apollographql.apollo3.cache.normalized.api.internal.Normalizer


fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    scalarAdapters: ScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
) = normalize(data, scalarAdapters, cacheKeyGenerator, CacheKey.rootKey().key)

@Suppress("UNCHECKED_CAST")
fun <D : Executable.Data> Executable<D>.normalize(
    data: D,
    scalarAdapters: ScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    rootKey: String,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter().toJson(writer, scalarAdapters, data)
  val variables = variables(scalarAdapters, true)
  return Normalizer(variables, rootKey, cacheKeyGenerator)
      .normalize(writer.root() as Map<String, Any?>, rootField().selections, rootField().type.rawType().name)
}

fun <D : Executable.Data> Executable<D>.readDataFromCache(
    scalarAdapters: ScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
) = readInternal(
    cacheKey = CacheKey.rootKey(),
    scalarAdapters = scalarAdapters,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
)

fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
    cacheKey: CacheKey,
    scalarAdapters: ScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
) = readInternal(
    cacheKey = cacheKey,
    scalarAdapters = scalarAdapters,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
)

private fun <D : Executable.Data> Executable<D>.readInternal(
    cacheKey: CacheKey,
    scalarAdapters: ScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
): D {
  val map = CacheBatchReader(
      cache = cache,
      cacheHeaders = cacheHeaders,
      cacheResolver = cacheResolver,
      variables = variables(scalarAdapters, true),
      rootKey = cacheKey.key,
      rootSelections = rootField().selections,
      rootTypename = rootField().type.rawType().name
  ).toMap()

  val reader = MapJsonReader(
      root = map,
  )
  return adapter().fromJson(
      reader,
      ApolloAdapter.DataDeserializeContext(
          scalarAdapters = scalarAdapters,
          booleanFalseVariables = booleanVariables(scalarAdapters),
          mergedDeferredFragmentIds = null,
      )
  )
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys()
  }?.toSet() ?: emptySet()
}
