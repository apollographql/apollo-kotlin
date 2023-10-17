package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CompositeAdapter
import com.apollographql.apollo3.api.CompositeAdapterContext
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.normalized.api.internal.CacheBatchReader
import com.apollographql.apollo3.cache.normalized.api.internal.Normalizer

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
) = normalize(data, customScalarAdapters, cacheKeyGenerator, EmptyMetadataGenerator, CacheKey.rootKey().key)

@ApolloExperimental
fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    metadataGenerator: MetadataGenerator,
) = normalize(data, customScalarAdapters, cacheKeyGenerator, metadataGenerator, CacheKey.rootKey().key)


@Suppress("UNCHECKED_CAST")
fun <D : Executable.Data> Executable<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    rootKey: String,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter().toJson(writer, customScalarAdapters, data)
  val variables = variables(customScalarAdapters, true)
  return Normalizer(variables, rootKey, cacheKeyGenerator, EmptyMetadataGenerator)
      .normalize(writer.root() as Map<String, Any?>, rootField().selections, rootField().type.rawType())
}

@ApolloExperimental
@Suppress("UNCHECKED_CAST")
fun <D : Executable.Data> Executable<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    metadataGenerator: MetadataGenerator,
    rootKey: String,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter().toJson(writer, customScalarAdapters, data)
  val variables = variables(customScalarAdapters)
  return Normalizer(variables, rootKey, cacheKeyGenerator, metadataGenerator)
      .normalize(writer.root() as Map<String, Any?>, rootField().selections, rootField().type.rawType())
}

fun <D : Executable.Data> Executable<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
): D = readDataFromCache(
    cacheKey = CacheKey.rootKey(),
    customScalarAdapters = customScalarAdapters,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
)

fun <D : Executable.Data> Executable<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
): D {
  val variables = variables(customScalarAdapters, true)
  return readInternal(
      cacheKey = cacheKey,
      cache = cache,
      cacheResolver = cacheResolver,
      cacheHeaders = cacheHeaders,
      variables = variables
  ).toData(adapter(), customScalarAdapters, variables)
}

fun <D : Executable.Data> Executable<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: ApolloResolver,
    cacheHeaders: CacheHeaders,
): D {
  val variables = variables(customScalarAdapters, true)
  return readInternal(
      cacheKey = cacheKey,
      cache = cache,
      cacheResolver = cacheResolver,
      cacheHeaders = cacheHeaders,
      variables = variables
  ).toData(adapter(), customScalarAdapters, variables)
}

@ApolloInternal
fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
): CacheData = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables,
)

@ApolloInternal
fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: ApolloResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
): CacheData = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables
)


private fun <D : Executable.Data> Executable<D>.readInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: Any,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
): CacheData {
  return CacheBatchReader(
      cache = cache,
      cacheHeaders = cacheHeaders,
      cacheResolver = cacheResolver,
      variables = variables,
      rootKey = cacheKey.key,
      rootSelections = rootField().selections,
      rootTypename = rootField().type.rawType().name
  ).collectData()
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys()
  }?.toSet() ?: emptySet()
}

@ApolloInternal
fun <D : Executable.Data> CacheData.toData(
    adapter: CompositeAdapter<D>,
    customScalarAdapters: CustomScalarAdapters,
    variables: Executable.Variables,
): D {
  val reader = MapJsonReader(
      root = toMap(),
  )

  return adapter.fromJson(reader, CompositeAdapterContext.Builder().falseVariables(variables.valueMap.filter { it.value == false }.keys).customScalarAdapters(customScalarAdapters).build())
}
