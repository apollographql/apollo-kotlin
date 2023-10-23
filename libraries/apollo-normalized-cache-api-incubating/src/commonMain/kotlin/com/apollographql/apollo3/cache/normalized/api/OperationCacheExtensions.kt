package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.normalized.api.internal.CacheBatchReader
import com.apollographql.apollo3.cache.normalized.api.internal.Normalizer
import kotlin.jvm.JvmOverloads

fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
) = normalize(data, customScalarAdapters, cacheKeyGenerator, EmptyMetadataGenerator, DefaultFieldNameGenerator, CacheKey.rootKey().key)

@ApolloExperimental
fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    metadataGenerator: MetadataGenerator,
    fieldNameGenerator: FieldNameGenerator,
) = normalize(data, customScalarAdapters, cacheKeyGenerator, metadataGenerator, fieldNameGenerator, CacheKey.rootKey().key)


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
  return Normalizer(variables, rootKey, cacheKeyGenerator, EmptyMetadataGenerator, DefaultFieldNameGenerator)
      .normalize(writer.root() as Map<String, Any?>, rootField().selections, rootField().type.rawType())
}

@ApolloExperimental
@Suppress("UNCHECKED_CAST")
fun <D : Executable.Data> Executable<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
    metadataGenerator: MetadataGenerator,
    fieldNameGenerator: FieldNameGenerator,
    rootKey: String,
): Map<String, Record> {
  val writer = MapJsonWriter()
  adapter().toJson(writer, customScalarAdapters, data)
  val variables = variables(customScalarAdapters)
  return Normalizer(variables, rootKey, cacheKeyGenerator, metadataGenerator, fieldNameGenerator)
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

@JvmOverloads
fun <D : Executable.Data> Executable<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    fieldNameGenerator: FieldNameGenerator = DefaultFieldNameGenerator,
): D {
  val variables = variables(customScalarAdapters, true)
  return readInternal(
      cacheKey = cacheKey,
      cache = cache,
      cacheResolver = cacheResolver,
      cacheHeaders = cacheHeaders,
      variables = variables,
      fieldNameGenerator = fieldNameGenerator,
  ).toData(adapter(), customScalarAdapters, variables)
}

@JvmOverloads
fun <D : Executable.Data> Executable<D>.readDataFromCache(
    cacheKey: CacheKey,
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: ApolloResolver,
    cacheHeaders: CacheHeaders,
    fieldNameGenerator: FieldNameGenerator = DefaultFieldNameGenerator,
): D {
  val variables = variables(customScalarAdapters, true)
  return readInternal(
      cacheKey = cacheKey,
      cache = cache,
      cacheResolver = cacheResolver,
      cacheHeaders = cacheHeaders,
      variables = variables,
      fieldNameGenerator = fieldNameGenerator,
  ).toData(adapter(), customScalarAdapters, variables)
}

@ApolloInternal
fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
    fieldNameGenerator: FieldNameGenerator,
): CacheData = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables,
    fieldNameGenerator = fieldNameGenerator,
)

@ApolloInternal
fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: ApolloResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
    fieldNameGenerator: FieldNameGenerator,
): CacheData = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables,
    fieldNameGenerator = fieldNameGenerator,
)


private fun <D : Executable.Data> Executable<D>.readInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: Any,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
    fieldNameGenerator: FieldNameGenerator,
): CacheData {
  return CacheBatchReader(
      cache = cache,
      cacheHeaders = cacheHeaders,
      cacheResolver = cacheResolver,
      variables = variables,
      rootKey = cacheKey.key,
      rootSelections = rootField().selections,
      rootTypename = rootField().type.rawType().name,
      fieldNameGenerator = fieldNameGenerator,
  ).collectData()
}

fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys()
  }?.toSet() ?: emptySet()
}

@ApolloInternal
fun <D : Executable.Data> CacheData.toData(
    adapter: Adapter<D>,
    customScalarAdapters: CustomScalarAdapters,
    variables: Executable.Variables,
): D {
  val reader = MapJsonReader(
      root = toMap(),
  )

  return adapter.fromJson(reader, customScalarAdapters.newBuilder().falseVariables(variables.valueMap.filter { it.value == false }.keys).build())
}
