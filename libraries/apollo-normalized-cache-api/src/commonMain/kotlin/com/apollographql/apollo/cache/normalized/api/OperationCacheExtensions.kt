@file:Suppress("DEPRECATION")

package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.api.variables
import com.apollographql.apollo.cache.normalized.api.internal.CacheBatchReader
import com.apollographql.apollo.cache.normalized.api.internal.Normalizer

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <D : Operation.Data> Operation<D>.normalize(
    data: D,
    customScalarAdapters: CustomScalarAdapters,
    cacheKeyGenerator: CacheKeyGenerator,
) = normalize(data, customScalarAdapters, cacheKeyGenerator, CacheKey.rootKey().key)

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
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
  return Normalizer(variables, rootKey, cacheKeyGenerator)
      .normalize(writer.root() as Map<String, Any?>, rootField().selections, rootField().type.rawType().name)
}

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <D : Executable.Data> Executable<D>.readDataFromCache(
    customScalarAdapters: CustomScalarAdapters,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
):D {
  val variables = variables(customScalarAdapters, true)
  return readInternal(
      cacheKey = CacheKey.rootKey(),
      cache = cache,
      cacheResolver = cacheResolver,
      cacheHeaders = cacheHeaders,
      variables = variables,
  ).toData(adapter(), customScalarAdapters, variables)
}

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun <D : Fragment.Data> Fragment<D>.readDataFromCache(
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

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@ApolloInternal
fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables,
): CacheData = readInternal(
    cacheKey = CacheKey.rootKey(),
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables
)

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@ApolloInternal
fun <D : Fragment.Data> Fragment<D>.readDataFromCacheInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
    cacheHeaders: CacheHeaders,
    variables: Executable.Variables
): CacheData = readInternal(
    cacheKey = cacheKey,
    cache = cache,
    cacheResolver = cacheResolver,
    cacheHeaders = cacheHeaders,
    variables = variables,
)

private fun <D : Executable.Data> Executable<D>.readInternal(
    cacheKey: CacheKey,
    cache: ReadOnlyNormalizedCache,
    cacheResolver: CacheResolver,
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

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
fun Collection<Record>?.dependentKeys(): Set<String> {
  return this?.flatMap {
    it.fieldKeys()
  }?.toSet() ?: emptySet()
}

@Deprecated("Use the new Normalized Cache at https://github.com/apollographql/apollo-kotlin-normalized-cache")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
@ApolloInternal
fun <D: Executable.Data> CacheData.toData(
    adapter: Adapter<D>,
    customScalarAdapters: CustomScalarAdapters,
    variables: Executable.Variables,
): D {
  val reader = MapJsonReader(
      root = toMap(),
  )

  return adapter.fromJson(reader, customScalarAdapters.newBuilder().falseVariables(variables.valueMap.filter { it.value == false }.keys).build())
}
