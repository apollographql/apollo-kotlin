package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter
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
  val variables = variables(customScalarAdapters)
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
) = readInternal(
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
) = readInternal(
    cacheKey = cacheKey,
    customScalarAdapters = customScalarAdapters,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
)

fun <D : Executable.Data> Executable<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: ApolloResolver,
    cacheHeaders: CacheHeaders,
) = readInternal(
    cacheKey = cacheKey,
    customScalarAdapters = customScalarAdapters,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
)


private fun <D : Executable.Data> Executable<D>.readInternal(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: Any,
    cacheHeaders: CacheHeaders,
): D {
  val map = CacheBatchReader(
      cache = cache,
      cacheHeaders = cacheHeaders,
      cacheResolver = cacheResolver,
      variables = variables(customScalarAdapters),
      rootKey = cacheKey.key,
      rootSelections = rootField().selections,
      rootTypename = rootField().type.rawType().name
  ).toMap()

  val reader = MapJsonReader(
      root = map,
  )
  return adapter().fromJson(reader, customScalarAdapters)
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys()
  }?.toSet() ?: emptySet()
}
